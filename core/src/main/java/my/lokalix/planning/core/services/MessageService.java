package my.lokalix.planning.core.services;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.mappers.MessageMapper;
import my.lokalix.planning.core.models.entities.MessageEntity;
import my.lokalix.planning.core.repositories.MessageRepository;
import my.lokalix.planning.core.security.LoggedUserDetailsService;
import my.lokalix.planning.core.services.validator.MessageValidators;
import my.lokalix.planning.core.utils.TimeUtils;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import my.zkonsulting.planning.generated.model.SWMessage;
import my.zkonsulting.planning.generated.model.SWMessageCreate;
import my.zkonsulting.planning.generated.model.SWMessageUpdate;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MessageService {

  private final MessageRepository messageRepository;
  private final MessageMapper messageMapper;
  private final MessageValidators messageValidators;
  private final LoggedUserDetailsService loggedUserDetailsService;

  public List<SWMessage> retrieve(List<MessageEntity> messages) {
    return messageMapper.toListSwMessages(messages);
  }

  /**
   * Creates a message and adds it to the parent entity.
   *
   * @param create the message creation DTO
   * @param addToParent consumer that adds the message to the parent entity
   * @param saveAndGetMessages supplier that saves the parent and returns the updated message list
   * @param sendEmailNotification optional runnable to send a notification email (can be null)
   */
  @Transactional
  public List<SWMessage> create(
      SWMessageCreate create,
      Consumer<MessageEntity> addToParent,
      Supplier<List<MessageEntity>> saveAndGetMessages,
      Runnable sendEmailNotification) {
    MessageEntity messageEntity = messageMapper.toMessageEntity(create);
    messageEntity.setUser(loggedUserDetailsService.getLoggedUserReference());
    if (sendEmailNotification != null) {
      sendEmailNotification.run();
    }
    addToParent.accept(messageEntity);
    return messageMapper.toListSwMessages(saveAndGetMessages.get());
  }

  @Transactional
  public SWMessage update(UUID messageUid, SWMessageUpdate body, List<MessageEntity> messages) {
    MessageEntity messageEntity = findMessage(messageUid, messages);
    messageValidators.checkIsMessageOwner(messageEntity);
    messageEntity.setContent(body.getContent());
    messageEntity.setUpdated(true);
    messageEntity.setLastModificationDate(TimeUtils.nowOffsetDateTimeUTC());
    return messageMapper.toSwMessage(messageRepository.save(messageEntity));
  }

  @Transactional
  public SWMessage delete(UUID messageUid, List<MessageEntity> messages) {
    MessageEntity messageEntity = findMessage(messageUid, messages);
    messageValidators.checkIsMessageOwner(messageEntity);
    messageEntity.setDeleted(true);
    messageEntity.setLastModificationDate(TimeUtils.nowOffsetDateTimeUTC());
    return messageMapper.toSwMessage(messageRepository.save(messageEntity));
  }

  @Transactional
  public SWMessage undo(UUID messageUid, List<MessageEntity> messages) {
    MessageEntity messageEntity = findMessage(messageUid, messages);
    if (!messageEntity.isDeleted()) {
      throw new GenericWithMessageException(
          "Cannot undo a non-deleted message", SWCustomErrorCode.GENERIC_ERROR);
    }
    messageValidators.checkIsMessageOwner(messageEntity);
    messageEntity.setDeleted(false);
    messageEntity.setLastModificationDate(TimeUtils.nowOffsetDateTimeUTC());
    return messageMapper.toSwMessage(messageRepository.save(messageEntity));
  }

  private MessageEntity findMessage(UUID messageUid, List<MessageEntity> messages) {
    if (CollectionUtils.isEmpty(messages)) {
      throw new EntityNotFoundException("Message was not found.");
    }
    return messages.stream()
        .filter(msg -> msg.getMessageId().equals(messageUid))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("Message was not found."));
  }
}
