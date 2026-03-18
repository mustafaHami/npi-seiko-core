package my.lokalix.planning.core.services.helper;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.models.entities.NpiOrderEntity;
import my.lokalix.planning.core.models.entities.ProcessLineEntity;
import my.lokalix.planning.core.models.enums.NpiOrderStatus;
import my.lokalix.planning.core.models.enums.ProcessLineStatus;
import my.lokalix.planning.core.models.excel.CellColorEnum;
import my.lokalix.planning.core.models.excel.CellStyleFormatEnum;
import my.lokalix.planning.core.models.excel.ExcelCellStyles;
import my.lokalix.planning.core.repositories.NpiOrderRepository;
import my.lokalix.planning.core.repositories.ProcessLineRepository;
import my.lokalix.planning.core.utils.ExcelUtils;
import my.lokalix.planning.core.utils.TimeUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NpiOrderHelper {
  private static final List<NpiOrderStatus> FINALIZED_STATUSES =
      List.of(NpiOrderStatus.COMPLETED, NpiOrderStatus.ABORTED);
  private final NpiOrderRepository npiOrderRepository;
  private final ProcessLineRepository processLineRepository;
  private final AppConfigurationProperties appConfigurationProperties;

  public byte[] buildOpenNpiOrder() throws IOException {
    Sort sort = Sort.by(Sort.Order.asc("purchaseOrderNumber"), Sort.Order.asc("workOrderId"));
    List<NpiOrderEntity> entities =
        npiOrderRepository.findByArchivedFalseAndStatusNotIn(FINALIZED_STATUSES, sort);
    String[] headers = {
      "Purchase Order #",
      "Work order #",
      "Part Number",
      "Quantity",
      "Status",
      "Customer name",
      "Current Process",
      "Target Delivery Date",
      "Planned Delivery Date",
      "Forecast Delivery Date",
      "Material latest delivery Date",
      "Creation Date",
      "Material Purchase",
      "Material Receiving",
      "Production",
      "Testing",
      "Shipping",
      "Customer Approval",
    };
    return buildNpiOrdersWorkbook(headers, entities, false);
  }

  public byte[] buildNpiOrderArchived() throws IOException {
    Sort sort = Sort.by(Sort.Order.asc("purchaseOrderNumber"), Sort.Order.asc("workOrderId"));
    List<NpiOrderEntity> entities = npiOrderRepository.findByArchivedTrue(sort);
    String[] headers = {
      "Purchase Order #",
      "Work order #",
      "Part Number",
      "Quantity",
      "Status",
      "Customer name",
      "Target Delivery Date",
      "Planned Delivery Date",
      "Forecast Delivery Date",
      "Material latest delivery Date",
      "Creation Date",
      "Finalization Date",
      "Material Purchase",
      "Material Receiving",
      "Production",
      "Testing",
      "Shipping",
      "Customer Approval",
    };
    return buildNpiOrdersWorkbook(headers, entities, true);
  }

  private byte[] buildNpiOrdersWorkbook(
      String[] headers, List<NpiOrderEntity> entities, boolean includeFinalizationDate)
      throws IOException {
    try (FileInputStream excelFile =
            new FileInputStream(
                appConfigurationProperties
                    .getExcelTemplatePaths()
                    .getStandardGenerationExcelFileTemplate());
        Workbook workbook = new XSSFWorkbook(excelFile);
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      ExcelCellStyles styles = new ExcelCellStyles(workbook);
      Sheet sheet = workbook.getSheetAt(0);
      writeHeaderRow(sheet, headers, styles);
      int rowIndex = 1;
      int col = 1;
      for (NpiOrderEntity npi : entities) {
        col =
            writeNpiOrderDataRow(sheet.createRow(rowIndex++), npi, styles, includeFinalizationDate);
      }
      for (int i = 0; i < col; i++) {
        sheet.autoSizeColumn(i);
      }
      workbook.write(bos);
      return bos.toByteArray();
    }
  }

  private int writeNpiOrderDataRow(
      Row row, NpiOrderEntity npi, ExcelCellStyles styles, boolean isArchived) {
    int col = 0;
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        npi.getPurchaseOrderNumber(),
        CellStyleFormatEnum.STRING,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row, col++, npi.getWorkOrderId(), CellStyleFormatEnum.STRING, CellColorEnum.WHITE, styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row, col++, npi.getPartNumber(), CellStyleFormatEnum.STRING, CellColorEnum.WHITE, styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row, col++, npi.getQuantity(), CellStyleFormatEnum.INTEGER, CellColorEnum.WHITE, styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        npi.getStatus() != null ? npi.getStatus().getHumanReadableValue() : null,
        CellStyleFormatEnum.STRING,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        StringUtils.isNotBlank(npi.getCustomerName()) ? npi.getCustomerName() : null,
        CellStyleFormatEnum.STRING,
        CellColorEnum.WHITE,
        styles);
    if (!isArchived) {
      ExcelUtils.createAndStyleCellLeftAlignment(
          row,
          col++,
          npi.getCurrentProcessName(),
          CellStyleFormatEnum.STRING,
          CellColorEnum.WHITE,
          styles);
    }

    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        npi.getTargetDeliveryDate() != null ? npi.getTargetDeliveryDate() : null,
        CellStyleFormatEnum.DATE,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        npi.getPlannedDeliveryDate() != null ? npi.getPlannedDeliveryDate() : null,
        CellStyleFormatEnum.DATE,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        npi.getForecastDeliveryDate() != null ? npi.getForecastDeliveryDate() : null,
        CellStyleFormatEnum.DATE,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        getMaterialDeliveryDate(npi.getProcessLines()),
        CellStyleFormatEnum.DATE,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        npi.getCreationDate().toLocalDate(),
        CellStyleFormatEnum.DATE,
        CellColorEnum.WHITE,
        styles);
    if (isArchived) {
      ExcelUtils.createAndStyleCellLeftAlignment(
          row,
          col++,
          npi.getFinalizationDate() != null ? npi.getFinalizationDate().toLocalDate() : null,
          CellStyleFormatEnum.DATE,
          CellColorEnum.WHITE,
          styles);
    }
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        buildProcessLineInfo(npi.getProcessLines(), ProcessLineEntity::getIsMaterialPurchase),
        CellStyleFormatEnum.STRING,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        buildProcessLineInfo(npi.getProcessLines(), ProcessLineEntity::getIsMaterialReceiving),
        CellStyleFormatEnum.STRING,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        buildProcessLineInfo(npi.getProcessLines(), ProcessLineEntity::getIsProduction),
        CellStyleFormatEnum.STRING,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        buildProcessLineInfo(npi.getProcessLines(), ProcessLineEntity::getIsTesting),
        CellStyleFormatEnum.STRING,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        buildProcessLineInfo(npi.getProcessLines(), ProcessLineEntity::getIsShipment),
        CellStyleFormatEnum.STRING,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        buildProcessLineInfo(npi.getProcessLines(), ProcessLineEntity::getIsCustomerApproval),
        CellStyleFormatEnum.STRING,
        CellColorEnum.WHITE,
        styles);
    return col;
  }

  private String buildProcessLineInfo(
      List<ProcessLineEntity> processLines, Predicate<ProcessLineEntity> typePredicate) {
    if (CollectionUtils.isEmpty(processLines)) return null;
    Optional<ProcessLineEntity> lineOpt = processLines.stream().filter(typePredicate).findFirst();
    if (lineOpt.isEmpty()) return null;
    ProcessLineEntity line = lineOpt.get();

    StringBuilder sb = new StringBuilder();
    sb.append(line.getStatus().getHumanReadableValue());
    if (line.getPlanTimeInHours() != null) {
      sb.append("; ")
          .append(line.getPlanTimeInHours().stripTrailingZeros().toPlainString())
          .append("h");
    }
    if (line.getStatus() == ProcessLineStatus.IN_PROGRESS
        && line.getRemainingTimeInHours() != null) {
      sb.append("; ")
          .append(line.getRemainingTimeInHours().stripTrailingZeros().toPlainString())
          .append("h remaining");
    }
    if (line.getCurrentStatusDate() != null) {
      sb.append("; ").append(line.getCurrentStatusDate().toLocalDate());
    }
    return sb.toString();
  }

  private LocalDate getMaterialDeliveryDate(List<ProcessLineEntity> processLines) {
    if (CollectionUtils.isEmpty(processLines)) return null;
    ProcessLineEntity processLine = processLines.getFirst();
    if (processLine == null) return null;
    return processLine.getMaterialLatestDeliveryDate() != null
        ? processLine.getMaterialLatestDeliveryDate()
        : null;
  }

  private void writeHeaderRow(Sheet sheet, String[] headers, ExcelCellStyles styles) {
    Row headerRow = sheet.createRow(0);
    for (int i = 0; i < headers.length; i++) {
      ExcelUtils.createAndStyleCellLeftAlignment(
          headerRow, i, headers[i], CellStyleFormatEnum.STRING, CellColorEnum.LIGHT_GREEN, styles);
    }
  }

  public void recalculateForecastDeliveryDate(NpiOrderEntity npiOrder) {
    List<ProcessLineEntity> allLines =
        processLineRepository.findAllByNpiOrderOrderByIndexIdAsc(npiOrder);

    LocalDate today = TimeUtils.nowLocalDate(appConfigurationProperties.getAppTimezone());
    final BigDecimal hoursPerDay = BigDecimal.valueOf(24.0);

    BigDecimal totalForecastHours = BigDecimal.valueOf(0);
    for (ProcessLineEntity line : allLines) {
      ProcessLineStatus status = line.getStatus();
      BigDecimal remainingTimeInHours = line.getRemainingTimeInHours();
      BigDecimal planTimeInHours = line.getPlanTimeInHours();

      if (status == ProcessLineStatus.IN_PROGRESS) {
        BigDecimal timeToAdd =
            remainingTimeInHours != null ? remainingTimeInHours : planTimeInHours;
        if (timeToAdd != null) {
          totalForecastHours = totalForecastHours.add(timeToAdd);
        }
      } else if (status == ProcessLineStatus.NOT_STARTED && planTimeInHours != null) {
        totalForecastHours = totalForecastHours.add(planTimeInHours);
      }
    }
    long forecastDays = totalForecastHours.divide(hoursPerDay, 0, RoundingMode.CEILING).longValue();
    npiOrder.setForecastDeliveryDate(today.plusDays(forecastDays));
    npiOrderRepository.save(npiOrder);
  }

  public int recalculateForecastDeliveryDateForAllActiveNpiOrders() {
    List<NpiOrderEntity> npiOrders = npiOrderRepository.findAllByArchivedFalse();
    for (NpiOrderEntity npiOrder : npiOrders) {
      recalculateForecastDeliveryDate(npiOrder);
    }
    return npiOrders.size();
  }
}
