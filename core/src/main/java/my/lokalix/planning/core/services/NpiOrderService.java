package my.lokalix.planning.core.services;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.mappers.FileMapper;
import my.lokalix.planning.core.mappers.NpiOrderMapper;
import my.lokalix.planning.core.mappers.ProcessLineMapper;
import my.lokalix.planning.core.mappers.ProcessLineStatusHistoryMapper;
import my.lokalix.planning.core.models.entities.FileInfoEntity;
import my.lokalix.planning.core.models.entities.NpiOrderEntity;
import my.lokalix.planning.core.models.entities.ProcessEntity;
import my.lokalix.planning.core.models.entities.ProcessLineEntity;
import my.lokalix.planning.core.models.entities.ProcessLineStatusHistoryEntity;
import my.lokalix.planning.core.models.enums.FileType;
import my.lokalix.planning.core.models.enums.NpiOrderStatus;
import my.lokalix.planning.core.models.enums.ProcessLineStatus;
import my.lokalix.planning.core.repositories.NpiOrderRepository;
import my.lokalix.planning.core.repositories.ProcessLineRepository;
import my.lokalix.planning.core.repositories.ProcessLineStatusHistoryRepository;
import my.lokalix.planning.core.repositories.ProcessRepository;
import my.lokalix.planning.core.security.LoggedUserDetailsService;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import my.lokalix.planning.core.services.helper.FileHelper;
import my.lokalix.planning.core.services.helper.NpiOrderHelper;
import my.lokalix.planning.core.services.validator.NpiValidator;
import my.lokalix.planning.core.services.validator.ProcessLineValidator;
import my.lokalix.planning.core.utils.ExcelUtils;
import my.lokalix.planning.core.utils.GlobalConstants;
import my.lokalix.planning.core.utils.TimeUtils;
import my.zkonsulting.planning.generated.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Service
public class NpiOrderService {

  private final NpiOrderMapper npiOrderMapper;
  private final ProcessLineMapper processLineMapper;
  private final ProcessLineStatusHistoryMapper processLineStatusHistoryMapper;
  private final FileMapper fileMapper;
  private final FileHelper fileHelper;
  private final NpiOrderRepository npiOrderRepository;
  private final ProcessRepository processRepository;
  private final ProcessLineRepository processLineRepository;
  private final ProcessLineStatusHistoryRepository processLineStatusHistoryRepository;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final ProcessLineValidator processLineValidator;
  private final AppConfigurationProperties appConfigurationProperties;
  private final NpiValidator npiValidator;
  private final LoggedUserDetailsService loggedUserDetailsService;
  private final NpiOrderHelper npiOrderHelper;

  @Transactional
  public SWNpiOrder createNpiOrder(SWNpiOrderCreate body) {
    List<ProcessEntity> processes = processRepository.findAllByOrderByCreationDateAsc();
    if (CollectionUtils.isEmpty(processes)) {
      throw new GenericWithMessageException(
          "Default processes have not been initialized", SWCustomErrorCode.GENERIC_ERROR);
    }

    NpiOrderEntity entity = npiOrderMapper.toNpiOrderEntity(body);
    buildProcessLines(entity, processes, body);
    calculateAndSetDeliveryDates(entity);
    NpiOrderEntity savedEntity = npiOrderRepository.save(entity);

    return npiOrderMapper.toSWNpiOrder(savedEntity);
  }

  @Transactional
  public SWNpiOrder retrieveNpiOrder(UUID uid) {
    NpiOrderEntity entity = entityRetrievalHelper.getMustExistNpiOrderById(uid);
    return npiOrderMapper.toSWNpiOrder(entity);
  }

  @Transactional
  public SWNpiOrder updateNpiOrder(UUID uid, SWNpiOrderUpdate body) {
    NpiOrderEntity entity = entityRetrievalHelper.getMustExistNpiOrderById(uid);
    if (entity.getStatus().isFinalStatus()) {
      throw new GenericWithMessageException(
          "Cannot update an NPI order with status " + entity.getStatus().getValue(),
          SWCustomErrorCode.GENERIC_ERROR);
    }
    npiOrderMapper.updateNpiOrderEntityFromDto(body, entity);

    entity
        .getProcessLines()
        .forEach(
            line -> {
              BigDecimal planTimeInHours = getPlanTimeForProcessLineFromUpdate(line, body);
              if (planTimeInHours != null) {
                line.setPlanTimeInHours(planTimeInHours);
              }
            });
    calculateAndSetDeliveryDates(entity);
    npiOrderHelper.recalculateForecastDeliveryDate(entity);
    return npiOrderMapper.toSWNpiOrder(npiOrderRepository.save(entity));
  }

  @Transactional
  public SWNpiOrder archiveNpiOrder(UUID uid) {
    NpiOrderEntity entity = entityRetrievalHelper.getMustExistNpiOrderById(uid);
    if (!(entity.getStatus().equals(NpiOrderStatus.COMPLETED)
        || entity.getStatus().equals(NpiOrderStatus.ABORTED))) {
      throw new GenericWithMessageException(
          "Cannot archive an NPI order with status " + entity.getStatus().getValue(),
          SWCustomErrorCode.GENERIC_ERROR);
    }
    entity.setArchived(true);
    entity.setArchivedAt(TimeUtils.nowOffsetDateTimeUTC());
    return npiOrderMapper.toSWNpiOrder(npiOrderRepository.save(entity));
  }

  @Transactional
  public SWNpiOrder abortNpiOrder(UUID uid) {
    NpiOrderEntity entity = entityRetrievalHelper.getMustExistNpiOrderById(uid);
    if (!entity.getStatus().isAbortable()) {
      throw new GenericWithMessageException(
          "Cannot abort an NPI order with status " + entity.getStatus().getValue(),
          SWCustomErrorCode.GENERIC_ERROR);
    }
    entity.setStatus(NpiOrderStatus.ABORTED);
    entity.setStatusDate(TimeUtils.nowOffsetDateTimeUTC());
    return npiOrderMapper.toSWNpiOrder(npiOrderRepository.save(entity));
  }

  @Transactional
  public SWProcess retrieveNpiOrderProcess(UUID npiOrderUid) {
    NpiOrderEntity npiOrder = entityRetrievalHelper.getMustExistNpiOrderById(npiOrderUid);
    List<ProcessLineEntity> lines =
        processLineRepository.findAllByNpiOrderOrderByIndexIdAsc(npiOrder);

    SWProcess process = new SWProcess();
    process.setUid(npiOrder.getNpiOrderId());
    process.setPartNumber(npiOrder.getPartNumber());
    process.setLines(processLineMapper.toListSWProcessLine(lines));
    return process;
  }

  @Transactional
  public List<SWProcessLine> updateNpiOrderProcessLineStatus(
      UUID npiOrderUid, UUID lineUid, SWProcessLineStatusUpdateBody body) throws Exception {
    NpiOrderEntity npiOrder = entityRetrievalHelper.getMustExistNpiOrderById(npiOrderUid);
    ProcessLineEntity line = entityRetrievalHelper.getMustExistProcessLineById(lineUid);
    npiValidator.validateNpiUpdatable(npiOrder);
    processLineValidator.validateStatusUpdate(line, body, npiOrder);

    ProcessLineStatus newStatus = ProcessLineStatus.fromValue(body.getStatus().getValue());
    if (newStatus.isRegressionFrom(line.getStatus())) {
      resetLineToDefaultState(line);
    }
    if (newStatus == ProcessLineStatus.IN_PROGRESS) {
      if (line.getIsMaterialPurchase()) {
        line.setMaterialLatestDeliveryDate(body.getMaterialLatestDeliveryDate());
      }
      if (line.getIsCustomerApproval()) {
        if (body.getStartingCustomerApprovalDate() == null) {
          throw new IllegalArgumentException(
              "Starting customer approval date cannot be null for in progress customer approval lines");
        }
        line.setStartingCustomerApprovalDate(body.getStartingCustomerApprovalDate());
      }
      if (line.getIsProduction() || line.getIsTesting()) {
        line.setRemainingTimeInHours(body.getRemainingTimeInHours());
      }
    }

    if (newStatus == ProcessLineStatus.COMPLETED) {
      line.setRemainingTimeInHours(BigDecimal.valueOf(0));
      if (line.getIsShipment() && body.getShippingDate() == null) {
        throw new IllegalArgumentException(
            "Shipping date cannot be null for completed shipment lines");
      }
      line.setShippingDate(body.getShippingDate());
      if (line.getIsCustomerApproval() && body.getApprovalCustomerDate() == null) {
        throw new IllegalArgumentException(
            "Approval customer date cannot be null for completed customer approval lines");
      }
      line.setApprovalCustomerDate(body.getApprovalCustomerDate());
      if (line.getIsTesting() && body.getFileUid() != null) {
        attachTestingDocumentToProcessLine(line, body.getFileUid());
      }
    }

    line.addStatus(
        TimeUtils.nowOffsetDateTimeUTC(),
        null,
        newStatus,
        loggedUserDetailsService.getLoggedUserReference());
    processLineRepository.save(line);
    resetFollowingLinesToDefaultState(npiOrder, line);
    npiOrderHelper.recalculateForecastDeliveryDate(npiOrder);

    npiOrderRepository.save(npiOrder);
    npiOrder.checkIfAllLinesIsCompleted();

    return processLineMapper.toListSWProcessLine(npiOrder.getProcessLines());
  }

  @Transactional
  public SWNpiOrdersPaginated searchNpiOrders(
      int offset, int limit, SWArchivedFilter archivedFilter, SWNpiOrderSearch body) {
    Sort sort = Sort.by(Sort.Direction.DESC, "creationDate");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);

    boolean hasStatusFilter = CollectionUtils.isNotEmpty(body.getStatuses());
    boolean hasSearchText = StringUtils.isNotBlank(body.getSearchText());

    List<NpiOrderStatus> statuses =
        hasStatusFilter
            ? body.getStatuses().stream().map(s -> NpiOrderStatus.fromValue(s.getValue())).toList()
            : null;

    Page<NpiOrderEntity> paginatedResults =
        switch (archivedFilter) {
          case NON_ARCHIVED_ONLY ->
              hasStatusFilter
                  ? hasSearchText
                      ? npiOrderRepository.findBySearchAndArchivedFalseAndStatusIn(
                          pageable, body.getSearchText(), statuses)
                      : npiOrderRepository.findAllByArchivedFalseAndStatusIn(pageable, statuses)
                  : hasSearchText
                      ? npiOrderRepository.findBySearchAndArchivedFalse(
                          pageable, body.getSearchText())
                      : npiOrderRepository.findAllByArchivedFalse(pageable);
          case ARCHIVED_ONLY ->
              hasSearchText
                  ? npiOrderRepository.findBySearchAndArchivedTrue(pageable, body.getSearchText())
                  : npiOrderRepository.findAllByArchivedTrue(pageable);
          default ->
              hasSearchText
                  ? npiOrderRepository.findBySearch(pageable, body.getSearchText())
                  : npiOrderRepository.findAll(pageable);
        };

    return populateNpiOrdersPaginatedResults(paginatedResults);
  }

  @Transactional
  public byte[] exportOpenNpiOrder() throws IOException {
    return npiOrderHelper.buildOpenNpiOrder();
  }

  @Transactional
  public byte[] exportArchivedNpiOrder() throws IOException {
    return npiOrderHelper.buildNpiOrderArchived();
  }

  @Transactional
  public List<SWFileInfo> retrieveProcessLineFilesMetadata(UUID npiOrderUid, UUID lineUid) {
    entityRetrievalHelper.getMustExistNpiOrderById(npiOrderUid);
    ProcessLineEntity line = entityRetrievalHelper.getMustExistProcessLineById(lineUid);
    return fileMapper.toListFileMetadata(line.getAttachedFiles());
  }

  @Transactional
  public List<SWFileInfo> uploadProcessLineFiles(
      UUID npiOrderUid, UUID lineUid, MultipartFile[] files) throws Exception {
    entityRetrievalHelper.getMustExistNpiOrderById(npiOrderUid);
    ProcessLineEntity line = entityRetrievalHelper.getMustExistProcessLineById(lineUid);
    List<String> filesAdded =
        fileHelper.uploadFiles(
            files,
            appConfigurationProperties.getProcessLineFilesPathDirectory() + line.getProcessLineId(),
            GlobalConstants.ALLOWED_FILE_EXTENSIONS);
    fileHelper.addFilesInAttachedFilesListInEntity(line, filesAdded, FileType.ANY);
    processLineRepository.save(line);
    return fileMapper.toListFileMetadata(line.getAttachedFiles());
  }

  @Transactional
  public Resource downloadProcessLineFiles(UUID npiOrderUid, UUID lineUid, List<UUID> fileUids)
      throws Exception {
    entityRetrievalHelper.getMustExistNpiOrderById(npiOrderUid);
    ProcessLineEntity line = entityRetrievalHelper.getMustExistProcessLineById(lineUid);
    return fileHelper.downloadFile(
        appConfigurationProperties.getProcessLineFilesPathDirectory() + line.getProcessLineId(),
        fileHelper.fileUidsToFileNames(fileUids),
        GlobalConstants.ZIP_FILE);
  }

  @Transactional
  public List<SWFileInfo> deleteProcessLineFiles(
      UUID npiOrderUid, UUID lineUid, List<UUID> fileUids) throws Exception {
    entityRetrievalHelper.getMustExistNpiOrderById(npiOrderUid);
    ProcessLineEntity line = entityRetrievalHelper.getMustExistProcessLineById(lineUid);
    List<Path> validPaths =
        fileHelper.deleteMultipleFiles(
            appConfigurationProperties.getProcessLineFilesPathDirectory() + line.getProcessLineId(),
            fileHelper.fileUidsToFileNames(fileUids));
    fileHelper.deleteFilesInAttachedFilesListInEntity(line, validPaths, FileType.ANY);
    processLineRepository.save(line);
    return fileMapper.toListFileMetadata(line.getAttachedFiles());
  }

  private void buildProcessLines(
      NpiOrderEntity npiOrder, List<ProcessEntity> processes, SWNpiOrderCreate body) {
    for (ProcessEntity process : processes) {
      ProcessLineEntity line = new ProcessLineEntity();
      line.setProcessName(process.getName());
      line.setIsMaterialPurchase(process.getIsMaterialPurchase());
      line.setIsMaterialReceiving(process.getIsMaterialReceiving());
      line.setIsProduction(process.getIsProduction());
      line.setIsTesting(process.getIsTesting());
      line.setIsShipment(process.getIsShipment());
      line.setIsCustomerApproval(process.getIsCustomerApproval());
      line.setPlanTimeInHours(getPlanTimeForProcess(process, body));
      npiOrder.addProcessLine(line);
    }
  }

  private BigDecimal getPlanTimeForProcess(ProcessEntity process, SWNpiOrderCreate body) {
    if (process.getIsMaterialPurchase()) return body.getMaterialPurchasePlanTimeInHours();
    if (process.getIsMaterialReceiving()) return body.getMaterialReceivingPlanTimeInHours();
    if (process.getIsProduction()) return body.getProductionPlanTimeInHours();
    if (process.getIsTesting()) return body.getTestingPlanTimeInHours();
    if (process.getIsShipment()) return body.getShippingPlanTimeInHours();
    if (process.getIsCustomerApproval()) return body.getCustomerApprovalPlanTimeInHours();
    return null;
  }

  private BigDecimal getPlanTimeForProcessLineFromUpdate(
      ProcessLineEntity line, SWNpiOrderUpdate body) {
    if (line.getIsMaterialPurchase()) return body.getMaterialPurchasePlanTimeInHours();
    if (line.getIsMaterialReceiving()) return body.getMaterialReceivingPlanTimeInHours();
    if (line.getIsProduction()) return body.getProductionPlanTimeInHours();
    if (line.getIsTesting()) return body.getTestingPlanTimeInHours();
    if (line.getIsShipment()) return body.getShippingPlanTimeInHours();
    if (line.getIsCustomerApproval()) return body.getCustomerApprovalPlanTimeInHours();
    return null;
  }

  private void resetFollowingLinesToDefaultState(
      NpiOrderEntity npiOrder, ProcessLineEntity referenceLine) throws Exception {
    List<ProcessLineEntity> allLines =
        processLineRepository.findAllByNpiOrderOrderByIndexIdAsc(npiOrder);
    for (ProcessLineEntity line : allLines) {
      if (line.getIndexId() <= referenceLine.getIndexId()) {
        continue;
      }
      resetLineToDefaultState(line);
      if (line.getStatus() != ProcessLineStatus.NOT_STARTED) {
        line.addStatus(
            TimeUtils.nowOffsetDateTimeUTC(),
            null,
            ProcessLineStatus.NOT_STARTED,
            loggedUserDetailsService.getLoggedUserReference());
      }
      processLineRepository.save(line);
    }
  }

  private void resetLineToDefaultState(ProcessLineEntity line) throws Exception {
    line.setRemainingTimeInHours(null);
    if (line.getIsMaterialPurchase()) {
      line.setMaterialLatestDeliveryDate(null);
    }
    if (line.getIsShipment()) {
      line.setShippingDate(null);
    }
    if (line.getIsCustomerApproval()) {
      line.setStartingCustomerApprovalDate(null);
      line.setApprovalCustomerDate(null);
    }
    if (line.getIsTesting() && CollectionUtils.isNotEmpty(line.getAttachedFiles())) {
      List<String> fileNames =
          line.getAttachedFiles().stream().map(FileInfoEntity::getFileName).toList();
      fileHelper.deleteMultipleFiles(
          appConfigurationProperties.getProcessLineFilesPathDirectory() + line.getProcessLineId(),
          fileNames);
      new ArrayList<>(line.getAttachedFiles()).forEach(line::removeAttachedFile);
    }
  }

  private void attachTestingDocumentToProcessLine(ProcessLineEntity line, UUID fileUid)
      throws Exception {
    FileInfoEntity tempFile = entityRetrievalHelper.getMustExistFileEntity(fileUid);
    String tempDirectory =
        appConfigurationProperties.getTemporaryFilesPathDirectory() + tempFile.getFileId();
    String targetDirectory =
        appConfigurationProperties.getProcessLineFilesPathDirectory() + line.getProcessLineId();
    fileHelper.copyFiles(List.of(tempFile.getFileName()), tempDirectory, targetDirectory);
    fileHelper.addFilesInAttachedFilesListInEntity(
        line, List.of(tempFile.getFileName()), FileType.ANY);
    fileHelper.deleteTemporaryFiles(List.of(fileUid));
  }

  private void calculateAndSetDeliveryDates(NpiOrderEntity entity) {
    if (entity.getOrderDate() == null) {
      return;
    }
    double totalHours =
        entity.getProcessLines().stream()
            .filter(l -> l.getPlanTimeInHours() != null)
            .mapToDouble(l -> l.getPlanTimeInHours().doubleValue())
            .sum();
    long totalDays = (long) Math.ceil(totalHours / 24.0);
    LocalDate baseDate =
        entity
                .getOrderDate()
                .isBefore(TimeUtils.nowLocalDate(appConfigurationProperties.getAppTimezone()))
            ? TimeUtils.nowLocalDate(appConfigurationProperties.getAppTimezone())
            : entity.getOrderDate();
    LocalDate plannedDate = baseDate.plusDays(totalDays);
    entity.setPlannedDeliveryDate(plannedDate);
    entity.setForecastDeliveryDate(plannedDate);
  }

  @Transactional
  public SWProcessLine updateProcessLineRemainingTime(
      UUID npiOrderUid, UUID lineUid, SWProcessLineRemainingTimeUpdate body) {
    NpiOrderEntity npiOrder = entityRetrievalHelper.getMustExistNpiOrderById(npiOrderUid);
    ProcessLineEntity line = entityRetrievalHelper.getMustExistProcessLineById(lineUid);
    processLineValidator.validateRemainingTimeUpdate(line);
    BigDecimal previousRemainingTimeInHours = line.getRemainingTimeInHours();
    line.setRemainingTimeInHours(body.getRemainingTimeInHours());
    if ((previousRemainingTimeInHours == null && body.getRemainingTimeInHours() != null)
        || (previousRemainingTimeInHours != null && body.getRemainingTimeInHours() == null)
        || (previousRemainingTimeInHours != null
            && body.getRemainingTimeInHours() != null
            && previousRemainingTimeInHours.compareTo(body.getRemainingTimeInHours()) != 0)) {
      processLineRepository.save(line);
      npiOrderHelper.recalculateForecastDeliveryDate(npiOrder);
      return processLineMapper.toSWProcessLine(line);
    }
    return processLineMapper.toSWProcessLine(processLineRepository.save(line));
  }

  public LocalDate importMaterialLatestDeliveryDate(
      UUID npiOrderUid, UUID lineUid, SWProcessLineMaterialDeliveryDateImport body) {
    entityRetrievalHelper.getMustExistNpiOrderById(npiOrderUid);
    ProcessLineEntity line = entityRetrievalHelper.getMustExistProcessLineById(lineUid);

    if (!line.getIsMaterialPurchase()) {
      throw new GenericWithMessageException(
          "This step is not a material purchase step", SWCustomErrorCode.GENERIC_ERROR);
    }
    npiValidator.validateNpiMaterialDeliveryDateFileConfig(body);
    FileInfoEntity fileInfo = entityRetrievalHelper.getMustExistFileEntity(body.getFileUid());
    String filePath =
        appConfigurationProperties.getTemporaryFilesPathDirectory()
            + fileInfo.getFileId()
            + "/"
            + fileInfo.getFileName();

    try (InputStream is = Files.newInputStream(Paths.get(filePath));
        Workbook workbook = WorkbookFactory.create(is)) {

      Sheet sheet = workbook.getSheetAt(body.getSheetIndex() - 1);
      if (sheet == null) {
        throw new GenericWithMessageException(
            "Sheet not found at index " + body.getSheetIndex(), SWCustomErrorCode.GENERIC_ERROR);
      }
      var row = sheet.getRow(body.getRow() - 1);
      if (row == null) {
        throw new GenericWithMessageException(
            "Row not found at index " + body.getRow(), SWCustomErrorCode.GENERIC_ERROR);
      }
      Cell cell = row.getCell(body.getColumn() - 1);
      if (cell == null) {
        throw new GenericWithMessageException(
            "Cell not found at column " + body.getColumn(), SWCustomErrorCode.GENERIC_ERROR);
      }
      LocalDate deliveryDate = ExcelUtils.loadDateCell(cell);
      if (deliveryDate == null) {
        throw new GenericWithMessageException(
            "No date found in the specified cell", SWCustomErrorCode.GENERIC_ERROR);
      }
      return deliveryDate;
    } catch (GenericWithMessageException e) {
      throw e;
    } catch (Exception e) {
      throw new GenericWithMessageException(
          "Failed to read file: " + e.getMessage(), SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  @Transactional
  public List<SWProcessLineStatusHistory> retrieveNpiOrderProcessLineStatusesHistory(
      UUID npiOrderUid, UUID lineUid) {
    entityRetrievalHelper.getMustExistNpiOrderById(npiOrderUid);
    ProcessLineEntity line = entityRetrievalHelper.getMustExistProcessLineById(lineUid);
    List<ProcessLineStatusHistoryEntity> history =
        processLineStatusHistoryRepository.findAllByProcessLineOrderByStartDateAsc(line);
    return processLineStatusHistoryMapper.toListSWProcessLineStatusHistory(history);
  }

  private SWNpiOrdersPaginated populateNpiOrdersPaginatedResults(
      Page<NpiOrderEntity> paginatedResults) {
    SWNpiOrdersPaginated result = new SWNpiOrdersPaginated();
    result.setResults(npiOrderMapper.toListSWNpiOrder(paginatedResults.getContent()));
    result.setPage(paginatedResults.getNumber());
    result.setPerPage(paginatedResults.getSize());
    result.setTotal((int) paginatedResults.getTotalElements());
    result.setHasPrev(paginatedResults.hasPrevious());
    result.setHasNext(paginatedResults.hasNext());
    return result;
  }
}
