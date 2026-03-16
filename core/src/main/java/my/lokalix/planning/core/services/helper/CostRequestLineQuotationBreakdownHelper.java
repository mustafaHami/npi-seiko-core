package my.lokalix.planning.core.services.helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.*;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.lokalix.planning.core.models.entities.admin.ExchangeRateEntity;
import my.lokalix.planning.core.models.entities.admin.GlobalConfigEntity;
import my.lokalix.planning.core.models.enums.MaterialType;
import my.lokalix.planning.core.models.enums.OtherCostLineCalculationStrategy;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostRequestLineQuotationBreakdownHelper {

  private static final byte[] LIGHT_GREEN_RGB = new byte[] {(byte) 0xE2, (byte) 0xEF, (byte) 0xDA};
  private static final byte[] LIGHT_GRAY_RGB = new byte[] {(byte) 0xD9, (byte) 0xD9, (byte) 0xD9};
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final CostRequestLineHelper costRequestLineHelper;
  private final AppConfigurationProperties appConfigurationProperties;

  public byte[] downloadQuotationBreakdown(CostRequestLineEntity line) {

    GlobalConfigEntity globalConfig = entityRetrievalHelper.getMustExistGlobalConfig();
    CostRequestEntity costRequestEntity = line.getCostRequest();

    List<CostRequestFrozenShipmentLocationEntity> frozenLocations =
        costRequestEntity.getFrozenShipmentLocations().stream()
            .filter(fl -> !fl.isMasked())
            .sorted(Comparator.comparingInt(CostRequestFrozenShipmentLocationEntity::getIndexId))
            .toList();
    boolean hasShipmentLocations = !frozenLocations.isEmpty();

    BigDecimal yield = costRequestLineHelper.resolveYield(line, globalConfig);
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
      String customerName =
          line.getCostRequest().getCustomer() != null
              ? line.getCostRequest().getCustomer().getName()
              : "";
      String projectName =
          line.getCostRequest().getProjectName() != null
              ? line.getCostRequest().getProjectName()
              : "";
      String statusHr = line.getStatus() != null ? line.getStatus().getHumanReadableValue() : "";
      String partNumber = line.getCustomerPartNumber() != null ? line.getCustomerPartNumber() : "";
      String partRevision =
          line.getCustomerPartNumberRevision() != null ? line.getCustomerPartNumberRevision() : "";
      String partName = line.getDescription() != null ? line.getDescription() : "";

      // Title style: bold blue font, centered — thick borders applied via RegionUtil after
      XSSFCellStyle titleStyle = workbook.createCellStyle();
      titleStyle.setAlignment(HorizontalAlignment.CENTER);
      titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
      XSSFFont titleFont = workbook.createFont();
      titleFont.setBold(true);
      titleFont.setFontHeightInPoints((short) 18);
      titleFont.setColor(new XSSFColor(new byte[] {(byte) 0, (byte) 112, (byte) 192}, null));
      titleStyle.setFont(titleFont);

      // Shared bold font for all green fill cells
      XSSFFont greenBoldFont = workbook.createFont();
      greenBoldFont.setBold(true);

      // Data cell style: light green fill + bottom border + bold
      XSSFCellStyle dataStyle = workbook.createCellStyle();
      dataStyle.setFillForegroundColor(new XSSFColor(LIGHT_GREEN_RGB, null));
      dataStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      dataStyle.setBorderBottom(BorderStyle.DOTTED);
      dataStyle.setFont(greenBoldFont);

      // Data cell style for G8: light green fill, no borders, bold
      XSSFCellStyle dataStyleNoBorder = workbook.createCellStyle();
      dataStyleNoBorder.setFillForegroundColor(new XSSFColor(LIGHT_GREEN_RGB, null));
      dataStyleNoBorder.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      dataStyleNoBorder.setFont(greenBoldFont);

      // Data cell style for REV cell (G8): light green fill, no borders, bold, centered
      XSSFCellStyle dataStyleNoBorderCentered = workbook.createCellStyle();
      dataStyleNoBorderCentered.setFillForegroundColor(new XSSFColor(LIGHT_GREEN_RGB, null));
      dataStyleNoBorderCentered.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      dataStyleNoBorderCentered.setAlignment(HorizontalAlignment.CENTER);
      dataStyleNoBorderCentered.setVerticalAlignment(VerticalAlignment.CENTER);
      dataStyleNoBorderCentered.setFont(greenBoldFont);

      // Data cell style for D6/D7: light green fill + bottom border + bold + left-aligned
      XSSFCellStyle dataStyleLeftAligned = workbook.createCellStyle();
      dataStyleLeftAligned.setFillForegroundColor(new XSSFColor(LIGHT_GREEN_RGB, null));
      dataStyleLeftAligned.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      dataStyleLeftAligned.setBorderBottom(BorderStyle.DOTTED);
      dataStyleLeftAligned.setAlignment(HorizontalAlignment.LEFT);
      dataStyleLeftAligned.setFont(greenBoldFont);

      // Section header style: light gray fill, bold font
      XSSFCellStyle sectionHeaderStyle = workbook.createCellStyle();
      sectionHeaderStyle.setFillForegroundColor(new XSSFColor(LIGHT_GRAY_RGB, null));
      sectionHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      XSSFFont sectionHeaderFont = workbook.createFont();
      sectionHeaderFont.setBold(true);
      sectionHeaderStyle.setFont(sectionHeaderFont);

      // Column header style: centered, bold
      XSSFCellStyle colHeaderStyle = workbook.createCellStyle();
      colHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
      colHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
      XSSFFont colHeaderFont = workbook.createFont();
      colHeaderFont.setBold(true);
      colHeaderStyle.setFont(colHeaderFont);

      // Subtotal label style: bold, right-aligned
      XSSFCellStyle subtotalLabelStyle = workbook.createCellStyle();
      subtotalLabelStyle.setAlignment(HorizontalAlignment.RIGHT);
      XSSFFont subtotalLabelFont = workbook.createFont();
      subtotalLabelFont.setBold(true);
      subtotalLabelStyle.setFont(subtotalLabelFont);

      // Yellow fill style for subtotal/total value cells
      XSSFCellStyle yellowValueStyle = workbook.createCellStyle();
      yellowValueStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
      yellowValueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      // Centered data style: for "No." column values
      XSSFCellStyle centeredDataStyle = workbook.createCellStyle();
      centeredDataStyle.setAlignment(HorizontalAlignment.CENTER);
      centeredDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

      // Green fill style for material data columns (C, D-E, F, O, P)
      XSSFCellStyle greenFillStyle = workbook.createCellStyle();
      greenFillStyle.setFillForegroundColor(new XSSFColor(LIGHT_GREEN_RGB, null));
      greenFillStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      List<Integer> quantities = line.getQuantitiesAsList();

      List<CostRequestFrozenShipmentLocationEntity> locationsToIterate =
          hasShipmentLocations ? frozenLocations : Collections.singletonList(null);

      for (Integer qty : quantities) {
        for (CostRequestFrozenShipmentLocationEntity currentLocation : locationsToIterate) {
          String rawSheetName =
              hasShipmentLocations
                  ? "MOQ-"
                      + qty
                      + "-"
                      + currentLocation.getCurrencyCode()
                      + "-"
                      + currentLocation.getShipmentLocation().getName()
                  : "MOQ-" + qty;
          Sheet sheet = workbook.createSheet(rawSheetName);

          // Column widths (pixels × 25 ≈ POI 1/256-char units at Calibri 11pt / 96 DPI)
          int[] colPx = {
            15, 65, 240, 205, 205, 173, 173, 240, 190, 65, 230, 185, 185, 185, 185, 185, 200, 15,
            15, 300, 150, 200
          };
          for (int c = 0; c < colPx.length; c++) {
            sheet.setColumnWidth(c, colPx[c] * 25);
          }

          // G2:L2 — merged, bold blue text, thick outside borders
          CellRangeAddress titleRegion = new CellRangeAddress(1, 1, 6, 11);
          sheet.addMergedRegion(titleRegion);
          Row row2 = sheet.createRow(1);
          Cell titleCell = row2.createCell(6);
          titleCell.setCellValue("QUOTATION BREAKDOWN");
          titleCell.setCellStyle(titleStyle);
          RegionUtil.setBorderTop(BorderStyle.THICK, titleRegion, sheet);
          RegionUtil.setBorderBottom(BorderStyle.THICK, titleRegion, sheet);
          RegionUtil.setBorderLeft(BorderStyle.THICK, titleRegion, sheet);
          RegionUtil.setBorderRight(BorderStyle.THICK, titleRegion, sheet);

          // Row 3 (index 2)
          Row row3 = sheet.createRow(2);
          row3.createCell(1).setCellValue("DATE"); // B3
          row3.createCell(3).setCellValue(dateStr); // D3

          // Row 4 (index 3)
          Row row4 = sheet.createRow(3);
          row4.createCell(1).setCellValue("COMPANY NAME"); // B4
          row4.createCell(3).setCellValue(customerName); // D4
          row4.createCell(9).setCellValue("QUOTATION STAGE"); // J4
          row4.createCell(11).setCellValue(statusHr); // L4

          // Row 5 (index 4)
          Row row5 = sheet.createRow(4);
          row5.createCell(1).setCellValue("PROJECT"); // B5
          row5.createCell(3).setCellValue(projectName); // D5
          row5.createCell(9).setCellValue("PART WEIGHT"); // J5

          // Row 6 (index 5)
          Row row6 = sheet.createRow(5);
          row6.createCell(1).setCellValue("ANNUAL QTY"); // B6
          row6.createCell(9).setCellValue("REFERENCE NO"); // J6

          // Row 7 (index 6)
          Row row7 = sheet.createRow(6);
          row7.createCell(1).setCellValue("MOQ"); // B7
          row7.createCell(3).setCellValue(qty); // D7
          row7.createCell(9).setCellValue("TARGET PRICE"); // J7

          // Row 8 (index 7)
          Row row8 = sheet.createRow(7);
          row8.createCell(1).setCellValue("PART NUMBER"); // B8
          row8.createCell(3).setCellValue(partNumber); // D8
          row8.createCell(6).setCellValue("REV"); // G8 — style applied by green fill loop below
          row8.createCell(7).setCellValue(partRevision); // H8
          row8.createCell(9).setCellValue("LME Price"); // J8

          // Row 9 (index 8)
          Row row9 = sheet.createRow(8);
          row9.createCell(1).setCellValue("PART NAME"); // B9
          row9.createCell(3).setCellValue(partName); // D9

          // Apply light green fill to D3:H9 (rows 2–8, cols 3–7).
          // All cells get a bottom border except G8 (row 7, col 6).
          for (int rowIdx = 2; rowIdx <= 8; rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) row = sheet.createRow(rowIdx);
            for (int colIdx = 3; colIdx <= 7; colIdx++) {
              Cell cell = row.getCell(colIdx);
              if (cell == null) cell = row.createCell(colIdx);
              boolean isG8 = (rowIdx == 7 && colIdx == 6);
              boolean isLeftAligned = (colIdx == 3 && (rowIdx == 5 || rowIdx == 6));
              cell.setCellStyle(
                  isG8
                      ? dataStyleNoBorderCentered
                      : isLeftAligned ? dataStyleLeftAligned : dataStyle);
            }
          }

          // Row heights for header block
          row2.setHeight((short) 600); // title row — a bit taller
          for (int ri = 2; ri <= 8; ri++) {
            Row r = sheet.getRow(ri);
            if (r != null) r.setHeight((short) 420);
          }

          // ---- Section (1): Indirect Material ----

          // Row 11 (index 10): B11:Q11 merged section header
          Row row11 = sheet.createRow(10);
          CellRangeAddress indirectTitleRegion = new CellRangeAddress(10, 10, 1, 16);
          sheet.addMergedRegion(indirectTitleRegion);
          Cell indirectTitleCell = row11.createCell(1); // B11
          indirectTitleCell.setCellValue("(1) Indirect Material");
          indirectTitleCell.setCellStyle(sectionHeaderStyle);

          // Row 12 (index 11): column headers
          // D12:E12 merged for "Manufacturer PN"; everything from "Maker" shifts right by 1.
          Row row12 = sheet.createRow(11);
          // B12–C12
          String[] preHeaders = {"No.", "System ID"};
          for (int i = 0; i < preHeaders.length; i++) {
            Cell cell = row12.createCell(1 + i); // B=1, C=2
            cell.setCellValue(preHeaders[i]);
            cell.setCellStyle(colHeaderStyle);
          }
          // D12:E12 merged: Manufacturer PN
          sheet.addMergedRegion(new CellRangeAddress(11, 11, 3, 4));
          Cell mfgPnHeaderCell = row12.createCell(3);
          mfgPnHeaderCell.setCellValue("Manufacturer PN");
          mfgPnHeaderCell.setCellStyle(colHeaderStyle);
          // F12–Q12: Maker + remaining headers (shifted right; Amount (MYR) lands at Q=16)
          String[] postHeaders = {
            "Maker", "Supplier", "SPQ", "MOQ", "LT", "Buying Price",
            "Currency Units", "Exchange Rate", "U/P (MYR)", "Usage", "UoM", "Amount (MYR)"
          };
          for (int i = 0; i < postHeaders.length; i++) {
            Cell cell = row12.createCell(5 + i); // F=5 … Q=16
            cell.setCellValue(postHeaders[i]);
            cell.setCellStyle(colHeaderStyle);
          }

          int rowIndex = 12;
          int indirectMaterialDataStartRowIdx = 12; // row 13 (0-based index 12)
          List<MaterialLineEntity> indirectMaterials =
              line.getOnlyMaterialLinesUsedForQuotation().stream()
                  .filter(ml -> ml.getMaterial().getMaterialType() == MaterialType.INDIRECT)
                  .toList();
          if (CollectionUtils.isNotEmpty(indirectMaterials)) {
            for (int i = 0; i < indirectMaterials.size(); i++) {
              Row row = sheet.createRow(rowIndex);
              MaterialLineEntity ml = indirectMaterials.get(i);
              if (ml.isHasMaterialSubstitute()) {
                ml = ml.getMaterialSubstitute();
              }
              Cell cell = row.createCell(1);
              cell.setCellValue(i + 1);
              cell = row.createCell(2); // C: System ID
              cell.setCellValue(ml.getMaterial().getSystemId());
              cell.setCellStyle(greenFillStyle);
              // D:E merged: Manufacturer PN
              sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 3, 4));
              cell = row.createCell(3);
              cell.setCellValue(ml.getMaterial().getManufacturerPartNumber());
              cell.setCellStyle(greenFillStyle);
              cell = row.createCell(5); // F: Maker
              cell.setCellValue(ml.getMaterial().getManufacturer().getName());
              cell.setCellStyle(greenFillStyle);
              MaterialLinePerCostRequestQuantityEntity materialLinePerQuantity =
                  ml.retrieveMatchingMaterialLinePerCostRequestQuantities(qty);
              MaterialSupplierEntity chosenMaterialSupplier =
                  materialLinePerQuantity.getChosenMaterialSupplier();
              cell = row.createCell(6); // G: Supplier
              cell.setCellValue(chosenMaterialSupplier.getSupplier().getName());
              cell = row.createCell(7); // H: SPQ
              if (materialLinePerQuantity.getStandardPackagingQuantity() != null) {
                cell.setCellValue(
                    materialLinePerQuantity.getStandardPackagingQuantity().doubleValue());
              }
              cell = row.createCell(8); // I: MOQ
              cell.setCellValue(materialLinePerQuantity.getMinimumOrderQuantity().doubleValue());
              cell = row.createCell(9); // J: LT
              cell.setCellValue(materialLinePerQuantity.getLeadTime());
              cell = row.createCell(10); // K: Buying Price
              cell.setCellValue(
                  materialLinePerQuantity
                      .getUnitPurchasingPriceInPurchasingCurrency()
                      .doubleValue());
              cell = row.createCell(11); // L: Currency Units
              cell.setCellValue(chosenMaterialSupplier.getPurchasingCurrency().getCode());
              cell = row.createCell(12); // M: Exchange Rate
              cell.setCellValue(
                  materialLinePerQuantity
                      .getPurchasingCurrencyExchangeRateToSystemCurrency()
                      .doubleValue());
              cell = row.createCell(13); // N: U/P (MYR)
              cell.setCellValue(
                  materialLinePerQuantity.getUnitPurchasingPriceInSystemCurrency().doubleValue());
              cell = row.createCell(14); // O: Usage
              cell.setCellValue(materialLinePerQuantity.getQuantity().doubleValue());
              cell.setCellStyle(greenFillStyle);
              cell = row.createCell(15); // P: UoM
              cell.setCellValue(
                  ml.getMaterial().getUnit() != null ? ml.getMaterial().getUnit().getName() : null);
              cell.setCellStyle(greenFillStyle);
              cell = row.createCell(16); // Q: Amount (MYR)
              cell.setCellValue(
                  materialLinePerQuantity.getTotalPurchasingPriceInSystemCurrency().doubleValue());
              rowIndex++;
            }
          }
          int indirectMaterialRowCount = indirectMaterials.size();

          // Subtotal row
          Row subtotalRow = sheet.createRow(rowIndex);
          CellRangeAddress subtotalLabelRegion =
              new CellRangeAddress(rowIndex, rowIndex, 14, 15); // O:P
          sheet.addMergedRegion(subtotalLabelRegion);
          Cell subtotalLabelCell = subtotalRow.createCell(14); // O column
          subtotalLabelCell.setCellValue("Sub Total (1)");
          subtotalLabelCell.setCellStyle(subtotalLabelStyle);

          // Q column (index 16): SUM of Q data rows
          Cell subtotalValueCell = subtotalRow.createCell(16); // Q column
          if (indirectMaterialRowCount > 0) {
            String sumRange =
                "Q"
                    + (indirectMaterialDataStartRowIdx + 1)
                    + ":Q"
                    + (indirectMaterialDataStartRowIdx + indirectMaterialRowCount);
            subtotalValueCell.setCellFormula("SUM(" + sumRange + ")");
          } else {
            subtotalValueCell.setCellValue(0);
          }
          subtotalValueCell.setCellStyle(yellowValueStyle);
          // Thin borders on all cells in the header and data rows (index 11 to rowIndex-1)
          for (int ri = 11; ri < rowIndex; ri++) {
            for (int ci = 1; ci <= 16; ci++) {
              CellRangeAddress addr = new CellRangeAddress(ri, ri, ci, ci);
              RegionUtil.setBorderTop(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderBottom(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderLeft(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderRight(BorderStyle.THIN, addr, sheet);
            }
          }
          // Thin borders on P and Q in the Sub Total (1) row
          for (int ci = 15; ci <= 16; ci++) {
            CellRangeAddress addr = new CellRangeAddress(rowIndex, rowIndex, ci, ci);
            RegionUtil.setBorderTop(BorderStyle.THIN, addr, sheet);
            RegionUtil.setBorderBottom(BorderStyle.THIN, addr, sheet);
            RegionUtil.setBorderLeft(BorderStyle.THIN, addr, sheet);
            RegionUtil.setBorderRight(BorderStyle.THIN, addr, sheet);
          }
          int subTotal1ExcelRow = rowIndex + 1; // 1-based Excel row for Sub Total (1)
          // Row heights for section (1): column headers + data rows + subtotal (skip section header
          // at 10)
          for (int ri = 11; ri <= rowIndex; ri++) {
            Row r = sheet.getRow(ri);
            if (r != null) r.setHeight((short) 420);
          }
          // Thick outer border for Section (1) Indirect Material
          CellRangeAddress section1Region = new CellRangeAddress(10, rowIndex, 1, 16);
          RegionUtil.setBorderTop(BorderStyle.THICK, section1Region, sheet);
          RegionUtil.setBorderBottom(BorderStyle.THICK, section1Region, sheet);
          RegionUtil.setBorderLeft(BorderStyle.THICK, section1Region, sheet);
          RegionUtil.setBorderRight(BorderStyle.THICK, section1Region, sheet);
          rowIndex++;
          rowIndex++;

          // ---- Section (2): Direct Materials ----
          int section2StartRowIdx = rowIndex;
          Row currentRow = sheet.createRow(rowIndex);
          CellRangeAddress directTitleRegion = new CellRangeAddress(rowIndex, rowIndex, 1, 16);
          sheet.addMergedRegion(directTitleRegion);
          Cell directTitleCell = currentRow.createCell(1);
          directTitleCell.setCellValue("(2) Direct Material");
          directTitleCell.setCellStyle(sectionHeaderStyle);
          rowIndex++;

          // Row: column headers
          currentRow = sheet.createRow(rowIndex);
          for (int i = 0; i < preHeaders.length; i++) {
            Cell cell = currentRow.createCell(1 + i); // B=1, C=2
            cell.setCellValue(preHeaders[i]);
            cell.setCellStyle(colHeaderStyle);
          }
          String[] postHeadersShipping = {
            "Supplier Shipment Method", "Percentage", "Shipping Cost"
          };
          for (int i = 0; i < postHeadersShipping.length; i++) {
            Cell cell = currentRow.createCell(19 + i); // T=19...
            cell.setCellValue(postHeadersShipping[i]);
            cell.setCellStyle(colHeaderStyle);
          }
          // Manufacturer PN merged header — on the same row as the other column headers
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 3, 4));
          mfgPnHeaderCell = currentRow.createCell(3);
          mfgPnHeaderCell.setCellValue("Manufacturer PN");
          mfgPnHeaderCell.setCellStyle(colHeaderStyle);
          for (int i = 0; i < postHeaders.length; i++) {
            Cell cell = currentRow.createCell(5 + i); // F=5 … Q=16
            cell.setCellValue(postHeaders[i]);
            cell.setCellStyle(colHeaderStyle);
          }
          rowIndex++;

          int directMaterialDataStartRowIdx = rowIndex;
          List<MaterialLineEntity> directMaterials =
              line.getOnlyMaterialLinesUsedForQuotation().stream()
                  .filter(ml -> ml.getMaterial().getMaterialType() == MaterialType.DIRECT)
                  .toList();
          if (CollectionUtils.isNotEmpty(directMaterials)) {
            for (int i = 0; i < directMaterials.size(); i++) {
              Row row = sheet.createRow(rowIndex);
              MaterialLineEntity ml = directMaterials.get(i);
              if (ml.isHasMaterialSubstitute()) {
                ml = ml.getMaterialSubstitute();
              }
              Cell cell = row.createCell(1);
              cell.setCellValue(i + 1);
              cell = row.createCell(2); // C: System ID
              cell.setCellValue(ml.getMaterial().getSystemId());
              cell.setCellStyle(greenFillStyle);
              // D:E merged: Manufacturer PN
              sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 3, 4));
              cell = row.createCell(3);
              cell.setCellValue(ml.getMaterial().getManufacturerPartNumber());
              cell.setCellStyle(greenFillStyle);
              cell = row.createCell(5); // F: Maker
              cell.setCellValue(ml.getMaterial().getManufacturer().getName());
              cell.setCellStyle(greenFillStyle);
              MaterialLinePerCostRequestQuantityEntity materialLinePerQuantity =
                  ml.retrieveMatchingMaterialLinePerCostRequestQuantities(qty);
              MaterialSupplierEntity chosenMaterialSupplier =
                  materialLinePerQuantity.getChosenMaterialSupplier();
              cell = row.createCell(6); // G: Supplier
              cell.setCellValue(chosenMaterialSupplier.getSupplier().getName());
              cell = row.createCell(7); // H: SPQ
              if (materialLinePerQuantity.getStandardPackagingQuantity() != null) {
                cell.setCellValue(
                    materialLinePerQuantity.getStandardPackagingQuantity().doubleValue());
              }
              cell = row.createCell(8); // I: MOQ
              cell.setCellValue(materialLinePerQuantity.getMinimumOrderQuantity().doubleValue());
              cell = row.createCell(9); // J: LT
              cell.setCellValue(materialLinePerQuantity.getLeadTime());
              cell = row.createCell(10); // K: Buying Price
              cell.setCellValue(
                  materialLinePerQuantity
                      .getUnitPurchasingPriceInPurchasingCurrency()
                      .doubleValue());
              cell = row.createCell(11); // L: Currency Units
              cell.setCellValue(chosenMaterialSupplier.getPurchasingCurrency().getCode());
              cell = row.createCell(12); // M: Exchange Rate
              cell.setCellValue(
                  materialLinePerQuantity
                      .getPurchasingCurrencyExchangeRateToSystemCurrency()
                      .doubleValue());
              cell = row.createCell(13); // N: U/P (MYR)
              cell.setCellValue(
                  materialLinePerQuantity.getUnitPurchasingPriceInSystemCurrency().doubleValue());
              cell = row.createCell(14); // O: Usage
              cell.setCellValue(materialLinePerQuantity.getQuantity().doubleValue());
              cell.setCellStyle(greenFillStyle);
              cell = row.createCell(15); // P: UoM
              cell.setCellValue(
                  ml.getMaterial().getUnit() != null ? ml.getMaterial().getUnit().getName() : null);
              cell.setCellStyle(greenFillStyle);
              cell = row.createCell(16); // Q: Amount (MYR)
              cell.setCellValue(
                  materialLinePerQuantity.getTotalPurchasingPriceInSystemCurrency().doubleValue());

              cell = row.createCell(19); // T

              cell.setCellValue(
                  chosenMaterialSupplier.getSupplier().getShipmentMethod() != null
                      ? chosenMaterialSupplier.getSupplier().getShipmentMethod().getName()
                      : "MISSING");
              cell = row.createCell(20);
              cell.setCellValue(
                  chosenMaterialSupplier.getSupplier().getShipmentMethod() != null
                      ? chosenMaterialSupplier
                          .getSupplier()
                          .getShipmentMethod()
                          .getPercentage()
                          .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                          .doubleValue()
                      : 0);
              cell = row.createCell(21);
              cell.setCellFormula("MAX(+Q" + (rowIndex + 1) + "*U" + (rowIndex + 1) + ",0)");
              rowIndex++;
            }
          }
          int directMaterialRowCount = directMaterials.size();

          // Subtotal row
          subtotalRow = sheet.createRow(rowIndex);
          subtotalLabelRegion = new CellRangeAddress(rowIndex, rowIndex, 14, 15); // O:P
          sheet.addMergedRegion(subtotalLabelRegion);
          subtotalLabelCell = subtotalRow.createCell(14); // O column
          subtotalLabelCell.setCellValue("Sub Total (2)");
          subtotalLabelCell.setCellStyle(subtotalLabelStyle);

          // Q column (index 16): SUM of Q data rows
          subtotalValueCell = subtotalRow.createCell(16); // Q column
          if (directMaterialRowCount > 0) {
            String sumRange =
                "Q"
                    + (directMaterialDataStartRowIdx + 1)
                    + ":Q"
                    + (directMaterialDataStartRowIdx + directMaterialRowCount);
            subtotalValueCell.setCellFormula("SUM(" + sumRange + ")");
          } else {
            subtotalValueCell.setCellValue(0);
          }
          subtotalValueCell.setCellStyle(yellowValueStyle);
          rowIndex++;

          // Subtotal row
          subtotalRow = sheet.createRow(rowIndex);
          subtotalLabelRegion = new CellRangeAddress(rowIndex, rowIndex, 14, 15); // O:P
          sheet.addMergedRegion(subtotalLabelRegion);
          subtotalLabelCell = subtotalRow.createCell(14); // O column
          subtotalLabelCell.setCellValue("Yield");
          subtotalLabelCell.setCellStyle(subtotalLabelStyle);

          subtotalValueCell = subtotalRow.createCell(16); // Q column
          subtotalValueCell.setCellValue(
              yield.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP).doubleValue());
          subtotalValueCell.setCellStyle(yellowValueStyle);
          rowIndex++;

          // Subtotal row
          subtotalRow = sheet.createRow(rowIndex);
          subtotalLabelRegion = new CellRangeAddress(rowIndex, rowIndex, 14, 15); // O:P
          sheet.addMergedRegion(subtotalLabelRegion);
          subtotalLabelCell = subtotalRow.createCell(14); // O column
          subtotalLabelCell.setCellValue("Sub Total (2) with Yield");
          subtotalLabelCell.setCellStyle(subtotalLabelStyle);

          // Q column (index 16)
          subtotalValueCell = subtotalRow.createCell(16); // Q column
          subtotalValueCell.setCellFormula("Q" + (rowIndex - 1) + "*(1+Q" + rowIndex + ")");
          subtotalValueCell.setCellStyle(yellowValueStyle);

          Cell shippingTotalLabelCell = subtotalRow.createCell(20); // U: "Total" label
          shippingTotalLabelCell.setCellValue("Total");
          shippingTotalLabelCell.setCellStyle(subtotalLabelStyle);
          subtotalValueCell = subtotalRow.createCell(21); // V column
          if (directMaterialRowCount > 0) {
            String sumRange =
                "V"
                    + (directMaterialDataStartRowIdx + 1)
                    + ":V"
                    + (directMaterialDataStartRowIdx + directMaterialRowCount);
            subtotalValueCell.setCellFormula("SUM(" + sumRange + ")");
          } else {
            subtotalValueCell.setCellValue(0);
          }
          subtotalValueCell.setCellStyle(yellowValueStyle);
          int subTotal2WithYieldExcelRow = rowIndex + 1; // 1-based Excel row for V column reference
          // Thin borders on header and data rows in section (2)
          for (int ri = section2StartRowIdx + 1; ri <= rowIndex - 3; ri++) {
            for (int ci = 1; ci <= 16; ci++) {
              CellRangeAddress addr = new CellRangeAddress(ri, ri, ci, ci);
              RegionUtil.setBorderTop(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderBottom(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderLeft(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderRight(BorderStyle.THIN, addr, sheet);
            }
            // Shipping cols T:V
            for (int ci = 19; ci <= 21; ci++) {
              CellRangeAddress addr = new CellRangeAddress(ri, ri, ci, ci);
              RegionUtil.setBorderTop(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderBottom(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderLeft(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderRight(BorderStyle.THIN, addr, sheet);
            }
          }
          // Thin borders on O, P, Q in the 3 subtotal rows (Sub Total 2, Yield, Sub Total 2 with
          // Yield)
          for (int ri = rowIndex - 2; ri <= rowIndex; ri++) {
            for (int ci = 14; ci <= 16; ci++) {
              CellRangeAddress addr = new CellRangeAddress(ri, ri, ci, ci);
              RegionUtil.setBorderTop(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderBottom(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderLeft(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderRight(BorderStyle.THIN, addr, sheet);
            }
          }
          // Thin borders on T:V in the shipping total row (Sub Total 2 with Yield row)
          for (int ci = 19; ci <= 21; ci++) {
            CellRangeAddress addr = new CellRangeAddress(rowIndex, rowIndex, ci, ci);
            RegionUtil.setBorderTop(BorderStyle.THIN, addr, sheet);
            RegionUtil.setBorderBottom(BorderStyle.THIN, addr, sheet);
            RegionUtil.setBorderLeft(BorderStyle.THIN, addr, sheet);
            RegionUtil.setBorderRight(BorderStyle.THIN, addr, sheet);
          }
          // Row heights for section (2): col headers + data rows + subtotals (skip section header)
          for (int ri = section2StartRowIdx + 1; ri <= rowIndex; ri++) {
            Row r = sheet.getRow(ri);
            if (r != null) r.setHeight((short) 420);
          }
          // Thick outer border for Section (2) Direct Material (B:Q only; shipping cols T:V
          // excluded)
          CellRangeAddress section2Region =
              new CellRangeAddress(section2StartRowIdx, rowIndex, 1, 16);
          RegionUtil.setBorderTop(BorderStyle.THICK, section2Region, sheet);
          RegionUtil.setBorderBottom(BorderStyle.THICK, section2Region, sheet);
          RegionUtil.setBorderLeft(BorderStyle.THICK, section2Region, sheet);
          RegionUtil.setBorderRight(BorderStyle.THICK, section2Region, sheet);
          rowIndex++;
          rowIndex++;

          // ---- Section (3): Processes ----
          int section3StartRowIdx = rowIndex;
          currentRow = sheet.createRow(rowIndex);
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 1, 16));
          Cell processesTitleCell = currentRow.createCell(1);
          processesTitleCell.setCellValue("(3) Processes");
          processesTitleCell.setCellStyle(sectionHeaderStyle);
          rowIndex++;

          // Column headers — pattern repeated twice: B–I, then J–Q
          currentRow = sheet.createRow(rowIndex);
          // First block: B(1)–I(8)
          currentRow.createCell(1).setCellValue("No.");
          currentRow.getCell(1).setCellStyle(colHeaderStyle);
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 2, 3)); // C:D
          Cell processHeader1 = currentRow.createCell(2);
          processHeader1.setCellValue("Process");
          processHeader1.setCellStyle(colHeaderStyle);
          String[] processColsSingle = {
            "Std cost", "Cycle time", "U/P (MYR)", "Qty", "Amount (MYR)"
          };
          for (int i = 0; i < processColsSingle.length; i++) {
            Cell cell = currentRow.createCell(4 + i); // E=4 … I=8
            cell.setCellValue(processColsSingle[i]);
            cell.setCellStyle(colHeaderStyle);
          }
          // Second block: J(9)–Q(16) — same headers
          currentRow.createCell(9).setCellValue("No.");
          currentRow.getCell(9).setCellStyle(colHeaderStyle);
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 10, 11)); // K:L
          Cell processHeader2 = currentRow.createCell(10);
          processHeader2.setCellValue("Process");
          processHeader2.setCellStyle(colHeaderStyle);
          for (int i = 0; i < processColsSingle.length; i++) {
            Cell cell = currentRow.createCell(12 + i); // M=12 … Q=16
            cell.setCellValue(processColsSingle[i]);
            cell.setCellStyle(colHeaderStyle);
          }
          rowIndex++;

          // Data rows: first half on left flank (B–I), second half on right flank (J–Q)
          List<ProcessLineEntity> processLines = line.getProcessLines();
          int processTotal = processLines.size();
          int processHalf = (processTotal + 1) / 2; // ceiling — determines row count
          int processDataStartRowIdx = rowIndex;

          for (int i = 0; i < processHalf; i++) {
            Row row = sheet.createRow(rowIndex);

            // Left flank: process line at index i
            ProcessLineEntity leftPl = processLines.get(i);
            ProcessLinePerCostRequestQuantityEntity leftPlpq =
                leftPl.getProcessLineForCostRequestQuantities().stream()
                    .filter(p -> p.getCostRequestQuantity() == qty)
                    .findFirst()
                    .orElseThrow(
                        () ->
                            new GenericWithMessageException(
                                "Process line per quantity not found",
                                SWCustomErrorCode.GENERIC_ERROR));
            BigDecimal leftCycleTime = leftPl.getProcessCycleTimeInSeconds();

            Cell cell = row.createCell(1); // B: No.
            cell.setCellValue(i + 1);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 2, 3)); // C:D
            cell = row.createCell(2); // C: Process
            cell.setCellValue(leftPl.getProcess().getName());
            cell.setCellStyle(greenFillStyle);
            cell = row.createCell(4); // E: Std cost
            cell.setCellValue(leftPl.getProcess().getCostPerMinute().doubleValue());
            cell = row.createCell(5); // F: Cycle time
            cell.setCellValue(leftCycleTime.doubleValue());
            cell.setCellStyle(greenFillStyle);
            cell = row.createCell(6); // G: U/P (MYR)
            cell.setCellValue(leftPlpq.getUnitCostInSystemCurrency().doubleValue());
            cell = row.createCell(7); // H: Qty
            cell.setCellValue(leftPlpq.getQuantity().doubleValue());
            cell.setCellStyle(greenFillStyle);
            cell = row.createCell(8); // I: Amount (MYR)
            cell.setCellValue(leftPlpq.getTotalCostInSystemCurrency().doubleValue());

            // Right flank: process line at index i + processHalf (if it exists)
            int rightIdx = i + processHalf;
            if (rightIdx < processTotal) {
              ProcessLineEntity rightPl = processLines.get(rightIdx);
              ProcessLinePerCostRequestQuantityEntity rightPlpq =
                  rightPl.getProcessLineForCostRequestQuantities().stream()
                      .filter(p -> p.getCostRequestQuantity() == qty)
                      .findFirst()
                      .orElseThrow(
                          () ->
                              new GenericWithMessageException(
                                  "Process line per quantity not found",
                                  SWCustomErrorCode.GENERIC_ERROR));
              BigDecimal rightCycleTime = rightPl.getProcessCycleTimeInSeconds();

              cell = row.createCell(9); // J: No.
              cell.setCellValue(rightIdx + 1);
              sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 10, 11)); // K:L
              cell = row.createCell(10); // K: Process
              cell.setCellValue(rightPl.getProcess().getName());
              cell.setCellStyle(greenFillStyle);
              cell = row.createCell(12); // M: Std cost
              cell.setCellValue(rightPl.getProcess().getCostPerMinute().doubleValue());
              cell = row.createCell(13); // N: Cycle time
              cell.setCellValue(rightCycleTime.doubleValue());
              cell.setCellStyle(greenFillStyle);
              cell = row.createCell(14); // O: U/P (MYR)
              cell.setCellValue(rightPlpq.getUnitCostInSystemCurrency().doubleValue());
              cell = row.createCell(15); // P: Qty
              cell.setCellValue(rightPlpq.getQuantity().doubleValue());
              cell.setCellStyle(greenFillStyle);
              cell = row.createCell(16); // Q: Amount (MYR)
              cell.setCellValue(rightPlpq.getTotalCostInSystemCurrency().doubleValue());
            }

            rowIndex++;
          }

          // Subtotal row
          Row processSubtotalRow = sheet.createRow(rowIndex);
          // G:H merged — Sub Total (3A), I — SUM of left flank I column
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 6, 7)); // G:H
          Cell subTotal3ACell = processSubtotalRow.createCell(6);
          subTotal3ACell.setCellValue("Sub Total (3A)");
          subTotal3ACell.setCellStyle(subtotalLabelStyle);
          Cell subTotal3AValueCell = processSubtotalRow.createCell(8); // I
          if (processHalf > 0) {
            String leftSumRange =
                "I" + (processDataStartRowIdx + 1) + ":I" + (processDataStartRowIdx + processHalf);
            subTotal3AValueCell.setCellFormula("SUM(" + leftSumRange + ")");
          } else {
            subTotal3AValueCell.setCellValue(0);
          }
          subTotal3AValueCell.setCellStyle(yellowValueStyle);
          // O:P merged — Sub Total (3B), Q — SUM of right flank Q column
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 14, 15)); // O:P
          Cell subTotal3BCell = processSubtotalRow.createCell(14);
          subTotal3BCell.setCellValue("Sub Total (3B)");
          subTotal3BCell.setCellStyle(subtotalLabelStyle);
          Cell subTotal3BValueCell = processSubtotalRow.createCell(16); // Q
          int rightFlankCount = processTotal - processHalf;
          if (rightFlankCount > 0) {
            String rightSumRange =
                "Q" + (processDataStartRowIdx + 1) + ":Q" + (processDataStartRowIdx + processHalf);
            subTotal3BValueCell.setCellFormula("SUM(" + rightSumRange + ")");
          } else {
            subTotal3BValueCell.setCellValue(0);
          }
          subTotal3BValueCell.setCellStyle(yellowValueStyle);
          rowIndex++;

          // Total row: O:P merged "Total (3A + 3B)", Q = subtotal I + subtotal Q
          // rowIndex now equals the Excel row number of the subtotal row (1-based)
          Row processTotalRow = sheet.createRow(rowIndex);
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 14, 15)); // O:P
          Cell processTotalLabelCell = processTotalRow.createCell(14);
          processTotalLabelCell.setCellValue("Sub Total (3)");
          processTotalLabelCell.setCellStyle(subtotalLabelStyle);
          Cell processTotalValueCell = processTotalRow.createCell(16); // Q
          processTotalValueCell.setCellFormula("I" + rowIndex + "+Q" + rowIndex);
          processTotalValueCell.setCellStyle(yellowValueStyle);
          int subTotal3ExcelRow = rowIndex + 1; // 1-based Excel row for Sub Total (3)
          // Thin borders on header and data rows in section (3)
          for (int ri = section3StartRowIdx + 1; ri <= rowIndex - 2; ri++) {
            for (int ci = 1; ci <= 16; ci++) {
              CellRangeAddress addr = new CellRangeAddress(ri, ri, ci, ci);
              RegionUtil.setBorderTop(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderBottom(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderLeft(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderRight(BorderStyle.THIN, addr, sheet);
            }
          }
          // Thin borders on subtotal rows: Sub Total (3A/3B) at rowIndex-1, Sub Total (3) at
          // rowIndex
          for (int ci = 6; ci <= 9; ci++) { // G:H label + I value (left subtotal)
            CellRangeAddress addr = new CellRangeAddress(rowIndex - 1, rowIndex - 1, ci, ci);
            RegionUtil.setBorderTop(BorderStyle.THIN, addr, sheet);
            RegionUtil.setBorderBottom(BorderStyle.THIN, addr, sheet);
            RegionUtil.setBorderLeft(BorderStyle.THIN, addr, sheet);
            RegionUtil.setBorderRight(BorderStyle.THIN, addr, sheet);
          }
          for (int ri = rowIndex - 1; ri <= rowIndex; ri++) { // O:P label + Q value (both rows)
            for (int ci = 14; ci <= 16; ci++) {
              CellRangeAddress addr = new CellRangeAddress(ri, ri, ci, ci);
              RegionUtil.setBorderTop(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderBottom(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderLeft(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderRight(BorderStyle.THIN, addr, sheet);
            }
          }
          // Thick outer border for Section (3) Processes
          CellRangeAddress section3Region =
              new CellRangeAddress(section3StartRowIdx, rowIndex, 1, 16);
          RegionUtil.setBorderTop(BorderStyle.THICK, section3Region, sheet);
          RegionUtil.setBorderBottom(BorderStyle.THICK, section3Region, sheet);
          RegionUtil.setBorderLeft(BorderStyle.THICK, section3Region, sheet);
          RegionUtil.setBorderRight(BorderStyle.THICK, section3Region, sheet);
          rowIndex++;
          rowIndex++; // blank separator row

          // ---- Sections (4) and (5): Tooling cost | Other cost (side by side) ----
          int section45StartRowIdx = rowIndex;
          List<ToolingCostLineEntity> toolingLines = line.getToolingCostLines();
          List<OtherCostLineEntity> allOtherCostLines = line.getOtherCostLines();
          List<OtherCostLineEntity> otherCostLines;

          if (hasShipmentLocations && currentLocation != null) {
            UUID locId = currentLocation.getShipmentLocation().getShipmentLocationId();
            otherCostLines =
                allOtherCostLines.stream()
                    .filter(
                        oc ->
                            !oc.isShipmentToCustomerLine()
                                || (oc.getShipmentLocation() != null
                                    && oc.getShipmentLocation()
                                        .getShipmentLocationId()
                                        .equals(locId)))
                    .toList();
          } else {
            otherCostLines = allOtherCostLines;
          }
          int otherCostTotalRows = otherCostLines.size() + 1; // +1 for Supplier Shipment
          int maxToolingOtherRows = Math.max(toolingLines.size(), otherCostTotalRows);

          // Section headers on the same row
          currentRow = sheet.createRow(rowIndex);
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 1, 7)); // B:H
          Cell toolingTitleCell = currentRow.createCell(1);
          toolingTitleCell.setCellValue("(4) Tooling cost");
          toolingTitleCell.setCellStyle(sectionHeaderStyle);
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 9, 16)); // J:Q
          Cell otherCostTitleCell = currentRow.createCell(9);
          otherCostTitleCell.setCellValue("(5) Other cost");
          otherCostTitleCell.setCellStyle(sectionHeaderStyle);
          rowIndex++;

          // Column headers on the same row
          currentRow = sheet.createRow(rowIndex);
          // Tooling headers: B, C:D merged, E, F, G, H
          currentRow.createCell(1).setCellStyle(colHeaderStyle);
          currentRow.getCell(1).setCellValue("No.");
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 2, 3)); // C:D
          Cell toolingDescHeader = currentRow.createCell(2);
          toolingDescHeader.setCellValue("Description");
          toolingDescHeader.setCellStyle(colHeaderStyle);
          String[] toolingColHeaders = {"Qty", "U/P (MYR)", "Cost (MYR)", "Remark"};
          for (int i = 0; i < toolingColHeaders.length; i++) {
            Cell cell = currentRow.createCell(4 + i); // E=4, F=5, G=6, H=7
            cell.setCellValue(toolingColHeaders[i]);
            cell.setCellStyle(colHeaderStyle);
          }
          // Other cost headers: J, Description merged, O, P, Q
          currentRow.createCell(9).setCellStyle(colHeaderStyle);
          currentRow.getCell(9).setCellValue("No.");
          if (hasShipmentLocations) {
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 10, 11)); // K:L
            Cell otherCostDescHeader = currentRow.createCell(10);
            otherCostDescHeader.setCellValue("Description");
            otherCostDescHeader.setCellStyle(colHeaderStyle);
            Cell shipLocHeader = currentRow.createCell(12); // M
            shipLocHeader.setCellValue("Shipment Location");
            shipLocHeader.setCellStyle(colHeaderStyle);
            Cell currencyHeader = currentRow.createCell(13); // N
            currencyHeader.setCellValue("Currency");
            currencyHeader.setCellStyle(colHeaderStyle);
          } else {
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 10, 13)); // K:L:M:N
            Cell otherCostDescHeader = currentRow.createCell(10);
            otherCostDescHeader.setCellValue("Description");
            otherCostDescHeader.setCellStyle(colHeaderStyle);
          }
          String[] otherCostColHeaders = {
            "Strategy", hasShipmentLocations ? "U/P" : "U/P (MYR)", "Cost (MYR)"
          };
          for (int i = 0; i < otherCostColHeaders.length; i++) {
            Cell cell = currentRow.createCell(14 + i); // O=14, P=15, Q=16
            cell.setCellValue(otherCostColHeaders[i]);
            cell.setCellStyle(colHeaderStyle);
          }
          rowIndex++;

          int toolingOtherDataStartRowIdx = rowIndex;

          for (int i = 0; i < maxToolingOtherRows; i++) {
            Row row = sheet.createRow(rowIndex);
            Cell cell;

            // Tooling left side
            if (i < toolingLines.size()) {
              ToolingCostLineEntity tl = toolingLines.get(i);
              cell = row.createCell(1); // B: No.
              cell.setCellValue(i + 1);
              cell.setCellStyle(centeredDataStyle);
              sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 2, 3)); // C:D
              cell = row.createCell(2); // C: Description
              cell.setCellValue(tl.getName());
              cell.setCellStyle(greenFillStyle);
              cell = row.createCell(4); // E: Qty
              cell.setCellValue(tl.getQuantity().doubleValue());
              cell.setCellStyle(greenFillStyle);
              cell = row.createCell(5); // F: U/P (MYR)
              cell.setCellValue(tl.getUnitCostInCurrency().doubleValue());
              cell = row.createCell(6); // G: Cost (MYR)
              cell.setCellValue(tl.getTotalCostInSystemCurrency().doubleValue());
              cell = row.createCell(7); // H: Remark
              cell.setCellValue(tl.isOutsourced() ? "Outsourced" : null);
            }

            // Other cost right side
            if (i < otherCostLines.size()) {
              OtherCostLineEntity oc = otherCostLines.get(i);
              OtherCostLinePerCostRequestQuantityEntity ocpq =
                  oc.getOtherCostLineForCostRequestQuantities().stream()
                      .filter(p -> p.getCostRequestQuantity() == qty)
                      .findFirst()
                      .orElseThrow(
                          () ->
                              new GenericWithMessageException(
                                  "Other cost line per quantity not found",
                                  SWCustomErrorCode.GENERIC_ERROR));
              cell = row.createCell(9); // J: No.
              cell.setCellValue(i + 1);
              cell.setCellStyle(centeredDataStyle);
              if (hasShipmentLocations) {
                sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 10, 11)); // K:L
              } else {
                sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 10, 13)); // K:L:M:N
              }
              cell = row.createCell(10); // K: Description
              cell.setCellValue(oc.getName());
              cell.setCellStyle(greenFillStyle);
              if (hasShipmentLocations) {
                cell = row.createCell(12); // M: Shipment Location
                cell.setCellValue(currentLocation.getShipmentLocation().getName());
                cell = row.createCell(13); // N: Currency
                cell.setCellValue(
                    oc.isShipmentToCustomerLine() ? currentLocation.getCurrencyCode() : "MYR");
              }
              cell = row.createCell(14); // O: Strategy
              cell.setCellValue(oc.getCalculationStrategy().getHumanReadableValue());
              cell = row.createCell(15); // P: U/P (MYR)
              cell.setCellValue(ocpq.getUnitCostInCurrency().doubleValue());
              cell = row.createCell(16); // Q: Cost (MYR)
              cell.setCellValue(ocpq.getTotalCostInSystemCurrency().doubleValue());
            } else if (i == otherCostLines.size()) {
              // Supplier Shipment — fixed last other-cost row
              cell = row.createCell(9); // J: No.
              cell.setCellValue(otherCostLines.size() + 1);
              cell.setCellStyle(centeredDataStyle);
              if (hasShipmentLocations) {
                sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 10, 11)); // K:L
              } else {
                sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 10, 13)); // K:L:M:N
              }
              cell = row.createCell(10); // K: Description
              cell.setCellValue("Supplier Shipment");
              cell.setCellStyle(greenFillStyle);
              if (hasShipmentLocations) {
                cell = row.createCell(12); // M: Shipment Location
                cell.setCellValue(currentLocation.getShipmentLocation().getName());
                cell = row.createCell(13); // N: Currency
                cell.setCellValue(currentLocation.getCurrencyCode());
              }
              cell = row.createCell(14); // O: Strategy
              cell.setCellValue(OtherCostLineCalculationStrategy.AS_IS.getHumanReadableValue());
              cell = row.createCell(16); // Q: Cost = V{subTotal2WithYield}
              cell.setCellFormula("V" + subTotal2WithYieldExcelRow);
            }

            rowIndex++;
          }

          // Subtotal row — Sub Total (4) and Sub Total (5) on the same row
          Row subTotal45Row = sheet.createRow(rowIndex);
          // Sub Total (4): E:F merged label, G value (Cost MYR column)
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 4, 5)); // E:F
          Cell subTotal4LabelCell = subTotal45Row.createCell(4);
          subTotal4LabelCell.setCellValue("Sub Total (4)");
          subTotal4LabelCell.setCellStyle(subtotalLabelStyle);
          Cell subTotal4ValueCell = subTotal45Row.createCell(6); // G
          if (!toolingLines.isEmpty()) {
            String sumRange =
                "G"
                    + (toolingOtherDataStartRowIdx + 1)
                    + ":G"
                    + (toolingOtherDataStartRowIdx + toolingLines.size());
            subTotal4ValueCell.setCellFormula("SUM(" + sumRange + ")");
          } else {
            subTotal4ValueCell.setCellValue(0);
          }
          subTotal4ValueCell.setCellStyle(yellowValueStyle);
          // Sub Total (5): O:P merged label, Q value (Cost MYR column)
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 14, 15)); // O:P
          Cell subTotal5LabelCell = subTotal45Row.createCell(14);
          subTotal5LabelCell.setCellValue("Sub Total (5)");
          subTotal5LabelCell.setCellStyle(subtotalLabelStyle);
          Cell subTotal5ValueCell = subTotal45Row.createCell(16); // Q
          if (otherCostTotalRows > 0) {
            String sumRange =
                "Q"
                    + (toolingOtherDataStartRowIdx + 1)
                    + ":Q"
                    + (toolingOtherDataStartRowIdx + otherCostTotalRows);
            subTotal5ValueCell.setCellFormula("SUM(" + sumRange + ")");
          } else {
            subTotal5ValueCell.setCellValue(0);
          }
          subTotal5ValueCell.setCellStyle(yellowValueStyle);
          int subTotal45ExcelRow = rowIndex + 1; // 1-based Excel row for Sub Total (4)/(5)
          // Thin borders on header and data rows in sections (4) and (5)
          for (int ri = section45StartRowIdx + 1; ri <= rowIndex - 1; ri++) {
            for (int[] range : new int[][] {{1, 7}, {9, 16}}) { // tooling B:H | other cost J:Q
              for (int c = range[0]; c <= range[1]; c++) {
                CellRangeAddress addr = new CellRangeAddress(ri, ri, c, c);
                RegionUtil.setBorderTop(BorderStyle.THIN, addr, sheet);
                RegionUtil.setBorderBottom(BorderStyle.THIN, addr, sheet);
                RegionUtil.setBorderLeft(BorderStyle.THIN, addr, sheet);
                RegionUtil.setBorderRight(BorderStyle.THIN, addr, sheet);
              }
            }
          }
          // Thin borders on subtotal row: E:F:G for tooling, O:P:Q for other cost
          for (int[] range : new int[][] {{4, 6}, {14, 16}}) {
            for (int c = range[0]; c <= range[1]; c++) {
              CellRangeAddress addr = new CellRangeAddress(rowIndex, rowIndex, c, c);
              RegionUtil.setBorderTop(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderBottom(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderLeft(BorderStyle.THIN, addr, sheet);
              RegionUtil.setBorderRight(BorderStyle.THIN, addr, sheet);
            }
          }
          // Thick outer border for Sections (4)+(5) Tooling + Other cost
          CellRangeAddress section45Region =
              new CellRangeAddress(section45StartRowIdx, rowIndex, 1, 16);
          RegionUtil.setBorderTop(BorderStyle.THICK, section45Region, sheet);
          RegionUtil.setBorderBottom(BorderStyle.THICK, section45Region, sheet);
          RegionUtil.setBorderLeft(BorderStyle.THICK, section45Region, sheet);
          RegionUtil.setBorderRight(BorderStyle.THICK, section45Region, sheet);
          rowIndex++;
          rowIndex++; // blank separator row

          // ---- Summary Section ----

          // Summary header: B:Q merged, section header style (gray fill + bold)
          int summaryStartRowIdx = rowIndex;
          currentRow = sheet.createRow(rowIndex);
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 1, 16)); // B:Q
          Cell summaryHeaderCell = currentRow.createCell(1);
          summaryHeaderCell.setCellValue("Summary");
          summaryHeaderCell.setCellStyle(sectionHeaderStyle);
          rowIndex++;

          // Sub-section headers: currency (B-E) and total costs (G-I)
          currentRow = sheet.createRow(rowIndex);
          String[] currencyHeaders = {"No.", "Currency", "Exchange Rate"};
          for (int i = 0; i < currencyHeaders.length; i++) {
            Cell cell = currentRow.createCell(1 + i); // B=1, C=2, D=3
            cell.setCellValue(currencyHeaders[i]);
            cell.setCellStyle(colHeaderStyle);
          }
          String[] totalCostsHeaders = {"No.", "Description", "Amount (MYR)"};
          for (int i = 0; i < totalCostsHeaders.length; i++) {
            Cell cell = currentRow.createCell(6 + i); // G=6, H=7, I=8
            cell.setCellValue(totalCostsHeaders[i]);
            cell.setCellStyle(colHeaderStyle);
          }
          String[] pricingHeaders = {
            "Percentage",
            "Selling Price (MYR)",
            "Selling Price (USD)",
            "Selling Price (SGD)",
            "Margin (MYR)"
          };
          for (int i = 0; i < pricingHeaders.length; i++) {
            Cell cell = currentRow.createCell(12 + i); // M=12, N=13, O=14, P=15, Q=16
            cell.setCellValue(pricingHeaders[i]);
            cell.setCellStyle(colHeaderStyle);
          }
          rowIndex++;

          // Currency data from MYR exchange rates

          CurrencyEntity currentCurrency =
              costRequestLineHelper.resolveCurrency(
                  line, appConfigurationProperties.getTargetCurrencyCode());
          List<ExchangeRateEntity> exchangeRates = currentCurrency.getExchangeRates();

          // Sort by currency-list-ordering, then alphabetically
          List<String> currencyOrder = appConfigurationProperties.getCurrencyListOrdering();
          if (CollectionUtils.isNotEmpty(currencyOrder)) {
            exchangeRates.sort(
                Comparator.comparing(
                        (ExchangeRateEntity er) -> {
                          String code = er.getToCurrency().getCode();
                          int index = currencyOrder.indexOf(code);
                          return index == -1 ? currencyOrder.size() : index;
                        })
                    .thenComparing(er -> er.getToCurrency().getCode()));
          } else {
            exchangeRates.sort(Comparator.comparing(er -> er.getToCurrency().getCode()));
          }

          // Extract USD and SGD mid-rates as MYR per 1 foreign unit (= 1/rate)
          double usdBuyingRate =
              exchangeRates.stream()
                  .filter(er -> "USD".equalsIgnoreCase(er.getToCurrency().getCode()))
                  .findFirst()
                  .filter(
                      er -> er.getRate() != null && er.getRate().compareTo(BigDecimal.ZERO) != 0)
                  .map(
                      er ->
                          BigDecimal.ONE
                              .divide(er.getRate(), 6, RoundingMode.HALF_UP)
                              .doubleValue())
                  .orElse(0.0);
          double sgdBuyingRate =
              exchangeRates.stream()
                  .filter(er -> "SGD".equalsIgnoreCase(er.getToCurrency().getCode()))
                  .findFirst()
                  .filter(
                      er -> er.getRate() != null && er.getRate().compareTo(BigDecimal.ZERO) != 0)
                  .map(
                      er ->
                          BigDecimal.ONE
                              .divide(er.getRate(), 6, RoundingMode.HALF_UP)
                              .doubleValue())
                  .orElse(0.0);

          // 5 cost rows + 1 total
          String[] costDescriptions = {
            "Indirect Material", "Direct Material", "Process cost", "Tooling cost", "Other cost"
          };
          String[] costFormulas = {
            "Q" + subTotal1ExcelRow,
            "Q" + subTotal2WithYieldExcelRow,
            "Q" + subTotal3ExcelRow,
            "G" + subTotal45ExcelRow,
            "Q" + subTotal45ExcelRow
          };

          int[] pricingPercentages = {10, 15, 20, 25, 30, 40, 50};

          int summaryDataStartRowIdx = rowIndex;
          // 1-based Excel row of the "Total cost" row (last of the 6 right-side rows)
          int totalCostExcelRow = summaryDataStartRowIdx + costDescriptions.length + 1;
          // +1 for MYR row prepended; max across all three sub-sections
          int summaryRowCount =
              Math.max(Math.max(exchangeRates.size() + 1, 6), pricingPercentages.length);

          for (int i = 0; i < summaryRowCount; i++) {
            Row row = sheet.createRow(rowIndex);
            Cell cell;

            // Left side: currency data (B=1, C=2, D=3, E=4)
            // Row 0 is always MYR with Selling=1 and Buying=1
            if (i == 0) {
              cell = row.createCell(1); // B: No.
              cell.setCellValue(1);
              cell.setCellStyle(centeredDataStyle);
              cell = row.createCell(2); // C: Currency
              cell.setCellValue("MYR");
              cell = row.createCell(3); // D: Exchange Rate
              cell.setCellValue(1);
            } else if (i - 1 < exchangeRates.size()) {
              ExchangeRateEntity er = exchangeRates.get(i - 1);
              cell = row.createCell(1); // B: No.
              cell.setCellValue(i + 1);
              cell.setCellStyle(centeredDataStyle);
              cell = row.createCell(2); // C: Currency (toCurrency code; fromCurrency is MYR)
              cell.setCellValue(er.getToCurrency().getCode());
              cell = row.createCell(3); // D: Exchange Rate — MYR per 1 foreign = 1/rate
              if (er.getRate() != null && er.getRate().compareTo(BigDecimal.ZERO) != 0) {
                cell.setCellValue(
                    BigDecimal.ONE.divide(er.getRate(), 6, RoundingMode.HALF_UP).doubleValue());
              }
            }

            // Middle: cost rows (G=6, H=7, I=8)
            if (i < costDescriptions.length) {
              cell = row.createCell(6); // G: No.
              cell.setCellValue(i + 1);
              cell.setCellStyle(centeredDataStyle);
              cell = row.createCell(7); // H: Description
              cell.setCellValue(costDescriptions[i]);
              cell = row.createCell(8); // I: Amount
              cell.setCellFormula(costFormulas[i]);
            } else if (i == costDescriptions.length) {
              // Total row: "Total cost" bold, SUM of the 5 cost rows above
              cell = row.createCell(7); // H: Description
              cell.setCellValue("Total cost");
              cell.setCellStyle(subtotalLabelStyle);
              cell = row.createCell(8); // I: Amount
              String totalSumRange =
                  "I"
                      + (summaryDataStartRowIdx + 1)
                      + ":I"
                      + (summaryDataStartRowIdx + costDescriptions.length);
              cell.setCellFormula("SUM(" + totalSumRange + ")");
              cell.setCellStyle(yellowValueStyle);
            }

            // Right side: pricing rows (M=12, N=13, O=14, P=15, Q=16)
            if (i < pricingPercentages.length) {
              int pct = pricingPercentages[i];
              int currentExcelRow = rowIndex + 1; // 1-based
              cell = row.createCell(12); // M: Percentage
              cell.setCellValue(pct);
              cell = row.createCell(13); // N: Selling Price (MYR) = Total cost * (1 + pct/100)
              cell.setCellFormula("I" + totalCostExcelRow + "*(1+" + pct + "/100)");
              cell = row.createCell(14); // O: Selling Price (USD) = N / USD buying rate
              if (usdBuyingRate != 0) {
                cell.setCellFormula("N" + currentExcelRow + "/" + usdBuyingRate);
              } else {
                cell.setCellValue(0);
              }
              cell = row.createCell(15); // P: Selling Price (SGD) = N / SGD buying rate
              if (sgdBuyingRate != 0) {
                cell.setCellFormula("N" + currentExcelRow + "/" + sgdBuyingRate);
              } else {
                cell.setCellValue(0);
              }
              cell = row.createCell(16); // Q: Margin (MYR) = N - Total cost
              cell.setCellFormula("N" + currentExcelRow + "-I" + totalCostExcelRow);
            }

            rowIndex++;
          }
          // Thin borders for each summary sub-section (sub-headers row + data rows)
          for (int ri = summaryStartRowIdx + 1; ri <= rowIndex - 1; ri++) {
            for (int[] range : new int[][] {{1, 3}, {6, 8}, {12, 16}}) {
              for (int c = range[0]; c <= range[1]; c++) {
                CellRangeAddress addr = new CellRangeAddress(ri, ri, c, c);
                RegionUtil.setBorderTop(BorderStyle.THIN, addr, sheet);
                RegionUtil.setBorderBottom(BorderStyle.THIN, addr, sheet);
                RegionUtil.setBorderLeft(BorderStyle.THIN, addr, sheet);
                RegionUtil.setBorderRight(BorderStyle.THIN, addr, sheet);
              }
            }
          }
          // Thick outer border for the Summary section
          CellRangeAddress summaryRegion =
              new CellRangeAddress(summaryStartRowIdx, rowIndex - 1, 1, 16);
          RegionUtil.setBorderTop(BorderStyle.THICK, summaryRegion, sheet);
          RegionUtil.setBorderBottom(BorderStyle.THICK, summaryRegion, sheet);
          RegionUtil.setBorderLeft(BorderStyle.THICK, summaryRegion, sheet);
          RegionUtil.setBorderRight(BorderStyle.THICK, summaryRegion, sheet);

          // ---- Signature block (2 rows below summary) ----
          rowIndex++; // blank row 1

          // Label row: "Prepared By" B:F | "Checked By" G:L | "Approved By" M:Q
          int signBlockStartRowIdx = rowIndex;
          Row signLabelRow = sheet.createRow(rowIndex);
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 1, 5)); // B:F
          Cell preparedByLabel = signLabelRow.createCell(1);
          preparedByLabel.setCellValue("Prepared By");
          preparedByLabel.setCellStyle(colHeaderStyle);
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 6, 11)); // G:L
          Cell checkedByLabel = signLabelRow.createCell(6);
          checkedByLabel.setCellValue("Checked By");
          checkedByLabel.setCellStyle(colHeaderStyle);
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 12, 16)); // M:Q
          Cell approvedByLabel = signLabelRow.createCell(12);
          approvedByLabel.setCellValue("Approved By");
          approvedByLabel.setCellStyle(colHeaderStyle);
          rowIndex++;

          // Signature boxes: same horizontal spans, spanning 3 rows vertically
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 2, 1, 5)); // B:F x3
          sheet.createRow(rowIndex).createCell(1);
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 2, 6, 11)); // G:L x3
          sheet.getRow(rowIndex).createCell(6);
          sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 2, 12, 16)); // M:Q x3
          sheet.getRow(rowIndex).createCell(12);
          rowIndex += 3;
          int signBlockEndRowIdx = rowIndex - 1;
          // Thick border around each individual signature column (outer rectangle)
          CellRangeAddress preparedByRegion =
              new CellRangeAddress(signBlockStartRowIdx, signBlockEndRowIdx, 1, 5); // B:F
          RegionUtil.setBorderTop(BorderStyle.THICK, preparedByRegion, sheet);
          RegionUtil.setBorderBottom(BorderStyle.THICK, preparedByRegion, sheet);
          RegionUtil.setBorderLeft(BorderStyle.THICK, preparedByRegion, sheet);
          RegionUtil.setBorderRight(BorderStyle.THICK, preparedByRegion, sheet);
          CellRangeAddress checkedByRegion =
              new CellRangeAddress(signBlockStartRowIdx, signBlockEndRowIdx, 6, 11); // G:L
          RegionUtil.setBorderTop(BorderStyle.THICK, checkedByRegion, sheet);
          RegionUtil.setBorderBottom(BorderStyle.THICK, checkedByRegion, sheet);
          RegionUtil.setBorderLeft(BorderStyle.THICK, checkedByRegion, sheet);
          RegionUtil.setBorderRight(BorderStyle.THICK, checkedByRegion, sheet);
          CellRangeAddress approvedByRegion =
              new CellRangeAddress(signBlockStartRowIdx, signBlockEndRowIdx, 12, 16); // M:Q
          RegionUtil.setBorderTop(BorderStyle.THICK, approvedByRegion, sheet);
          RegionUtil.setBorderBottom(BorderStyle.THICK, approvedByRegion, sheet);
          RegionUtil.setBorderLeft(BorderStyle.THICK, approvedByRegion, sheet);
          RegionUtil.setBorderRight(BorderStyle.THICK, approvedByRegion, sheet);
          // Thick bottom on the label row — separates label from the signature box below
          CellRangeAddress preparedByLabelRow =
              new CellRangeAddress(signBlockStartRowIdx, signBlockStartRowIdx, 1, 5);
          RegionUtil.setBorderBottom(BorderStyle.THICK, preparedByLabelRow, sheet);
          CellRangeAddress checkedByLabelRow =
              new CellRangeAddress(signBlockStartRowIdx, signBlockStartRowIdx, 6, 11);
          RegionUtil.setBorderBottom(BorderStyle.THICK, checkedByLabelRow, sheet);
          CellRangeAddress approvedByLabelRow =
              new CellRangeAddress(signBlockStartRowIdx, signBlockStartRowIdx, 12, 16);
          RegionUtil.setBorderBottom(BorderStyle.THICK, approvedByLabelRow, sheet);
        } // end locationIterations loop
      }

      workbook.write(out);
      return out.toByteArray();

    } catch (IOException e) {
      log.error("Error generating quotation breakdown XLSX", e);
      throw new GenericWithMessageException(
          "Error generating quotation breakdown XLSX", SWCustomErrorCode.GENERIC_ERROR);
    }
  }
}
