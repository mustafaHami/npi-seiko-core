package my.lokalix.planning.core.services;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.mappers.FileMapper;
import my.lokalix.planning.core.mappers.ToolingCostLineMapper;
import my.lokalix.planning.core.models.entities.CostRequestEntity;
import my.lokalix.planning.core.models.entities.CostRequestLineEntity;
import my.lokalix.planning.core.models.entities.ToolingCostLineEntity;
import my.lokalix.planning.core.models.entities.UserEntity;
import my.lokalix.planning.core.models.enums.OutsourcingStatus;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.repositories.ToolingCostLineRepository;
import my.lokalix.planning.core.security.LoggedUserDetailsService;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import my.lokalix.planning.core.services.helper.FileHelper;
import my.lokalix.planning.core.services.helper.UserHelper;
import my.lokalix.planning.core.utils.GlobalConstants;
import my.zkonsulting.planning.generated.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class ToolingCostLineService {

  private final ToolingCostLineRepository toolingCostLineRepository;
  private final ToolingCostLineMapper toolingCostLineMapper;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final FileHelper fileHelper;
  private final FileMapper filesMapper;
  private final AppConfigurationProperties appConfigurationProperties;
  private final MessageService messageService;
  private final LoggedUserDetailsService loggedUserDetailsService;
  private final UserHelper userHelper;
  private final EmailService emailService;

  @Transactional
  public SWToolingCostLinesPaginated searchToolingCostLines(
      int offset, int limit, SWBasicSearch body) {
    Sort sort = Sort.by(Sort.Direction.DESC, "creationDate");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);
    Page<ToolingCostLineEntity> page;

    if (StringUtils.isNotBlank(body.getSearchText())) {
      page =
          toolingCostLineRepository.findByOutsourcingStatusAndSearch(
              pageable, OutsourcingStatus.TO_BE_ESTIMATED, body.getSearchText());
    } else {
      page =
          toolingCostLineRepository.findByOutsourcingStatus(
              pageable, OutsourcingStatus.TO_BE_ESTIMATED);
    }

    return populateToolingCostLinesPaginatedResults(page);
  }

  private SWToolingCostLinesPaginated populateToolingCostLinesPaginatedResults(
      Page<ToolingCostLineEntity> page) {
    SWToolingCostLinesPaginated result = new SWToolingCostLinesPaginated();
    result.setResults(toolingCostLineMapper.toListSWToolingCostLine(page.getContent()));
    result.setPage(page.getNumber());
    result.setPerPage(page.getSize());
    result.setTotal((int) page.getTotalElements());
    result.setHasPrev(page.hasPrevious());
    result.setHasNext(page.hasNext());
    return result;
  }

  @Transactional
  public SWToolingCostLine estimateToolingCostLine(UUID uid, SWToolingCostLineEstimate body) {
    ToolingCostLineEntity toolingCostLine =
        entityRetrievalHelper.getMustExistToolingCostLineById(uid);

    if (toolingCostLine.getOutsourcingStatus() != OutsourcingStatus.TO_BE_ESTIMATED) {
      throw new GenericWithMessageException(
          "Tooling cost line is not in TO_BE_ESTIMATED status", SWCustomErrorCode.GENERIC_ERROR);
    }

    toolingCostLine.setUnitCostInCurrency(body.getUnitCostInCurrency());
    toolingCostLine.setOutsourcingStatus(OutsourcingStatus.ESTIMATED);
    toolingCostLine.buildCalculatedFields(appConfigurationProperties.getTargetCurrencyCode());

    List<UserEntity> users = userHelper.getAllActiveUsersByRole(UserRole.ENGINEERING);
    if (CollectionUtils.isNotEmpty(users)) {
      List<String> emails = users.stream().map(UserEntity::getLogin).toList();
      String description = toolingCostLine.getName();
      String partNumber = toolingCostLine.getCostRequestLine().getCustomerPartNumber();
      String partNumberRevision =
          toolingCostLine.getCostRequestLine().getCustomerPartNumberRevision();
      String ref =
          toolingCostLine.getCostRequestLine().getCostRequest().getCostRequestReferenceNumber();
      String rev =
          String.valueOf(
              toolingCostLine.getCostRequestLine().getCostRequest().getCostRequestRevision());
      emailService.sendOutsourcedToolingLineEstimatedEmail(
          emails, description, partNumber, partNumberRevision, ref, rev);
    }

    return toolingCostLineMapper.toSwToolingCostLine(
        toolingCostLineRepository.save(toolingCostLine));
  }

  @Transactional
  public SWToolingCostLine rejectToolingCostLine(UUID uid, SWToolingCostLineReject body) {
    ToolingCostLineEntity toolingCostLine =
        entityRetrievalHelper.getMustExistToolingCostLineById(uid);

    if (toolingCostLine.getOutsourcingStatus() != OutsourcingStatus.TO_BE_ESTIMATED) {
      throw new GenericWithMessageException(
          "Tooling cost line is not in TO_BE_ESTIMATED status", SWCustomErrorCode.GENERIC_ERROR);
    }

    toolingCostLine.setOutsourcingStatus(OutsourcingStatus.REJECTED);
    toolingCostLine.setRejectReason(body.getRejectReason());
    toolingCostLine.setOutsourced(false);

    List<UserEntity> users = userHelper.getAllActiveUsersByRole(UserRole.ENGINEERING);
    if (CollectionUtils.isNotEmpty(users)) {
      List<String> emails = users.stream().map(UserEntity::getLogin).toList();
      String description = toolingCostLine.getName();
      String partNumber = toolingCostLine.getCostRequestLine().getCustomerPartNumber();
      String partNumberRevision =
          toolingCostLine.getCostRequestLine().getCustomerPartNumberRevision();
      String ref =
          toolingCostLine.getCostRequestLine().getCostRequest().getCostRequestReferenceNumber();
      String rev =
          String.valueOf(
              toolingCostLine.getCostRequestLine().getCostRequest().getCostRequestRevision());
      emailService.sendOutsourcedToolingLineRejectedEmail(
          emails, body.getRejectReason(), description, partNumber, partNumberRevision, ref, rev);
    }

    return toolingCostLineMapper.toSwToolingCostLine(
        toolingCostLineRepository.save(toolingCostLine));
  }

  @Transactional
  public List<SWFileInfo> retrieveToolingCostLineFilesMetadata(UUID uid) {
    ToolingCostLineEntity toolingCostLine =
        entityRetrievalHelper.getMustExistToolingCostLineById(uid);
    return filesMapper.toListFileMetadata(toolingCostLine.getAttachedFiles());
  }

  @Transactional
  public Resource downloadToolingCostLineFiles(UUID uid, @Valid List<UUID> fileUids)
      throws Exception {
    ToolingCostLineEntity toolingCostLine =
        entityRetrievalHelper.getMustExistToolingCostLineById(uid);

    return fileHelper.downloadFile(
        appConfigurationProperties.getToolingCostLineFilesPathDirectory()
            + toolingCostLine.getToolingCostLineId(),
        fileHelper.fileUidsToFileNames(fileUids),
        GlobalConstants.ZIP_FILE);
  }

  // ===========================
  // Message Methods
  // ===========================

  @Transactional
  public List<SWMessage> retrieveMessages(UUID uid) {
    ToolingCostLineEntity toolingCostLine =
        entityRetrievalHelper.getMustExistToolingCostLineById(uid);
    return messageService.retrieve(toolingCostLine.getMessages());
  }

  @Transactional
  public List<SWMessage> createMessage(UUID uid, SWMessageCreate create) {
    ToolingCostLineEntity toolingCostLine =
        entityRetrievalHelper.getMustExistToolingCostLineById(uid);
    if (!toolingCostLine.isOutsourced()) {
      throw new GenericWithMessageException(
          "The tooling cost line is not outsourced. You may need to refresh your page.");
    }
    UserRole role =
        loggedUserDetailsService.hasRole(UserRole.ENGINEERING)
            ? UserRole.PROCUREMENT
            : UserRole.ENGINEERING;
    List<UserEntity> users = userHelper.getAllActiveUsersByRole(role);
    Runnable emailNotification = null;
    if (CollectionUtils.isNotEmpty(users)) {
      CostRequestLineEntity costRequestLine = toolingCostLine.getCostRequestLine();
      CostRequestEntity costRequest = costRequestLine.getCostRequest();
      List<String> emails = users.stream().map(UserEntity::getLogin).toList();
      String from = loggedUserDetailsService.getLoggedUserLogin();
      String description = toolingCostLine.getName();
      String partNumber = costRequestLine.getCustomerPartNumber();
      String partNumberRevision = costRequestLine.getCustomerPartNumberRevision();
      String ref = costRequest.getCostRequestReferenceNumber();
      String rev = String.valueOf(costRequest.getCostRequestRevision());
      emailNotification =
          () ->
              emailService.sendCostRequestToolingLineNewMessageEmail(
                  emails, role, from, description, partNumber, partNumberRevision, ref, rev);
    }
    return messageService.create(
        create,
        toolingCostLine::addMessage,
        () -> toolingCostLineRepository.save(toolingCostLine).getMessages(),
        emailNotification);
  }

  @Transactional
  public SWMessage updateMessage(UUID uid, UUID messageUid, SWMessageUpdate body) {
    ToolingCostLineEntity toolingCostLine =
        entityRetrievalHelper.getMustExistToolingCostLineById(uid);
    return messageService.update(messageUid, body, toolingCostLine.getMessages());
  }

  @Transactional
  public SWMessage deleteMessage(UUID uid, UUID messageUid) {
    ToolingCostLineEntity toolingCostLine =
        entityRetrievalHelper.getMustExistToolingCostLineById(uid);
    return messageService.delete(messageUid, toolingCostLine.getMessages());
  }

  @Transactional
  public SWMessage undoMessage(UUID uid, UUID messageUid) {
    ToolingCostLineEntity toolingCostLine =
        entityRetrievalHelper.getMustExistToolingCostLineById(uid);
    return messageService.undo(messageUid, toolingCostLine.getMessages());
  }
}
