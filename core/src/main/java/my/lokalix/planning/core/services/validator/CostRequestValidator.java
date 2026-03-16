package my.lokalix.planning.core.services.validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.*;
import my.lokalix.planning.core.models.entities.admin.TermsAndConditionsDysonEntity;
import my.lokalix.planning.core.models.entities.admin.TermsAndConditionsNonDysonEntity;
import my.lokalix.planning.core.models.enums.*;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CostRequestValidator {

  private final AppConfigurationProperties appConfigurationProperties;

  public void validateQuantitiesCount(List<Integer> quantities) {
    int max = appConfigurationProperties.getMaxNumberOfQuantitiesPerCostRequestLine();
    if (CollectionUtils.isNotEmpty(quantities) && quantities.size() > max) {
      throw new GenericWithMessageException(
          "A request for quotation line cannot have more than " + max + " quantities",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateNotFinalStatus(CostRequestEntity costRequest) {
    validateNotAborted(costRequest);
    if (costRequest.getStatus().isFinalStatus()) {
      throw new GenericWithMessageException(
          "Cannot modify a request for quotation that is in a final state ("
              + costRequest.getStatus().finalStatus()
              + ")",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateNotAborted(CostRequestEntity costRequest) {
    if (costRequest.getStatus() == CostRequestStatus.ABORTED) {
      throw new GenericWithMessageException(
          "Cost request is already ABORTED", SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateIsReadyToQuote(CostRequestEntity costRequest) {
    if (costRequest.getStatus() != CostRequestStatus.READY_TO_QUOTE) {
      throw new GenericWithMessageException(
          "The request for quotation must have the status READY TO QUOTE",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateIsActive(CostRequestEntity costRequest) {
    if (costRequest.getStatus() != CostRequestStatus.ACTIVE) {
      throw new GenericWithMessageException(
          "The request for quotation must have the status ACTIVE", SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateIsFinalStatus(CostRequestEntity costRequest) {
    if (!costRequest.getStatus().isFinalStatus()) {
      throw new GenericWithMessageException(
          "Cannot archive a request for quotation that is not in a final state ("
              + costRequest.getStatus().finalStatus()
              + ")",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateAbort(CostRequestEntity costRequest) {
    this.validateNotAborted(costRequest);
    if (!costRequest.getStatus().isFinalStatus()) {
      throw new GenericWithMessageException(
          "Cannot abort a request for quotation that is not in a final state ( WON, LOST)",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateIsDataFreezeStatus(CostRequestEntity costRequest) {
    if (!costRequest.getStatus().isActiveDataFreezeStatus()) {
      throw new GenericWithMessageException(
          "The request for quotation must be ACTIVE, WON or LOST", SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateForGenerateQuotationPDF(CostRequestEntity costRequest) {
    validateNotAborted(costRequest);
    if (!costRequest.getStatus().equals(CostRequestStatus.READY_TO_QUOTE)) {
      throw new GenericWithMessageException(
          "The request for quotation must be READY TO QUOTE", SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateNotArchived(CostRequestEntity costRequest) {
    if (costRequest.isArchived()) {
      throw new GenericWithMessageException(
          "Cost request is already archived", SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateNonDysonTcCompleteForPdf(Optional<TermsAndConditionsNonDysonEntity> tcOpt) {
    if (tcOpt.isEmpty()) {
      throw new GenericWithMessageException(
          "Terms and conditions for this customer are not configured",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    TermsAndConditionsNonDysonEntity tc = tcOpt.get();
    List<String> missing = new ArrayList<>();
    if (tc.getValidityNumberOfDays() == null) missing.add("validity number of days");
    if (tc.getDeliveryCharges() == null) missing.add("delivery charges");
    if (tc.getStorageAcceptDeliveryNumberMonths() == null)
      missing.add("storage: accept delivery number of months");
    if (tc.getStorageMinimumStorageFee() == null) missing.add("storage: minimum storage fee");
    if (tc.getNonCancellationNumberWorkingDays() == null)
      missing.add("non-cancellation number of working days");
    if (tc.getNonRescheduledNumberWeeks() == null) missing.add("non-reschedule number of weeks");
    if (tc.getClaimsPackagingDamageNumberDays() == null)
      missing.add("claims: packaging damage number of days");
    if (StringUtils.isBlank(tc.getForecastLeadTime())) missing.add("forecast lead time");
    if (tc.getLatePaymentPenalty() == null) missing.add("late payment penalty");
    if (CollectionUtils.isNotEmpty(missing)) {
      throw new GenericWithMessageException(
          "Missing T&C fields required for PDF generation: " + String.join(", ", missing),
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateNoDuplicateDraftMaterialLine(
      List<MaterialLineDraftEntity> existingDrafts,
      String manufacturerName,
      String manufacturerPartNumber) {
    if (CollectionUtils.isEmpty(existingDrafts)) return;
    boolean hasDuplicate =
        existingDrafts.stream()
                .filter(
                    d ->
                        StringUtils.isNotBlank(d.getDraftManufacturerName())
                            && d.getDraftManufacturerName().equalsIgnoreCase(manufacturerName)
                            && StringUtils.isNotBlank(d.getManufacturerPartNumber())
                            && d.getManufacturerPartNumber()
                                .equalsIgnoreCase(manufacturerPartNumber))
                .count()
            >= 2;
    if (hasDuplicate) {
      throw new GenericWithMessageException(
          "A material with the same manufacturer and manufacturer P/N already exists for this line",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateDysonTcCompleteForPdf(Optional<TermsAndConditionsDysonEntity> tcOpt) {
    if (tcOpt.isEmpty()) {
      throw new GenericWithMessageException(
          "Terms and conditions for this Dyson customer are not configured",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    TermsAndConditionsDysonEntity tc = tcOpt.get();
    List<String> missing = new ArrayList<>();
    if (tc.getValidityNumberOfDays() == null) missing.add("validity number of days");
    if (tc.getCurrencyExchangeRate() == null) missing.add("currency exchange rate");
    if (tc.getMinimumDeliveryQuantity() == null) missing.add("minimum delivery quantity");
    if (tc.getStorageAcceptDeliveryNumberMonths() == null)
      missing.add("storage: accept delivery number of months");
    if (tc.getStorageMinimumStorageFee() == null) missing.add("storage: minimum storage fee");
    if (tc.getNonCancellationNumberWorkingDays() == null)
      missing.add("non-cancellation number of working days");
    if (tc.getNonRescheduledNumberWeeks() == null) missing.add("non-reschedule number of weeks");
    if (tc.getClaimsPackagingDamageNumberDays() == null)
      missing.add("claims: packaging damage number of days");
    if (StringUtils.isBlank(tc.getForecastLeadTime())) missing.add("forecast lead time");
    if (tc.getLatePaymentPenalty() == null) missing.add("late payment penalty");
    if (CollectionUtils.isNotEmpty(missing)) {
      throw new GenericWithMessageException(
          "Missing Dyson T&C fields required for PDF generation: " + String.join(", ", missing),
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateNoDuplicateMaterialLine(
      List<MaterialLineEntity> existingLines, MaterialEntity material) {
    if (CollectionUtils.isEmpty(existingLines)) return;
    boolean hasDuplicate =
        existingLines.stream().filter(ml -> ml.getMaterial().equals(material)).count() >= 2;
    if (hasDuplicate) {
      throw new GenericWithMessageException(
          "This material already exists for this line", SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateCostRequestLineEstimation(CostRequestLineEntity lineEntity) {
    if (!lineEntity.getStatus().equals(CostRequestStatus.READY_TO_ESTIMATE)) {
      throw new GenericWithMessageException(
          "Only request for quotation lines in READY TO ESTIMATE status can be estimated",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    if (CollectionUtils.isNotEmpty(lineEntity.getProcessLines())
        && lineEntity.getProcessLines().stream()
            .noneMatch(ml -> ml.getProcess().isSetupProcess())) {
      throw new GenericWithMessageException(
          "A setup process is mandatory when at least 1 process is present, please add one before validating the estimation",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    List<MaterialLineEntity> usefulMaterialLineEntities =
        lineEntity.getOnlyMaterialLinesUsedForQuotation();
    if (CollectionUtils.isNotEmpty(usefulMaterialLineEntities)
        && usefulMaterialLineEntities.stream()
            .anyMatch(ml -> ml.getMaterial().getStatus() != MaterialStatus.ESTIMATED)) {
      throw new GenericWithMessageException(
          "Cannot estimate request for quotation line with unestimated materials",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    if (CollectionUtils.isNotEmpty(lineEntity.getToolingCostLines())
        && lineEntity.getToolingCostLines().stream()
            .anyMatch(tcl -> tcl.getOutsourcingStatus() == OutsourcingStatus.TO_BE_ESTIMATED)) {
      throw new GenericWithMessageException(
          "Cannot estimate request for quotation line with unestimated outsourced tooling",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (CollectionUtils.isEmpty(lineEntity.getOtherCostLines())
        && CollectionUtils.isEmpty(lineEntity.getProcessLines())
        && CollectionUtils.isEmpty(usefulMaterialLineEntities)
        && CollectionUtils.isEmpty(lineEntity.getToolingCostLines())) {
      throw new GenericWithMessageException(
          "Cannot estimate: No costs have been added yet. Please add material, process, other or tooling costs first.",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (CollectionUtils.isNotEmpty(lineEntity.getToolingCostLines())) {
      if (lineEntity.getToolingCostLines().stream()
          .anyMatch(t -> t.isOutsourced() && !t.getOutsourcingStatus().isEstimated())) {
        throw new GenericWithMessageException(
            "Cannot estimate: some outsourced tooling costs are not yet estimated.",
            SWCustomErrorCode.GENERIC_ERROR);
      }
      if (lineEntity.getToolingCostLines().stream()
          .anyMatch(
              t ->
                  t.getUnitCostInCurrency() == null
                      || t.getUnitCostInCurrency().compareTo(BigDecimal.ZERO) == 0)) {
        throw new GenericWithMessageException(
            "Cannot estimate: some outsourced tooling costs have a zero or missing unit cost.",
            SWCustomErrorCode.GENERIC_ERROR);
      }
    }
  }

  public void validateCostRequestLineReadyForMarkup(CostRequestLineEntity lineEntity) {
    if (!lineEntity.getStatus().equals(CostRequestStatus.READY_TO_VALIDATE)) {
      throw new GenericWithMessageException(
          "Only request for quotation lines in READY TO VALIDATE status can be validated",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    if (CollectionUtils.isNotEmpty(lineEntity.getProcessLines())
        && lineEntity.getProcessLines().stream()
            .noneMatch(ml -> ml.getProcess().isSetupProcess())) {
      throw new GenericWithMessageException(
          "A setup process is mandatory when at least 1 process is present, please add one before validating the estimation",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    List<MaterialLineEntity> usefulMaterialLineEntities =
        lineEntity.getOnlyMaterialLinesUsedForQuotation();
    if (CollectionUtils.isNotEmpty(usefulMaterialLineEntities)
        && usefulMaterialLineEntities.stream()
            .anyMatch(ml -> ml.getMaterial().getStatus() != MaterialStatus.ESTIMATED)) {
      throw new GenericWithMessageException(
          "Cannot estimate request for quotation line with unestimated materials",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    if (CollectionUtils.isNotEmpty(lineEntity.getToolingCostLines())
        && lineEntity.getToolingCostLines().stream()
            .anyMatch(tcl -> tcl.getOutsourcingStatus() == OutsourcingStatus.TO_BE_ESTIMATED)) {
      throw new GenericWithMessageException(
          "Cannot estimate request for quotation line with unestimated outsourced tooling",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (CollectionUtils.isEmpty(lineEntity.getOtherCostLines())
        && CollectionUtils.isEmpty(lineEntity.getProcessLines())
        && CollectionUtils.isEmpty(usefulMaterialLineEntities)
        && CollectionUtils.isEmpty(lineEntity.getToolingCostLines())) {
      throw new GenericWithMessageException(
          "Cannot estimate: No costs have been added yet. Please add material, process, other or tooling costs first.",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (CollectionUtils.isNotEmpty(lineEntity.getToolingCostLines())) {
      if (lineEntity.getToolingCostLines().stream()
          .anyMatch(t -> t.isOutsourced() && !t.getOutsourcingStatus().isEstimated())) {
        throw new GenericWithMessageException(
            "Cannot estimate: some outsourced tooling costs are not yet estimated.",
            SWCustomErrorCode.GENERIC_ERROR);
      }
      if (lineEntity.getToolingCostLines().stream()
          .anyMatch(
              t ->
                  t.getUnitCostInCurrency() == null
                      || t.getUnitCostInCurrency().compareTo(BigDecimal.ZERO) == 0)) {
        throw new GenericWithMessageException(
            "Cannot estimate: some outsourced tooling costs have a zero or missing unit cost.",
            SWCustomErrorCode.GENERIC_ERROR);
      }
    }
  }

  public void validatePdfIsGenerated(CostRequestEntity costRequest) {
    if (CollectionUtils.isEmpty(costRequest.getAttachedFilesPerFileType(FileType.QUOTATION_PDF))) {
      throw new GenericWithMessageException(
          "Quotation PDF must first be generated and sent to customer.",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateExtendExpiration(LocalDate newExpirationDate) {
    if (newExpirationDate == null || !newExpirationDate.isAfter(LocalDate.now())) {
      throw new GenericWithMessageException(
          "New expiration date must be in the future.", SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateIsUpdatableStatus(CostRequestEntity costRequest) {
    if (!costRequest.getStatus().isPendingAndUpdatable()) {
      throw new GenericWithMessageException(
          "Cost request is not updatable in its current status (" + costRequest.getStatus() + ")",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }
}
