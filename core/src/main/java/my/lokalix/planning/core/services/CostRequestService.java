package my.lokalix.planning.core.services;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.mappers.*;
import my.lokalix.planning.core.models.DownloadedFileOutput;
import my.lokalix.planning.core.models.entities.*;
import my.lokalix.planning.core.models.entities.admin.*;
import my.lokalix.planning.core.models.enums.*;
import my.lokalix.planning.core.models.excel.CellColorEnum;
import my.lokalix.planning.core.models.excel.CellStyleFormatEnum;
import my.lokalix.planning.core.models.excel.ExcelCellStyles;
import my.lokalix.planning.core.models.interfaces.FileInterface;
import my.lokalix.planning.core.repositories.*;
import my.lokalix.planning.core.repositories.admin.*;
import my.lokalix.planning.core.security.LoggedUserDetailsService;
import my.lokalix.planning.core.services.helper.*;
import my.lokalix.planning.core.services.validator.CostRequestValidator;
import my.lokalix.planning.core.utils.ExcelUtils;
import my.lokalix.planning.core.utils.GlobalConstants;
import my.lokalix.planning.core.utils.TextUtils;
import my.lokalix.planning.core.utils.TimeUtils;
import my.zkonsulting.planning.generated.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Slf4j
@Service
public class CostRequestService {
  private final CostRequestMapper costRequestMapper;
  private final CostRequestRepository costRequestRepository;
  private final CostRequestLineRepository costRequestLineRepository;
  private final FileMapper filesMapper;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final CostRequestHelper costRequestHelper;
  private final FileHelper fileHelper;
  private final AppConfigurationProperties appConfigurationProperties;
  private final MaterialRepository materialRepository;
  private final CustomerRepository customerRepository;
  private final CostRequestValidator costRequestValidator;
  private final LoggedUserDetailsService loggedUserDetailsService;
  private final MessageService messageService;
  private final UserHelper userHelper;
  private final EmailService emailService;
  private final CostRequestArchivingHelper costRequestArchivingHelper;
  private final SupplierAndManufacturerRepository supplierAndManufacturerRepository;
  private final MaterialCategoryRepository materialCategoryRepository;
  private final UnitRepository unitRepository;
  private final CostRequestLineMapper costRequestLineMapper;
  private final EnumMapper enumMapper;
  private final CostRequestPdfHelper costRequestPdfHelper;
  private final TemporaryFileService temporaryFileService;

  // ===========================
  // CostRequest Methods
  // ===========================
  @Transactional
  public byte[] exportOpenCostRequests() throws IOException {
    return costRequestHelper.buildOpenCostRequests();
  }

  @Transactional
  public byte[] exportArchivedCostRequests() throws IOException {
    return costRequestHelper.buildArchivedCostRequests();
  }

  @Transactional
  public void approvedByCustomer(UUID uuid) {
    CostRequestEntity costRequest = entityRetrievalHelper.getMustExistCostRequestById(uuid);
    costRequestValidator.validateIsActive(costRequest);
    costRequest.setStatus(CostRequestStatus.WON);
    costRequestRepository.save(checkAndArchiveCostRequest(costRequest));
  }

  @Transactional
  public void rejectedByCustomer(UUID uuid, SWRejectBody body) {
    CostRequestEntity costRequest = entityRetrievalHelper.getMustExistCostRequestById(uuid);
    costRequestValidator.validateIsActive(costRequest);
    costRequest.setStatus(CostRequestStatus.LOST);
    costRequest.setRejectByCustomerReason(body.getReason());
    costRequestRepository.save(checkAndArchiveCostRequest(costRequest));
  }

  @Transactional
  public void createdNewRevisionOfCostRequest(UUID uuid) {
    CostRequestEntity costRequest = entityRetrievalHelper.getMustExistCostRequestById(uuid);
    costRequestValidator.validateIsActive(costRequest);
    costRequest.setStatus(CostRequestStatus.NEW_REVISION_CREATED);
    costRequestRepository.save(checkAndArchiveCostRequest(costRequest));
  }

  @Transactional
  public SWCostRequest extendCostRequestExpiration(UUID uid, SWCostRequestExtendExpiration body) {
    CostRequestEntity costRequest = entityRetrievalHelper.getMustExistCostRequestById(uid);
    costRequestValidator.validateExtendExpiration(body.getNewExpirationDate());
    costRequest.setExpirationDate(body.getNewExpirationDate());
    costRequestRepository.save(costRequest);
    return costRequestMapper.toSWCostRequest(costRequest);
  }

  @Transactional
  public List<SWCustomCostRequestLine> listOfAllCostRequestLinesPendingApproval() {
    List<CostRequestLineEntity> costRequestLineEntities =
        costRequestLineRepository.findByStatus(CostRequestStatus.PENDING_APPROVAL);
    return costRequestLineMapper.toListSWCustomCostRequestLine(costRequestLineEntities);
  }

  @Transactional
  public List<SWMessage> retrieveMessages(UUID uid) {
    CostRequestEntity entity = entityRetrievalHelper.getMustExistCostRequestById(uid);
    return messageService.retrieve(entity.getMessages());
  }

  @Transactional
  public List<SWMessage> createMessage(UUID uid, SWMessageCreate create) {
    CostRequestEntity entity = entityRetrievalHelper.getMustExistCostRequestById(uid);
    UserRole role =
        loggedUserDetailsService.hasRole(UserRole.ENGINEERING)
            ? UserRole.PROJECT_MANAGER
            : UserRole.ENGINEERING;
    List<UserEntity> users = userHelper.getAllActiveUsersByRole(role);
    Runnable emailNotification = null;
    if (CollectionUtils.isNotEmpty(users)) {
      List<String> emails = users.stream().map(UserEntity::getLogin).toList();
      String from = loggedUserDetailsService.getLoggedUserLogin();
      String ref = entity.getCostRequestReferenceNumber();
      String rev = String.valueOf(entity.getCostRequestRevision());
      emailNotification = () -> emailService.sendCostRequestNewMessageEmail(emails, from, ref, rev);
    }
    return messageService.create(
        create,
        entity::addMessage,
        () -> costRequestRepository.save(entity).getMessages(),
        emailNotification);
  }

  @Transactional
  public SWCostRequest createCostRequest(SWCostRequestCreate body) throws Exception {
    CostRequestEntity entity = costRequestMapper.toCostRequestEntity(body);
    entity.setCostRequestReferenceNumber(costRequestHelper.nextCostRequestReferenceNumber());
    setCostRequestCustomerEmail(entity, body.getCustomerEmails());
    CustomerEntity customer = entityRetrievalHelper.getMustExistCustomerById(body.getCustomerId());
    entity.setCustomer(customer);

    if (CollectionUtils.isEmpty(customer.getShipmentLocations())) {
      CurrencyEntity currency =
          entityRetrievalHelper.getMustExistCurrencyById(body.getCurrencyId());
      entity.setCurrency(currency);
    } else {
      if (body.getCurrencyId() != null) {
        CurrencyEntity currency =
            entityRetrievalHelper.getMustExistCurrencyById(body.getCurrencyId());
        entity.setCurrency(currency);
      } else {
        entity.setCurrency(null);
      }
    }

    entity = costRequestRepository.save(entity);

    // Copy files (without deleting from temp) and collect IDs for later deletion
    List<UUID> costRequestFileIds =
        fileHelper.copyFilesFromTemporaryDirectoryToEntityDirectory(
            entity, entity.getCostRequestId().toString(), body.getFilesIds());
    List<UUID> allFileIdsToDeleteFromTemp = new ArrayList<>(costRequestFileIds);

    // Create lines if provided
    if (CollectionUtils.isNotEmpty(body.getLines())) {
      for (SWCostRequestLineCreate lineDto : body.getLines()) {
        CostRequestLineEntity lineEntity = new CostRequestLineEntity();
        entity.addLine(lineEntity);
        lineEntity.setCustomerPartNumber(lineDto.getCustomerPartNumber());
        lineEntity.setCustomerPartNumberRevision(lineDto.getCustomerPartNumberRevision());
        lineEntity.setDescription(lineDto.getDescription());
        lineEntity.setCostingMethodType(
            enumMapper.asCostingMethodType(lineDto.getCostingMethodType()));
        if (lineDto.getProductNameId() != null) {
          lineEntity.setProductName(
              entityRetrievalHelper.getMustExistProductNameById(lineDto.getProductNameId()));
        } else {
          lineEntity.setProductName(null);
        }
        costRequestValidator.validateQuantitiesCount(lineDto.getQuantities());
        lineEntity.setQuantities(TextUtils.concatenateListWithSeparator(lineDto.getQuantities()));

        // Copy files (without deleting from temp) and collect IDs for later deletion
        List<UUID> lineFileIds =
            fileHelper.copyFilesFromTemporaryDirectoryToEntityDirectory(
                lineEntity, lineEntity.getCostRequestLineId().toString(), lineDto.getFilesIds());
        allFileIdsToDeleteFromTemp.addAll(lineFileIds);

        costRequestLineRepository.save(lineEntity);
      }
    }

    // Reload to include lines
    CostRequestEntity savedEntity = costRequestRepository.save(entity);

    // Only delete temporary files after everything succeeded
    fileHelper.deleteTemporaryFiles(allFileIdsToDeleteFromTemp);

    return costRequestMapper.toSWCostRequest(savedEntity);
  }

  @Transactional
  public SWMessage updateMessages(UUID uid, UUID messageUid, SWMessageUpdate body) {
    CostRequestEntity entity = entityRetrievalHelper.getMustExistCostRequestById(uid);
    return messageService.update(messageUid, body, entity.getMessages());
  }

  @Transactional
  public SWMessage deleteMessages(UUID uid, UUID messageUid) {
    CostRequestEntity entity = entityRetrievalHelper.getMustExistCostRequestById(uid);
    return messageService.delete(messageUid, entity.getMessages());
  }

  @Transactional
  public SWMessage undoMessages(UUID uid, UUID messageUid) {
    CostRequestEntity entity = entityRetrievalHelper.getMustExistCostRequestById(uid);
    return messageService.undo(messageUid, entity.getMessages());
  }

  private void setCostRequestCustomerEmail(
      CostRequestEntity costRequestEntity, List<String> customerEmails) {
    if (CollectionUtils.isEmpty(customerEmails)) {
      costRequestEntity.setCustomerEmails(null);
      return;
    }
    costRequestEntity.setCustomerEmails(TextUtils.concatenateListWithSeparator(customerEmails));
  }

  @Transactional
  public SWCostRequest retrieveCostRequest(UUID uid) {
    CostRequestEntity entity = entityRetrievalHelper.getMustExistCostRequestById(uid);
    return costRequestMapper.toSWCostRequest(entity);
  }

  @Transactional
  public SWCostRequest updateCostRequest(UUID uid, SWCostRequestUpdate body) {
    CostRequestEntity entity = entityRetrievalHelper.getMustExistCostRequestById(uid);
    costRequestMapper.updateCostRequestEntityFromDto(body, entity);
    if (entity.getCustomer() == null
        || !body.getCustomerId().equals(entity.getCustomer().getCustomerId())) {
      CustomerEntity customer =
          entityRetrievalHelper.getMustExistCustomerById(body.getCustomerId());

      // If it was CLONED or NEW REVISION, the other cost lines would have been cloned as well
      // If customer changes, it may break the other cost lines shipment locations
      // Remove all other cost lines if customer changes
      if (entity.getCustomer() != null) {
        if (CollectionUtils.isNotEmpty(entity.getLines())) {
          for (CostRequestLineEntity line : entity.getLines()) {
            line.clearOtherCostLines();
          }
        }
      }
      entity.setCustomer(customer);
    }
    if (CollectionUtils.isEmpty(entity.getCustomer().getShipmentLocations())) {
      CurrencyEntity currency =
          entityRetrievalHelper.getMustExistCurrencyById(body.getCurrencyId());
      entity.setCurrency(currency);
    } else {
      if (body.getCurrencyId() != null) {
        CurrencyEntity currency =
            entityRetrievalHelper.getMustExistCurrencyById(body.getCurrencyId());
        entity.setCurrency(currency);
      } else {
        entity.setCurrency(null);
      }
    }
    setCostRequestCustomerEmail(entity, body.getCustomerEmails());
    return costRequestMapper.toSWCostRequest(costRequestRepository.save(entity));
  }

  @Transactional
  public SWCostRequest createCostRequestNewRevision(UUID uid) {
    CostRequestEntity entity = entityRetrievalHelper.getMustExistCostRequestById(uid);
    if (!entity.getStatus().isActiveDataFreezeStatus()) {
      throw new GenericWithMessageException(
          "Cost request must be in ACTIVE, WON or LOST status to create new revision",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    // Cache to avoid fetching the same currency multiple times within the same transaction,
    // which would trigger repeated auto-flushes and cause duplicate-identity errors in Hibernate
    Map<String, CurrencyEntity> activeCurrencyByOriginalCode = new HashMap<>();
    CostRequestEntity newCostRequest =
        toCloneCostRequestEntity(entity, activeCurrencyByOriginalCode);
    updateCostRequestRevisionValue(newCostRequest);
    newCostRequest = costRequestRepository.save(newCostRequest);
    cloneCostRequestLines(
        newCostRequest,
        entity.getLines(),
        activeCurrencyByOriginalCode,
        entityRetrievalHelper.getMustExistGlobalConfig());
    return costRequestMapper.toSWCostRequest(costRequestRepository.save(newCostRequest));
  }

  @Transactional
  public SWCostRequest cloneCostRequest(UUID uid, SWCostRequestClone body) {
    CostRequestEntity entity = entityRetrievalHelper.getMustExistCostRequestById(uid);
    // Cache to avoid fetching the same currency multiple times within the same transaction,
    // which would trigger repeated auto-flushes and cause duplicate-identity errors in Hibernate
    Map<String, CurrencyEntity> activeCurrencyByOriginalCode = new HashMap<>();
    CostRequestEntity newCostRequest =
        toCloneCostRequestEntity(entity, activeCurrencyByOriginalCode);
    newCostRequest.setCostRequestReferenceNumber(
        costRequestHelper.nextCostRequestReferenceNumber());
    newCostRequest.setClonedFromReferenceNumber(entity.getCostRequestReferenceNumber());
    newCostRequest = costRequestRepository.save(newCostRequest);
    List<CostRequestLineEntity> linesToCopied = new ArrayList<>();
    for (UUID lineUid : body.getLineUids()) {
      CostRequestLineEntity lineEntity =
          entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
      linesToCopied.add(lineEntity);
    }
    cloneCostRequestLines(
        newCostRequest,
        linesToCopied,
        activeCurrencyByOriginalCode,
        entityRetrievalHelper.getMustExistGlobalConfig());
    return costRequestMapper.toSWCostRequest(costRequestRepository.save(newCostRequest));
  }

  private void cloneFiles(
      FileInterface fileInterfaceOfNewRequest,
      List<FileInfoEntity> filesToCloned,
      String oldCostRequestId,
      String newCostRequestId,
      boolean isLine) {
    // Copy attached files
    if (CollectionUtils.isNotEmpty(filesToCloned)) {
      List<String> fileNamesToCopy =
          filesToCloned.stream().map(FileInfoEntity::getFileName).toList();
      String configurationPath =
          isLine
              ? appConfigurationProperties.getCostRequestLineFilesPathDirectory()
              : appConfigurationProperties.getCostRequestFilesPathDirectory();
      if (CollectionUtils.isNotEmpty(fileNamesToCopy)) {
        try {
          fileHelper.copyFiles(
              fileNamesToCopy,
              configurationPath + oldCostRequestId,
              configurationPath + newCostRequestId);
        } catch (Exception e) {
          // Log and rethrow to trigger transaction rollback
          log.error(
              "Failed to copy files from {} to {}: {}",
              oldCostRequestId,
              newCostRequestId,
              e.getMessage());
          throw new GenericWithMessageException(
              "File copy operation failed: " + e.getMessage(), SWCustomErrorCode.GENERIC_ERROR);
        }
      }
      for (FileInfoEntity fileInfoEntity : filesToCloned) {
        FileInfoEntity copyFileInfoEntity = new FileInfoEntity();
        copyFileInfoEntity.setFileName(fileInfoEntity.getFileName());
        copyFileInfoEntity.setType(fileInfoEntity.getType());
        fileInterfaceOfNewRequest.addAttachedFile(copyFileInfoEntity);
      }
    }
  }

  @Transactional
  public SWCostRequestsPaginated searchCostRequests(
      int offset, int limit, SWArchivedFilter archivedFilter, SWCostRequestSearch filters) {
    Page<CostRequestEntity> paginatedCostRequests =
        executeCostRequestSearch(offset, limit, archivedFilter, filters);
    return populateCostRequestsPaginatedResults(
        paginatedCostRequests, filters.getCostRequestLineAcceptedStatuses());
  }

  @Transactional
  public SWCostRequestsPaginated searchEngineeringCostRequests(
      int offset, int limit, SWArchivedFilter archivedFilter, SWCostRequestSearch filters) {
    Page<CostRequestEntity> paginatedCostRequests =
        executeCostRequestSearchForEngineering(offset, limit, archivedFilter, filters);
    return populateCostRequestsPaginatedResults(
        paginatedCostRequests, filters.getCostRequestLineAcceptedStatuses());
  }

  /**
   * Engineering-specific search: only returns cost requests that have at least one line matching
   * the provided line statuses (or any line if no line status filter). Filtering is done at DB
   * level via EXISTS to guarantee correct pagination.
   *
   * <p>Boolean flags short-circuit the IN clause in JPQL, avoiding Hibernate errors with empty
   * collections. A non-empty placeholder list is always passed so Hibernate can bind safely.
   */
  private Page<CostRequestEntity> executeCostRequestSearchForEngineering(
      int offset, int limit, SWArchivedFilter archivedFilter, SWCostRequestSearch filters) {
    Sort sort =
        archivedFilter == SWArchivedFilter.ARCHIVED_ONLY
            ? Sort.by(Sort.Direction.DESC, "creationDate")
            : Sort.unsorted();
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);

    boolean hasSearchText = StringUtils.isNotBlank(filters.getSearchText());
    String searchText =
        hasSearchText
            ? filters.getSearchText()
            : ""; // never null — avoids bytea inference by PostgreSQL

    boolean hasCrStatuses = CollectionUtils.isNotEmpty(filters.getCostRequestAcceptedStatuses());
    List<CostRequestStatus> crStatuses =
        hasCrStatuses
            ? filters.getCostRequestAcceptedStatuses().stream()
                .map(s -> CostRequestStatus.fromValue(s.getValue()))
                .toList()
            : List.of(CostRequestStatus.ABORTED); // non-empty placeholder (boolean short-circuits)

    boolean hasLineStatuses =
        CollectionUtils.isNotEmpty(filters.getCostRequestLineAcceptedStatuses());
    List<CostRequestStatus> lineStatuses =
        hasLineStatuses
            ? filters.getCostRequestLineAcceptedStatuses().stream()
                .map(s -> CostRequestStatus.fromValue(s.getValue()))
                .toList()
            : List.of(CostRequestStatus.ABORTED); // non-empty placeholder (boolean short-circuits)

    if (archivedFilter == SWArchivedFilter.ARCHIVED_ONLY) {
      return costRequestRepository.findForEngineeringArchivedOnly(
          pageable,
          hasSearchText,
          searchText,
          hasCrStatuses,
          crStatuses,
          hasLineStatuses,
          lineStatuses);
    }
    Boolean archived = archivedFilter == SWArchivedFilter.NON_ARCHIVED_ONLY ? false : null;
    return costRequestRepository.findForEngineeringNonArchivedOrAll(
        pageable,
        archived,
        hasSearchText,
        searchText,
        hasCrStatuses,
        crStatuses,
        hasLineStatuses,
        lineStatuses);
  }

  private Page<CostRequestEntity> executeCostRequestSearch(
      int offset, int limit, SWArchivedFilter archivedFilter, SWCostRequestSearch filters) {
    Sort sort =
        archivedFilter == SWArchivedFilter.ARCHIVED_ONLY
            ? Sort.by(Sort.Direction.DESC, "creationDate")
            : Sort.unsorted();
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);

    String searchText = filters.getSearchText();
    boolean hasStatusFilter = CollectionUtils.isNotEmpty(filters.getCostRequestAcceptedStatuses());

    List<CostRequestStatus> costRequestStatuses =
        hasStatusFilter
            ? filters.getCostRequestAcceptedStatuses().stream()
                .map(s -> CostRequestStatus.fromValue(s.getValue()))
                .toList()
            : null;

    if (StringUtils.isBlank(searchText)) {
      if (hasStatusFilter) {
        return switch (archivedFilter) {
          case NON_ARCHIVED_ONLY ->
              costRequestRepository.findByArchivedFalseAndStatusIn(pageable, costRequestStatuses);
          case ARCHIVED_ONLY ->
              costRequestRepository.findByArchivedTrueAndStatusIn(pageable, costRequestStatuses);
          default -> costRequestRepository.findByStatusIn(pageable, costRequestStatuses);
        };
      } else {
        return switch (archivedFilter) {
          case NON_ARCHIVED_ONLY -> costRequestRepository.findByArchivedFalse(pageable);
          case ARCHIVED_ONLY -> costRequestRepository.findByArchivedTrue(pageable);
          default -> costRequestRepository.findAll(pageable);
        };
      }
    } else {
      if (hasStatusFilter) {
        return switch (archivedFilter) {
          case NON_ARCHIVED_ONLY ->
              costRequestRepository.findBySearchAndArchivedFalseAndStatusIn(
                  pageable, searchText, costRequestStatuses);
          case ARCHIVED_ONLY ->
              costRequestRepository.findBySearchAndArchivedTrueAndStatusIn(
                  pageable, searchText, costRequestStatuses);
          default ->
              costRequestRepository.findBySearchAndStatusIn(
                  pageable, searchText, costRequestStatuses);
        };
      } else {
        return switch (archivedFilter) {
          case NON_ARCHIVED_ONLY ->
              costRequestRepository.findBySearchAndArchivedFalse(pageable, searchText);
          case ARCHIVED_ONLY ->
              costRequestRepository.findBySearchAndArchivedTrue(pageable, searchText);
          default -> costRequestRepository.findBySearch(pageable, searchText);
        };
      }
    }
  }

  public CostRequestEntity toCloneCostRequestEntity(
      CostRequestEntity costRequestEntityToCloned,
      Map<String, CurrencyEntity> activeCurrencyByOriginalCode) {
    if (costRequestEntityToCloned == null) {
      return null;
    }
    CostRequestEntity newCostRequestEntity = new CostRequestEntity();

    // Get active (non-archived) version of Customer
    CustomerEntity customerEntity = costRequestEntityToCloned.getCustomer();
    if (customerEntity != null) {
      CustomerEntity activeCustomer = getActiveVersionOfCustomer(customerEntity);
      if (activeCustomer != null) {
        newCostRequestEntity.setCustomer(activeCustomer);
      }
    }

    // Get active (non-archived) version of Currency
    CurrencyEntity currencyEntity = costRequestEntityToCloned.getCurrency();
    if (currencyEntity != null) {
      CurrencyEntity activeCurrency =
          costRequestHelper.getActiveVersionOfCurrency(
              currencyEntity, activeCurrencyByOriginalCode);
      if (activeCurrency != null) {
        newCostRequestEntity.setCurrency(activeCurrency);
      } else {
        throw new GenericWithMessageException(
            "Currency with ID " + currencyEntity.getCurrencyId() + " does not exist",
            SWCustomErrorCode.GENERIC_ERROR);
      }
    }
    newCostRequestEntity.setCostRequestReferenceNumber(
        costRequestEntityToCloned.getCostRequestReferenceNumber());
    newCostRequestEntity.setRequestorName(costRequestEntityToCloned.getRequestorName());
    newCostRequestEntity.setProjectName(costRequestEntityToCloned.getProjectName());
    newCostRequestEntity.setCustomerEmails(costRequestEntityToCloned.getCustomerEmails());
    newCostRequestEntity.setPurchaseOrderExpectedDate(
        costRequestEntityToCloned.getPurchaseOrderExpectedDate());
    newCostRequestEntity.setArchived(false);
    cloneFiles(
        newCostRequestEntity,
        costRequestEntityToCloned.getAttachedFilesPerFileType(FileType.ANY),
        costRequestEntityToCloned.getCostRequestId().toString(),
        newCostRequestEntity.getCostRequestId().toString(),
        false);
    return newCostRequestEntity;
  }

  private void updateCostRequestRevisionValue(CostRequestEntity costRequestEntity) {
    long rfqCount =
        costRequestRepository.countByRfqReferenceNumber(
            costRequestEntity.getCostRequestReferenceNumber());
    // no need to add 1 after the count because
    // the revision starts to 0
    costRequestEntity.setCostRequestRevision((int) rfqCount);
  }

  private void cloneCostRequestLines(
      CostRequestEntity newCostRequestEntity,
      List<CostRequestLineEntity> linesToCopied,
      Map<String, CurrencyEntity> activeCurrencyByOriginalCode,
      GlobalConfigEntity globalConfig) {
    if (CollectionUtils.isEmpty(linesToCopied)) return;
    // Cache to avoid fetching the same product name multiple times within the same transaction,
    // which would trigger repeated auto-flushes and cause duplicate-identity errors in Hibernate
    Map<String, ProductNameEntity> activeProductNameByOriginalName = new HashMap<>();
    // Cache to avoid fetching the same manufacturer multiple times within the same transaction,
    // which would trigger repeated auto-flushes and cause duplicate-identity errors in Hibernate
    Map<String, SupplierManufacturerEntity> activeManufacturerByOriginalCode = new HashMap<>();
    // Cache to avoid fetching the same material category multiple times within the same
    // transaction, which would trigger repeated auto-flushes and cause duplicate-identity errors in
    // Hibernate
    Map<String, MaterialCategoryEntity> activeMaterialCategoryByOriginalName = new HashMap<>();
    // Cache to avoid fetching the same unit multiple times within the same transaction, which
    // would trigger repeated auto-flushes and cause duplicate-identity errors in Hibernate
    Map<String, UnitEntity> activeUnitByOriginalName = new HashMap<>();
    // Cache to avoid fetching the same unit multiple times within the same transaction, which
    // would trigger repeated auto-flushes and cause duplicate-identity errors in Hibernate
    Map<String, ProcessEntity> activeProcessByOriginalName = new HashMap<>();
    // Cache to avoid fetching the same unit multiple times within the same transaction, which
    // would trigger repeated auto-flushes and cause duplicate-identity errors in Hibernate
    Map<String, ShipmentLocationEntity> activeDysonShipmentLocationByOriginalName = new HashMap<>();
    for (CostRequestLineEntity lineToClone : linesToCopied) {
      CostRequestLineEntity clonedLine = new CostRequestLineEntity();
      clonedLine.setCostRequest(newCostRequestEntity);
      clonedLine.setCustomerPartNumber(lineToClone.getCustomerPartNumber());
      clonedLine.setCustomerPartNumberRevision(lineToClone.getCustomerPartNumberRevision());
      clonedLine.setDescription(lineToClone.getDescription());
      clonedLine.setCostingMethodType(lineToClone.getCostingMethodType());
      clonedLine.setMarkup(lineToClone.getMarkup());
      clonedLine.setToolingMarkup(lineToClone.getToolingMarkup());
      clonedLine.setToolingStrategy(lineToClone.getToolingStrategy());

      // Get active (non-archived) version of ProductName if exists
      ProductNameEntity productNameEntity = lineToClone.getProductName();
      clonedLine.setProductName(
          costRequestHelper.getActiveVersionOfProductName(
              productNameEntity, activeProductNameByOriginalName));

      clonedLine.setQuantities(lineToClone.getQuantities());

      // Clone material lines (only non-archived materials)
      cloneMaterialLines(
          lineToClone,
          clonedLine,
          activeManufacturerByOriginalCode,
          activeMaterialCategoryByOriginalName,
          activeUnitByOriginalName);
      cloneProcessLines(lineToClone, clonedLine, activeProcessByOriginalName);
      cloneToolingCostLines(lineToClone, clonedLine, activeCurrencyByOriginalCode);
      cloneOtherCostLines(
          lineToClone,
          clonedLine,
          activeCurrencyByOriginalCode,
          activeDysonShipmentLocationByOriginalName,
          globalConfig);

      cloneFiles(
          clonedLine,
          lineToClone.getAttachedFiles(),
          lineToClone.getCostRequestLineId().toString(),
          clonedLine.getCostRequestLineId().toString(),
          true);
      // Save first: pre-set UUID causes save() to call em.merge(), which returns a NEW managed
      // entity. The original clonedLine becomes detached. Adding the managed entity to the
      // collection prevents "different object with same identifier" errors on auto-flush.
      CostRequestLineEntity savedLine = costRequestLineRepository.save(clonedLine);
      newCostRequestEntity.addLine(savedLine);
    }
  }

  private void cloneProcessLines(
      CostRequestLineEntity lineToClone,
      CostRequestLineEntity clonedLine,
      Map<String, ProcessEntity> activeProcessByOriginalName) {
    if (CollectionUtils.isNotEmpty(lineToClone.getProcessLines())) {
      for (ProcessLineEntity processLine : lineToClone.getProcessLines()) {
        ProcessEntity process = processLine.getProcess();
        ProcessEntity activeProcess =
            costRequestHelper.getActiveVersionOfProcess(process, activeProcessByOriginalName);
        if (activeProcess == null) {
          log.warn("Skipping process line - process {} has no active version", process.getName());
          continue;
        }

        ProcessLineEntity clonedProcessLine = new ProcessLineEntity();
        clonedProcessLine.setCostRequestLine(clonedLine);
        clonedProcessLine.setProcess(activeProcess);
        clonedProcessLine.setProcessCycleTimeInSeconds(processLine.getProcessCycleTimeInSeconds());
        clonedProcessLine.setQuantity(processLine.getQuantity());
        clonedProcessLine.setUnitCostInSystemCurrency(processLine.getUnitCostInSystemCurrency());
        clonedProcessLine.setTotalCostInSystemCurrency(processLine.getTotalCostInSystemCurrency());
        clonedLine.addProcessLine(clonedProcessLine);
      }
    }
  }

  private void cloneToolingCostLines(
      CostRequestLineEntity lineToClone,
      CostRequestLineEntity clonedLine,
      Map<String, CurrencyEntity> activeCurrencyByOriginalCode) {
    if (CollectionUtils.isNotEmpty(lineToClone.getToolingCostLines())) {
      for (ToolingCostLineEntity toolingLine : lineToClone.getToolingCostLines()) {
        CurrencyEntity currency = toolingLine.getCurrency();
        CurrencyEntity activeCurrency =
            costRequestHelper.getActiveVersionOfCurrency(currency, activeCurrencyByOriginalCode);
        if (activeCurrency == null) {
          log.warn("Skipping tooling line - currency {} has no active version", currency.getCode());
          continue;
        }

        ToolingCostLineEntity clonedToolingLine = new ToolingCostLineEntity();
        clonedToolingLine.setCostRequestLine(clonedLine);
        clonedToolingLine.setName(toolingLine.getName());
        clonedToolingLine.setCurrency(activeCurrency);
        clonedToolingLine.setQuantity(toolingLine.getQuantity());
        clonedToolingLine.setUnitCostInCurrency(toolingLine.getUnitCostInCurrency());
        clonedToolingLine.setTotalCostInSystemCurrency(toolingLine.getTotalCostInSystemCurrency());
        clonedLine.addToolingCostLine(clonedToolingLine);
      }
    }
  }

  private void cloneOtherCostLines(
      CostRequestLineEntity lineToClone,
      CostRequestLineEntity clonedLine,
      Map<String, CurrencyEntity> activeCurrencyByOriginalCode,
      Map<String, ShipmentLocationEntity> activeDysonShipmentLocationByOriginalName,
      GlobalConfigEntity globalConfig) {
    if (CollectionUtils.isNotEmpty(lineToClone.getOtherCostLines())) {
      Map<String, BigDecimal> nameToValueMap =
          GlobalConstants.extractOtherCostNamesFromGlobalConfig(globalConfig);
      for (OtherCostLineEntity otherLine : lineToClone.getOtherCostLines()) {
        CurrencyEntity currency = otherLine.getCurrency();
        CurrencyEntity activeCurrency =
            costRequestHelper.getActiveVersionOfCurrency(currency, activeCurrencyByOriginalCode);
        if (activeCurrency == null) {
          log.warn("Skipping other line - currency {} has no active version", currency.getCode());
          continue;
        }

        ShipmentLocationEntity shipmentLocation = otherLine.getShipmentLocation();
        ShipmentLocationEntity activeDysonShipmentLocation = null;
        if (shipmentLocation != null) {
          activeDysonShipmentLocation =
              costRequestHelper.getActiveVersionOfShipmentLocation(
                  shipmentLocation, activeDysonShipmentLocationByOriginalName);
          if (activeDysonShipmentLocation == null) {
            log.warn(
                "Skipping other line - shipment location {} has no active version",
                shipmentLocation.getName());
            continue;
          }
        }

        OtherCostLineEntity clonedOtherLine = new OtherCostLineEntity();
        clonedOtherLine.setCostRequestLine(clonedLine);
        clonedOtherLine.setName(otherLine.getName());
        clonedOtherLine.setCalculationStrategy(otherLine.getCalculationStrategy());
        clonedOtherLine.setPackagingLine(otherLine.isPackagingLine());
        clonedOtherLine.setFixedLine(otherLine.isFixedLine());
        clonedOtherLine.setEditableLine(otherLine.isEditableLine());
        clonedOtherLine.setShipmentToCustomerLine(otherLine.isShipmentToCustomerLine());
        clonedOtherLine.setShipmentLocation(activeDysonShipmentLocation);
        clonedOtherLine.setPackagingSize(otherLine.getPackagingSize());
        clonedOtherLine.setCurrency(activeCurrency);
        clonedOtherLine.setUnitCostInCurrency(otherLine.getUnitCostInCurrency());

        if (otherLine.isFixedLine() && !otherLine.isEditableLine()) {
          if (otherLine.isPackagingLine()) {
            switch (otherLine.getPackagingSize()) {
              case SMALL -> {
                clonedOtherLine.setUnitCostInCurrency(globalConfig.getSmallPackagingCost());
              }
              case LARGE -> {
                clonedOtherLine.setUnitCostInCurrency(globalConfig.getLargePackagingCost());
              }
              default ->
                  throw new IllegalStateException(
                      "Unexpected value: " + otherLine.getPackagingSize());
            }
          } else {
            BigDecimal value = nameToValueMap.get(otherLine.getName());
            // NULL in case of Shipment To Customer & Extra Cost...
            if (value != null) {
              clonedOtherLine.setUnitCostInCurrency(value);
            }
          }
        }
        clonedOtherLine.buildCalculatedFields(appConfigurationProperties.getTargetCurrencyCode());
        clonedLine.addOtherCostLine(clonedOtherLine);
      }
    }
  }

  /*
  If Cost request pending status that mean his material lines are drafts.
  else the material lines created or linked with existing material.
  When need to copy cost request line (Revision/Copy) need to create material line draft
  and his depend to cost request status.
   */
  private void cloneMaterialLines(
      CostRequestLineEntity lineToClone,
      CostRequestLineEntity clonedLine,
      Map<String, SupplierManufacturerEntity> activeManufacturerByOriginalCode,
      Map<String, MaterialCategoryEntity> activeMaterialCategoryByOriginalName,
      Map<String, UnitEntity> activeUnitByOriginalName) {
    if (lineToClone.getStatus().isPendingAndUpdatable()
        && CollectionUtils.isNotEmpty(lineToClone.getDraftMaterialLines())) {

      // Process draft material lines
      for (MaterialLineDraftEntity draftMaterialLine : lineToClone.getDraftMaterialLines()) {
        // Get active (non-archived) versions of Manufacturer and Category
        SupplierManufacturerEntity manufacturer = draftMaterialLine.getManufacturer();
        SupplierManufacturerEntity activeManufacturer = null;
        if (manufacturer != null) {
          activeManufacturer =
              costRequestHelper.getActiveVersionOfManufacturer(
                  manufacturer, activeManufacturerByOriginalCode);
          if (activeManufacturer == null) {
            log.warn(
                "Cloning draft material - manufacturer {} has no active version",
                manufacturer.getCode());
          }
        }

        MaterialCategoryEntity category = draftMaterialLine.getCategory();
        MaterialCategoryEntity activeCategory = null;
        if (category != null) {
          activeCategory =
              costRequestHelper.getActiveVersionOfMaterialCategory(
                  category, activeMaterialCategoryByOriginalName);
          if (activeCategory == null) {
            log.warn("Cloning material - category {} has no active version", category.getName());
          }
        }

        UnitEntity unit = draftMaterialLine.getUnit();
        UnitEntity activeUnit = null;
        if (unit != null) {
          activeUnit = costRequestHelper.getActiveVersionOfUnit(unit, activeUnitByOriginalName);
          if (activeUnit == null) {
            log.warn("Cloning draft material - unit {} has no active version", unit.getName());
          }
        }

        // Create draft with material information
        MaterialLineDraftEntity draft = new MaterialLineDraftEntity();
        draft.setCostRequestLine(clonedLine);
        draft.setManufacturer(activeManufacturer);
        draft.setManufacturerPartNumber(draftMaterialLine.getManufacturerPartNumber());
        draft.setMaterialType(draftMaterialLine.getMaterialType());
        draft.setDescription(draftMaterialLine.getDescription());
        draft.setCategory(activeCategory);
        draft.setUnit(activeUnit);
        draft.setMissingData(draftMaterialLine.getMissingData());
        draft.setQuantity(draftMaterialLine.getQuantity());
        // Flag if material exists in database (useful for frontend display)

        draft.setDraftManufacturerName(
            activeManufacturer != null
                ? activeManufacturer.getName()
                : draftMaterialLine.getDraftManufacturerName());
        draft.setDraftCategoryName(
            activeCategory != null
                ? activeCategory.getName()
                : draftMaterialLine.getDraftCategoryName());
        draft.setDraftUnitName(
            activeUnit != null ? activeUnit.getName() : draftMaterialLine.getDraftUnitName());
        draft.setMarkedNotUsedForQuote(draftMaterialLine.isMarkedNotUsedForQuote());
        clonedLine.addDraftMaterialLine(draft);
      }
    } else if (CollectionUtils.isNotEmpty(lineToClone.getMaterialLines())) {
      // Process regular material lines
      for (MaterialLineEntity materialLine : lineToClone.getMaterialLines()) {
        MaterialEntity material = materialLine.getMaterial();

        // Get active (non-archived) versions of Manufacturer and Category
        SupplierManufacturerEntity manufacturer = material.getManufacturer();
        SupplierManufacturerEntity activeManufacturer = null;
        if (manufacturer != null) {
          activeManufacturer =
              costRequestHelper.getActiveVersionOfManufacturer(
                  manufacturer, activeManufacturerByOriginalCode);
          if (activeManufacturer == null) {
            log.warn(
                "Cloning material {} - manufacturer {} has no active version",
                material.getSystemId(),
                manufacturer.getName());
          }
        }

        MaterialCategoryEntity category = material.getCategory();
        MaterialCategoryEntity activeCategory = null;
        if (category != null) {
          activeCategory =
              costRequestHelper.getActiveVersionOfMaterialCategory(
                  category, activeMaterialCategoryByOriginalName);
          if (activeCategory == null) {
            log.warn(
                "Cloning material {} - category {} has no active version",
                material.getSystemId(),
                category.getName());
          }
        }

        UnitEntity unit = material.getUnit();
        UnitEntity activeUnit = null;
        if (unit != null) {
          activeUnit = costRequestHelper.getActiveVersionOfUnit(unit, activeUnitByOriginalName);
          if (activeUnit == null) {
            log.warn("Cloning draft material - unit {} has no active version", unit.getName());
          }
        }

        // Create draft with material information
        MaterialLineDraftEntity draft = new MaterialLineDraftEntity();
        draft.setCostRequestLine(clonedLine);
        draft.setManufacturer(activeManufacturer);
        draft.setManufacturerPartNumber(material.getManufacturerPartNumber());
        draft.setMaterialType(material.getMaterialType());
        draft.setDescription(material.getDescription());
        draft.setCategory(activeCategory);
        draft.setUnit(activeUnit);
        draft.setQuantity(materialLine.getQuantity());

        draft.setDraftManufacturerName(
            activeManufacturer != null
                ? activeManufacturer.getName()
                : material.getDraftManufacturerName());
        draft.setDraftCategoryName(
            activeCategory != null ? activeCategory.getName() : material.getDraftCategoryName());
        draft.setDraftUnitName(
            activeUnit != null ? activeUnit.getName() : material.getDraftUnitName());
        draft.setMarkedNotUsedForQuote(materialLine.isMarkedNotUsedForQuote());
        clonedLine.addDraftMaterialLine(draft);
      }
    }
  }

  private SWCostRequestsPaginated populateCostRequestsPaginatedResults(
      Page<CostRequestEntity> paginatedCostRequests,
      List<SWCostRequestStatus> lineAcceptedStatuses) {
    SWCostRequestsPaginated costRequestsPaginated = new SWCostRequestsPaginated();
    List<SWCostRequest> results =
        loggedUserDetailsService.hasRole(UserRole.ENGINEERING)
            ? costRequestMapper.toListSwCostRequestForEngineering(
                paginatedCostRequests.getContent())
            : costRequestMapper.toListSWCostRequest(paginatedCostRequests.getContent());
    if (CollectionUtils.isNotEmpty(lineAcceptedStatuses)) {
      results.forEach(
          cr ->
              cr.setLines(
                  cr.getLines().stream()
                      .filter(line -> lineAcceptedStatuses.contains(line.getStatus()))
                      .toList()));
    }
    costRequestsPaginated.setResults(results);
    costRequestsPaginated.setPage(paginatedCostRequests.getNumber());
    costRequestsPaginated.setPerPage(paginatedCostRequests.getSize());
    costRequestsPaginated.setTotal((int) paginatedCostRequests.getTotalElements());
    costRequestsPaginated.setHasPrev(paginatedCostRequests.hasPrevious());
    costRequestsPaginated.setHasNext(paginatedCostRequests.hasNext());
    return costRequestsPaginated;
  }

  @Transactional
  public List<SWFileInfo> retrieveCostRequestFilesMetadata(UUID uid) {
    CostRequestEntity costRequestEntity = entityRetrievalHelper.getMustExistCostRequestById(uid);
    return filesMapper.toListFileMetadata(
        costRequestEntity.getAttachedFilesPerFileType(FileType.ANY));
  }

  @Transactional
  public List<SWFileInfo> uploadCostRequestFiles(UUID uid, MultipartFile[] files) throws Exception {
    CostRequestEntity costRequestEntity = entityRetrievalHelper.getMustExistCostRequestById(uid);
    List<String> filesAdded =
        fileHelper.uploadFiles(
            files,
            appConfigurationProperties.getCostRequestFilesPathDirectory()
                + costRequestEntity.getCostRequestId(),
            GlobalConstants.ALLOWED_FILE_EXTENSIONS);
    fileHelper.addFilesInAttachedFilesListInEntity(costRequestEntity, filesAdded, FileType.ANY);
    costRequestEntity = costRequestRepository.save(costRequestEntity);
    return filesMapper.toListFileMetadata(
        costRequestEntity.getAttachedFilesPerFileType(FileType.ANY));
  }

  @Transactional
  public Resource downloadCostRequestFiles(UUID uid, @Valid List<UUID> fileUids) throws Exception {
    CostRequestEntity costRequestEntity = entityRetrievalHelper.getMustExistCostRequestById(uid);

    return fileHelper.downloadFile(
        appConfigurationProperties.getCostRequestFilesPathDirectory()
            + costRequestEntity.getCostRequestId(),
        fileHelper.fileUidsToFileNames(fileUids),
        GlobalConstants.ZIP_FILE);
  }

  @Transactional
  public List<SWFileInfo> deleteCostRequestFiles(UUID uid, @Valid List<UUID> fileUids)
      throws Exception {
    CostRequestEntity costRequestEntity = entityRetrievalHelper.getMustExistCostRequestById(uid);
    List<Path> validPaths =
        fileHelper.deleteMultipleFiles(
            appConfigurationProperties.getCostRequestFilesPathDirectory()
                + costRequestEntity.getCostRequestId(),
            fileHelper.fileUidsToFileNames(fileUids));
    fileHelper.deleteFilesInAttachedFilesListInEntity(costRequestEntity, validPaths, FileType.ANY);
    costRequestEntity = costRequestRepository.save(costRequestEntity);
    return filesMapper.toListFileMetadata(
        costRequestEntity.getAttachedFilesPerFileType(FileType.ANY));
  }

  @Transactional
  public void validateCostRequestForReview(UUID uid) {
    CostRequestEntity costRequest = entityRetrievalHelper.getMustExistCostRequestById(uid);

    // Verify cost request is in PENDING_INFORMATION status
    if (costRequest.getStatus() != CostRequestStatus.PENDING_INFORMATION) {
      throw new GenericWithMessageException(
          "Only requests for quotation in Pending Information status can be passed to Ready For Review",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (costRequest.getCustomer() == null) {
      throw new GenericWithMessageException(
          "Customer is required for cost request", SWCustomErrorCode.GENERIC_ERROR);
    }
    List<UserEntity> users = userHelper.getAllActiveUsersByRole(UserRole.ENGINEERING);
    if (CollectionUtils.isNotEmpty(users)) {
      List<String> emails = users.stream().map(UserEntity::getLogin).toList();
      String ref = costRequest.getCostRequestReferenceNumber();
      String rev = String.valueOf(costRequest.getCostRequestRevision());
      emailService.sendNewCostRequestForReviewEmail(emails, ref, rev);
    }

    costRequest.setStatus(CostRequestStatus.READY_FOR_REVIEW);
    costRequestRepository.save(costRequest);
  }

  @Transactional
  public void validateCostRequestForEstimation(UUID uid) {
    CostRequestEntity costRequest = entityRetrievalHelper.getMustExistCostRequestById(uid);

    // Verify cost request is in READY_FOR_REVIEW status
    if (costRequest.getStatus() != CostRequestStatus.READY_FOR_REVIEW) {
      throw new GenericWithMessageException(
          "Only requests for quotation in Ready For Review status can be passed to Ready To Estimated",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    // Validate at least one line exists
    if (CollectionUtils.isEmpty(costRequest.getLines())) {
      throw new GenericWithMessageException(
          "The request for quotation must have at least one line", SWCustomErrorCode.GENERIC_ERROR);
    }

    // Validate no draft material line has missing data
    List<CostRequestLineEntity> linesUnderReview =
        costRequest.getLines().stream()
            .filter(line -> line.getStatus() == CostRequestStatus.READY_FOR_REVIEW)
            .toList();
    boolean hasMissingData =
        linesUnderReview.stream()
            .flatMap(line -> line.getDraftMaterialLines().stream())
            .anyMatch(draft -> StringUtils.isNotBlank(draft.getMissingData()));
    if (hasMissingData) {
      throw new GenericWithMessageException(
          "All material lines must have manufacturer, manufacturer P/N and quantity before the request for quotation can be validated",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    boolean atLeastOneNewMaterialCreated = false;

    // For each line under review, convert draft material lines to actual material lines
    if (CollectionUtils.isNotEmpty(linesUnderReview)) {
      for (CostRequestLineEntity line : linesUnderReview) {
        boolean newMaterialCreated = costRequestHelper.convertDraftsToMaterialLines(line);
        atLeastOneNewMaterialCreated = atLeastOneNewMaterialCreated || newMaterialCreated;
        line.setStatus(CostRequestStatus.READY_TO_ESTIMATE);
      }
    }

    // Freeze the customer shipment locations as soon as at least 1 line has moved to READY TO
    // ESTIMATE
    if (CollectionUtils.isEmpty(costRequest.getFrozenShipmentLocations())
        && CollectionUtils.isNotEmpty(costRequest.getCustomer().getShipmentLocations())) {
      List<CostRequestFrozenShipmentLocationEntity> archivedFrozenShipmentLocationEntity =
          costRequestArchivingHelper.createCostRequestFrozenShipmentLocationEntity(
              costRequest.getCustomer().getShipmentLocations(), new HashMap<>());
      if (CollectionUtils.isNotEmpty(archivedFrozenShipmentLocationEntity)) {
        for (CostRequestFrozenShipmentLocationEntity entity :
            archivedFrozenShipmentLocationEntity) {
          costRequest.addFrozenShipmentLocation(entity);
        }
      }
    }

    if (atLeastOneNewMaterialCreated) {
      List<UserEntity> users = userHelper.getAllActiveUsersByRole(UserRole.PROCUREMENT);
      if (CollectionUtils.isNotEmpty(users)) {
        List<String> emails = users.stream().map(UserEntity::getLogin).toList();
        emailService.sendNewMaterialToEstimateEmail(emails);
      }
    }

    // Update status to READY_TO_ESTIMATE
    costRequest.setStatus(CostRequestStatus.READY_TO_ESTIMATE);
    costRequestRepository.save(costRequest);
  }

  @Transactional
  public SWCostRequest abortCostRequest(UUID uid) {
    CostRequestEntity costRequest = entityRetrievalHelper.getMustExistCostRequestById(uid);

    costRequestValidator.validateNotFinalStatus(costRequest);

    // Archive all data for the cost request
    costRequestArchivingHelper.archiveCostRequestDataFreeze(costRequest);

    // Update status to ABORTED (this will cascade to all lines via setStatus())
    costRequest.setStatus(CostRequestStatus.ABORTED);
    CostRequestEntity savedCostRequest = costRequestRepository.save(costRequest);
    return costRequestMapper.toSWCostRequest(savedCostRequest);
  }

  @Transactional
  public SWCostRequest archiveCostRequest(UUID uid) {
    CostRequestEntity costRequest = entityRetrievalHelper.getMustExistCostRequestById(uid);
    CostRequestEntity savedCostRequest =
        costRequestRepository.save(checkAndArchiveCostRequest(costRequest));
    return costRequestMapper.toSWCostRequest(savedCostRequest);
  }

  private CostRequestEntity checkAndArchiveCostRequest(CostRequestEntity costRequest) {
    costRequestValidator.validateIsFinalStatus(costRequest);
    costRequestValidator.validateNotArchived(costRequest);

    // Set archived flag
    costRequest.setArchived(true);
    return costRequest;
  }

  /**
   * Get the active (non-archived) version of a customer. If the customer is not archived, returns
   * it as-is. If archived, searches for the non-archived version in DB by code and name.
   *
   * @param customer The customer entity (may be archived or not)
   * @return The active customer, or null if not found
   */
  private CustomerEntity getActiveVersionOfCustomer(CustomerEntity customer) {
    if (customer == null) {
      return null;
    }

    // If not archived, return as-is
    if (!customer.isArchived()) {
      return customer;
    }

    // Search for active version in DB
    Optional<CustomerEntity> activeVersion =
        customerRepository.findByCodeIgnoreCaseAndArchivedFalse(customer.getCode());
    if (activeVersion.isPresent()) {
      log.debug(
          "Found active version of archived customer {} (code: {})",
          customer.getName(),
          customer.getCode());
      return activeVersion.get();
    }

    log.warn(
        "No active version found for archived customer {} (code: {})",
        customer.getName(),
        customer.getCode());
    return null;
  }

  @Transactional
  public byte[] downloadStandardBom() throws IOException {
    try (FileInputStream excelFile =
            new FileInputStream(
                appConfigurationProperties.getExcelTemplatePaths().getStandardBom());
        Workbook workbook = new XSSFWorkbook(excelFile);
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      ExcelCellStyles excelCellStyles = new ExcelCellStyles(workbook);

      CellColorEnum color = CellColorEnum.WHITE;
      Sheet sheet = workbook.getSheetAt(1);
      List<SupplierManufacturerEntity> activeManufacturers =
          supplierAndManufacturerRepository.findAllByTypeInAndArchivedFalse(
              List.of(SupplierAndManufacturerType.MANUFACTURER, SupplierAndManufacturerType.BOTH),
              Sort.unsorted());
      activeManufacturers.sort(
          (a, b) -> {
            if (a.getCode() == null) return 1;
            if (b.getCode() == null) return -1;
            return TextUtils.compareNaturally(a.getCode(), b.getCode());
          });
      int rowNumber = 1;
      if (CollectionUtils.isNotEmpty(activeManufacturers)) {
        int cellNumber = 0;
        Row currentRow;
        for (SupplierManufacturerEntity manufacturer : activeManufacturers) {
          cellNumber = 0;
          currentRow = sheet.createRow(rowNumber);
          ExcelUtils.createAndStyleCellLeftAlignment(
              currentRow,
              cellNumber++,
              manufacturer.getCode(),
              CellStyleFormatEnum.STRING,
              color,
              excelCellStyles);
          ExcelUtils.createAndStyleCellLeftAlignment(
              currentRow,
              cellNumber++,
              manufacturer.getName(),
              CellStyleFormatEnum.STRING,
              color,
              excelCellStyles);
          rowNumber++;
        }
        for (int i = 0; i < cellNumber; i++) {
          sheet.autoSizeColumn(i);
        }
      }

      sheet = workbook.getSheetAt(2);
      rowNumber = 1;
      List<MaterialCategoryEntity> activeMaterialCategories =
          materialCategoryRepository.findAllByArchivedFalse(Sort.by(Sort.Direction.ASC, "name"));
      if (CollectionUtils.isNotEmpty(activeMaterialCategories)) {
        int cellNumber = 0;
        Row currentRow;
        for (MaterialCategoryEntity materialCategory : activeMaterialCategories) {
          cellNumber = 0;
          currentRow = sheet.createRow(rowNumber);
          ExcelUtils.createAndStyleCellLeftAlignment(
              currentRow,
              cellNumber++,
              materialCategory.getName(),
              CellStyleFormatEnum.STRING,
              color,
              excelCellStyles);
          rowNumber++;
        }
        for (int i = 0; i < cellNumber; i++) {
          sheet.autoSizeColumn(i);
        }
      }

      sheet = workbook.getSheetAt(3);
      rowNumber = 1;
      List<UnitEntity> activeUnits =
          unitRepository.findAllByArchivedFalse(Sort.by(Sort.Direction.ASC, "name"));
      if (CollectionUtils.isNotEmpty(activeUnits)) {
        int cellNumber = 0;
        Row currentRow;
        for (UnitEntity unit : activeUnits) {
          cellNumber = 0;
          currentRow = sheet.createRow(rowNumber);
          ExcelUtils.createAndStyleCellLeftAlignment(
              currentRow,
              cellNumber++,
              unit.getName(),
              CellStyleFormatEnum.STRING,
              color,
              excelCellStyles);
          rowNumber++;
        }
        for (int i = 0; i < cellNumber; i++) {
          sheet.autoSizeColumn(i);
        }
      }

      workbook.write(bos);
      return bos.toByteArray();
    }
  }

  @Transactional
  public void importCostRequestCustomBom(UUID uid, SWCustomBomImportBody body) throws Exception {
    CostRequestEntity costRequest = entityRetrievalHelper.getMustExistCostRequestById(uid);
    costRequestValidator.validateIsUpdatableStatus(costRequest);

    Resource temporaryFile =
        temporaryFileService.downloadTemporaryFiles(List.of(body.getTemporaryFileId()));
    if (temporaryFile == null) {
      throw new GenericWithMessageException(
          "Custom BOM file not found", SWCustomErrorCode.GENERIC_ERROR);
    }
    String originalFilename = temporaryFile.getFilename();
    if (StringUtils.isBlank(originalFilename)
        || (!originalFilename.toLowerCase().endsWith(".xlsx")
            && !originalFilename.toLowerCase().endsWith(".xls"))) {
      throw new GenericWithMessageException(
          "The file must be a .xls or .xlsx file.", SWCustomErrorCode.GENERIC_ERROR);
    }

    costRequestValidator.validateQuantitiesCount(body.getQuantities());

    try (Workbook workbook = new XSSFWorkbook(temporaryFile.getInputStream())) {
      Set<Integer> sheetNumbers = new HashSet<>();
      if (body.getBomConfig().getPartNumberCell() != null) {
        sheetNumbers.add(body.getBomConfig().getPartNumberCell().getSheetNumber());
      }
      if (body.getBomConfig().getRevisionCell() != null) {
        sheetNumbers.add(body.getBomConfig().getRevisionCell().getSheetNumber());
      }
      if (body.getBomConfig().getDescriptionCell() != null) {
        sheetNumbers.add(body.getBomConfig().getDescriptionCell().getSheetNumber());
      }
      if (body.getBomConfig().getMaterials() != null) {
        sheetNumbers.add(body.getBomConfig().getMaterials().getSheetNumber());
      }

      String partNumber = "MISSING";
      String revision = "MISSING";
      String description = "MISSING";
      List<MaterialLineDraftEntity> draftMaterialLines = new ArrayList<>();
      for (int sheetNumber : sheetNumbers) {
        Sheet sheet;
        try {
          sheet = workbook.getSheetAt(sheetNumber - 1);
        } catch (Exception e) {
          throw new GenericWithMessageException(
              "The file does not have a sheet at index "
                  + sheetNumber
                  + ". Please review your configuration.",
              SWCustomErrorCode.GENERIC_ERROR);
        }
        if (sheet == null) {
          throw new GenericWithMessageException(
              "Sheet at index " + sheetNumber + " not found. Please review your configuration.",
              SWCustomErrorCode.GENERIC_ERROR);
        }
        if (body.getBomConfig().getPartNumberCell() != null
            && body.getBomConfig().getPartNumberCell().getSheetNumber() == sheetNumber) {
          partNumber =
              ExcelUtils.loadStringCellForceNumericToLong(
                  sheet
                      .getRow(body.getBomConfig().getPartNumberCell().getRowNumber() - 1)
                      .getCell(body.getBomConfig().getPartNumberCell().getColumnNumber() - 1));
        }
        if (body.getBomConfig().getRevisionCell() != null
            && body.getBomConfig().getRevisionCell().getSheetNumber() == sheetNumber) {
          revision =
              ExcelUtils.loadStringCellForceNumericToLong(
                  sheet
                      .getRow(body.getBomConfig().getRevisionCell().getRowNumber() - 1)
                      .getCell(body.getBomConfig().getRevisionCell().getColumnNumber() - 1));
        }
        if (body.getBomConfig().getDescriptionCell() != null
            && body.getBomConfig().getDescriptionCell().getSheetNumber() == sheetNumber) {
          description =
              ExcelUtils.loadStringCellForceNumericToLong(
                  sheet
                      .getRow(body.getBomConfig().getDescriptionCell().getRowNumber() - 1)
                      .getCell(body.getBomConfig().getDescriptionCell().getColumnNumber() - 1));
        }
        if (body.getBomConfig().getMaterials() != null
            && body.getBomConfig().getMaterials().getSheetNumber() == sheetNumber) {
          for (int rowIndex = body.getBomConfig().getMaterials().getStartAtRowNumber() - 1;
              rowIndex <= sheet.getLastRowNum();
              rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            String manufacturerName =
                body.getBomConfig().getMaterials().getManufacturerNameColumnNumber() != null
                    ? ExcelUtils.loadStringCellForceNumericToLong(
                        row.getCell(
                            body.getBomConfig().getMaterials().getManufacturerNameColumnNumber()
                                - 1))
                    : null;
            String manufacturerPartNumber =
                body.getBomConfig().getMaterials().getManufacturerPartNumberColumnNumber() != null
                    ? ExcelUtils.loadStringCellForceNumericToLong(
                        row.getCell(
                            body.getBomConfig()
                                    .getMaterials()
                                    .getManufacturerPartNumberColumnNumber()
                                - 1))
                    : null;
            String draftDescription =
                body.getBomConfig().getMaterials().getDescriptionColumnNumber() != null
                    ? ExcelUtils.loadStringCellForceNumericToLong(
                        row.getCell(
                            body.getBomConfig().getMaterials().getDescriptionColumnNumber() - 1))
                    : null;
            String quantityStr =
                body.getBomConfig().getMaterials().getQuantityColumnNumber() != null
                    ? ExcelUtils.loadStringCell(
                        row.getCell(
                            body.getBomConfig().getMaterials().getQuantityColumnNumber() - 1))
                    : null;
            String unitName =
                body.getBomConfig().getMaterials().getUnitColumnNumber() != null
                    ? ExcelUtils.loadStringCellForceNumericToLong(
                        row.getCell(body.getBomConfig().getMaterials().getUnitColumnNumber() - 1))
                    : null;

            if (StringUtils.isAllBlank(
                manufacturerName,
                manufacturerPartNumber,
                draftDescription,
                quantityStr,
                unitName)) {
              continue;
            }

            SupplierManufacturerEntity manufacturer = null;
            if (StringUtils.isNotBlank(manufacturerName)) {
              manufacturer =
                  supplierAndManufacturerRepository
                      .findFirstByTypeInAndNameIgnoreCaseAndArchivedFalse(
                          List.of(
                              SupplierAndManufacturerType.SUPPLIER,
                              SupplierAndManufacturerType.MANUFACTURER,
                              SupplierAndManufacturerType.BOTH),
                          manufacturerName)
                      .stream()
                      .findFirst()
                      .orElse(null);
              // If SUPPLIER only found, then mark it as BOTH
              if (manufacturer != null
                  && manufacturer.getType() == SupplierAndManufacturerType.SUPPLIER) {
                manufacturer.setType(SupplierAndManufacturerType.BOTH);
                manufacturer = supplierAndManufacturerRepository.save(manufacturer);
              }
            }

            UnitEntity unit = null;
            if (StringUtils.isNotBlank(unitName)) {
              unit =
                  unitRepository.findFirstByNameIgnoreCaseAndArchivedFalse(unitName).orElse(null);
            }

            BigDecimal quantity = null;
            if (StringUtils.isNotBlank(quantityStr)) {
              try {
                quantity = new BigDecimal(quantityStr);
              } catch (NumberFormatException e) {
                log.warn("Invalid quantity '{}' at row {}, skipping", quantityStr, rowIndex + 1);
              }
            }

            // Check if material exists in database
            Optional<MaterialEntity> existingMaterial = Optional.empty();
            if (manufacturer != null && StringUtils.isNotBlank(manufacturerPartNumber)) {
              // Case 1: manufacturer + part number (no category)
              existingMaterial =
                  materialRepository
                      .findFirstByManufacturerAndPartNumberAndArchivedFalse(
                          manufacturer, manufacturerPartNumber)
                      .stream()
                      .findFirst();
            } else if (StringUtils.isNotBlank(manufacturerName)
                && StringUtils.isNotBlank(manufacturerPartNumber)) {
              // Case 2: no manufacturer found — match on draftManufacturerName + part number
              existingMaterial =
                  materialRepository
                      .findFirstByDraftManufacturerNameAndPartNumberAndArchivedFalse(
                          manufacturerName, manufacturerPartNumber)
                      .stream()
                      .findFirst();
            }

            String effectiveDescription = draftDescription;
            MaterialCategoryEntity effectiveCategory = null;
            UnitEntity effectiveUnit = unit;
            if (existingMaterial.isPresent()) {
              MaterialEntity existingMat = existingMaterial.get();
              effectiveDescription = existingMat.getDescription();
              effectiveCategory = existingMat.getCategory();
              effectiveUnit = existingMat.getUnit();
            }

            List<String> missingFields = new ArrayList<>();
            if (manufacturer == null && StringUtils.isBlank(manufacturerName))
              missingFields.add("Manufacturer");
            if (StringUtils.isBlank(manufacturerPartNumber))
              missingFields.add("Manufacturer part number");
            if (quantity == null) missingFields.add("Quantity");

            MaterialLineDraftEntity draft = new MaterialLineDraftEntity();
            draft.setMaterialType(MaterialType.DIRECT);
            draft.setManufacturer(manufacturer);
            draft.setManufacturerPartNumber(manufacturerPartNumber);
            draft.setDescription(effectiveDescription);
            draft.setCategory(effectiveCategory);
            draft.setQuantity(quantity);
            draft.setUnit(effectiveUnit);

            draft.setDraftManufacturerName(manufacturerName);
            draft.setDraftUnitName(unitName);

            if (CollectionUtils.isNotEmpty(missingFields)) {
              draft.setMissingData(String.join(", ", missingFields));
            }

            final SupplierManufacturerEntity finalManufacturer = manufacturer;
            final String finalManufacturerPartNumber = manufacturerPartNumber;
            boolean isDuplicate =
                draftMaterialLines.stream()
                    .anyMatch(
                        existing -> {
                          boolean sameManufacturer =
                              finalManufacturer != null
                                  && finalManufacturer.equals(existing.getManufacturer());
                          boolean samePartNumber =
                              StringUtils.isNotBlank(finalManufacturerPartNumber)
                                  && finalManufacturerPartNumber.equalsIgnoreCase(
                                      existing.getManufacturerPartNumber());
                          return sameManufacturer && samePartNumber;
                        });
            if (!isDuplicate) {
              draftMaterialLines.add(draft);
            }
          }
        }
      }

      final String finalPartNumber = partNumber;
      final String finalRevision = revision;
      CostRequestLineEntity lineEntity =
          costRequest.getLines().stream()
              .filter(
                  l ->
                      finalPartNumber.equalsIgnoreCase(l.getCustomerPartNumber())
                          && finalRevision.equalsIgnoreCase(l.getCustomerPartNumberRevision()))
              .findFirst()
              .orElse(null);

      if (lineEntity == null) {
        lineEntity = new CostRequestLineEntity();
        lineEntity.setCostRequest(costRequest);
        lineEntity.setCustomerPartNumber(partNumber);
        lineEntity.setCustomerPartNumberRevision(revision);
        lineEntity.setDescription(description);
        lineEntity.setCostingMethodType(enumMapper.asCostingMethodType(body.getCostingMethod()));
        lineEntity.setQuantities(TextUtils.concatenateListWithSeparator(body.getQuantities()));
        lineEntity.setStatus(costRequest.getStatus());
        costRequest.addLine(lineEntity);
        costRequestRepository.save(costRequest);
      } else {
        lineEntity.setCostingMethodType(enumMapper.asCostingMethodType(body.getCostingMethod()));
        lineEntity.setQuantities(TextUtils.concatenateListWithSeparator(body.getQuantities()));
      }
      if (CollectionUtils.isNotEmpty(draftMaterialLines)) {
        for (MaterialLineDraftEntity draft : draftMaterialLines) {
          lineEntity.addDraftMaterialLine(draft);
        }
      }
      costRequestLineRepository.save(lineEntity);
    }
  }

  @Transactional
  public void importCostRequestStandardBom(UUID uid, MultipartFile[] files) throws IOException {
    if (files == null || files.length != 1) {
      throw new GenericWithMessageException(
          "Exactly one file must be provided", SWCustomErrorCode.GENERIC_ERROR);
    }
    MultipartFile file = files[0];
    String originalFilename = file.getOriginalFilename();
    if (StringUtils.isBlank(originalFilename)
        || !originalFilename.toLowerCase().endsWith(".xlsx")) {
      throw new GenericWithMessageException(
          "The file must be a .xlsx file. The template is downloadable.",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    CostRequestEntity costRequest = entityRetrievalHelper.getMustExistCostRequestById(uid);
    costRequestValidator.validateIsUpdatableStatus(costRequest);

    try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);

      Row row3 = sheet.getRow(2);
      Row row4 = sheet.getRow(3);
      if (row3 == null || row4 == null) {
        throw new GenericWithMessageException(
            "Excel file is missing required header rows", SWCustomErrorCode.GENERIC_ERROR);
      }

      String partNumber = ExcelUtils.loadStringCellForceNumericToLong(row3.getCell(1)); // B3
      String revision = ExcelUtils.loadStringCellForceNumericToLong(row3.getCell(4)); // E3
      String costingMethodStr = ExcelUtils.loadStringCellForceNumericToLong(row3.getCell(6)); // G3
      String description = ExcelUtils.loadStringCellForceNumericToLong(row4.getCell(1)); // B4
      String quantitiesStr = ExcelUtils.loadStringCell(row4.getCell(4)); // E4

      if (StringUtils.isBlank(partNumber)) {
        throw new GenericWithMessageException(
            "B3 (part number) must not be blank", SWCustomErrorCode.GENERIC_ERROR);
      }
      if (StringUtils.isBlank(revision)) {
        throw new GenericWithMessageException(
            "E3 (revision) must not be blank", SWCustomErrorCode.GENERIC_ERROR);
      }
      if (StringUtils.isBlank(costingMethodStr)) {
        throw new GenericWithMessageException(
            "G3 (costing method) must not be blank", SWCustomErrorCode.GENERIC_ERROR);
      }
      CostingMethodType costingMethodType;
      try {
        costingMethodType = CostingMethodType.fromValue(costingMethodStr);
      } catch (IllegalArgumentException e) {
        throw new GenericWithMessageException(
            "G3 (costing method) has an invalid value '"
                + costingMethodStr
                + "'. Accepted values: BUDGETARY, HV, NPI",
            SWCustomErrorCode.GENERIC_ERROR);
      }
      if (StringUtils.isBlank(quantitiesStr)) {
        throw new GenericWithMessageException(
            "E4 (quantities) must not be blank", SWCustomErrorCode.GENERIC_ERROR);
      }

      List<Integer> quantities;
      try {
        quantities =
            Arrays.stream(quantitiesStr.split(";"))
                .map(String::trim)
                .map(Integer::parseInt)
                .toList();
      } catch (NumberFormatException e) {
        throw new GenericWithMessageException(
            "E4 (quantities) must contain integers separated by semi-colons",
            SWCustomErrorCode.GENERIC_ERROR);
      }
      costRequestValidator.validateQuantitiesCount(quantities);

      final String finalPartNumber = partNumber;
      final String finalRevision = revision;
      CostRequestLineEntity lineEntity =
          costRequest.getLines().stream()
              .filter(
                  l ->
                      finalPartNumber.equalsIgnoreCase(l.getCustomerPartNumber())
                          && finalRevision.equalsIgnoreCase(l.getCustomerPartNumberRevision()))
              .findFirst()
              .orElse(null);

      if (lineEntity == null) {
        lineEntity = new CostRequestLineEntity();
        lineEntity.setCostRequest(costRequest);
        lineEntity.setCustomerPartNumber(partNumber);
        lineEntity.setCustomerPartNumberRevision(revision);
        lineEntity.setDescription(description);
        lineEntity.setCostingMethodType(costingMethodType);
        lineEntity.setQuantities(TextUtils.concatenateListWithSeparator(quantities));
        lineEntity.setStatus(costRequest.getStatus());
        costRequest.addLine(lineEntity);
        costRequestRepository.save(costRequest);
      } else {
        lineEntity.setCostingMethodType(costingMethodType);
        lineEntity.setQuantities(TextUtils.concatenateListWithSeparator(quantities));
      }

      for (int rowIndex = 7; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) continue;

        String manufacturerCode = ExcelUtils.loadStringCellForceNumericToLong(row.getCell(0)); // A
        String manufacturerName = ExcelUtils.loadStringCellForceNumericToLong(row.getCell(1)); // B
        String manufacturerPartNumber =
            ExcelUtils.loadStringCellForceNumericToLong(row.getCell(2)); // C
        String draftDescription = ExcelUtils.loadStringCellForceNumericToLong(row.getCell(3)); // D
        String categoryName = ExcelUtils.loadStringCellForceNumericToLong(row.getCell(4)); // E
        String quantityStr = ExcelUtils.loadStringCell(row.getCell(5)); // F
        String unitName = ExcelUtils.loadStringCellForceNumericToLong(row.getCell(6)); // G

        if (StringUtils.isAllBlank(
            manufacturerCode,
            manufacturerName,
            manufacturerPartNumber,
            draftDescription,
            categoryName,
            quantityStr,
            unitName)) {
          continue;
        }

        SupplierManufacturerEntity manufacturer = null;
        if (StringUtils.isNotBlank(manufacturerCode)) {
          manufacturer =
              supplierAndManufacturerRepository
                  .findFirstByTypeInAndCodeIgnoreCaseAndArchivedFalse(
                      List.of(
                          SupplierAndManufacturerType.SUPPLIER,
                          SupplierAndManufacturerType.MANUFACTURER,
                          SupplierAndManufacturerType.BOTH),
                      manufacturerCode)
                  .orElse(null);
          // If SUPPLIER only found, then mark it as BOTH
          if (manufacturer != null
              && manufacturer.getType() == SupplierAndManufacturerType.SUPPLIER) {
            manufacturer.setType(SupplierAndManufacturerType.BOTH);
            manufacturer = supplierAndManufacturerRepository.save(manufacturer);
          }
        }
        if (manufacturer == null && StringUtils.isNotBlank(manufacturerName)) {
          manufacturer =
              supplierAndManufacturerRepository
                  .findFirstByTypeInAndNameIgnoreCaseAndArchivedFalse(
                      List.of(
                          SupplierAndManufacturerType.SUPPLIER,
                          SupplierAndManufacturerType.MANUFACTURER,
                          SupplierAndManufacturerType.BOTH),
                      manufacturerName)
                  .orElse(null);
          // If SUPPLIER only found, then mark it as BOTH
          if (manufacturer != null
              && manufacturer.getType() == SupplierAndManufacturerType.SUPPLIER) {
            manufacturer.setType(SupplierAndManufacturerType.BOTH);
            manufacturer = supplierAndManufacturerRepository.save(manufacturer);
          }
        }

        MaterialCategoryEntity category = null;
        if (StringUtils.isNotBlank(categoryName)) {
          category =
              materialCategoryRepository
                  .findFirstByNameIgnoreCaseAndArchivedFalse(categoryName)
                  .orElse(null);
        }

        UnitEntity unit = null;
        if (StringUtils.isNotBlank(unitName)) {
          unit = unitRepository.findFirstByNameIgnoreCaseAndArchivedFalse(unitName).orElse(null);
        }

        BigDecimal quantity = null;
        if (StringUtils.isNotBlank(quantityStr)) {
          try {
            quantity = new BigDecimal(quantityStr);
          } catch (NumberFormatException e) {
            log.warn("Invalid quantity '{}' at row {}, skipping", quantityStr, rowIndex + 1);
          }
        }

        // Check if material exists in database
        Optional<MaterialEntity> existingMaterial = Optional.empty();
        if (manufacturer != null
            && category != null
            && StringUtils.isNotBlank(manufacturerPartNumber)) {
          // Case 1: manufacturer + category + part number
          existingMaterial =
              materialRepository
                  .findFirstByManufacturerAndCategoryAndPartNumberAndArchivedFalse(
                      manufacturer, category, manufacturerPartNumber)
                  .stream()
                  .findFirst();
        } else if (manufacturer != null && StringUtils.isNotBlank(manufacturerPartNumber)) {
          // Case 2: manufacturer + part number (no category)
          existingMaterial =
              materialRepository
                  .findFirstByManufacturerAndPartNumberAndArchivedFalse(
                      manufacturer, manufacturerPartNumber)
                  .stream()
                  .findFirst();
        } else if (StringUtils.isNotBlank(manufacturerName)
            && StringUtils.isNotBlank(manufacturerPartNumber)) {
          // Case 3: no manufacturer found — match on draftManufacturerName + part number
          existingMaterial =
              materialRepository
                  .findFirstByDraftManufacturerNameAndPartNumberAndArchivedFalse(
                      manufacturerName, manufacturerPartNumber)
                  .stream()
                  .findFirst();
        }

        String effectiveDescription = draftDescription;
        MaterialCategoryEntity effectiveCategory = category;
        UnitEntity effectiveUnit = unit;
        if (existingMaterial.isPresent()) {
          MaterialEntity existingMat = existingMaterial.get();
          effectiveDescription = existingMat.getDescription();
          effectiveCategory = existingMat.getCategory();
          effectiveUnit = existingMat.getUnit();
        }

        List<String> missingFields = new ArrayList<>();
        if (manufacturer == null && StringUtils.isBlank(manufacturerName))
          missingFields.add("Manufacturer");
        if (StringUtils.isBlank(manufacturerPartNumber))
          missingFields.add("Manufacturer part number");
        if (quantity == null) missingFields.add("Quantity");

        MaterialLineDraftEntity draft = new MaterialLineDraftEntity();
        draft.setMaterialType(MaterialType.DIRECT);
        draft.setManufacturer(manufacturer);
        draft.setManufacturerPartNumber(manufacturerPartNumber);
        draft.setDescription(effectiveDescription);
        draft.setCategory(effectiveCategory);
        draft.setQuantity(quantity);
        draft.setUnit(effectiveUnit);

        draft.setDraftManufacturerName(manufacturerName);
        draft.setDraftCategoryName(categoryName);
        draft.setDraftUnitName(unitName);

        if (CollectionUtils.isNotEmpty(missingFields)) {
          draft.setMissingData(String.join(", ", missingFields));
        }

        final SupplierManufacturerEntity finalManufacturer = manufacturer;
        final String finalManufacturerPartNumber = manufacturerPartNumber;
        boolean isDuplicate =
            lineEntity.getDraftMaterialLines().stream()
                .anyMatch(
                    existing -> {
                      boolean sameManufacturer =
                          finalManufacturer != null
                              && finalManufacturer.equals(existing.getManufacturer());
                      boolean samePartNumber =
                          StringUtils.isNotBlank(finalManufacturerPartNumber)
                              && finalManufacturerPartNumber.equalsIgnoreCase(
                                  existing.getManufacturerPartNumber());
                      return sameManufacturer && samePartNumber;
                    });
        if (!isDuplicate) {
          lineEntity.addDraftMaterialLine(draft);
        }
      }
      costRequestLineRepository.save(lineEntity);
    }
  }

  @Transactional
  public DownloadedFileOutput downloadCostRequestPdf(UUID uid) throws Exception {
    CostRequestEntity costRequestEntity = entityRetrievalHelper.getMustExistCostRequestById(uid);
    costRequestValidator.validateNotAborted(costRequestEntity);
    costRequestValidator.validateIsDataFreezeStatus(costRequestEntity);

    String directory =
        appConfigurationProperties.getCostRequestFilesPathDirectory()
            + costRequestEntity.getCostRequestId();

    List<FileInfoEntity> pdfFiles =
        costRequestEntity.getAttachedFilesPerFileType(FileType.QUOTATION_PDF);
    if (CollectionUtils.isEmpty(pdfFiles)) {
      throw new GenericWithMessageException(
          "No quotation PDF found for this request for quotation", SWCustomErrorCode.GENERIC_ERROR);
    }
    FileInfoEntity pdfFileInfo = pdfFiles.getFirst();
    Resource resource =
        fileHelper.downloadFile(directory, List.of(pdfFileInfo.getFileName()), null);
    return new DownloadedFileOutput(
        pdfFileInfo.getFileName(), resource.getInputStream().readAllBytes());
  }

  @Transactional
  public DownloadedFileOutput generateCostRequestPdf(
      UUID uid, SWGenerateQuotationPdfBody extraInfos) throws Exception {
    CostRequestEntity costRequestEntity = entityRetrievalHelper.getMustExistCostRequestById(uid);
    costRequestValidator.validateNotAborted(costRequestEntity);
    costRequestValidator.validateIsReadyToQuote(costRequestEntity);

    String directory =
        appConfigurationProperties.getCostRequestFilesPathDirectory()
            + costRequestEntity.getCostRequestId();

    // READY_TO_QUOTE: generate, persist to disk and attach to entity
    LocalDateTime datetime =
        TimeUtils.nowLocalDateTime(appConfigurationProperties.getAppTimezone());
    String filename =
        "quotation--"
            + costRequestEntity.getCostRequestReferenceNumber()
            + "-"
            + costRequestEntity.getCostRequestRevision()
            + "--"
            + datetime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + ".pdf";
    byte[] pdfBytes =
        costRequestPdfHelper.buildCostRequestQuotationPdf(costRequestEntity, extraInfos);

    // Remove any existing QUOTATION_PDF file from disk and entity
    List<FileInfoEntity> existingPdfFiles =
        new ArrayList<>(costRequestEntity.getAttachedFilesPerFileType(FileType.QUOTATION_PDF));
    if (CollectionUtils.isNotEmpty(existingPdfFiles)) {
      for (FileInfoEntity existing : existingPdfFiles) {
        try {
          fileHelper.deleteFile(directory, existing.getFileName());
        } catch (Exception e) {
          log.warn("Failed to delete file {} from disk", existing.getFileName(), e);
        }
        costRequestEntity.removeAttachedFile(existing);
      }
    }

    // Write new PDF to disk and attach to entity
    MultipartFile pdfMultipartFile =
        new MockMultipartFile(filename, filename, "application/pdf", pdfBytes);
    List<String> filesAdded =
        fileHelper.uploadFiles(
            new MultipartFile[] {pdfMultipartFile},
            directory,
            GlobalConstants.ALLOWED_FILE_EXTENSIONS);
    fileHelper.addFilesInAttachedFilesListInEntity(
        costRequestEntity, filesAdded, FileType.QUOTATION_PDF);
    costRequestEntity.setStatus(CostRequestStatus.ACTIVE);
    costRequestArchivingHelper.archiveCostRequestDataFreeze(costRequestEntity);
    costRequestRepository.save(costRequestEntity);

    return new DownloadedFileOutput(filename, pdfBytes);
  }

  @Transactional
  public SWSubstituteMaterialsComment retrieveCostRequestSubstituteMaterialsComment(UUID uid) {
    CostRequestEntity costRequestEntity = entityRetrievalHelper.getMustExistCostRequestById(uid);
    costRequestValidator.validateNotAborted(costRequestEntity);
    costRequestValidator.validateIsReadyToQuote(costRequestEntity);

    StringBuilder sb = new StringBuilder();
    for (CostRequestLineEntity line : costRequestEntity.getLines()) {
      if (line.getStatus() != CostRequestStatus.READY_TO_QUOTE) continue;
      List<MaterialLineEntity> linesWithSubstitute =
          line.getOnlyMaterialLinesUsedForQuotation().stream()
              .filter(MaterialLineEntity::isHasMaterialSubstitute)
              .toList();
      if (CollectionUtils.isEmpty(linesWithSubstitute)) continue;
      if (!sb.isEmpty()) sb.append("\n");
      sb.append("Material substitute for ")
          .append(line.getCustomerPartNumber())
          .append(" ")
          .append(line.getCustomerPartNumberRevision())
          .append(":");
      for (MaterialLineEntity ml : linesWithSubstitute) {
        MaterialEntity original = ml.getMaterial();
        MaterialEntity substitute = ml.getMaterialSubstitute().getMaterial();
        sb.append("\n- ")
            .append(original.getManufacturer().getName())
            .append(" (")
            .append(original.getManufacturerPartNumber())
            .append(") ==> ")
            .append(substitute.getManufacturer().getName())
            .append(" (")
            .append(substitute.getManufacturerPartNumber())
            .append(")");
      }
    }
    return new SWSubstituteMaterialsComment().comment(sb.toString());
  }
}
