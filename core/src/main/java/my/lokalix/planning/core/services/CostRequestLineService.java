package my.lokalix.planning.core.services;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.mappers.*;
import my.lokalix.planning.core.models.*;
import my.lokalix.planning.core.models.entities.*;
import my.lokalix.planning.core.models.entities.admin.*;
import my.lokalix.planning.core.models.enums.*;
import my.lokalix.planning.core.repositories.*;
import my.lokalix.planning.core.repositories.admin.*;
import my.lokalix.planning.core.security.LoggedUserDetailsService;
import my.lokalix.planning.core.services.helper.*;
import my.lokalix.planning.core.services.validator.CostRequestValidator;
import my.lokalix.planning.core.services.validator.MaterialValidator;
import my.lokalix.planning.core.utils.GlobalConstants;
import my.lokalix.planning.core.utils.TextUtils;
import my.lokalix.planning.core.utils.TimeUtils;
import my.zkonsulting.planning.generated.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Slf4j
@Service
public class CostRequestLineService {
  private final CostRequestLineMapper costRequestLineMapper;
  private final CostRequestRepository costRequestRepository;
  private final CostRequestLineRepository costRequestLineRepository;
  private final FileMapper filesMapper;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final FileHelper fileHelper;
  private final AppConfigurationProperties appConfigurationProperties;
  private final ProcessLineMapper processLineMapper;
  private final OtherCostLineMapper otherCostLineMapper;
  private final ToolingCostLineMapper toolingCostLineMapper;
  private final MaterialRepository materialRepository;
  private final MaterialLineMapper materialLineMapper;
  private final MaterialSupplierMapper materialSupplierMapper;
  private final ProcessUsageCountRepository processUsageCountRepository;
  private final CostRequestValidator costRequestValidator;
  private final CostRequestArchivingHelper costRequestArchivingHelper;
  private final ToolingCostLineRepository toolingCostLineRepository;
  private final SystemIdHelper systemIdHelper;
  private final EnumMapper enumMapper;
  private final MessageService messageService;
  private final LoggedUserDetailsService loggedUserDetailsService;
  private final UserHelper userHelper;
  private final EmailService emailService;
  private final CostRequestHelper costRequestHelper;
  private final CostRequestLineHelper costRequestLineHelper;
  private final CurrencyMapper currencyMapper;
  private final MaterialValidator materialValidator;
  private final CostRequestLineCalculationsHelper costRequestLineCalculationsHelper;
  private final CostRequestLineQuotationBreakdownHelper costRequestLineQuotationBreakdownHelper;
  private final CostRequestFrozenShipmentLocationRepository
      costRequestFrozenShipmentLocationRepository;

  @Transactional
  public byte[] exportProductionBomOfCostRequestLine(UUID uid, UUID lineUid) throws IOException {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    return costRequestHelper.buildProductionBomOfCostRequestLine(lineEntity);
  }

  @Transactional
  public SWCostRequestLine createCostRequestLine(UUID costRequestUid, SWCostRequestLineCreate body)
      throws Exception {
    CostRequestEntity costRequest =
        entityRetrievalHelper.getMustExistCostRequestById(costRequestUid);

    // Prevent creation of new lines after ready to estimate
    if (!costRequest.getStatus().isPendingAndUpdatable()) {
      throw new GenericWithMessageException(
          "Cannot create new request for quotation line after ready to estimate",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    // Create line entity
    CostRequestLineEntity lineEntity = new CostRequestLineEntity();
    lineEntity.setCostRequest(costRequest);
    lineEntity.setCustomerPartNumber(body.getCustomerPartNumber());
    lineEntity.setCustomerPartNumberRevision(body.getCustomerPartNumberRevision());
    lineEntity.setDescription(body.getDescription());
    lineEntity.setCostingMethodType(enumMapper.asCostingMethodType(body.getCostingMethodType()));
    if (body.getProductNameId() != null) {
      lineEntity.setProductName(
          entityRetrievalHelper.getMustExistProductNameById(body.getProductNameId()));
    } else {
      lineEntity.setProductName(null);
    }
    lineEntity.setStatus(costRequest.getStatus());

    costRequestValidator.validateQuantitiesCount(body.getQuantities());
    lineEntity.setQuantities(TextUtils.concatenateListWithSeparator(body.getQuantities()));

    // Copy files (without deleting from temp) and collect IDs for later deletion
    List<UUID> fileIdsToDeleteFromTemp =
        fileHelper.copyFilesFromTemporaryDirectoryToEntityDirectory(
            lineEntity, lineEntity.getCostRequestLineId().toString(), body.getFilesIds());

    costRequest.addLine(lineEntity);
    costRequestRepository.save(costRequest);

    // Only delete temporary files after everything succeeded
    fileHelper.deleteTemporaryFiles(fileIdsToDeleteFromTemp);

    return costRequestLineMapper.toSwCostRequestLine(lineEntity);
  }

  @Transactional
  public SWCostRequestLine retrieveCostRequestLine(UUID uid, UUID lineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);

    return costRequestLineMapper.toSwCostRequestLine(lineEntity);
  }

  @Transactional
  public SWCostRequestLine updateCostRequestLine(
      UUID uid, UUID lineUid, SWCostRequestLineUpdate body) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);

    // Prevent modification after ready to estimate
    CostRequestEntity costRequest = lineEntity.getCostRequest();
    if (costRequest.getStatus().isEstimatedStatus()) {
      throw new GenericWithMessageException(
          "Cannot modify request for quotation line after ready to estimate",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    // Update entity from DTO
    costRequestValidator.validateQuantitiesCount(body.getQuantities());
    costRequestLineMapper.updateCostRequestLineEntityFromDto(body, lineEntity);

    if (body.getProductNameId() != null) {
      lineEntity.setProductName(
          entityRetrievalHelper.getMustExistProductNameById(body.getProductNameId()));
    } else {
      lineEntity.setProductName(null);
    }

    lineEntity = costRequestLineRepository.save(lineEntity);

    return costRequestLineMapper.toSwCostRequestLine(lineEntity);
  }

  @Transactional
  public void validateCostRequestLineForEstimation(UUID uid, UUID lineUid) {
    CostRequestLineEntity costRequestLine =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);

    // Verify cost request line is in READY_FOR_REVIEW status
    if (costRequestLine.getStatus() != CostRequestStatus.READY_FOR_REVIEW) {
      throw new GenericWithMessageException(
          "Only lines in Ready For Review status can be passed to Ready To Estimate",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    // Validate no draft material line has missing data
    boolean hasMissingData =
        costRequestLine.getDraftMaterialLines().stream()
            .anyMatch(draft -> StringUtils.isNotBlank(draft.getMissingData()));
    if (hasMissingData) {
      throw new GenericWithMessageException(
          "All material lines must have manufacturer, manufacturer P/N and quantity before the request for quotation can be validated",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    boolean atLeastOneNewMaterialCreated =
        costRequestHelper.convertDraftsToMaterialLines(costRequestLine);
    costRequestLine.setStatus(CostRequestStatus.READY_TO_ESTIMATE);
    costRequestLineRepository.save(costRequestLine);

    CostRequestEntity costRequest = costRequestLine.getCostRequest();

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

    recalculateCostRequestStatus(costRequest);

    if (atLeastOneNewMaterialCreated) {
      List<UserEntity> users = userHelper.getAllActiveUsersByRole(UserRole.PROCUREMENT);
      if (CollectionUtils.isNotEmpty(users)) {
        List<String> emails = users.stream().map(UserEntity::getLogin).toList();
        emailService.sendNewMaterialToEstimateEmail(emails);
      }
    }

    costRequestRepository.save(costRequest);
  }

  @Transactional
  public List<SWCostRequestLine> revertCostRequestLineForReestimation(
      UUID uid, UUID lineUid, SWCostRequestStatus costRequestStatus) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    CostRequestEntity costRequest = lineEntity.getCostRequest();

    if (lineEntity.getStatus().isDataFreezeStatus()) {
      throw new GenericWithMessageException(
          "Impossible to re-estimate a request for quotation line that is in a final state ("
              + lineEntity.getStatus().getHumanReadableValue()
              + ")",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    if (!lineEntity.getStatus().isAfterEngineeringEstimation()) {
      throw new GenericWithMessageException(
          "Only request for quotation lines already estimated by Engineering can be reverted for re-estimation",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    if (lineEntity.getStatus() == CostRequestStatus.PENDING_APPROVAL) {
      throw new GenericWithMessageException(
          "PENDING APPROVAL request for quotation lines cannot be reverted for re-estimation",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    if (costRequestStatus != SWCostRequestStatus.READY_TO_VALIDATE
        && costRequestStatus != SWCostRequestStatus.READY_TO_ESTIMATE) {
      throw new GenericWithMessageException(
          "Impossible to revert to a status other than READY TO VALIDATE or READY TO ESTIMATE",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    lineEntity.setReverted(true);
    lineEntity.setMarkup(null);
    lineEntity.setToolingMarkup(null);
    lineEntity.setPriceRejectReason(null);
    lineEntity.clearAllPerQuantityData();
    if (costRequestStatus == SWCostRequestStatus.READY_TO_ESTIMATE) {
      lineEntity.setOutsourced(false);
      lineEntity.setOutsourcingStatus(null);
      lineEntity.setOutsourcedCostInSystemCurrency(null);
      lineEntity.setOutsourcingRejectReason(null);
      lineEntity.setStatus(CostRequestStatus.READY_TO_ESTIMATE);
      List<UserEntity> engineers = userHelper.getAllActiveUsersByRole(UserRole.ENGINEERING);
      if (CollectionUtils.isNotEmpty(engineers)) {
        List<String> engineerEmails = engineers.stream().map(UserEntity::getLogin).toList();
        emailService.sendCostRequestLineRevertedToReadyToEstimateEmail(
            engineerEmails,
            lineEntity.getCustomerPartNumber(),
            lineEntity.getCustomerPartNumberRevision(),
            costRequest.getCostRequestReferenceNumber(),
            String.valueOf(costRequest.getCostRequestRevision()));
      }
    } else { // costRequestStatus == SWCostRequestStatus.READY_TO_VALIDATE
      lineEntity.setStatus(CostRequestStatus.READY_TO_VALIDATE);
    }
    recalculateCostRequestStatus(costRequest);
    costRequestRepository.save(costRequest);
    return costRequestLineMapper.toListSwCostRequestLine(costRequest.getLines());
  }

  @Transactional
  public SWCostRequestLine validateEstimationCostRequestLine(UUID uid, UUID lineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    CostRequestEntity costRequest = lineEntity.getCostRequest();
    if (lineEntity.isOutsourced()) {
      if (!lineEntity.getOutsourcingStatus().isEstimated()) {
        throw new GenericWithMessageException(
            "The line has been outsourced and is still pending procurement estimation.");
      }
    } else {
      costRequestValidator.validateCostRequestLineEstimation(lineEntity);
    }

    createGlobalConfigOtherCostLines(lineEntity);

    // Validate: set status to READY_TO_VALIDATE
    lineEntity.setReverted(false);
    lineEntity.setStatus(CostRequestStatus.READY_TO_VALIDATE);
    CostRequestLineEntity savedLine = costRequestLineRepository.save(lineEntity);

    recalculateCostRequestStatus(costRequest);
    costRequestRepository.save(costRequest);

    List<UserEntity> users = userHelper.getAllActiveUsersByRole(UserRole.PROJECT_MANAGER);
    if (CollectionUtils.isNotEmpty(users)) {
      List<String> emails = users.stream().map(UserEntity::getLogin).toList();
      String partNumber = lineEntity.getCustomerPartNumber();
      String partNumberRevision = lineEntity.getCustomerPartNumberRevision();
      String ref = costRequest.getCostRequestReferenceNumber();
      String rev = String.valueOf(costRequest.getCostRequestRevision());
      emailService.sendNewCostRequestLineReadyToValidateEmail(
          emails, partNumber, partNumberRevision, ref, rev);
    }

    return costRequestLineMapper.toSwCostRequestLine(savedLine);
  }

  private void createGlobalConfigOtherCostLines(CostRequestLineEntity lineEntity) {
    GlobalConfigEntity globalConfig = entityRetrievalHelper.getMustExistGlobalConfig();

    List<CostRequestFrozenShipmentLocationEntity> crFrozenShipmentLocationEntities =
        lineEntity.getCostRequest().getFrozenShipmentLocations();

    CurrencyEntity targetCurrency =
        entityRetrievalHelper.getMustExistCurrencyByCode(
            appConfigurationProperties.getTargetCurrencyCode());

    record CostLineTemplate(
        String name,
        BigDecimal value,
        OtherCostLineCalculationStrategy strategy,
        boolean editable) {}

    List<CostLineTemplate> templates = new ArrayList<>();
    templates.add(
        new CostLineTemplate(
            GlobalConstants.OTHER_COST_LINE_EXTRA_PROCESS_COST,
            BigDecimal.ZERO,
            OtherCostLineCalculationStrategy.MULTIPLIED_BY_QUANTITY,
            true));
    templates.add(
        new CostLineTemplate(
            GlobalConstants.OTHER_COST_LINE_PACKAGING_COST,
            globalConfig.getSmallPackagingCost(),
            OtherCostLineCalculationStrategy.DIVIDED_BY_QUANTITY,
            false));
    templates.add(
        new CostLineTemplate(
            GlobalConstants.OTHER_COST_LINE_LABOR_COST,
            globalConfig.getLaborCost(),
            OtherCostLineCalculationStrategy.MULTIPLIED_BY_QUANTITY,
            false));
    templates.add(
        new CostLineTemplate(
            GlobalConstants.OTHER_COST_LINE_OVERHEAD_COST,
            globalConfig.getOverheadCost(),
            OtherCostLineCalculationStrategy.MULTIPLIED_BY_QUANTITY,
            false));
    templates.add(
        new CostLineTemplate(
            GlobalConstants.OTHER_COST_LINE_INTERNAL_TRANSPORTATION,
            globalConfig.getInternalTransportation(),
            OtherCostLineCalculationStrategy.MULTIPLIED_BY_QUANTITY,
            false));
    templates.add(
        new CostLineTemplate(
            GlobalConstants.OTHER_COST_LINE_DEPRECIATION_COST,
            globalConfig.getDepreciationCost(),
            OtherCostLineCalculationStrategy.MULTIPLIED_BY_QUANTITY,
            false));
    templates.add(
        new CostLineTemplate(
            GlobalConstants.OTHER_COST_LINE_ADMINISTRATION_COST,
            globalConfig.getAdministrationCost(),
            OtherCostLineCalculationStrategy.MULTIPLIED_BY_QUANTITY,
            false));
    templates.add(
        new CostLineTemplate(
            GlobalConstants.OTHER_COST_LINE_STANDARD_JIGS_AND_FIXTURES,
            globalConfig.getStandardJigsAndFixturesCost(),
            OtherCostLineCalculationStrategy.MULTIPLIED_BY_QUANTITY,
            false));

    for (CostLineTemplate template : templates) {
      String name = template.name();
      boolean alreadyExists =
          lineEntity.getOtherCostLines().stream().anyMatch(l -> l.getName().equals(name));
      if (!alreadyExists) {
        OtherCostLineEntity otherCostLine = new OtherCostLineEntity();
        otherCostLine.setName(name);
        otherCostLine.setFixedLine(true);
        otherCostLine.setEditableLine(template.editable());
        otherCostLine.setCurrency(targetCurrency);
        otherCostLine.setCalculationStrategy(template.strategy());
        otherCostLine.setUnitCostInCurrency(template.value());
        if (name.equals(GlobalConstants.OTHER_COST_LINE_PACKAGING_COST)) {
          otherCostLine.setPackagingLine(true);
          otherCostLine.setPackagingSize(PackagingSize.SMALL);
        }
        lineEntity.addOtherCostLine(otherCostLine);
        otherCostLine.buildCalculatedFields(appConfigurationProperties.getTargetCurrencyCode());
      }
    }

    // Manage separately the shipment to customer lines
    if (crFrozenShipmentLocationEntities.isEmpty()) {
      if (lineEntity.getOtherCostLines().stream()
          .noneMatch(
              l ->
                  l.getName()
                      .equals(
                          GlobalConstants
                              .OTHER_COST_LINE_SHIPMENT_TO_CUSTOMER))) { // Avoid recreating if
        // reverted CR line
        OtherCostLineEntity otherCostLine = new OtherCostLineEntity();
        otherCostLine.setShipmentToCustomerLine(true);
        otherCostLine.setName(GlobalConstants.OTHER_COST_LINE_SHIPMENT_TO_CUSTOMER);
        otherCostLine.setFixedLine(true);
        otherCostLine.setEditableLine(true);
        otherCostLine.setCurrency(targetCurrency);
        otherCostLine.setCalculationStrategy(OtherCostLineCalculationStrategy.DIVIDED_BY_QUANTITY);
        otherCostLine.setUnitCostInCurrency(BigDecimal.ZERO);
        lineEntity.addOtherCostLine(otherCostLine);
        otherCostLine.buildCalculatedFields(targetCurrency.getCode());
      }
    } else {
      for (CostRequestFrozenShipmentLocationEntity crFrozenShipmentLocationEntity :
          crFrozenShipmentLocationEntities) {
        if (lineEntity.getOtherCostLines().stream()
            .noneMatch(
                l ->
                    l.getName().equals(GlobalConstants.OTHER_COST_LINE_SHIPMENT_TO_CUSTOMER)
                        && l.getShipmentLocation()
                            .equals(crFrozenShipmentLocationEntity.getShipmentLocation())
                        && l.getCurrency()
                            .getCode()
                            .equals(
                                crFrozenShipmentLocationEntity
                                    .getCurrencyCode()))) { // Avoid recreating if reverted CR line
          OtherCostLineEntity otherCostLine = new OtherCostLineEntity();
          otherCostLine.setShipmentToCustomerLine(true);
          otherCostLine.setName(GlobalConstants.OTHER_COST_LINE_SHIPMENT_TO_CUSTOMER);
          otherCostLine.setFixedLine(true);
          otherCostLine.setEditableLine(true);
          otherCostLine.setCurrency(
              entityRetrievalHelper.getMustExistCurrencyByCode(
                  crFrozenShipmentLocationEntity.getCurrencyCode()));
          otherCostLine.setShipmentLocation(crFrozenShipmentLocationEntity.getShipmentLocation());
          otherCostLine.setCalculationStrategy(
              OtherCostLineCalculationStrategy.DIVIDED_BY_QUANTITY);
          otherCostLine.setUnitCostInCurrency(BigDecimal.ZERO);
          lineEntity.addOtherCostLine(otherCostLine);
          otherCostLine.buildCalculatedFields(crFrozenShipmentLocationEntity.getCurrencyCode());
        }
      }
    }
  }

  @Transactional
  public void validateCostRequestLineForReadyForMarkup(UUID uid, UUID lineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    CostRequestEntity costRequest = lineEntity.getCostRequest();
    if (lineEntity.isOutsourced()) {
      if (!lineEntity.getOutsourcingStatus().isEstimated()) {
        throw new GenericWithMessageException(
            "The line has been outsourced and is still pending procurement estimation.");
      }
    } else {
      costRequestValidator.validateCostRequestLineReadyForMarkup(lineEntity);
    }

    // Validate: set status to READY_FOR_MARKUP
    lineEntity.setReverted(false);
    lineEntity.setStatus(CostRequestStatus.READY_FOR_MARKUP);
    CostRequestLineEntity savedLine = costRequestLineRepository.save(lineEntity);

    // Update the used processes usage counts
    if (CollectionUtils.isNotEmpty(lineEntity.getProcessLines())) {
      for (ProcessLineEntity processLineEntity : lineEntity.getProcessLines()) {
        processUsageCountRepository.incrementProcessUsageCount(processLineEntity.getProcess());
      }
    }

    // Update the parent CR total lines cost
    recalculateCostRequestTotalEstimatedLinesCost(savedLine);

    // Build the estimations per quantity
    buildCostRequestLineAllEstimationsPerQuantity(savedLine);

    recalculateCostRequestStatus(costRequest);
    costRequestRepository.save(costRequest);
  }

  private void buildCostRequestLineAllEstimationsPerQuantity(CostRequestLineEntity line) {
    buildMaterialLinesEstimationsPerQuantity(
        line.getQuantitiesAsList(),
        line.getOnlyMaterialLinesUsedForQuotation(),
        line.getCostingMethodType());
    buildProcessLinesEstimationsPerQuantity(line.getQuantitiesAsList(), line.getProcessLines());
    buildToolingCostLinesEstimationsPerQuantity(
        line.getQuantitiesAsList(), line.getToolingCostLines());
    buildOtherCostLinesEstimationsPerQuantity(line.getQuantitiesAsList(), line.getOtherCostLines());
  }

  private void buildMaterialLinesEstimationsPerQuantity(
      List<Integer> quantities,
      List<MaterialLineEntity> materialLines,
      CostingMethodType costingMethodType) {
    if (CollectionUtils.isNotEmpty(materialLines)) {
      for (MaterialLineEntity materialLine : materialLines) {
        for (Integer quantity : quantities) {
          MaterialLinePerCostRequestQuantityEntity entity =
              new MaterialLinePerCostRequestQuantityEntity();
          entity.setCostRequestQuantity(quantity);
          entity.setMaterialLine(materialLine);
          entity.setChosenMaterialSupplier(materialLine.getChosenMaterialSupplier());
          entity.buildCalculatedFields(
              costingMethodType, appConfigurationProperties.getTargetCurrencyCode());
          materialLine.addMaterialLinePerCostRequestQuantity(entity);
          if (materialLine.isHasMaterialSubstitute()) {
            MaterialLineEntity materialSubstitute = materialLine.getMaterialSubstitute();
            MaterialLinePerCostRequestQuantityEntity entityForSubstitute =
                new MaterialLinePerCostRequestQuantityEntity();
            entityForSubstitute.setCostRequestQuantity(quantity);
            entityForSubstitute.setMaterialLine(materialSubstitute);
            entityForSubstitute.setChosenMaterialSupplier(
                materialSubstitute.getChosenMaterialSupplier());
            entityForSubstitute.buildCalculatedFields(
                costingMethodType, appConfigurationProperties.getTargetCurrencyCode());
            materialSubstitute.addMaterialLinePerCostRequestQuantity(entityForSubstitute);
          }
        }
      }
    }
  }

  private void buildProcessLinesEstimationsPerQuantity(
      List<Integer> quantities, List<ProcessLineEntity> processLines) {
    if (CollectionUtils.isNotEmpty(processLines)) {
      for (ProcessLineEntity processLine : processLines) {
        for (Integer quantity : quantities) {
          ProcessLinePerCostRequestQuantityEntity entity =
              new ProcessLinePerCostRequestQuantityEntity();
          entity.setCostRequestQuantity(quantity);
          entity.setProcessLine(processLine);
          entity.buildCalculatedFields();
          processLine.addProcessLinePerCostRequestQuantity(entity);
        }
      }
    }
  }

  private void buildToolingCostLinesEstimationsPerQuantity(
      List<Integer> quantities, List<ToolingCostLineEntity> toolingCostLines) {
    if (CollectionUtils.isNotEmpty(toolingCostLines)) {
      for (ToolingCostLineEntity toolingCostLine : toolingCostLines) {
        for (Integer quantity : quantities) {
          ToolingCostLinePerCostRequestQuantityEntity entity =
              new ToolingCostLinePerCostRequestQuantityEntity();
          entity.setCostRequestQuantity(quantity);
          entity.setToolingCostLine(toolingCostLine);
          entity.buildCalculatedFields(appConfigurationProperties.getTargetCurrencyCode());
          toolingCostLine.addToolingCostLinePerCostRequestQuantity(entity);
        }
      }
    }
  }

  private void buildOtherCostLinesEstimationsPerQuantity(
      List<Integer> quantities, List<OtherCostLineEntity> lines) {
    if (CollectionUtils.isNotEmpty(lines)) {
      for (OtherCostLineEntity otherCostLine : lines) {
        for (Integer quantity : quantities) {
          OtherCostLinePerCostRequestQuantityEntity entity =
              new OtherCostLinePerCostRequestQuantityEntity();
          entity.setCostRequestQuantity(quantity);
          entity.setOtherCostLine(otherCostLine);
          entity.buildCalculatedFields(appConfigurationProperties.getTargetCurrencyCode());
          otherCostLine.addOtherCostLinePerCostRequestQuantity(entity);
        }
      }
    }
  }

  @Transactional
  public List<SWCostRequestLineCostingPerQuantity> retrieveCostRequestLineCosts(
      UUID uid, UUID lineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);

    List<SWCostRequestLineCostingPerQuantity> result = new ArrayList<>();

    SWCostRequestLineCostingPerQuantity baseEntry = new SWCostRequestLineCostingPerQuantity();
    baseEntry.setQuantity(1);
    baseEntry.setMaterialCostLines(retrieveCostRequestLineMaterialLines(lineEntity));
    baseEntry.setProcessCostLines(
        processLineMapper.toListSwProcessCostLine(lineEntity.getProcessLines()));
    baseEntry.setToolingCostLines(
        toolingCostLineMapper.toListSWToolingCostLine(lineEntity.getToolingCostLines()));
    if (!loggedUserDetailsService.hasRole(UserRole.ENGINEERING)) {
      baseEntry.setOtherCostLines(
          otherCostLineMapper.toListSWOtherCostLine(lineEntity.getOtherCostLines()));
    }

    result.add(baseEntry);

    if (lineEntity.getStatus().isEstimatedStatus()) {
      List<Integer> quantities = lineEntity.getQuantitiesAsList();
      for (int i = 0; i < quantities.size(); i++) {
        int quantity = quantities.get(i);
        if (quantity == 1) {
          continue; // already managed with baseEntry
        }
        SWCostRequestLineCostingPerQuantity perQuantityEntry =
            new SWCostRequestLineCostingPerQuantity();
        perQuantityEntry.setQuantity(quantity);
        perQuantityEntry.setMaterialCostLines(
            extractMaterialLinesPerQuantity(lineEntity.getOnlyMaterialLinesUsedForQuotation(), i));
        perQuantityEntry.setProcessCostLines(
            extractProcessCostLinesPerQuantity(lineEntity.getProcessLines(), i));
        perQuantityEntry.setToolingCostLines(
            extractToolingCostLinesPerQuantity(lineEntity.getToolingCostLines(), i));
        if (!loggedUserDetailsService.hasRole(UserRole.ENGINEERING)) {
          perQuantityEntry.setOtherCostLines(
              extractOtherCostLinesPerQuantity(lineEntity.getOtherCostLines(), i));
        }
        result.add(perQuantityEntry);
      }
    }

    return result;
  }

  private List<@Valid SWMaterialCostLine> extractMaterialLinesPerQuantity(
      List<MaterialLineEntity> lines, int i) {
    List<SWMaterialCostLine> perQuantityMaterialCostLines = new ArrayList<>();
    for (MaterialLineEntity materialLineEntity : lines) {
      MaterialLinePerCostRequestQuantityEntity mlPerQuantity =
          materialLineEntity.getMaterialLineForCostRequestQuantities().get(i);
      SWMaterialCostLine materialCostLine =
          materialLineMapper.toSwMaterialCostLinePerQuantity(materialLineEntity);
      materialCostLine.setQuantity(mlPerQuantity.getQuantity());
      materialCostLine.setUnitPurchasingPriceInSystemCurrency(
          mlPerQuantity.getUnitPurchasingPriceInSystemCurrency());
      materialCostLine.setTotalPurchasingPriceInSystemCurrency(
          mlPerQuantity.getTotalPurchasingPriceInSystemCurrency());
      materialCostLine.setChosenSupplierAndMoq(
          new SWChosenSupplierAndMoq()
              .chosenSupplier(
                  materialSupplierMapper.toSwMaterialSupplier(
                      mlPerQuantity.getChosenMaterialSupplier()))
              .chosenMoq(
                  new SWMaterialSupplierMoqLine()
                      .minimumOrderQuantity(mlPerQuantity.getMinimumOrderQuantity())
                      .unitPurchasingPriceInPurchasingCurrency(
                          mlPerQuantity.getUnitPurchasingPriceInPurchasingCurrency())
                      .leadTime(mlPerQuantity.getLeadTime())));
      if (materialLineEntity.isHasMaterialSubstitute()) {
        MaterialLinePerCostRequestQuantityEntity mlSubstitutePerQuantity =
            materialLineEntity
                .getMaterialSubstitute()
                .getMaterialLineForCostRequestQuantities()
                .get(i);
        // We get the material substitute existing
        SWMaterialSubstituteCostLine materialSubstituteCostLine =
            materialCostLine.getMaterialSubstitute();
        if (materialSubstituteCostLine != null) {
          materialSubstituteCostLine.setQuantity(mlSubstitutePerQuantity.getQuantity());
          materialSubstituteCostLine.setUnitPurchasingPriceInSystemCurrency(
              mlSubstitutePerQuantity.getUnitPurchasingPriceInSystemCurrency());
          materialSubstituteCostLine.setTotalPurchasingPriceInSystemCurrency(
              mlSubstitutePerQuantity.getTotalPurchasingPriceInSystemCurrency());
          materialSubstituteCostLine.setChosenSupplierAndMoq(
              new SWChosenSupplierAndMoq()
                  .chosenSupplier(
                      materialSupplierMapper.toSwMaterialSupplier(
                          mlSubstitutePerQuantity.getChosenMaterialSupplier()))
                  .chosenMoq(
                      new SWMaterialSupplierMoqLine()
                          .minimumOrderQuantity(mlSubstitutePerQuantity.getMinimumOrderQuantity())
                          .unitPurchasingPriceInPurchasingCurrency(
                              mlSubstitutePerQuantity.getUnitPurchasingPriceInPurchasingCurrency())
                          .leadTime(mlSubstitutePerQuantity.getLeadTime())));
        }
      }
      perQuantityMaterialCostLines.add(materialCostLine);
    }
    return perQuantityMaterialCostLines;
  }

  private List<SWProcessCostLine> extractProcessCostLinesPerQuantity(
      List<ProcessLineEntity> lines, int i) {
    List<SWProcessCostLine> perQuantityProcessCostLines = new ArrayList<>();
    for (ProcessLineEntity processLineEntity : lines) {
      ProcessLinePerCostRequestQuantityEntity perQuantity =
          processLineEntity.getProcessLineForCostRequestQuantities().get(i);
      SWProcessCostLine processCostLine = processLineMapper.toSwProcessCostLine(processLineEntity);
      processCostLine.setQuantity(perQuantity.getQuantity());
      processCostLine.setUnitCostInSystemCurrency(perQuantity.getUnitCostInSystemCurrency());
      processCostLine.setTotalCostInSystemCurrency(perQuantity.getTotalCostInSystemCurrency());
      perQuantityProcessCostLines.add(processCostLine);
    }
    return perQuantityProcessCostLines;
  }

  private List<SWToolingCostLine> extractToolingCostLinesPerQuantity(
      List<ToolingCostLineEntity> lines, int i) {
    List<SWToolingCostLine> perQuantityToolingCostLines = new ArrayList<>();
    for (ToolingCostLineEntity toolingCostLineEntity : lines) {
      ToolingCostLinePerCostRequestQuantityEntity perQuantity =
          toolingCostLineEntity.getToolingCostLineForCostRequestQuantities().get(i);
      SWToolingCostLine otherCostLine =
          toolingCostLineMapper.toSwToolingCostLine(toolingCostLineEntity);
      otherCostLine.setQuantity(perQuantity.getQuantity());
      otherCostLine.setUnitCostInCurrency(perQuantity.getUnitCostInCurrency());
      otherCostLine.setTotalCostInCurrency(perQuantity.getTotalCostInSystemCurrency());
      perQuantityToolingCostLines.add(otherCostLine);
    }
    return perQuantityToolingCostLines;
  }

  private List<SWOtherCostLine> extractOtherCostLinesPerQuantity(
      List<OtherCostLineEntity> lines, int i) {
    List<SWOtherCostLine> perQuantityOtherCostLines = new ArrayList<>();
    for (OtherCostLineEntity otherCostLineEntity : lines) {
      OtherCostLinePerCostRequestQuantityEntity perQuantity =
          otherCostLineEntity.getOtherCostLineForCostRequestQuantities().get(i);
      SWOtherCostLine otherCostLine = otherCostLineMapper.toSWOtherCostLine(otherCostLineEntity);
      otherCostLine.setUnitCostInCurrency(perQuantity.getUnitCostInCurrency());
      otherCostLine.setTotalCostInSystemCurrency(perQuantity.getTotalCostInSystemCurrency());
      perQuantityOtherCostLines.add(otherCostLine);
    }
    return perQuantityOtherCostLines;
  }

  private List<@Valid SWMaterialCostLine> retrieveCostRequestLineMaterialLines(
      CostRequestLineEntity lineEntity) {
    if (lineEntity.getStatus() == CostRequestStatus.PENDING_INFORMATION) {
      return materialLineMapper.toListSwMaterialCostLineFromDraft(
          lineEntity.getDraftMaterialLines());
    } else {
      return materialLineMapper.toListSwMaterialCostLineBase(lineEntity.getMaterialLines());
    }
  }

  @Transactional
  public List<SWProcessCostLine> createProcessCostLine(
      UUID uid, UUID lineUid, SWProcessCostLineCreate body) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    ProcessEntity process = entityRetrievalHelper.getMustExistProcessById(body.getProcessId());

    ProcessLineEntity processLine = new ProcessLineEntity();
    processLine.setProcess(process);
    processLine.setQuantity(body.getQuantity());
    processLine.setProcessCycleTimeInSeconds(body.getProcessCycleTimeInSeconds());
    lineEntity.addProcessLine(processLine);

    boolean isDyson = lineEntity.getCostRequest().getCustomer().isDyson();
    processLine.buildCalculatedFields();

    autoAddSetupProcessLineIfMissing(lineEntity, isDyson);

    return processLineMapper.toListSwProcessCostLine(lineEntity.getProcessLines());
  }

  @Transactional
  public List<SWProcessCostLine> updateProcessCostLine(
      UUID uid, UUID lineUid, UUID costingLineUid, SWProcessCostLineUpdate body) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    ProcessLineEntity processLine =
        lineEntity.getProcessLines().stream()
            .filter(l -> l.getProcessLineId().equals(costingLineUid))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Process line not found"));

    processLine.setQuantity(body.getQuantity());
    processLine.setProcessCycleTimeInSeconds(body.getProcessCycleTimeInSeconds());

    boolean isDyson = lineEntity.getCostRequest().getCustomer().isDyson();
    processLine.buildCalculatedFields();

    autoAddSetupProcessLineIfMissing(lineEntity, isDyson);

    return processLineMapper.toListSwProcessCostLine(lineEntity.getProcessLines());
  }

  @Transactional
  public List<SWProcessCostLine> deleteProcessCostLine(
      UUID uid, UUID lineUid, UUID costingLineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    ProcessLineEntity processLine =
        lineEntity.getProcessLines().stream()
            .filter(l -> l.getProcessLineId().equals(costingLineUid))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Process line not found"));

    lineEntity.removeProcessLine(processLine);

    boolean isDyson = lineEntity.getCostRequest().getCustomer().isDyson();
    autoAddSetupProcessLineIfMissing(lineEntity, isDyson);

    return processLineMapper.toListSwProcessCostLine(lineEntity.getProcessLines());
  }

  @Transactional
  public List<SWToolingCostLine> createToolingCostLine(
      UUID uid, UUID lineUid, SWToolingCostLineCreate body) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    CurrencyEntity targetCurrency =
        entityRetrievalHelper.getMustExistCurrencyByCode(
            appConfigurationProperties.getTargetCurrencyCode());

    ToolingCostLineEntity toolingCostLine = new ToolingCostLineEntity();
    toolingCostLine.setName(body.getDescription());
    toolingCostLine.setQuantity(body.getQuantity());
    toolingCostLine.setUnitCostInCurrency(body.getUnitCostInCurrency());
    toolingCostLine.setCurrency(targetCurrency);
    toolingCostLine.setToolingPartNumber(body.getToolingPartNumber());
    lineEntity.addToolingCostLine(toolingCostLine);
    toolingCostLine.buildCalculatedFields(appConfigurationProperties.getTargetCurrencyCode());

    return toolingCostLineMapper.toListSWToolingCostLine(lineEntity.getToolingCostLines());
  }

  @Transactional
  public List<SWToolingCostLine> updateToolingCostLine(
      UUID uid, UUID lineUid, UUID costingLineUid, SWToolingCostLineUpdate body) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    ToolingCostLineEntity toolingCostLine =
        lineEntity.getToolingCostLines().stream()
            .filter(l -> l.getToolingCostLineId().equals(costingLineUid))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Tooling cost line not found"));

    toolingCostLine.setName(body.getDescription());
    toolingCostLine.setQuantity(body.getQuantity());
    if (!toolingCostLine.isOutsourced()) {
      toolingCostLine.setUnitCostInCurrency(body.getUnitCostInCurrency());
    }
    toolingCostLine.setToolingPartNumber(body.getToolingPartNumber());
    toolingCostLine.buildCalculatedFields(appConfigurationProperties.getTargetCurrencyCode());

    return toolingCostLineMapper.toListSWToolingCostLine(lineEntity.getToolingCostLines());
  }

  @Transactional
  public List<SWToolingCostLine> deleteToolingCostLine(
      UUID uid, UUID lineUid, UUID costingLineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    ToolingCostLineEntity toolingCostLine =
        lineEntity.getToolingCostLines().stream()
            .filter(l -> l.getToolingCostLineId().equals(costingLineUid))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Tooling cost line not found"));

    lineEntity.removeToolingCostLine(toolingCostLine);

    return toolingCostLineMapper.toListSWToolingCostLine(lineEntity.getToolingCostLines());
  }

  @Transactional
  public List<SWOtherCostLine> createOtherCostLine(
      UUID uid, UUID lineUid, SWOtherCostLineCreate body) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);

    CurrencyEntity currency =
        entityRetrievalHelper.getMustExistCurrencyByCode(
            appConfigurationProperties.getTargetCurrencyCode());
    OtherCostLineEntity otherCostLine = new OtherCostLineEntity();
    otherCostLine.setName(body.getDescription());
    otherCostLine.setFixedLine(false);
    otherCostLine.setEditableLine(true);
    otherCostLine.setCurrency(currency);
    otherCostLine.setCalculationStrategy(
        enumMapper.asOtherCostLineCalculationStrategy(body.getCalculationStrategy()));
    otherCostLine.setUnitCostInCurrency(body.getUnitCostInCurrency());
    lineEntity.addOtherCostLine(otherCostLine);
    otherCostLine.buildCalculatedFields(appConfigurationProperties.getTargetCurrencyCode());

    return otherCostLineMapper.toListSWOtherCostLine(lineEntity.getOtherCostLines());
  }

  @Transactional
  public List<SWOtherCostLine> updateOtherCostLine(
      UUID uid, UUID lineUid, UUID costingLineUid, SWOtherCostLineUpdate body) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    OtherCostLineEntity otherCostLine =
        lineEntity.getOtherCostLines().stream()
            .filter(l -> l.getOtherCostLineId().equals(costingLineUid))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Other cost line not found"));
    otherCostLine.setName(body.getDescription());
    otherCostLine.setCalculationStrategy(
        enumMapper.asOtherCostLineCalculationStrategy(body.getCalculationStrategy()));
    otherCostLine.setPackagingSize(enumMapper.asPackagingSize(body.getPackagingSize()));
    otherCostLine.setUnitCostInCurrency(body.getUnitCostInCurrency());
    otherCostLine.buildCalculatedFields(appConfigurationProperties.getTargetCurrencyCode());

    return otherCostLineMapper.toListSWOtherCostLine(lineEntity.getOtherCostLines());
  }

  @Transactional
  public List<SWOtherCostLine> deleteOtherCostLine(UUID uid, UUID lineUid, UUID costingLineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    OtherCostLineEntity otherCostLine =
        lineEntity.getOtherCostLines().stream()
            .filter(l -> l.getOtherCostLineId().equals(costingLineUid))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Other cost line not found"));

    lineEntity.removeOtherCostLine(otherCostLine);

    return otherCostLineMapper.toListSWOtherCostLine(lineEntity.getOtherCostLines());
  }

  @Transactional
  public List<SWOtherCostLine> maskUnmaskOtherCostLine(
      UUID uid, UUID lineUid, UUID costingLineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    OtherCostLineEntity otherCostLine =
        lineEntity.getOtherCostLines().stream()
            .filter(l -> l.getOtherCostLineId().equals(costingLineUid))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Other cost line not found"));

    if (!otherCostLine.isShipmentToCustomerLine()) {
      throw new GenericWithMessageException(
          "Only shipment-to-customer lines can be masked", SWCustomErrorCode.GENERIC_ERROR);
    }

    otherCostLine.setMasked(!otherCostLine.isMasked());

    UUID shipmentLocationId = otherCostLine.getShipmentLocation().getShipmentLocationId();
    String currencyCode = otherCostLine.getCurrency().getCode();

    CostRequestEntity costRequest = lineEntity.getCostRequest();
    CostRequestFrozenShipmentLocationEntity frozenLocation =
        costRequest.getFrozenShipmentLocations().stream()
            .filter(
                fl ->
                    fl.getShipmentLocation().getShipmentLocationId().equals(shipmentLocationId)
                        && fl.getCurrencyCode().equals(currencyCode))
            .findFirst()
            .orElseThrow(
                () ->
                    new GenericWithMessageException(
                        "Matching frozen shipment location not found",
                        SWCustomErrorCode.GENERIC_ERROR));

    frozenLocation.setMasked(otherCostLine.isMasked());
    costRequestFrozenShipmentLocationRepository.save(frozenLocation);
    costRequestLineRepository.save(lineEntity);

    return otherCostLineMapper.toListSWOtherCostLine(lineEntity.getOtherCostLines());
  }

  @Transactional
  public List<SWMaterialCostLine> retrieveCostRequestLineMaterials(UUID uid, UUID lineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    if (lineEntity.getStatus().isPendingAndUpdatable()
        || (lineEntity.getStatus() == CostRequestStatus.ABORTED
            && lineEntity.getMaterialLines().isEmpty())) {
      // Second condition if to help as if aborted we do not know at what stage it was (before
      // READY TO/ ESTIMATE or after)
      return materialLineMapper.toListSwMaterialCostLineFromDraft(
          lineEntity.getDraftMaterialLines());
    } else {
      return materialLineMapper.toListSwMaterialCostLineBase(lineEntity.getMaterialLines());
    }
  }

  @Transactional
  public List<SWMaterialCostLine> createMaterialCostLine(
      UUID uid, UUID lineUid, SWMaterialCostLineCreate body) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);

    Optional<SupplierManufacturerEntity> optManufacturer =
        entityRetrievalHelper.getOptionalManufacturerByName(body.getManufacturerName());
    Optional<MaterialCategoryEntity> optCategory =
        entityRetrievalHelper.getOptionalCategoryByName(body.getCategoryName());
    Optional<UnitEntity> optUnit = entityRetrievalHelper.getOptionalUnitByName(body.getUnit());

    // Check if material exists in database
    Optional<MaterialEntity> existingMaterial;
    if (optManufacturer.isPresent() && optCategory.isPresent()) {
      // Case 1: manufacturer + category + part number
      existingMaterial =
          materialRepository
              .findFirstByManufacturerAndCategoryAndPartNumberAndArchivedFalse(
                  optManufacturer.get(), optCategory.get(), body.getManufacturerPartNumber())
              .stream()
              .findFirst();
    } else if (optManufacturer.isPresent()) {
      // Case 2: manufacturer + part number (no category)
      existingMaterial =
          materialRepository
              .findFirstByManufacturerAndPartNumberAndArchivedFalse(
                  optManufacturer.get(), body.getManufacturerPartNumber())
              .stream()
              .findFirst();
    } else {
      // Case 3: no manufacturer found — match on draftManufacturerName + part number
      existingMaterial =
          materialRepository
              .findFirstByDraftManufacturerNameAndPartNumberAndArchivedFalse(
                  body.getManufacturerName(), body.getManufacturerPartNumber())
              .stream()
              .findFirst();
    }

    if (lineEntity.getStatus().isPendingAndUpdatable()) {
      // Create draft material line with material information
      MaterialLineDraftEntity draft = new MaterialLineDraftEntity();
      draft.setCostRequestLine(lineEntity);
      draft.setManufacturer(optManufacturer.orElse(null));
      draft.setManufacturerPartNumber(body.getManufacturerPartNumber());
      draft.setMaterialType(MaterialType.fromValue(body.getMaterialType().getValue()));
      draft.setDescription(body.getDescription());
      draft.setCategory(optCategory.orElse(null));
      draft.setUnit(optUnit.orElse(null));
      draft.setQuantity(body.getQuantity());

      draft.setDraftManufacturerName(body.getManufacturerName());
      draft.setDraftCategoryName(body.getCategoryName());
      draft.setDraftUnitName(body.getUnit());

      lineEntity.addDraftMaterialLine(draft);

      costRequestValidator.validateNoDuplicateDraftMaterialLine(
          lineEntity.getDraftMaterialLines(),
          body.getManufacturerName(),
          body.getManufacturerPartNumber());

      lineEntity = costRequestLineRepository.save(lineEntity);

      return materialLineMapper.toListSwMaterialCostLineFromDraft(
          lineEntity.getDraftMaterialLines());
    } else {
      MaterialEntity material;
      if (existingMaterial.isPresent()) {
        // Reuse existing material
        material = existingMaterial.get();
      } else {
        // Create new material from draft information
        material = new MaterialEntity();
        material.setManufacturer(optManufacturer.orElse(null));
        material.setManufacturerPartNumber(body.getManufacturerPartNumber());
        material.setDescription(body.getDescription());
        material.setCategory(optCategory.orElse(null));
        material.setUnit(optUnit.orElse(null));
        material.setMaterialType(MaterialType.fromValue(body.getMaterialType().getValue()));

        material.setDraftManufacturerName(body.getManufacturerName());
        material.setDraftCategoryName(body.getCategoryName());
        material.setDraftUnitName(body.getUnit());

        String systemId = null;
        if (optManufacturer.isPresent() && optCategory.isPresent()) {
          // Generate system ID
          systemId =
              systemIdHelper.generateSystemId(
                  optManufacturer.get(), optCategory.get(), body.getManufacturerPartNumber());
        }
        material.setSystemId(systemId);
        // Set status to TO_BE_ESTIMATED since no suppliers are provided
        material.setStatus(MaterialStatus.TO_BE_ESTIMATED);
        material = materialRepository.save(material);
      }

      // Create MaterialLineEntity
      MaterialLineEntity materialLine = new MaterialLineEntity();
      materialLine.setMaterial(material);
      materialLine.setQuantity(body.getQuantity());
      lineEntity.addMaterialLine(materialLine);
      lineEntity = costRequestLineRepository.save(lineEntity);

      costRequestValidator.validateNoDuplicateMaterialLine(lineEntity.getMaterialLines(), material);

      materialLine.buildCalculatedFields(
          lineEntity.getCostingMethodType(), appConfigurationProperties.getTargetCurrencyCode());

      return materialLineMapper.toListSwMaterialCostLineFromDraft(
          lineEntity.getDraftMaterialLines());
    }
  }

  @Transactional
  public List<SWMaterialCostLine> markOrUnmarkUsedMaterialCostLineForQuote(
      UUID uid, UUID lineUid, UUID costingLineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    if (!lineEntity.getStatus().isPendingAndUpdatable()) {
      throw new GenericWithMessageException(
          "The cost request line already went for estimation", SWCustomErrorCode.GENERIC_ERROR);
    }
    MaterialLineDraftEntity draftMaterialLine =
        lineEntity.getDraftMaterialLines().stream()
            .filter(l -> l.getMaterialLineDraftId().equals(costingLineUid))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Material cost line not found"));

    draftMaterialLine.setMarkedNotUsedForQuote(!draftMaterialLine.isMarkedNotUsedForQuote());
    return materialLineMapper.toListSwMaterialCostLineFromDraft(lineEntity.getDraftMaterialLines());
  }

  @Transactional
  public List<SWMaterialCostLine> chooseMaterialCostLineSupplierOfMaterialSubstitute(
      UUID uid, UUID lineUid, UUID costingLineUid, @Valid SWMaterialCostLineSupplierUpdate body) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    MaterialLineEntity materialLine =
        lineEntity.getMaterialLines().stream()
            .filter(l -> l.getMaterialLineId().equals(costingLineUid))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Material cost line not found"));
    if (materialLine.getMaterialSubstitute() == null) {
      throw new GenericWithMessageException(
          "No material substitute found for this material line", SWCustomErrorCode.GENERIC_ERROR);
    }
    MaterialSupplierEntity newMaterialSupplier =
        entityRetrievalHelper.getMustExistMaterialSupplierById(body.getMaterialSupplierId());
    materialLine.getMaterialSubstitute().setChosenMaterialSupplier(newMaterialSupplier);
    materialLine
        .getMaterialSubstitute()
        .buildCalculatedFields(
            lineEntity.getCostingMethodType(), appConfigurationProperties.getTargetCurrencyCode());
    return materialLineMapper.toListSwMaterialCostLineBase(lineEntity.getMaterialLines());
  }

  @Transactional
  public List<SWMaterialCostLine> chooseMaterialCostLineSupplier(
      UUID uid, UUID lineUid, UUID costingLineUid, @Valid SWMaterialCostLineSupplierUpdate body) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    MaterialLineEntity materialLine =
        lineEntity.getMaterialLines().stream()
            .filter(l -> l.getMaterialLineId().equals(costingLineUid))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Material cost line not found"));
    MaterialSupplierEntity newMaterialSupplier =
        entityRetrievalHelper.getMustExistMaterialSupplierById(body.getMaterialSupplierId());
    materialLine.setChosenMaterialSupplier(newMaterialSupplier);
    materialLine.buildCalculatedFields(
        lineEntity.getCostingMethodType(), appConfigurationProperties.getTargetCurrencyCode());
    return materialLineMapper.toListSwMaterialCostLineBase(lineEntity.getMaterialLines());
  }

  @Transactional
  public List<SWMaterialCostLine> updateMaterialCostLine(
      UUID uid, UUID lineUid, UUID costingLineUid, SWMaterialCostLineUpdate body) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);

    // Get manufacturer and category
    Optional<SupplierManufacturerEntity> optManufacturer =
        entityRetrievalHelper.getOptionalManufacturerByName(body.getManufacturerName());
    Optional<MaterialCategoryEntity> optCategory =
        entityRetrievalHelper.getOptionalCategoryByName(body.getCategoryName());
    Optional<UnitEntity> optUnit = entityRetrievalHelper.getOptionalUnitByName(body.getUnit());

    // Check if material exists in database
    Optional<MaterialEntity> existingMaterial;
    if (optManufacturer.isPresent() && optCategory.isPresent()) {
      // Case 1: manufacturer + category + part number
      existingMaterial =
          materialRepository
              .findFirstByManufacturerAndCategoryAndPartNumberAndArchivedFalse(
                  optManufacturer.get(), optCategory.get(), body.getManufacturerPartNumber())
              .stream()
              .findFirst();
    } else if (optManufacturer.isPresent()) {
      // Case 2: manufacturer + part number (no category)
      existingMaterial =
          materialRepository
              .findFirstByManufacturerAndPartNumberAndArchivedFalse(
                  optManufacturer.get(), body.getManufacturerPartNumber())
              .stream()
              .findFirst();
    } else {
      // Case 3: no manufacturer found — match on draftManufacturerName + part number
      existingMaterial =
          materialRepository
              .findFirstByDraftManufacturerNameAndPartNumberAndArchivedFalse(
                  body.getManufacturerName(), body.getManufacturerPartNumber())
              .stream()
              .findFirst();
    }

    if (lineEntity.getStatus().isPendingAndUpdatable()) {
      MaterialLineDraftEntity draftMaterialLine =
          lineEntity.getDraftMaterialLines().stream()
              .filter(l -> l.getMaterialLineDraftId().equals(costingLineUid))
              .findFirst()
              .orElseThrow(() -> new EntityNotFoundException("Material cost line not found"));
      draftMaterialLine.setManufacturer(optManufacturer.orElse(null));
      draftMaterialLine.setManufacturerPartNumber(body.getManufacturerPartNumber());
      draftMaterialLine.setMaterialType(MaterialType.fromValue(body.getMaterialType().getValue()));
      draftMaterialLine.setDescription(body.getDescription());
      draftMaterialLine.setCategory(optCategory.orElse(null));
      draftMaterialLine.setUnit(optUnit.orElse(null));
      draftMaterialLine.setMissingData(null);
      draftMaterialLine.setQuantity(body.getQuantity());

      draftMaterialLine.setDraftManufacturerName(body.getManufacturerName());
      draftMaterialLine.setDraftCategoryName(body.getCategoryName());
      draftMaterialLine.setDraftUnitName(body.getUnit());

      costRequestValidator.validateNoDuplicateDraftMaterialLine(
          lineEntity.getDraftMaterialLines(),
          body.getManufacturerName(),
          body.getManufacturerPartNumber());

      lineEntity = costRequestLineRepository.save(lineEntity);

      return materialLineMapper.toListSwMaterialCostLineFromDraft(
          lineEntity.getDraftMaterialLines());
    } else {
      MaterialLineEntity materialLine =
          lineEntity.getMaterialLines().stream()
              .filter(l -> l.getMaterialLineId().equals(costingLineUid))
              .findFirst()
              .orElseThrow(() -> new EntityNotFoundException("Material cost line not found"));

      MaterialEntity material;
      if (existingMaterial.isPresent()) {
        // Reuse existing material
        material = existingMaterial.get();
      } else {
        // Create new material from draft information
        material = new MaterialEntity();
        material.setManufacturer(optManufacturer.orElse(null));
        material.setManufacturerPartNumber(body.getManufacturerPartNumber());
        material.setDescription(body.getDescription());
        material.setCategory(optCategory.orElse(null));
        material.setUnit(optUnit.orElse(null));
        material.setMaterialType(MaterialType.fromValue(body.getMaterialType().getValue()));

        material.setDraftManufacturerName(body.getManufacturerName());
        material.setDraftCategoryName(body.getCategoryName());
        material.setDraftUnitName(body.getUnit());

        String systemId = null;
        if (optManufacturer.isPresent() && optCategory.isPresent()) {
          // Generate system ID
          systemId =
              systemIdHelper.generateSystemId(
                  optManufacturer.get(), optCategory.get(), body.getManufacturerPartNumber());
        }
        material.setSystemId(systemId);
        // Set status to TO_BE_ESTIMATED since no suppliers are provided
        material.setStatus(MaterialStatus.TO_BE_ESTIMATED);
        material = materialRepository.save(material);
      }

      // Update MaterialLineEntity
      materialLine.setMaterial(material);
      materialLine.setQuantity(body.getQuantity());

      costRequestValidator.validateNoDuplicateMaterialLine(lineEntity.getMaterialLines(), material);

      materialLine.buildCalculatedFields(
          lineEntity.getCostingMethodType(), appConfigurationProperties.getTargetCurrencyCode());

      return materialLineMapper.toListSwMaterialCostLineBase(lineEntity.getMaterialLines());
    }
  }

  @Transactional
  public List<SWMaterialCostLine> deleteMaterialCostLine(
      UUID uid, UUID lineUid, UUID costingLineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    if (lineEntity.getStatus().isPendingAndUpdatable()) {
      MaterialLineDraftEntity draftMaterialLine =
          lineEntity.getDraftMaterialLines().stream()
              .filter(l -> l.getMaterialLineDraftId().equals(costingLineUid))
              .findFirst()
              .orElseThrow(() -> new EntityNotFoundException("Material cost line not found"));
      lineEntity.removeDraftMaterialLine(draftMaterialLine);
      lineEntity = costRequestLineRepository.save(lineEntity);
      return materialLineMapper.toListSwMaterialCostLineFromDraft(
          lineEntity.getDraftMaterialLines());
    } else {
      MaterialLineEntity materialLine =
          lineEntity.getMaterialLines().stream()
              .filter(l -> l.getMaterialLineId().equals(costingLineUid))
              .findFirst()
              .orElseThrow(() -> new EntityNotFoundException("Material cost line not found"));
      lineEntity.removeMaterialLine(materialLine);
      return materialLineMapper.toListSwMaterialCostLineBase(lineEntity.getMaterialLines());
    }
  }

  @Transactional
  public SWMaterialCostLine retrieveMaterialLineMaterialSubstitute(
      UUID uid, UUID lineUid, UUID costingLineUid) {
    entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    MaterialLineEntity materialLine =
        entityRetrievalHelper.getMustExistMaterialLineById(costingLineUid);
    return materialLineMapper.toSwMaterialCostLineBase(materialLine);
  }

  @Transactional
  public SWMaterialCostLine createMaterialSubstituteMaterialLine(
      UUID uid, UUID lineUid, UUID costingLineUid, SWMaterialSubstituteCreate body) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    MaterialLineEntity materialLine =
        lineEntity.getMaterialLines().stream()
            .filter(l -> l.getMaterialLineId().equals(costingLineUid))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Material cost line not found"));
    MaterialEntity materialSubstitute =
        entityRetrievalHelper.getMustExistMaterialById(body.getMaterialSubstituteId());
    if (materialSubstitute == null || !materialSubstitute.getStatus().isEstimated()) {
      throw new GenericWithMessageException(
          "A material substitute must be an estimated material", SWCustomErrorCode.GENERIC_ERROR);
    }
    List<MaterialLineEntity> materialLines = lineEntity.getMaterialLines();
    if (CollectionUtils.isNotEmpty(materialLines)) {
      for (MaterialLineEntity materialLineEntity : materialLines) {
        materialValidator.checkDuplicateMaterial(
            materialLineEntity.getMaterial(), materialSubstitute);
        if (materialLineEntity.isHasMaterialSubstitute()) {
          materialValidator.checkDuplicateMaterial(
              materialLineEntity.getMaterialSubstitute().getMaterial(), materialSubstitute);
        }
      }
    }
    MaterialLineEntity substitute =
        materialLine.getMaterialSubstitute() != null
            ? materialLine.getMaterialSubstitute()
            : new MaterialLineEntity();
    substitute.setMaterial(materialSubstitute);
    substitute.setQuantity(body.getQuantity());
    substitute.setCostRequestLine(lineEntity);
    substitute.setSubstituteMaterial(true);
    substitute.setIndexId(0);
    substitute.buildCalculatedFields(
        lineEntity.getCostingMethodType(), appConfigurationProperties.getTargetCurrencyCode());

    materialLine.setMaterialSubstitute(substitute);
    materialLine.setHasMaterialSubstitute(true);
    materialLine.buildCalculatedFields(
        lineEntity.getCostingMethodType(), appConfigurationProperties.getTargetCurrencyCode());
    costRequestLineRepository.save(lineEntity);

    return materialLineMapper.toSwMaterialCostLineBase(
        entityRetrievalHelper.getMustExistMaterialLineById(costingLineUid));
  }

  @Transactional
  public SWMaterialCostLine deleteMaterialSubstituteMaterialLine(
      UUID uid, UUID lineUid, UUID costingLineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    MaterialLineEntity materialLine =
        lineEntity.getMaterialLines().stream()
            .filter(l -> l.getMaterialLineId().equals(costingLineUid))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Material cost line not found"));

    if (materialLine.getMaterialSubstitute() == null) {
      throw new GenericWithMessageException(
          "No material substitute found for this material line", SWCustomErrorCode.GENERIC_ERROR);
    }

    materialLine.setMaterialSubstitute(null);
    materialLine.setHasMaterialSubstitute(false);
    materialLine.buildCalculatedFields(
        lineEntity.getCostingMethodType(), appConfigurationProperties.getTargetCurrencyCode());
    costRequestLineRepository.save(lineEntity);

    return materialLineMapper.toSwMaterialCostLineBase(
        entityRetrievalHelper.getMustExistMaterialLineById(costingLineUid));
  }

  private void autoAddSetupProcessLineIfMissing(CostRequestLineEntity lineEntity, boolean isDyson) {
    if (!lineEntity.getProcessLines().isEmpty()
        && lineEntity.getProcessLines().stream()
            .noneMatch(pl -> pl.getProcess().isSetupProcess())) {
      ProcessEntity setupProcess = entityRetrievalHelper.getMustExistSetupProcess();
      ProcessLineEntity setupProcessLine = new ProcessLineEntity();
      setupProcessLine.setProcess(setupProcess);
      setupProcessLine.setQuantity(BigDecimal.ONE);
      setupProcessLine.setProcessCycleTimeInSeconds(
          isDyson
              ? setupProcess.getDysonCycleTimeInSeconds()
              : setupProcess.getNonDysonCycleTimeInSeconds());
      lineEntity.addProcessLine(setupProcessLine);
      setupProcessLine.buildCalculatedFields();
    }
  }

  private void recalculateCostRequestStatus(CostRequestEntity costRequest) {
    List<CostRequestLineEntity> lines = costRequest.getLines();
    if (CollectionUtils.isEmpty(lines)) {
      return;
    }

    // Rule 1: all lines aborted
    if (lines.stream().allMatch(l -> l.getStatus() == CostRequestStatus.ABORTED)) {
      costRequest.setStatus(CostRequestStatus.ABORTED);
      return;
    }

    List<CostRequestLineEntity> activeLines =
        lines.stream().filter(l -> l.getStatus() != CostRequestStatus.ABORTED).toList();

    // Rule 2a-pre: all active lines PRICE_APPROVED
    if (activeLines.stream().allMatch(l -> l.getStatus() == CostRequestStatus.PRICE_APPROVED)) {
      costRequest.setStatus(CostRequestStatus.READY_TO_QUOTE);
      return;
    }

    // Rule 2a: any active line reverted
    if (activeLines.stream().anyMatch(CostRequestLineEntity::isReverted)) {
      costRequest.setStatus(CostRequestStatus.PENDING_REESTIMATION);
      return;
    }

    // Rule 2b: any active line READY_FOR_REVIEW
    if (activeLines.stream().anyMatch(l -> l.getStatus() == CostRequestStatus.READY_FOR_REVIEW)) {
      costRequest.setStatus(CostRequestStatus.READY_FOR_REVIEW);
      return;
    }

    // Rule 2c: any active line READY_TO_ESTIMATE
    if (activeLines.stream().anyMatch(l -> l.getStatus() == CostRequestStatus.READY_TO_ESTIMATE)) {
      costRequest.setStatus(CostRequestStatus.READY_TO_ESTIMATE);
      return;
    }

    // Rule 2d: any active line READY_TO_VALIDATE
    if (activeLines.stream().anyMatch(l -> l.getStatus() == CostRequestStatus.READY_TO_VALIDATE)) {
      costRequest.setStatus(CostRequestStatus.READY_TO_VALIDATE);
      return;
    }

    // Rule 2e: any active line at markup stage
    if (activeLines.stream()
        .anyMatch(
            l ->
                l.getStatus() == CostRequestStatus.READY_FOR_MARKUP
                    || l.getStatus() == CostRequestStatus.PENDING_APPROVAL
                    || l.getStatus() == CostRequestStatus.PRICE_REJECTED
                    || l.getStatus() == CostRequestStatus.PRICE_APPROVED)) {
      costRequest.setStatus(CostRequestStatus.READY_FOR_MARKUP);
    }
  }

  @Transactional
  public List<SWEstimationDetailsPerShipmentToCustomer> retrieveCostRequestLineEstimationDetails(
      UUID uid, UUID lineUid, String currencyCode) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    CurrencyEntity currency =
        entityRetrievalHelper.getMustExistCurrencyByCode(
            appConfigurationProperties.getTargetCurrencyCode());
    BigDecimal targetExchangeRate = currency.findExchangeRate(currencyCode);
    if (!lineEntity.getStatus().isEstimatedStatus()) {
      throw new GenericWithMessageException("Cannot retrieve estimation details for line");
    }
    GlobalConfigEntity globalConfig = entityRetrievalHelper.getMustExistGlobalConfig();
    BigDecimal additionalProcessRatePerMethodType =
        costRequestLineHelper.resolveAdditionalProcessRate(lineEntity, globalConfig);
    BigDecimal materialYieldPercentage =
        costRequestLineHelper.resolveYield(lineEntity, globalConfig);

    return buildEstimationDetailsEntries(
        lineEntity,
        currency,
        targetExchangeRate,
        additionalProcessRatePerMethodType,
        materialYieldPercentage);
  }

  @Transactional
  public void validateCostRequestLinePrice(UUID uid, UUID lineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    // Verification if can continue
    if (!lineEntity.getStatus().isMarkupUpdatable()) {
      throw new GenericWithMessageException(
          "Only request for quotation lines in READY FOR MARKUP status can validate price");
    }
    if (lineEntity.getMarkup() == null) {
      throw new GenericWithMessageException("Markup is required and must be applied");
    }
    if (lineEntity.getToolingStrategy() == ToolingStrategy.SEPARATED
        && lineEntity.getToolingMarkup() == null) {
      throw new GenericWithMessageException(
          "Tooling markup is required for SEPARATED tooling strategy");
    }
    GlobalConfigEntity globalConfigEntity = entityRetrievalHelper.getMustExistGlobalConfig();
    if (globalConfigEntity.getMarkupApprovalStrategy() == null) {
      throw new GenericWithMessageException(
          "System markup approval strategy is not set. Please contact you administrator");
    }
    if (globalConfigEntity
        .getMarkupApprovalStrategy()
        .equals(MarkupApprovalStrategy.FOR_ALL_QUOTATIONS)) {
      lineEntity.setStatus(CostRequestStatus.PENDING_APPROVAL);
    } else if (globalConfigEntity.getBaseMarkup() == null
        || globalConfigEntity.getMarkupRange() == null) {
      throw new GenericWithMessageException(
          "Custom markup rules are not set. Please contact you administrator");
    } else {
      BigDecimal baseMarkup = globalConfigEntity.getBaseMarkup();
      BigDecimal markupRange = globalConfigEntity.getMarkupRange();
      BigDecimal maxMarkup = baseMarkup.add(markupRange);
      BigDecimal minMarkup = baseMarkup.subtract(markupRange);
      BigDecimal markup = lineEntity.getMarkup();
      BigDecimal toolingMarkup = lineEntity.getToolingMarkup();

      boolean isMarkupOutOfRange =
          markup.compareTo(minMarkup) < 0 || markup.compareTo(maxMarkup) > 0;
      boolean isToolingMarkupOutOfRange =
          lineEntity.getToolingStrategy() == ToolingStrategy.SEPARATED
              && (toolingMarkup.compareTo(minMarkup) < 0 || toolingMarkup.compareTo(maxMarkup) > 0);
      if (isMarkupOutOfRange || isToolingMarkupOutOfRange) {
        lineEntity.setStatus(CostRequestStatus.PENDING_APPROVAL);
      } else {
        lineEntity.setStatus(CostRequestStatus.PRICE_APPROVED);
        lineEntity.setPriceRejectReason(null);
      }
    }
    boolean isPendingApproval = lineEntity.getStatus() == CostRequestStatus.PENDING_APPROVAL;
    lineEntity = costRequestLineRepository.save(lineEntity);

    if (isPendingApproval) {
      List<UserEntity> managers = userHelper.getAllActiveUsersByRole(UserRole.MANAGEMENT);
      if (CollectionUtils.isNotEmpty(managers)) {
        CostRequestEntity costRequest = lineEntity.getCostRequest();
        List<String> emails = managers.stream().map(UserEntity::getLogin).toList();
        emailService.sendCostRequestLinePendingApprovalEmail(
            emails,
            lineEntity.getCustomerPartNumber(),
            lineEntity.getCustomerPartNumberRevision(),
            costRequest.getCostRequestReferenceNumber(),
            String.valueOf(costRequest.getCostRequestRevision()));
      }
    }

    CostRequestEntity costRequest = lineEntity.getCostRequest();
    if (costRequest.checkIfAllLinesHaveTheStatusesOrAreAborted(
        List.of(CostRequestStatus.PRICE_APPROVED, CostRequestStatus.READY_TO_QUOTE))) {
      costRequest.setStatus(CostRequestStatus.READY_TO_QUOTE);
      costRequestHelper.setStatusAllNonAbortedLinesOfCostRequestStatus(
          costRequest, CostRequestStatus.READY_TO_QUOTE);
      costRequestRepository.save(costRequest);
    }
  }

  @Transactional
  public SWCostRequestLine approvePriceByManagement(UUID uid, UUID lineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    if (!lineEntity.getStatus().equals(CostRequestStatus.PENDING_APPROVAL)) {
      throw new GenericWithMessageException(
          "Only request for quotation lines in PENDING APPROVAL status can be approved",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    lineEntity.setStatus(CostRequestStatus.PRICE_APPROVED);
    lineEntity.setPriceRejectReason(null);
    lineEntity = costRequestLineRepository.save(lineEntity);

    List<UserEntity> projectManagers = userHelper.getAllActiveUsersByRole(UserRole.PROJECT_MANAGER);
    if (CollectionUtils.isNotEmpty(projectManagers)) {
      CostRequestEntity costRequest = lineEntity.getCostRequest();
      List<String> emails = projectManagers.stream().map(UserEntity::getLogin).toList();
      emailService.sendCostRequestLinePriceApprovedEmail(
          emails,
          lineEntity.getCustomerPartNumber(),
          lineEntity.getCustomerPartNumberRevision(),
          costRequest.getCostRequestReferenceNumber(),
          String.valueOf(costRequest.getCostRequestRevision()));
    }

    CostRequestEntity costRequest = lineEntity.getCostRequest();
    if (costRequest.checkIfAllLinesHaveTheStatusesOrAreAborted(
        List.of(CostRequestStatus.PRICE_APPROVED, CostRequestStatus.READY_TO_QUOTE))) {
      costRequest.setStatus(CostRequestStatus.READY_TO_QUOTE);
      costRequestHelper.setStatusAllNonAbortedLinesOfCostRequestStatus(
          costRequest, CostRequestStatus.READY_TO_QUOTE);
      // Archive all data for the cost request
      costRequestRepository.save(costRequest);
    }
    return costRequestLineMapper.toSwCostRequestLine(lineEntity);
  }

  @Transactional
  public SWCostRequestLine rejectPriceByManagement(UUID uid, UUID lineUid, SWRejectBody body) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    if (!lineEntity.getStatus().equals(CostRequestStatus.PENDING_APPROVAL)) {
      throw new GenericWithMessageException(
          "Only request for quotation lines in PENDING APPROVAL status can be rejected",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    lineEntity.setStatus(CostRequestStatus.PRICE_REJECTED);
    lineEntity.setPriceRejectReason(body.getReason());
    lineEntity = costRequestLineRepository.save(lineEntity);

    List<UserEntity> projectManagers = userHelper.getAllActiveUsersByRole(UserRole.PROJECT_MANAGER);
    if (CollectionUtils.isNotEmpty(projectManagers)) {
      CostRequestEntity costRequest = lineEntity.getCostRequest();
      List<String> emails = projectManagers.stream().map(UserEntity::getLogin).toList();
      emailService.sendCostRequestLinePriceRejectedEmail(
          emails,
          lineEntity.getCustomerPartNumber(),
          lineEntity.getCustomerPartNumberRevision(),
          costRequest.getCostRequestReferenceNumber(),
          String.valueOf(costRequest.getCostRequestRevision()),
          body.getReason());
    }

    return costRequestLineMapper.toSwCostRequestLine(lineEntity);
  }

  @Transactional
  public void deleteCostRequestLine(UUID uid, UUID lineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    if (!lineEntity.getStatus().isDeletable()) {
      throw new GenericWithMessageException(
          "Only request for quotation lines in PENDING INFORMATION or READY FOR REVIEW status can be deleted",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    CostRequestEntity costRequest = lineEntity.getCostRequest();
    costRequest.removeLine(lineEntity);
    recalculateCostRequestStatus(costRequest);
    costRequestRepository.save(costRequest);
  }

  @Transactional
  public SWCostRequestLine abortCostRequestLine(UUID uid, UUID lineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    if (lineEntity.getStatus().equals(CostRequestStatus.ABORTED)) {
      throw new GenericWithMessageException(
          "Cost request line is already aborted", SWCustomErrorCode.GENERIC_ERROR);
    }
    lineEntity.setStatus(CostRequestStatus.ABORTED);
    lineEntity = costRequestLineRepository.save(lineEntity);

    CostRequestEntity costRequest = lineEntity.getCostRequest();
    recalculateCostRequestStatus(costRequest);
    if (costRequest.getStatus() == CostRequestStatus.ABORTED) {
      costRequestArchivingHelper.archiveCostRequestDataFreeze(costRequest);
    }
    costRequestRepository.save(costRequest);
    return costRequestLineMapper.toSwCostRequestLine(lineEntity);
  }

  @Transactional
  public List<SWEstimationDetailsPerShipmentToCustomer> setCostRequestLineMarkup(
      UUID uid, UUID lineUid, String currencyCode, BigDecimal markup) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    if (!lineEntity.getStatus().isMarkupUpdatable()) {
      throw new GenericWithMessageException(
          "Only request for quotation lines in READY FOR MARKUP status can have markup applied",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    lineEntity.setMarkup(markup);

    GlobalConfigEntity globalConfig = entityRetrievalHelper.getMustExistGlobalConfig();

    costRequestLineRepository.save(lineEntity);

    BigDecimal additionalProcessRatePerMethodType =
        costRequestLineHelper.resolveAdditionalProcessRate(lineEntity, globalConfig);
    BigDecimal materialYieldPercentage =
        costRequestLineHelper.resolveYield(lineEntity, globalConfig);

    CurrencyEntity targetCurrency = entityRetrievalHelper.getMustExistCurrencyByCode(currencyCode);

    return buildEstimationDetailsEntries(
        lineEntity,
        targetCurrency,
        BigDecimal.ONE,
        additionalProcessRatePerMethodType,
        materialYieldPercentage);
  }

  @Transactional
  public List<SWEstimationDetailsPerShipmentToCustomer> setCostRequestLineToolingMarkup(
      UUID uid, UUID lineUid, String currencyCode, BigDecimal markup) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    if (!lineEntity.getStatus().isMarkupUpdatable()) {
      throw new GenericWithMessageException(
          "Only request for quotation lines in READY FOR MARKUP status can have markup applied",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    lineEntity.setToolingMarkup(markup);

    GlobalConfigEntity globalConfig = entityRetrievalHelper.getMustExistGlobalConfig();

    costRequestLineRepository.save(lineEntity);

    BigDecimal additionalProcessRatePerMethodType =
        costRequestLineHelper.resolveAdditionalProcessRate(lineEntity, globalConfig);
    BigDecimal materialYieldPercentage =
        costRequestLineHelper.resolveYield(lineEntity, globalConfig);

    CurrencyEntity targetCurrency = entityRetrievalHelper.getMustExistCurrencyByCode(currencyCode);

    return buildEstimationDetailsEntries(
        lineEntity,
        targetCurrency,
        BigDecimal.ONE,
        additionalProcessRatePerMethodType,
        materialYieldPercentage);
  }

  @Transactional
  public SWCostRequestLine setCostRequestLineToolingStrategy(
      UUID uid, UUID lineUid, SWToolingStrategy strategy) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    if (!lineEntity.getStatus().isMarkupUpdatable()) {
      throw new GenericWithMessageException(
          "Only request for quotation lines in READY FOR MARKUP status can change the tooling strategy",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    lineEntity.setToolingStrategy(enumMapper.asToolingStrategy(strategy));
    lineEntity = costRequestLineRepository.save(lineEntity);

    return costRequestLineMapper.toSwCostRequestLine(lineEntity);
  }

  private List<SWEstimationDetailsPerShipmentToCustomer> buildEstimationDetailsEntries(
      CostRequestLineEntity lineEntity,
      CurrencyEntity targetCurrency,
      BigDecimal targetExchangeRate,
      BigDecimal additionalProcessRatePerMethodType,
      BigDecimal materialYieldPercentage) {
    BigDecimal lineMarkup = lineEntity.getMarkup();
    BigDecimal lineToolingMarkup = lineEntity.getToolingMarkup();
    // markup is a percentage (e.g. 10 = 10%), so multiplier = 1 + markup/100
    BigDecimal markupMultiplier =
        lineMarkup != null
            ? BigDecimal.ONE.add(lineMarkup.divide(new BigDecimal(100), 6, RoundingMode.HALF_UP))
            : null;
    // markup is a percentage (e.g. 10 = 10%), so multiplier = 1 + markup/100
    BigDecimal toolingMarkupMultiplier =
        lineToolingMarkup != null
            ? BigDecimal.ONE.add(
                lineToolingMarkup.divide(new BigDecimal(100), 6, RoundingMode.HALF_UP))
            : null;

    List<SWEstimationDetailsPerShipmentToCustomer> swEstimationDetailsPerShipmentToCustomers =
        new ArrayList<>();

    if (CollectionUtils.isNotEmpty(lineEntity.getCostRequest().getFrozenShipmentLocations())) {
      List<OtherCostLineShipmentLocationInfos> shipmentLocations =
          lineEntity.getOtherCostLines().stream()
              .filter(ocl -> !ocl.isMasked())
              .filter(OtherCostLineEntity::isShipmentToCustomerLine)
              .map(
                  ocl ->
                      new OtherCostLineShipmentLocationInfos(
                          ocl.getOtherCostLineId(), ocl.getShipmentLocation(), ocl.getCurrency()))
              .toList();
      if (CollectionUtils.isEmpty(shipmentLocations)) {
        throw new GenericWithMessageException(
            "No shipment to customer other cost line found for customer");
      }
      for (OtherCostLineShipmentLocationInfos shipmentLocation : shipmentLocations) {
        SWEstimationDetailsPerShipmentToCustomer swEstimationDetailsPerShipmentToCustomer =
            new SWEstimationDetailsPerShipmentToCustomer();
        swEstimationDetailsPerShipmentToCustomer.setCurrency(
            currencyMapper.toSWCurrencySummary(shipmentLocation.getCurrency()));
        swEstimationDetailsPerShipmentToCustomer.setLocationName(
            shipmentLocation.getShipmentLocation().getName());
        List<SWEstimationDetailsPerToolingStrategy> swEstimationDetailsPerToolingStrategies =
            new ArrayList<>(2);
        swEstimationDetailsPerToolingStrategies.add(
            buildEstimatedDetailsForToolingStrategy(
                lineEntity,
                targetExchangeRate,
                additionalProcessRatePerMethodType,
                materialYieldPercentage,
                markupMultiplier,
                toolingMarkupMultiplier,
                SWToolingStrategy.AMORTIZED,
                shipmentLocation.getOtherCostLineId()));
        swEstimationDetailsPerToolingStrategies.add(
            buildEstimatedDetailsForToolingStrategy(
                lineEntity,
                targetExchangeRate,
                additionalProcessRatePerMethodType,
                materialYieldPercentage,
                markupMultiplier,
                toolingMarkupMultiplier,
                SWToolingStrategy.SEPARATED,
                shipmentLocation.getOtherCostLineId()));
        swEstimationDetailsPerShipmentToCustomer.setEstimationDetailsPerToolingStrategy(
            swEstimationDetailsPerToolingStrategies);
        swEstimationDetailsPerShipmentToCustomers.add(swEstimationDetailsPerShipmentToCustomer);
      }
    } else {
      SWEstimationDetailsPerShipmentToCustomer swEstimationDetailsPerShipmentToCustomer =
          new SWEstimationDetailsPerShipmentToCustomer();
      swEstimationDetailsPerShipmentToCustomer.setCurrency(
          currencyMapper.toSWCurrencySummary(targetCurrency));
      swEstimationDetailsPerShipmentToCustomer.setLocationName("NON-DYSON");
      List<SWEstimationDetailsPerToolingStrategy> swEstimationDetailsPerToolingStrategies =
          new ArrayList<>(2);
      swEstimationDetailsPerToolingStrategies.add(
          buildEstimatedDetailsForToolingStrategy(
              lineEntity,
              targetExchangeRate,
              additionalProcessRatePerMethodType,
              materialYieldPercentage,
              markupMultiplier,
              toolingMarkupMultiplier,
              SWToolingStrategy.AMORTIZED,
              null));
      swEstimationDetailsPerToolingStrategies.add(
          buildEstimatedDetailsForToolingStrategy(
              lineEntity,
              targetExchangeRate,
              additionalProcessRatePerMethodType,
              materialYieldPercentage,
              markupMultiplier,
              toolingMarkupMultiplier,
              SWToolingStrategy.SEPARATED,
              null));
      swEstimationDetailsPerShipmentToCustomer.setEstimationDetailsPerToolingStrategy(
          swEstimationDetailsPerToolingStrategies);
      swEstimationDetailsPerShipmentToCustomers.add(swEstimationDetailsPerShipmentToCustomer);
    }
    return swEstimationDetailsPerShipmentToCustomers;
  }

  private @NonNull SWEstimationDetailsPerToolingStrategy buildEstimatedDetailsForToolingStrategy(
      CostRequestLineEntity lineEntity,
      BigDecimal targetExchangeRate,
      BigDecimal additionalProcessRatePerMethodType,
      BigDecimal materialYieldPercentage,
      BigDecimal markupMultiplier,
      BigDecimal toolingMarkupMultiplier,
      SWToolingStrategy toolingStrategy,
      UUID shipmentLocationOtherCostLineId) {
    SWEstimationDetailsPerToolingStrategy swEstimationDetailsForToolingStrategy =
        new SWEstimationDetailsPerToolingStrategy();
    swEstimationDetailsForToolingStrategy.setToolingStrategy(toolingStrategy);
    swEstimationDetailsForToolingStrategy.setEstimationDetailsPerQuantity(
        buildEstimationEntriesForToolingStrategy(
            lineEntity,
            targetExchangeRate,
            additionalProcessRatePerMethodType,
            materialYieldPercentage,
            markupMultiplier,
            toolingMarkupMultiplier,
            toolingStrategy,
            shipmentLocationOtherCostLineId));
    return swEstimationDetailsForToolingStrategy;
  }

  private List<SWEstimationDetailsPerQuantity> buildEstimationEntriesForToolingStrategy(
      CostRequestLineEntity lineEntity,
      BigDecimal targetExchangeRate,
      BigDecimal additionalProcessRatePerMethodType,
      BigDecimal materialYieldPercentage,
      BigDecimal markupMultiplier,
      BigDecimal toolingMarkupMultiplier,
      SWToolingStrategy toolingStrategy,
      UUID shipmentLocationOtherCostLineId) {
    List<SWEstimationDetailsPerQuantity> swEstimationDetails = new ArrayList<>();
    // Base entry (quantity == 1)
    SWEstimationDetailsPerQuantity baseEntry = new SWEstimationDetailsPerQuantity();
    baseEntry.setQuantity(1);
    baseEntry.setYieldApplied(materialYieldPercentage);

    TotalCosts baseTotalCosts =
        costRequestLineCalculationsHelper.calculateTotalCosts(
            lineEntity.getOnlyMaterialLinesUsedForQuotation(),
            lineEntity.getProcessLines(),
            lineEntity.getOtherCostLines(),
            lineEntity.getToolingCostLines(),
            additionalProcessRatePerMethodType,
            materialYieldPercentage,
            shipmentLocationOtherCostLineId,
            markupMultiplier,
            toolingMarkupMultiplier,
            targetExchangeRate);

    TotalMaterialCosts materialCosts = baseTotalCosts.getTotalMaterialCosts();
    baseEntry.setTotalMaterialShippingCostInTargetCurrency(
        materialCosts.getMaterialShippingCostInTargetCurrency());
    baseEntry.setTotalMaterialCostInTargetCurrency(
        materialCosts.getMaterialCostWithoutShippingCostInTargetCurrency());
    baseEntry.setTotalMaterialCostInTargetCurrencyWithYield(
        materialCosts.getMaterialCostWithoutShippingCostWithYieldInTargetCurrency());

    TotalProcessCosts processCosts = baseTotalCosts.getTotalProcessCosts();
    baseEntry.setTotalProcessCostInTargetCurrencyWithAdditional(
        processCosts.getTotalProcessCostWithAdditionalInTargetCurrency());

    TotalOtherCosts otherCosts = baseTotalCosts.getTotalOtherCosts();
    baseEntry.setTotalOtherCostInTargetCurrency(otherCosts.getTotalOtherCostInTargetCurrency());

    TotalToolingCosts toolingCosts = baseTotalCosts.getTotalToolingCosts();
    baseEntry.setTotalToolingCostInTargetCurrency(
        toolingCosts.getTotalToolingCostInTargetCurrency());

    switch (toolingStrategy) {
      case AMORTIZED -> {
        baseEntry.setTotalCostInTargetCurrency(
            baseTotalCosts.getTotalCostWithToolingInTargetCurrency());
        baseEntry.setTotalCostWithMarkupInTargetCurrency(
            baseTotalCosts.getTotalCostWithToolingWithMarkupInTargetCurrency());
      }
      case SEPARATED -> {
        baseEntry.setTotalCostInTargetCurrency(
            baseTotalCosts.getTotalCostWithoutToolingInTargetCurrency());
        baseEntry.setTotalCostWithMarkupInTargetCurrency(
            baseTotalCosts.getTotalCostWithoutToolingWithMarkupInTargetCurrency());
        baseEntry.setTotalToolingCostWithMarkupInTargetCurrency(
            baseTotalCosts.getTotalToolingCostWithMarkupInTargetCurrency());
      }
      default -> throw new IllegalStateException("Unexpected value: " + toolingStrategy);
    }
    swEstimationDetails.add(baseEntry);

    List<Integer> quantities = lineEntity.getQuantitiesAsList();
    for (int i = 0; i < quantities.size(); i++) {
      int quantity = quantities.get(i);
      if (quantity == 1) {
        continue;
      }
      SWEstimationDetailsPerQuantity swPerQuantityEntry = new SWEstimationDetailsPerQuantity();
      swPerQuantityEntry.setQuantity(quantity);
      swPerQuantityEntry.setYieldApplied(materialYieldPercentage);

      int finalI = i;
      TotalCosts perQtyTotalCosts =
          costRequestLineCalculationsHelper.calculateTotalCostsPerQuantity(
              lineEntity.getOnlyMaterialLinesUsedForQuotation().stream()
                  .map(ml -> ml.getMaterialLineForCostRequestQuantities().get(finalI))
                  .toList(),
              lineEntity.getProcessLines().stream()
                  .map(pl -> pl.getProcessLineForCostRequestQuantities().get(finalI))
                  .toList(),
              lineEntity.getOtherCostLines().stream()
                  .map(ocl -> ocl.getOtherCostLineForCostRequestQuantities().get(finalI))
                  .toList(),
              lineEntity.getToolingCostLines().stream()
                  .map(tl -> tl.getToolingCostLineForCostRequestQuantities().get(finalI))
                  .toList(),
              additionalProcessRatePerMethodType,
              materialYieldPercentage,
              shipmentLocationOtherCostLineId,
              markupMultiplier,
              toolingMarkupMultiplier,
              targetExchangeRate);

      swPerQuantityEntry.setTotalMaterialShippingCostInTargetCurrency(
          perQtyTotalCosts.getTotalMaterialCosts().getMaterialShippingCostInTargetCurrency());
      swPerQuantityEntry.setTotalMaterialCostInTargetCurrency(
          perQtyTotalCosts
              .getTotalMaterialCosts()
              .getMaterialCostWithoutShippingCostInTargetCurrency());
      swPerQuantityEntry.setTotalMaterialCostInTargetCurrencyWithYield(
          perQtyTotalCosts
              .getTotalMaterialCosts()
              .getMaterialCostWithoutShippingCostWithYieldInTargetCurrency());

      swPerQuantityEntry.setTotalProcessCostInTargetCurrencyWithAdditional(
          perQtyTotalCosts
              .getTotalProcessCosts()
              .getTotalProcessCostWithAdditionalInTargetCurrency());

      swPerQuantityEntry.setTotalOtherCostInTargetCurrency(
          perQtyTotalCosts.getTotalOtherCosts().getTotalOtherCostInTargetCurrency());

      swPerQuantityEntry.setTotalToolingCostInTargetCurrency(
          perQtyTotalCosts.getTotalToolingCosts().getTotalToolingCostInTargetCurrency());

      switch (toolingStrategy) {
        case AMORTIZED -> {
          swPerQuantityEntry.setTotalCostInTargetCurrency(
              perQtyTotalCosts.getTotalCostWithToolingInTargetCurrency());
          swPerQuantityEntry.setTotalCostWithMarkupInTargetCurrency(
              perQtyTotalCosts.getTotalCostWithToolingWithMarkupInTargetCurrency());
        }
        case SEPARATED -> {
          swPerQuantityEntry.setTotalCostInTargetCurrency(
              perQtyTotalCosts.getTotalCostWithoutToolingInTargetCurrency());
          swPerQuantityEntry.setTotalCostWithMarkupInTargetCurrency(
              perQtyTotalCosts.getTotalCostWithoutToolingWithMarkupInTargetCurrency());
          swPerQuantityEntry.setTotalToolingCostWithMarkupInTargetCurrency(
              perQtyTotalCosts.getTotalToolingCostWithMarkupInTargetCurrency());
        }
        default -> throw new IllegalStateException("Unexpected value: " + toolingStrategy);
      }
      swEstimationDetails.add(swPerQuantityEntry);
    }
    return swEstimationDetails;
  }

  @Transactional
  public List<SWFileInfo> retrieveCostRequestLineFilesMetadata(UUID uid, UUID lineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    return filesMapper.toListFileMetadata(lineEntity.getAttachedFiles());
  }

  @Transactional
  public List<SWFileInfo> uploadCostRequestLineFiles(UUID uid, UUID lineUid, MultipartFile[] files)
      throws Exception {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    List<String> filesAdded =
        fileHelper.uploadFiles(
            files,
            appConfigurationProperties.getCostRequestLineFilesPathDirectory()
                + lineEntity.getCostRequestLineId(),
            GlobalConstants.ALLOWED_FILE_EXTENSIONS);
    fileHelper.addFilesInAttachedFilesListInEntity(lineEntity, filesAdded, FileType.ANY);
    lineEntity = costRequestLineRepository.save(lineEntity);
    return filesMapper.toListFileMetadata(lineEntity.getAttachedFiles());
  }

  @Transactional
  public Resource downloadCostRequestLineFiles(UUID uid, UUID lineUid, @Valid List<UUID> fileUids)
      throws Exception {
    CostRequestLineEntity costRequestLineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);

    return fileHelper.downloadFile(
        appConfigurationProperties.getCostRequestLineFilesPathDirectory()
            + costRequestLineEntity.getCostRequestLineId(),
        fileHelper.fileUidsToFileNames(fileUids),
        GlobalConstants.ZIP_FILE);
  }

  @Transactional
  public List<SWFileInfo> deleteCostRequestLineFiles(
      UUID uid, UUID lineUid, @Valid List<UUID> fileUids) throws Exception {
    CostRequestLineEntity costRequestLineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    List<Path> validPaths =
        fileHelper.deleteMultipleFiles(
            appConfigurationProperties.getCostRequestLineFilesPathDirectory()
                + costRequestLineEntity.getCostRequestLineId(),
            fileHelper.fileUidsToFileNames(fileUids));
    fileHelper.deleteFilesInAttachedFilesListInEntity(
        costRequestLineEntity, validPaths, FileType.ANY);
    costRequestLineEntity = costRequestLineRepository.save(costRequestLineEntity);
    return filesMapper.toListFileMetadata(costRequestLineEntity.getAttachedFiles());
  }

  @Transactional
  public List<SWCostRequestLine> outsourceCostRequestLine(UUID uid, UUID lineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    if (lineEntity.isOutsourced()) {
      throw new GenericWithMessageException(
          "This request for quotation line is already outsourced");
    }
    lineEntity.setOutsourcingRejectReason(null);
    lineEntity.setOutsourced(true);
    lineEntity.setOutsourcingStatus(OutsourcingStatus.TO_BE_ESTIMATED);
    lineEntity = costRequestLineRepository.save(lineEntity);

    List<UserEntity> users = userHelper.getAllActiveUsersByRole(UserRole.PROCUREMENT);
    if (CollectionUtils.isNotEmpty(users)) {
      List<String> emails = users.stream().map(UserEntity::getLogin).toList();
      emailService.sendNewOutsourcedCostRequestLineEmail(emails);
    }

    return costRequestLineMapper.toListSwCostRequestLine(lineEntity.getCostRequest().getLines());
  }

  @Transactional
  public SWCustomCostRequestLinesPaginated searchCostRequestLinesToBeEstimated(
      int offset, int limit, SWBasicSearch body) {
    Sort sort = Sort.by(Sort.Direction.DESC, "creationDate");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);
    Page<CostRequestLineEntity> page;

    if (StringUtils.isNotBlank(body.getSearchText())) {
      page =
          costRequestLineRepository.findByOutsourcingStatusAndSearch(
              pageable, OutsourcingStatus.TO_BE_ESTIMATED, body.getSearchText());
    } else {
      page =
          costRequestLineRepository.findByOutsourcingStatus(
              pageable, OutsourcingStatus.TO_BE_ESTIMATED);
    }

    return populateCostRequestLinesPaginatedResults(page);
  }

  @Transactional
  public SWCustomCostRequestLinesPaginated searchCostRequestLinesForPlanning(
      int offset, int limit, SWBasicSearch body) {
    Sort sort = Sort.by(Sort.Direction.DESC, "creationDate");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);
    Page<CostRequestLineEntity> page;

    if (StringUtils.isNotBlank(body.getSearchText())) {
      page =
          costRequestLineRepository.findByStatusInAndSearchAndParentArchivedFalse(
              pageable,
              List.of(CostRequestStatus.ACTIVE, CostRequestStatus.WON),
              body.getSearchText());
    } else {
      page =
          costRequestLineRepository.findByStatusInAndCostRequest_ArchivedFalse(
              pageable, List.of(CostRequestStatus.ACTIVE, CostRequestStatus.WON));
    }

    return populateCostRequestLinesPaginatedResults(page);
  }

  private SWCustomCostRequestLinesPaginated populateCostRequestLinesPaginatedResults(
      Page<CostRequestLineEntity> page) {
    SWCustomCostRequestLinesPaginated result = new SWCustomCostRequestLinesPaginated();
    result.setResults(costRequestLineMapper.toListSWCustomCostRequestLine(page.getContent()));
    result.setPage(page.getNumber());
    result.setPerPage(page.getSize());
    result.setTotal((int) page.getTotalElements());
    result.setHasPrev(page.hasPrevious());
    result.setHasNext(page.hasNext());
    return result;
  }

  @Transactional
  public void estimateOutsourcedCostRequestLine(UUID uid, SWCostRequestLineEstimate body) {
    CostRequestLineEntity costRequestLine =
        entityRetrievalHelper.getMustExistCostRequestLineById(uid);

    if (costRequestLine.getOutsourcingStatus() != OutsourcingStatus.TO_BE_ESTIMATED) {
      throw new GenericWithMessageException(
          "Cost request line is not in TO_BE_ESTIMATED status", SWCustomErrorCode.GENERIC_ERROR);
    }

    costRequestLine.setOutsourcedCostInSystemCurrency(body.getUnitCostInCurrency());
    costRequestLine.setOutsourcingStatus(OutsourcingStatus.ESTIMATED);
    costRequestLineRepository.save(costRequestLine);

    List<UserEntity> users = userHelper.getAllActiveUsersByRole(UserRole.ENGINEERING);
    if (CollectionUtils.isNotEmpty(users)) {
      List<String> emails = users.stream().map(UserEntity::getLogin).toList();
      String partNumber = costRequestLine.getCustomerPartNumber();
      String partNumberRevision = costRequestLine.getCustomerPartNumberRevision();
      String ref = costRequestLine.getCostRequest().getCostRequestReferenceNumber();
      String rev = String.valueOf(costRequestLine.getCostRequest().getCostRequestRevision());
      emailService.sendOutsourcedCostRequestLineEstimatedEmail(
          emails, partNumber, partNumberRevision, ref, rev);
    }
  }

  @Transactional
  public void rejectOutsourcedCostRequestLine(UUID uid, SWCostRequestLineReject body) {
    CostRequestLineEntity costRequestLine =
        entityRetrievalHelper.getMustExistCostRequestLineById(uid);

    if (costRequestLine.getOutsourcingStatus() != OutsourcingStatus.TO_BE_ESTIMATED) {
      throw new GenericWithMessageException(
          "Cost request line is not in TO_BE_ESTIMATED status", SWCustomErrorCode.GENERIC_ERROR);
    }

    costRequestLine.setOutsourcingStatus(OutsourcingStatus.REJECTED);
    costRequestLine.setOutsourcingRejectReason(body.getRejectReason());
    costRequestLine.setOutsourced(false);
    costRequestLineRepository.save(costRequestLine);

    List<UserEntity> users = userHelper.getAllActiveUsersByRole(UserRole.ENGINEERING);
    if (CollectionUtils.isNotEmpty(users)) {
      List<String> emails = users.stream().map(UserEntity::getLogin).toList();
      String partNumber = costRequestLine.getCustomerPartNumber();
      String partNumberRevision = costRequestLine.getCustomerPartNumberRevision();
      String ref = costRequestLine.getCostRequest().getCostRequestReferenceNumber();
      String rev = String.valueOf(costRequestLine.getCostRequest().getCostRequestRevision());
      emailService.sendOutsourcedCostRequestLineRejectedEmail(
          emails, body.getRejectReason(), partNumber, partNumberRevision, ref, rev);
    }
  }

  @Transactional
  public List<SWFileInfo> retrieveCostRequestLineFilesMetadata(UUID uid) {
    CostRequestLineEntity costRequestLine =
        entityRetrievalHelper.getMustExistCostRequestLineById(uid);
    return filesMapper.toListFileMetadata(costRequestLine.getAttachedFiles());
  }

  @Transactional
  public Resource downloadCostRequestLineFiles(UUID uid, @Valid List<UUID> fileUids)
      throws Exception {
    CostRequestLineEntity costRequestLine =
        entityRetrievalHelper.getMustExistCostRequestLineById(uid);

    return fileHelper.downloadFile(
        appConfigurationProperties.getCostRequestLineFilesPathDirectory()
            + costRequestLine.getCostRequestLineId(),
        fileHelper.fileUidsToFileNames(fileUids),
        GlobalConstants.ZIP_FILE);
  }

  @Transactional
  public List<SWToolingCostLine> outsourceToolingCostLine(
      UUID uid, UUID lineUid, UUID costingLineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    ToolingCostLineEntity toolingCostLine =
        lineEntity.getToolingCostLines().stream()
            .filter(l -> l.getToolingCostLineId().equals(costingLineUid))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Tooling cost line not found"));
    if (toolingCostLine.isOutsourced()) {
      throw new GenericWithMessageException("This tooling cost line is already outsourced");
    }
    toolingCostLine.setRejectReason(null);
    toolingCostLine.setOutsourced(true);
    toolingCostLine.setOutsourcingStatus(OutsourcingStatus.TO_BE_ESTIMATED);
    lineEntity = costRequestLineRepository.save(lineEntity);

    List<UserEntity> users = userHelper.getAllActiveUsersByRole(UserRole.PROCUREMENT);
    if (CollectionUtils.isNotEmpty(users)) {
      List<String> emails = users.stream().map(UserEntity::getLogin).toList();
      emailService.sendNewOutsourcedToolingEmail(emails);
    }

    return toolingCostLineMapper.toListSWToolingCostLine(lineEntity.getToolingCostLines());
  }

  @Transactional
  public List<SWFileInfo> retrieveToolingCostLineFilesMetadata(
      UUID uid, UUID lineUid, UUID costingLineUid) {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    ToolingCostLineEntity toolingCostLine =
        lineEntity.getToolingCostLines().stream()
            .filter(l -> l.getToolingCostLineId().equals(costingLineUid))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Tooling cost line not found"));
    return filesMapper.toListFileMetadata(toolingCostLine.getAttachedFiles());
  }

  @Transactional
  public List<SWFileInfo> uploadToolingCostLineFiles(
      UUID uid, UUID lineUid, UUID costingLineUid, MultipartFile[] files) throws Exception {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    ToolingCostLineEntity toolingCostLine =
        lineEntity.getToolingCostLines().stream()
            .filter(l -> l.getToolingCostLineId().equals(costingLineUid))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Tooling cost line not found"));

    List<String> filesAdded =
        fileHelper.uploadFiles(
            files,
            appConfigurationProperties.getToolingCostLineFilesPathDirectory()
                + toolingCostLine.getToolingCostLineId(),
            GlobalConstants.ALLOWED_FILE_EXTENSIONS);
    fileHelper.addFilesInAttachedFilesListInEntity(toolingCostLine, filesAdded, FileType.ANY);
    toolingCostLine = toolingCostLineRepository.save(toolingCostLine);
    return filesMapper.toListFileMetadata(toolingCostLine.getAttachedFiles());
  }

  @Transactional
  public Resource downloadToolingCostLineFiles(
      UUID uid, UUID lineUid, UUID costingLineUid, @Valid List<UUID> fileUids) throws Exception {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    ToolingCostLineEntity toolingCostLine =
        lineEntity.getToolingCostLines().stream()
            .filter(l -> l.getToolingCostLineId().equals(costingLineUid))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Tooling cost line not found"));

    return fileHelper.downloadFile(
        appConfigurationProperties.getToolingCostLineFilesPathDirectory()
            + toolingCostLine.getToolingCostLineId(),
        fileHelper.fileUidsToFileNames(fileUids),
        GlobalConstants.ZIP_FILE);
  }

  @Transactional
  public List<SWFileInfo> deleteToolingCostLineFiles(
      UUID uid, UUID lineUid, UUID costingLineUid, @Valid List<UUID> fileUids) throws Exception {
    CostRequestLineEntity lineEntity =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    ToolingCostLineEntity toolingCostLine =
        lineEntity.getToolingCostLines().stream()
            .filter(l -> l.getToolingCostLineId().equals(costingLineUid))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Tooling cost line not found"));

    List<Path> validPaths =
        fileHelper.deleteMultipleFiles(
            appConfigurationProperties.getToolingCostLineFilesPathDirectory()
                + toolingCostLine.getToolingCostLineId(),
            fileHelper.fileUidsToFileNames(fileUids));
    fileHelper.deleteFilesInAttachedFilesListInEntity(toolingCostLine, validPaths, FileType.ANY);
    toolingCostLine = toolingCostLineRepository.save(toolingCostLine);
    return filesMapper.toListFileMetadata(toolingCostLine.getAttachedFiles());
  }

  /**
   * Recalculate and persist the parent cost request's totalLinesCostInTargetCurrency by summing all
   * cost types (material, process, tooling, other) across all its lines.
   */
  private void recalculateCostRequestTotalEstimatedLinesCost(CostRequestLineEntity lineEntity) {
    CostRequestEntity costRequest = lineEntity.getCostRequest();
    costRequest.buildCalculatedFields();
    costRequestRepository.save(costRequest);
  }

  // ===========================
  // Message Methods
  // ===========================

  @Transactional
  public List<SWMessage> retrieveMessages(UUID uid, UUID lineUid) {
    CostRequestLineEntity line =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    return messageService.retrieve(line.getMessages());
  }

  @Transactional
  public List<SWMessage> createMessage(UUID uid, UUID lineUid, SWMessageCreate create) {
    CostRequestLineEntity line =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    if (!line.isOutsourced()) {
      throw new GenericWithMessageException(
          "The cost request line is not outsourced. You may need to refresh your page.");
    }
    UserRole role =
        loggedUserDetailsService.hasRole(UserRole.ENGINEERING)
            ? UserRole.PROCUREMENT
            : UserRole.ENGINEERING;
    List<UserEntity> users = userHelper.getAllActiveUsersByRole(role);
    Runnable emailNotification = null;
    if (CollectionUtils.isNotEmpty(users)) {
      CostRequestEntity costRequest = line.getCostRequest();
      List<String> emails = users.stream().map(UserEntity::getLogin).toList();
      String from = loggedUserDetailsService.getLoggedUserLogin();
      String partNumber = line.getCustomerPartNumber();
      String partNumberRevision = line.getCustomerPartNumberRevision();
      String ref = costRequest.getCostRequestReferenceNumber();
      String rev = String.valueOf(costRequest.getCostRequestRevision());
      emailNotification =
          () ->
              emailService.sendCostRequestLineNewMessageEmail(
                  emails, from, partNumber, partNumberRevision, ref, rev);
    }
    return messageService.create(
        create,
        line::addMessage,
        () -> costRequestLineRepository.save(line).getMessages(),
        emailNotification);
  }

  @Transactional
  public SWMessage updateMessage(UUID uid, UUID lineUid, UUID messageUid, SWMessageUpdate body) {
    CostRequestLineEntity line =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    return messageService.update(messageUid, body, line.getMessages());
  }

  @Transactional
  public SWMessage deleteMessage(UUID uid, UUID lineUid, UUID messageUid) {
    CostRequestLineEntity line =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    return messageService.delete(messageUid, line.getMessages());
  }

  @Transactional
  public SWMessage undoMessage(UUID uid, UUID lineUid, UUID messageUid) {
    CostRequestLineEntity line =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    return messageService.undo(messageUid, line.getMessages());
  }

  @Transactional
  public DownloadedFileOutput downloadQuotationBreakdown(UUID uid, UUID lineUid) {
    CostRequestLineEntity line =
        entityRetrievalHelper.getMustExistCostRequestLineEntity(uid, lineUid);
    if (!line.getStatus().isEstimatedStatus()) {
      throw new GenericWithMessageException(
          "Cost request line is not yet estimated", SWCustomErrorCode.GENERIC_ERROR);
    }
    LocalDateTime datetime =
        TimeUtils.nowLocalDateTime(appConfigurationProperties.getAppTimezone());
    String filename =
        "quotation-breakdown--"
            + line.getCustomerPartNumber()
            + "-"
            + line.getCustomerPartNumberRevision()
            + "--"
            + datetime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + ".xlsx";
    return new DownloadedFileOutput(
        filename, costRequestLineQuotationBreakdownHelper.downloadQuotationBreakdown(line));
  }
}
