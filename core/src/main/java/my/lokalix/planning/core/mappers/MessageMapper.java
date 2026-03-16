package my.lokalix.planning.core.mappers;

import jakarta.annotation.Resource;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.models.entities.MessageEntity;
import my.lokalix.planning.core.security.LoggedUserDetailsService;
import my.lokalix.planning.core.utils.TimeUtils;
import my.zkonsulting.planning.generated.model.SWMessage;
import my.zkonsulting.planning.generated.model.SWMessageCreate;
import org.apache.commons.collections4.CollectionUtils;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class MessageMapper {
  @Resource AppConfigurationProperties appConfigurationProperties;
  @Resource private LoggedUserDetailsService loggedUserDetailsService;

  public abstract List<SWMessage> toListSwMessages(List<MessageEntity> entity);

  @Mapping(source = "messageId", target = "uid")
  @Mapping(source = "creationDate", target = "creationDate", qualifiedByName = "formatDate")
  @Mapping(
      source = "lastModificationDate",
      target = "lastModificationDate",
      qualifiedByName = "formatDate")
  @Mapping(source = "user.userId", target = "userUid")
  @Mapping(source = "user.login", target = "userLogin")
  public abstract SWMessage toSwMessage(MessageEntity entity);

  public abstract MessageEntity toMessageEntity(SWMessageCreate create);

  @Named("formatDate")
  String formatDate(OffsetDateTime date) {
    if (date != null) {
      return TimeUtils.formatAsStringInZone(date, appConfigurationProperties.getAppTimezone());
    } else {
      return null;
    }
  }

  public MessageEntity cloneMessageEntity(MessageEntity original) {
    MessageEntity clone = new MessageEntity();
    clone.setContent(original.getContent());
    clone.setUser(original.getUser());
    clone.setDeleted(original.isDeleted());
    clone.setUpdated(original.isUpdated());
    clone.setLastModificationDate(original.getLastModificationDate());
    clone.setCorrelationId(original.getCorrelationId());

    return clone;
  }

  public MessageEntity toMessageEntityWithCorrelation(SWMessageCreate create) {
    MessageEntity entity = toMessageEntity(create);
    entity.setCorrelationId(UUID.randomUUID()); // Nouveau correlationId unique
    return entity;
  }

  @Named("countReceivedMessages")
  int countReceivedMessages(List<MessageEntity> messages) {
    if (CollectionUtils.isNotEmpty(messages)) {
      UUID loggedUserId = loggedUserDetailsService.getLoggedUserId();
      return (int)
          messages.stream().filter(m -> !m.getUser().getUserId().equals(loggedUserId)).count();
    }
    return 0;
  }
}
