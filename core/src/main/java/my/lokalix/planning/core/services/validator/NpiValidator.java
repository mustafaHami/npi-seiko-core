package my.lokalix.planning.core.services.validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.NpiOrderEntity;
import my.lokalix.planning.core.utils.TimeUtils;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import my.zkonsulting.planning.generated.model.SWProcessLineMaterialDeliveryDateImport;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NpiValidator {
  public void validateNpiUpdatable(NpiOrderEntity npiOrder) {
    if (npiOrder == null) {
      throw new GenericWithMessageException("NPI order cannot be null");
    }
    if (npiOrder.getStatus().isFinalStatus())
      throw new GenericWithMessageException("NPI order cannot be updated after final status");
  }

  public void validateNpiMaterialDeliveryDateFileConfig(
      SWProcessLineMaterialDeliveryDateImport body) {
    if (body == null) {
      throw new GenericWithMessageException("Material delivery date file config cannot be null");
    }
    if (body.getSheetIndex() == null || body.getSheetIndex() < 0) {
      throw new GenericWithMessageException("Sheet index cannot be less than 0");
    }
    if (body.getColumn() == null || body.getColumn() < 0) {
      throw new GenericWithMessageException("Column cannot be less than 0");
    }
    if (body.getRow() == null || body.getRow() < 0) {
      throw new GenericWithMessageException("Row cannot be less than 0");
    }
  }

  public void validateNpiOrderDates(NpiOrderEntity entity) {
    LocalDate materialPurchaseDate = entity.getMaterialPurchaseEstimatedDate();
    LocalDate shippingDate = entity.getShippingEstimatedDate();
    LocalDate customerApprovalDate = entity.getCustomerApprovalEstimatedDate();

    if (materialPurchaseDate == null || shippingDate == null || customerApprovalDate == null) {
      return;
    }

    rejectWeekend(materialPurchaseDate, "Material purchase estimated date");
    rejectWeekend(shippingDate, "Shipping estimated date");
    rejectWeekend(customerApprovalDate, "Customer approval estimated date");

    long receivingDays = ceilDays(entity.getMaterialReceivingPlanTimeInDays());
    long productionDays = ceilDays(entity.getProductionPlanTimeInDays());
    long testingDays = ceilDays(entity.getTestingPlanTimeInDays());

    LocalDate minShippingDate =
        TimeUtils.addBusinessDays(materialPurchaseDate, receivingDays + productionDays + testingDays);

    if (shippingDate.isBefore(minShippingDate)) {
      throw new GenericWithMessageException(
          "Shipping date ("
              + shippingDate
              + ") cannot be before the estimated end of testing ("
              + minShippingDate
              + ")",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    if (!customerApprovalDate.isAfter(shippingDate)) {
      throw new GenericWithMessageException(
          "Customer approval estimated date ("
              + customerApprovalDate
              + ") must be after the shipping date ("
              + shippingDate
              + ")",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  private void rejectWeekend(LocalDate date, String fieldName) {
    DayOfWeek day = date.getDayOfWeek();
    if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
      throw new GenericWithMessageException(
          fieldName + " (" + date + ") cannot fall on a weekend",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  private long ceilDays(BigDecimal days) {
    if (days == null) return 0;
    return days.setScale(0, RoundingMode.CEILING).longValue();
  }

  public void validateReportDateRange(LocalDate startDate, LocalDate endDate) {
    if (startDate == null) {
      throw new GenericWithMessageException("startDate cannot be null");
    }
    if (endDate == null) {
      throw new GenericWithMessageException("endDate cannot be null");
    }
    if (startDate.isAfter(endDate)) {
      throw new GenericWithMessageException("startDate cannot be after endDate");
    }
  }
}
