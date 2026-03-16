package my.lokalix.planning.core.services.helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.OtherCostLineShipmentLocationInfos;
import my.lokalix.planning.core.models.TotalCosts;
import my.lokalix.planning.core.models.entities.CostRequestEntity;
import my.lokalix.planning.core.models.entities.CostRequestFrozenShipmentLocationEntity;
import my.lokalix.planning.core.models.entities.CostRequestLineEntity;
import my.lokalix.planning.core.models.entities.OtherCostLineEntity;
import my.lokalix.planning.core.models.entities.admin.*;
import my.lokalix.planning.core.models.enums.CostRequestStatus;
import my.lokalix.planning.core.repositories.TermsAndConditionsDysonRepository;
import my.lokalix.planning.core.repositories.TermsAndConditionsNonDysonRepository;
import my.lokalix.planning.core.services.validator.CostRequestValidator;
import my.lokalix.planning.core.utils.CurrencyUtils;
import my.lokalix.planning.core.utils.NumberUtils;
import my.lokalix.planning.core.utils.TimeUtils;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import my.zkonsulting.planning.generated.model.SWGenerateQuotationPdfBody;
import my.zkonsulting.planning.generated.model.SWQuotationLineExtraInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostRequestPdfHelper {
  private final TermsAndConditionsDysonRepository termsAndConditionsDysonRepository;
  private final TermsAndConditionsNonDysonRepository termsAndConditionsNonDysonRepository;
  private final CostRequestValidator costRequestValidator;
  private final AppConfigurationProperties appConfigurationProperties;
  private final CostRequestLineCalculationsHelper costRequestLineCalculationsHelper;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final CostRequestLineHelper costRequestLineHelper;

  public byte[] buildCostRequestQuotationPdf(
      CostRequestEntity costRequestEntity, SWGenerateQuotationPdfBody extraInfos) {
    try (PDDocument document = new PDDocument()) {
      PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
      PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

      float margin = 25f;
      float pageWidth = PDRectangle.A4.getWidth();
      float pageHeight = PDRectangle.A4.getHeight();
      float rowHeight = 18f;
      float cellPadding = 3f;
      float infoFontSize = 10f;
      float infoLineHeight = 14f;
      float tableFontSize = 9f;

      CurrencyEntity systemCurrency =
          entityRetrievalHelper.getMustExistCurrencyByCode(
              appConfigurationProperties.getTargetCurrencyCode());

      List<CostRequestFrozenShipmentLocationEntity> frozenLocations =
          costRequestEntity.getFrozenShipmentLocations().stream()
              .filter(fl -> !fl.isMasked())
              .sorted(Comparator.comparingInt(CostRequestFrozenShipmentLocationEntity::getIndexId))
              .toList();
      boolean hasShipmentLocations = !frozenLocations.isEmpty();

      // Table column widths (total = pageWidth - 2*margin = 545.28)
      // Fixed cols: No.(25) + PartNumber(130) + MOQ(50) + LeadTime(55) + Remarks(75) = 335
      // Remaining 210.28 split equally among location price columns
      float[] colWidths;
      String[] colHeaders;
      if (hasShipmentLocations) {
        int locCount = frozenLocations.size();
        float locColWidth = (545.28f - 335f) / locCount;
        colWidths = new float[3 + locCount + 2];
        colWidths[0] = 25f;
        colWidths[1] = 130f;
        colWidths[2] = 50f;
        for (int i = 0; i < locCount; i++) colWidths[3 + i] = locColWidth;
        colWidths[3 + locCount] = 55f;
        colWidths[3 + locCount + 1] = 75f;

        colHeaders = new String[3 + locCount + 2];
        colHeaders[0] = "No.";
        colHeaders[1] = "Part Number";
        colHeaders[2] = "MOQ (Pc)";
        for (int i = 0; i < locCount; i++) {
          CostRequestFrozenShipmentLocationEntity loc = frozenLocations.get(i);
          colHeaders[3 + i] =
              loc.getShipmentLocation().getName() + " (" + loc.getCurrencyCode() + ")\nU/P";
        }
        colHeaders[3 + locCount] = "Lead Time";
        colHeaders[3 + locCount + 1] = "Remarks";
      } else {
        colWidths = new float[] {25f, 180f, 70f, 70f, 75f, 125f};
        colHeaders =
            new String[] {"No.", "Part Number", "MOQ (Pc)", "Unit Price", "Lead Time", "Remarks"};
      }
      boolean[] allCenterCols = new boolean[colWidths.length];
      Arrays.fill(allCenterCols, true);

      List<CostRequestLineEntity> lines =
          costRequestEntity.getLines().stream()
              .filter(line -> line.getStatus() == CostRequestStatus.READY_TO_QUOTE)
              .toList();

      // Build flat list of (line, lineIndex, quantity, unitPrice, location)
      // For multi shipment location: one row per (line × quantity × shipmentLocation)
      // For non-Dyson: one row per (line × quantity)
      record RowEntry(
          CostRequestLineEntity line,
          int lineIndex,
          int quantity,
          List<BigDecimal> unitPrices,
          List<String> currencyCodes) {}
      BigDecimal totalToolingPrice = BigDecimal.ZERO;
      List<RowEntry> rowEntries = new ArrayList<>();

      GlobalConfigEntity globalConfig = entityRetrievalHelper.getMustExistGlobalConfig();

      for (int li = 0; li < lines.size(); li++) {
        CostRequestLineEntity lineEntity = lines.get(li);

        BigDecimal lineMarkup = lineEntity.getMarkup();
        BigDecimal lineToolingMarkup = lineEntity.getToolingMarkup();
        BigDecimal markupMultiplier =
            lineMarkup != null
                ? BigDecimal.ONE.add(
                    lineMarkup.divide(new BigDecimal(100), 6, RoundingMode.HALF_UP))
                : null;
        BigDecimal toolingMarkupMultiplier =
            lineToolingMarkup != null
                ? BigDecimal.ONE.add(
                    lineToolingMarkup.divide(new BigDecimal(100), 6, RoundingMode.HALF_UP))
                : null;

        BigDecimal additionalProcessRatePerMethodType =
            costRequestLineHelper.resolveAdditionalProcessRate(lineEntity, globalConfig);
        BigDecimal materialYieldPercentage =
            costRequestLineHelper.resolveYield(lineEntity, globalConfig);

        List<Integer> qtys = lineEntity.getQuantitiesAsList();

        if (hasShipmentLocations) {
          // Map each frozen location to its matching shipment-to-customer OtherCostLine
          List<OtherCostLineShipmentLocationInfos> locInfos = new ArrayList<>();
          for (CostRequestFrozenShipmentLocationEntity frozenLoc : frozenLocations) {
            OtherCostLineEntity matchingOcl =
                lineEntity.getOtherCostLines().stream()
                    .filter(OtherCostLineEntity::isShipmentToCustomerLine)
                    .filter(
                        ocl ->
                            ocl.getShipmentLocation() != null
                                && ocl.getShipmentLocation()
                                    .getShipmentLocationId()
                                    .equals(frozenLoc.getShipmentLocation().getShipmentLocationId())
                                && ocl.getCurrency().getCode().equals(frozenLoc.getCurrencyCode()))
                    .findFirst()
                    .orElseThrow(
                        () ->
                            new GenericWithMessageException(
                                "No shipment to customer other cost line found for location '"
                                    + frozenLoc.getShipmentLocation().getName()
                                    + "'",
                                SWCustomErrorCode.GENERIC_ERROR));
            locInfos.add(
                new OtherCostLineShipmentLocationInfos(
                    matchingOcl.getOtherCostLineId(),
                    matchingOcl.getShipmentLocation(),
                    matchingOcl.getCurrency()));
          }

          List<String> locationCurrencyCodes =
              locInfos.stream().map(loc -> loc.getCurrency().getCode()).toList();

          for (int i = 0; i < qtys.size(); i++) {
            int qty = qtys.get(i);
            int finalI = i;
            List<BigDecimal> unitPricesForRow = new ArrayList<>();
            for (int locIdx = 0; locIdx < locInfos.size(); locIdx++) {
              OtherCostLineShipmentLocationInfos loc = locInfos.get(locIdx);
              BigDecimal locExchangeRate =
                  systemCurrency.findExchangeRate(loc.getCurrency().getCode());
              TotalCosts totalCosts =
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
                      loc.getOtherCostLineId(),
                      null,
                      null,
                      locExchangeRate);

              BigDecimal totalCostWithMarkup;
              switch (lineEntity.getToolingStrategy()) {
                case AMORTIZED ->
                    totalCostWithMarkup =
                        totalCosts
                            .getTotalCostWithToolingInTargetCurrency()
                            .multiply(markupMultiplier);
                case SEPARATED -> {
                  totalCostWithMarkup =
                      totalCosts
                          .getTotalCostWithoutToolingInTargetCurrency()
                          .multiply(markupMultiplier);
                  if (i == 0 && locIdx == 0) {
                    totalToolingPrice =
                        totalToolingPrice.add(
                            totalCosts
                                .getTotalToolingCostInTargetCurrency()
                                .multiply(toolingMarkupMultiplier));
                  }
                }
                default ->
                    throw new IllegalStateException(
                        "Unexpected value: " + lineEntity.getToolingStrategy());
              }
              unitPricesForRow.add(
                  totalCostWithMarkup.divide(new BigDecimal(qty), 6, RoundingMode.HALF_UP));
            }
            rowEntries.add(
                new RowEntry(lineEntity, li, qty, unitPricesForRow, locationCurrencyCodes));
          }
        } else {
          for (int i = 0; i < qtys.size(); i++) {
            int qty = qtys.get(i);
            int finalI = i;
            TotalCosts totalCosts =
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
                    null,
                    null,
                    null,
                    systemCurrency.findExchangeRate(costRequestEntity.getCurrency().getCode()));

            BigDecimal totalCostWithMarkup;
            switch (lineEntity.getToolingStrategy()) {
              case AMORTIZED ->
                  totalCostWithMarkup =
                      totalCosts
                          .getTotalCostWithToolingInTargetCurrency()
                          .multiply(markupMultiplier);
              case SEPARATED -> {
                totalCostWithMarkup =
                    totalCosts
                        .getTotalCostWithoutToolingInTargetCurrency()
                        .multiply(markupMultiplier);
                if (i == 0) {
                  totalToolingPrice =
                      totalToolingPrice.add(
                          totalCosts
                              .getTotalToolingCostInTargetCurrency()
                              .multiply(toolingMarkupMultiplier));
                }
              }
              default ->
                  throw new IllegalStateException(
                      "Unexpected value: " + lineEntity.getToolingStrategy());
            }

            String crCurrencyCode =
                costRequestEntity.getCurrency() != null
                    ? costRequestEntity.getCurrency().getCode()
                    : "";
            rowEntries.add(
                new RowEntry(
                    lineEntity,
                    li,
                    qty,
                    List.of(
                        totalCostWithMarkup.divide(new BigDecimal(qty), 6, RoundingMode.HALF_UP)),
                    List.of(crCurrencyCode)));
          }
        }
      }
      List<SWQuotationLineExtraInfo> lineExtraInfos = extraInfos.getLineExtraInfos();
      if (lineExtraInfos == null || lineExtraInfos.size() != lines.size()) {
        throw new GenericWithMessageException(
            "lineExtraInfos must contain exactly "
                + lines.size()
                + " entr"
                + (lines.size() == 1 ? "y" : "ies")
                + " (one per QUOTED line)",
            SWCustomErrorCode.GENERIC_ERROR);
      }

      // Pre-build row data and compute variable heights for each data row
      List<String[]> rowDataList = new ArrayList<>();
      List<Float> rowHeights = new ArrayList<>();
      for (int i = 0; i < rowEntries.size(); i++) {
        RowEntry entry = rowEntries.get(i);
        String[] rowData =
            buildRowData(
                entry.line(),
                i,
                entry.quantity(),
                entry.unitPrices(),
                entry.currencyCodes(),
                lineExtraInfos.get(entry.lineIndex()));
        rowDataList.add(rowData);
        rowHeights.add(
            computeRowHeight(
                rowData, fontRegular, tableFontSize, colWidths, cellPadding, rowHeight));
      }

      // Add a single TOOLING COST row if any line uses the SEPARATED tooling strategy
      if (totalToolingPrice.compareTo(BigDecimal.ZERO) > 0) {
        String[] toolingRow;
        if (hasShipmentLocations) {
          int locCount = frozenLocations.size();
          toolingRow = new String[3 + locCount + 2];
          toolingRow[0] = String.valueOf(rowDataList.size() + 1);
          String toolingCurrencyCode = frozenLocations.getFirst().getCurrencyCode();
          toolingRow[1] = "TOOLING COST (" + toolingCurrencyCode + ")";
          toolingRow[2] = "1";
          for (int i = 0; i < locCount; i++) {
            toolingRow[3 + i] =
                i == 0
                    ? CurrencyUtils.getCurrencyPrefix(toolingCurrencyCode)
                        + formatPdfPrice(totalToolingPrice)
                    : "";
          }
          toolingRow[3 + locCount] = "";
          toolingRow[3 + locCount + 1] = "";
        } else {
          String quotationCurrencyCode =
              costRequestEntity.getCurrency() != null
                  ? costRequestEntity.getCurrency().getCode()
                  : appConfigurationProperties.getTargetCurrencyCode();
          toolingRow =
              new String[] {
                String.valueOf(rowDataList.size() + 1),
                "TOOLING COST",
                "1",
                CurrencyUtils.getCurrencyPrefix(quotationCurrencyCode)
                    + formatPdfPrice(totalToolingPrice),
                "",
                ""
              };
        }
        rowDataList.add(toolingRow);
        rowHeights.add(
            computeRowHeight(
                toolingRow, fontRegular, tableFontSize, colWidths, cellPadding, rowHeight));
      }

      int lineCount = rowDataList.size();

      float logoHeight = 60f;
      float headerRowHeight =
          computeRowHeight(colHeaders, fontBold, tableFontSize, colWidths, cellPadding, rowHeight);
      // Common header height: logo + gap + 6 info rows + gap (same on every page)
      float commonHeaderHeight =
          logoHeight + 25f + infoLineHeight * (hasShipmentLocations ? 5 : 6) + 15f;
      float quotationTitleHeight = 26f;
      float availableHeightPage1 =
          pageHeight - 2 * margin - commonHeaderHeight - quotationTitleHeight - headerRowHeight;
      float availableHeightOtherTablePages =
          pageHeight - 2 * margin - commonHeaderHeight - headerRowHeight;
      float availableHeightTcPages = pageHeight - 2 * margin - commonHeaderHeight;

      // Compute table page assignments: each entry is [startRowIndex, endRowIndex]
      List<int[]> pageAssignments = new ArrayList<>();
      int idx = 0;
      {
        int startIdx = idx;
        float used = 0f;
        while (idx < lineCount) {
          float rh = rowHeights.get(idx);
          if (used + rh > availableHeightPage1) break;
          used += rh;
          idx++;
        }
        pageAssignments.add(new int[] {startIdx, idx});
      }
      while (idx < lineCount) {
        int startIdx = idx;
        float used = 0f;
        while (idx < lineCount) {
          float rh = rowHeights.get(idx);
          if (used + rh > availableHeightOtherTablePages) break;
          used += rh;
          idx++;
        }
        if (idx == startIdx) idx++; // safety: always advance at least one row
        pageAssignments.add(new int[] {startIdx, Math.min(idx, lineCount)});
      }
      if (pageAssignments.isEmpty()) {
        pageAssignments.add(new int[] {0, 0});
      }
      int tablePageCount = pageAssignments.size();

      // Pre-compute if the signature block can fit below the table on the last table page.
      // sigTotalNeeded: gap(20) + "Kindly..." line(10) + gap(10) + sig block(130)
      float sigFontSize = 8f;
      float sigLineSpacing = sigFontSize + 2f;
      String globalComment =
          StringUtils.isNotBlank(extraInfos.getGlobalComment())
              ? extraInfos.getGlobalComment().trim()
              : null;
      float globalCommentHeight = 0f;
      if (globalComment != null) {
        List<String> commentLines =
            wrapText(globalComment, fontRegular, sigFontSize, pageWidth - 2 * margin);
        globalCommentHeight = commentLines.size() * sigLineSpacing + 6f; // 6f gap after block
      }
      float sigTotalNeeded = 20f + globalCommentHeight + sigLineSpacing + 10f + 130f;
      int sigExtraPageCount;
      {
        int lastIdx = pageAssignments.size() - 1;
        int[] lastAsgn = pageAssignments.get(lastIdx);
        float avail = (lastIdx == 0) ? availableHeightPage1 : availableHeightOtherTablePages;
        float used = 0f;
        for (int i = lastAsgn[0]; i < lastAsgn[1]; i++) used += rowHeights.get(i);
        sigExtraPageCount = (avail - used >= sigTotalNeeded) ? 0 : 1;
      }

      // Pre-load T&C and count its pages so totalPages is known before rendering
      CustomerEntity customer = costRequestEntity.getCustomer();
      List<TcLine> tcBlocks = null;
      int tcPageCount = 0;
      if (customer != null) {
        if (customer.isDyson()) {
          Optional<TermsAndConditionsDysonEntity> tcOpt =
              termsAndConditionsDysonRepository.findById(customer.getCustomerId());
          costRequestValidator.validateDysonTcCompleteForPdf(tcOpt);
          costRequestEntity.setExpirationDate(
              TimeUtils.nowLocalDate(appConfigurationProperties.getAppTimezone())
                  .plusDays(tcOpt.get().getValidityNumberOfDays()));
          tcBlocks = buildDysonTcBlocks(tcOpt.get(), LocalDate.now());
        } else {
          Optional<TermsAndConditionsNonDysonEntity> tcOpt =
              termsAndConditionsNonDysonRepository.findById(customer.getCustomerId());
          costRequestValidator.validateNonDysonTcCompleteForPdf(tcOpt);
          costRequestEntity.setExpirationDate(
              TimeUtils.nowLocalDate(appConfigurationProperties.getAppTimezone())
                  .plusDays(tcOpt.get().getValidityNumberOfDays()));
          tcBlocks = buildNonDysonTcBlocks(tcOpt.get());
        }
        tcPageCount =
            countTcPages(
                tcBlocks, fontBold, fontRegular, availableHeightTcPages, pageWidth - 2 * margin);
      }

      int totalPages = tablePageCount + sigExtraPageCount + tcPageCount;

      // Load logo from classpath
      byte[] logoBytes = null;
      try (InputStream logoStream =
          getClass().getResourceAsStream("/images/seiko-logo-extracted-from-quotation-excel.png")) {
        if (logoStream != null) {
          logoBytes = logoStream.readAllBytes();
        }
      } catch (Exception e) {
        log.warn("Could not load Seiko logo: {}", e.getMessage());
      }

      String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
      String currencyCode =
          hasShipmentLocations
              ? null
              : costRequestEntity.getCurrency() != null
                  ? costRequestEntity.getCurrency().getCode()
                  : "";
      String customerName =
          costRequestEntity.getCustomer() != null ? costRequestEntity.getCustomer().getName() : "";
      String refNumber = costRequestEntity.getCostRequestReferenceNumber();
      String revision = String.valueOf(costRequestEntity.getCostRequestRevision());

      TcHeaderContext headerCtx =
          new TcHeaderContext(
              document,
              logoBytes,
              fontBold,
              fontRegular,
              infoFontSize,
              infoLineHeight,
              logoHeight,
              margin,
              pageWidth,
              pageHeight,
              customerName,
              refNumber,
              dateStr,
              currencyCode,
              revision,
              extraInfos.getPaymentTerms() != null ? extraInfos.getPaymentTerms() : "",
              totalPages);

      for (int pageNum = 1; pageNum <= tablePageCount; pageNum++) {
        int[] assignment = pageAssignments.get(pageNum - 1);
        int startRow = assignment[0];
        int endRow = assignment[1];
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
          float y = drawPageHeader(cs, headerCtx, pageNum);

          if (pageNum == 1) {
            // "Quotation" heading (only on first table page)
            cs.beginText();
            cs.setFont(fontBold, 16f);
            cs.newLineAtOffset(margin, y);
            cs.showText("Quotation");
            cs.endText();
            y -= quotationTitleHeight;
          }

          // Draw table header row
          float tableTop = y;
          y =
              drawTableRow(
                  cs,
                  fontBold,
                  tableFontSize,
                  margin,
                  y,
                  headerRowHeight,
                  colWidths,
                  colHeaders,
                  cellPadding,
                  pageWidth,
                  allCenterCols);

          // Draw data rows
          for (int i = startRow; i < endRow; i++) {
            y =
                drawTableRow(
                    cs,
                    fontRegular,
                    tableFontSize,
                    margin,
                    y,
                    rowHeights.get(i),
                    colWidths,
                    rowDataList.get(i),
                    cellPadding,
                    pageWidth,
                    allCenterCols);
          }

          // Close table with bottom border
          cs.moveTo(margin, y);
          cs.lineTo(pageWidth - margin, y);
          cs.stroke();

          // Draw vertical lines for table columns
          float tableBottom = y;
          float colX = margin;
          for (float colWidth : colWidths) {
            cs.moveTo(colX, tableTop);
            cs.lineTo(colX, tableBottom);
            cs.stroke();
            colX += colWidth;
          }
          // Right border
          cs.moveTo(colX, tableTop);
          cs.lineTo(colX, tableBottom);
          cs.stroke();
        }
      }

      // Draw signature block on (or after) the last table page
      PDType1Font fontItalicSig = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
      {
        int lastIdx = pageAssignments.size() - 1;
        int[] lastAsgn = pageAssignments.get(lastIdx);
        float avail = (lastIdx == 0) ? availableHeightPage1 : availableHeightOtherTablePages;
        float used = 0f;
        for (int i = lastAsgn[0]; i < lastAsgn[1]; i++) used += rowHeights.get(i);
        float remaining = avail - used;

        PDPage sigPage;
        PDPageContentStream sigCs;
        float sigY;
        int sigPageNum;

        if (remaining >= sigTotalNeeded) {
          // Fits on last table page — open in append mode
          sigPage = document.getPage(document.getNumberOfPages() - 1);
          sigCs =
              new PDPageContentStream(
                  document, sigPage, PDPageContentStream.AppendMode.APPEND, true, true);
          // Compute y: top of content area minus everything above the remaining space
          sigY = margin + remaining;
          sigPageNum = tablePageCount;
        } else {
          // Needs its own page
          sigPage = new PDPage(PDRectangle.A4);
          document.addPage(sigPage);
          sigCs = new PDPageContentStream(document, sigPage);
          sigPageNum = tablePageCount + 1;
          sigY = drawPageHeader(sigCs, headerCtx, sigPageNum);
        }

        try {
          sigY -= 20f;

          // Global comment (optional), rendered before signature text
          if (globalComment != null) {
            List<String> commentLines =
                wrapText(globalComment, fontRegular, sigFontSize, pageWidth - 2 * margin);
            for (String line : commentLines) {
              sigCs.beginText();
              sigCs.setFont(fontRegular, sigFontSize);
              sigCs.newLineAtOffset(margin, sigY - sigFontSize);
              sigCs.showText(sanitizeTextForPdf(line));
              sigCs.endText();
              sigY -= sigLineSpacing;
            }
            sigY -= 6f;
          }

          // "Kindly call me if there is any queries." (italic)
          sigCs.beginText();
          sigCs.setFont(fontItalicSig, sigFontSize);
          sigCs.newLineAtOffset(margin, sigY - sigFontSize);
          sigCs.showText("Kindly call me if there is any queries.");
          sigCs.endText();
          sigY -= sigLineSpacing + 10f;

          float barWidth = 120f;
          float leftX = margin;
          float rightX = margin + (pageWidth - 2 * margin) / 2f;

          // "Prepared by," (left) and "Approved by," (right)
          sigCs.beginText();
          sigCs.setFont(fontRegular, sigFontSize);
          sigCs.newLineAtOffset(leftX, sigY - sigFontSize);
          sigCs.showText("Prepared by,");
          sigCs.endText();
          sigCs.beginText();
          sigCs.setFont(fontRegular, sigFontSize);
          sigCs.newLineAtOffset(rightX, sigY - sigFontSize);
          sigCs.showText("Approved by,");
          sigCs.endText();
          sigY -= sigLineSpacing + 16f;

          // Names above the strokes
          String preparedByName =
              extraInfos.getPreparedBy() != null ? extraInfos.getPreparedBy() : "";
          String approvedByName =
              extraInfos.getApprovedBy() != null ? extraInfos.getApprovedBy() : "";
          sigCs.beginText();
          sigCs.setFont(fontRegular, sigFontSize);
          sigCs.newLineAtOffset(
              leftX
                  + Math.max(
                      0f, (barWidth - textWidth(preparedByName, fontRegular, sigFontSize)) / 2f),
              sigY - sigFontSize);
          sigCs.showText(sanitizeTextForPdf(preparedByName));
          sigCs.endText();
          sigCs.beginText();
          sigCs.setFont(fontRegular, sigFontSize);
          sigCs.newLineAtOffset(
              rightX
                  + Math.max(
                      0f, (barWidth - textWidth(approvedByName, fontRegular, sigFontSize)) / 2f),
              sigY - sigFontSize);
          sigCs.showText(sanitizeTextForPdf(approvedByName));
          sigCs.endText();
          sigY -= sigLineSpacing + 4f;

          // Bars below names
          sigCs.moveTo(leftX, sigY);
          sigCs.lineTo(leftX + barWidth, sigY);
          sigCs.stroke();
          sigCs.moveTo(rightX, sigY);
          sigCs.lineTo(rightX + barWidth, sigY);
          sigCs.stroke();
          sigY -= sigLineSpacing * 2;

          // "Accepted by,"
          sigCs.beginText();
          sigCs.setFont(fontRegular, sigFontSize);
          sigCs.newLineAtOffset(leftX, sigY - sigFontSize);
          sigCs.showText("Accepted by,");
          sigCs.endText();
          sigY -= sigLineSpacing + 28f;

          // Bar below "Accepted by"
          sigCs.moveTo(leftX, sigY);
          sigCs.lineTo(leftX + barWidth, sigY);
          sigCs.stroke();
          sigY -= sigLineSpacing;

          // "Customer's Signature & Stamping"
          sigCs.beginText();
          sigCs.setFont(fontRegular, sigFontSize);
          sigCs.newLineAtOffset(leftX, sigY - sigFontSize);
          sigCs.showText("Customer's Signature & Stamping");
          sigCs.endText();
        } finally {
          sigCs.close();
        }
      }

      // Render T&C on its own page(s)
      if (tcBlocks != null) {
        renderDysonTc(tcBlocks, headerCtx, tablePageCount + sigExtraPageCount + 1);
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      document.save(out);
      return out.toByteArray();

    } catch (IOException e) {
      log.error("Error generating quotation PDF", e);
      throw new GenericWithMessageException(
          "Error generating quotation PDF", SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  private void drawInfoLine(
      PDPageContentStream cs,
      PDType1Font fontBold,
      PDType1Font fontRegular,
      float fontSize,
      float x,
      float y,
      String label,
      String value)
      throws IOException {
    cs.beginText();
    cs.setFont(fontBold, fontSize);
    cs.newLineAtOffset(x, y);
    cs.showText(sanitizeTextForPdf(label));
    cs.setFont(fontRegular, fontSize);
    cs.showText(sanitizeTextForPdf(value != null ? value : ""));
    cs.endText();
  }

  private void drawAlignedInfoLine(
      PDPageContentStream cs,
      PDType1Font fontBold,
      PDType1Font fontRegular,
      float fontSize,
      float labelStartX,
      float colonX,
      float y,
      String labelBeforeColon,
      String value)
      throws IOException {
    float colonWidth = fontBold.getStringWidth(":") / 1000f * fontSize;
    float gap = 4f;

    cs.beginText();
    cs.setFont(fontBold, fontSize);
    cs.newLineAtOffset(labelStartX, y);
    cs.showText(sanitizeTextForPdf(labelBeforeColon));
    cs.endText();

    cs.beginText();
    cs.setFont(fontBold, fontSize);
    cs.newLineAtOffset(colonX, y);
    cs.showText(":");
    cs.endText();

    cs.beginText();
    cs.setFont(fontRegular, fontSize);
    cs.newLineAtOffset(colonX + colonWidth + gap, y);
    cs.showText(sanitizeTextForPdf(value != null ? value : ""));
    cs.endText();
  }

  private float drawTableRow(
      PDPageContentStream cs,
      PDType1Font font,
      float fontSize,
      float margin,
      float y,
      float rowHeight,
      float[] colWidths,
      String[] cells,
      float cellPadding,
      float pageWidth,
      boolean[] centerColumns)
      throws IOException {
    float rowTop = y;
    float rowBottom = y - rowHeight;
    float lineSpacing = fontSize + 3f;

    // Top border of row
    cs.moveTo(margin, rowTop);
    cs.lineTo(pageWidth - margin, rowTop);
    cs.stroke();

    // Draw cell text (with word wrap)
    float cellX = margin;
    for (int col = 0; col < colWidths.length; col++) {
      String text = col < cells.length && cells[col] != null ? cells[col] : "";
      List<String> wrappedLines = wrapText(text, font, fontSize, colWidths[col] - cellPadding * 2);
      int numLines = wrappedLines.size();
      // Vertically center the text block within the row
      float firstLineBaseline =
          rowBottom + (rowHeight + (numLines - 1) * lineSpacing - fontSize) / 2f;
      boolean center = centerColumns != null && col < centerColumns.length && centerColumns[col];
      for (int li = 0; li < numLines; li++) {
        String lineText = wrappedLines.get(li);
        float textX;
        if (center) {
          textX = cellX + (colWidths[col] - textWidth(lineText, font, fontSize)) / 2f;
        } else {
          textX = cellX + cellPadding;
        }
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(textX, firstLineBaseline - li * lineSpacing);
        cs.showText(sanitizeTextForPdf(lineText));
        cs.endText();
      }
      cellX += colWidths[col];
    }

    return rowBottom;
  }

  private String[] buildRowData(
      CostRequestLineEntity line,
      int rowIndex,
      int quantity,
      List<BigDecimal> unitPrices,
      List<String> currencyCodes,
      SWQuotationLineExtraInfo extraInfo) {
    String no = String.valueOf(rowIndex + 1);
    String partNumber = line.getCustomerPartNumber() != null ? line.getCustomerPartNumber() : "";
    String moq = String.valueOf(quantity);
    String leadTime = extraInfo.getLeadTime() != null ? extraInfo.getLeadTime() : "";
    String remarks = extraInfo.getRemarks() != null ? extraInfo.getRemarks() : "";

    if (unitPrices.size() == 1) {
      String priceStr =
          CurrencyUtils.getCurrencyPrefix(currencyCodes.get(0)) + formatPdfPrice(unitPrices.get(0));
      return new String[] {no, partNumber, moq, priceStr, leadTime, remarks};
    }
    String[] row = new String[3 + unitPrices.size() + 2];
    row[0] = no;
    row[1] = partNumber;
    row[2] = moq;
    for (int i = 0; i < unitPrices.size(); i++) {
      row[3 + i] =
          CurrencyUtils.getCurrencyPrefix(currencyCodes.get(i)) + formatPdfPrice(unitPrices.get(i));
    }
    row[3 + unitPrices.size()] = leadTime;
    row[3 + unitPrices.size() + 1] = remarks;
    return row;
  }

  private String formatPdfPrice(BigDecimal price) {
    if (price == null) return "";
    DecimalFormat df = new DecimalFormat("#,##0.00");
    df.setRoundingMode(RoundingMode.HALF_UP);
    return sanitizeTextForPdf(df.format(price));
  }

  /**
   * Sanitizes text for PDF rendering by replacing characters not supported by WinAnsiEncoding. This
   * prevents IllegalArgumentException when rendering PDFs with standard fonts like Helvetica.
   */
  private String sanitizeTextForPdf(String text) {
    if (text == null) {
      return "";
    }

    return text
        // Replace NARROW NO-BREAK SPACE (U+202F) with regular space
        .replace("\u202F", " ")
        // Replace NO-BREAK SPACE (U+00A0) with regular space
        .replace("\u00A0", " ")
        // Replace other common problematic characters
        .replace("\u2013", "-") // EN DASH
        .replace("\u2014", "-") // EM DASH
        .replace("\u2018", "'") // LEFT SINGLE QUOTATION MARK
        .replace("\u2019", "'") // RIGHT SINGLE QUOTATION MARK
        .replace("\u201C", "\"") // LEFT DOUBLE QUOTATION MARK
        .replace("\u201D", "\"") // RIGHT DOUBLE QUOTATION MARK
        .replace("\u2022", "*") // BULLET
        .replace("\u2026", "..."); // HORIZONTAL ELLIPSIS
  }

  private float textWidth(String text, PDType1Font font, float fontSize) {
    try {
      return font.getStringWidth(text) / 1000f * fontSize;
    } catch (Exception e) {
      return text.length() * fontSize * 0.6f;
    }
  }

  private List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) {
    if (text == null || text.isEmpty()) return List.of("");
    List<String> result = new ArrayList<>();
    for (String paragraph : text.split("\n", -1)) {
      result.addAll(wrapParagraph(paragraph, font, fontSize, maxWidth));
    }
    return result.isEmpty() ? List.of("") : result;
  }

  private List<String> wrapParagraph(
      String text, PDType1Font font, float fontSize, float maxWidth) {
    if (text.isEmpty()) return List.of("");
    List<String> lines = new ArrayList<>();
    String[] words = text.split(" ", -1);
    StringBuilder current = new StringBuilder();
    for (String word : words) {
      String candidate = current.isEmpty() ? word : current + " " + word;
      if (textWidth(candidate, font, fontSize) <= maxWidth) {
        current = new StringBuilder(candidate);
      } else if (current.length() > 0) {
        lines.add(current.toString());
        current = new StringBuilder(word);
      } else {
        // Single word wider than column — break character by character
        for (char c : word.toCharArray()) {
          String test = current.toString() + c;
          if (textWidth(test, font, fontSize) <= maxWidth) {
            current.append(c);
          } else {
            if (current.length() > 0) lines.add(current.toString());
            current = new StringBuilder(String.valueOf(c));
          }
        }
      }
    }
    if (current.length() > 0) lines.add(current.toString());
    return lines.isEmpty() ? List.of("") : lines;
  }

  private float computeRowHeight(
      String[] cells,
      PDType1Font font,
      float fontSize,
      float[] colWidths,
      float cellPadding,
      float minRowHeight) {
    float lineSpacing = fontSize + 3f;
    int maxLines = 1;
    for (int col = 0; col < colWidths.length && col < cells.length; col++) {
      String text = cells[col] != null ? cells[col] : "";
      int count = wrapText(text, font, fontSize, colWidths[col] - cellPadding * 2).size();
      if (count > maxLines) maxLines = count;
    }
    return Math.max(minRowHeight, maxLines * lineSpacing + cellPadding * 2);
  }

  private List<TcLine> buildDysonTcBlocks(TermsAndConditionsDysonEntity tc, LocalDate today) {
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    String todayStr = today.format(fmt);
    String validityEnd =
        tc.getValidityNumberOfDays() != null
            ? today.plusDays(tc.getValidityNumberOfDays()).format(fmt)
            : "";
    String exchangeRate =
        tc.getCurrencyExchangeRate() != null
            ? NumberUtils.formatDoubleToTwoDigits(tc.getCurrencyExchangeRate().doubleValue())
            : "";
    String minQty =
        tc.getMinimumDeliveryQuantity() != null
            ? String.valueOf(tc.getMinimumDeliveryQuantity())
            : "";
    String storageMonths =
        tc.getStorageAcceptDeliveryNumberMonths() != null
            ? String.valueOf(tc.getStorageAcceptDeliveryNumberMonths())
            : "";
    String storageFee =
        tc.getStorageMinimumStorageFee() != null
            ? NumberUtils.formatDoubleToTwoDigits(tc.getStorageMinimumStorageFee().doubleValue())
            : "";
    String nonCancelDays =
        tc.getNonCancellationNumberWorkingDays() != null
            ? String.valueOf(tc.getNonCancellationNumberWorkingDays())
            : "";
    String nonReschedWeeks =
        tc.getNonRescheduledNumberWeeks() != null
            ? String.valueOf(tc.getNonRescheduledNumberWeeks())
            : "";
    String claimsDays =
        tc.getClaimsPackagingDamageNumberDays() != null
            ? NumberUtils.formatDoubleToTwoDigits(
                tc.getClaimsPackagingDamageNumberDays().doubleValue())
            : "";
    String forecastLt = tc.getForecastLeadTime() != null ? tc.getForecastLeadTime() : "";
    String latePayment =
        tc.getLatePaymentPenalty() != null
            ? NumberUtils.formatDoubleToTwoDigits(tc.getLatePaymentPenalty().doubleValue())
            : "";

    float ind = 12f;
    List<TcLine> b = new ArrayList<>();
    b.add(new TcLine("Terms & Conditions (T&C).", true, 0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("1. Validity of Prices", true, 0));
    b.add(
        new TcLine(
            "The prices quoted are valid for orders delivered or invoiced between "
                + todayStr
                + " and "
                + validityEnd
                + ".",
            false,
            0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("2. Currency", true, 0));
    b.add(
        new TcLine(
            "All prices are quoted in USD at an exchange rate of "
                + exchangeRate
                + " and exclude applicable taxes and duties, unless stated otherwise.",
            false,
            0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("3. Price Changes", true, 0));
    b.add(new TcLine("Prices are subject to change in the event of:", false, 0));
    b.add(new TcLine("a) Design or drawing modifications", false, ind));
    b.add(new TcLine("b) Fluctuations in raw material costs", false, ind));
    b.add(new TcLine("c) Additional customer requirements", false, ind));
    b.add(
        new TcLine(
            "Any changes will be communicated in writing and agreed upon prior to implementation.",
            false,
            0,
            true));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("4. Delivery Charges", true, 0));
    b.add(
        new TcLine(
            "4.1 Delivery and transportation charges are included for DAP MY, DAP PH, and DAP CN/HK (Incoterms).",
            false,
            0));
    b.add(
        new TcLine(
            "4.2 Minimum delivery quantity "
                + minQty
                + "pcs per shipment DAP CN/HK/PH, less than quantity "
                + minQty
                + "pcs will be additional charges.",
            false,
            0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("5. Tools for Production", true, 0));
    b.add(
        new TcLine(
            "All costs for production tools will be borne by Dyson and quoted separately. Ownership of production tools remains with Dyson unless otherwise agreed.",
            false,
            0));
    b.add(
        new TcLine(
            "The supplier (SDM) will issue a formal exposure claim at the project's end-of-life",
            false,
            0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("6. Excess Material (Exposure Claims)", true, 0));
    b.add(
        new TcLine(
            "The buyer is responsible for any excess material arising from MOQ requirements or unused stock. (EOL) for settlement.",
            false,
            0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("7. Storage", true, 0));
    b.add(
        new TcLine(
            "If the buyer cannot accept delivery within "
                + storageMonths
                + " months of the agreed schedule, SDM may require full prepayment and charge a minimum storage fee of "
                + storageFee
                + "% per month of the goods' value.",
            false,
            0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("8. Packaging & Labeling", true, 0));
    b.add(
        new TcLine(
            "The packing method and materials will be designed by SDM. Any additional requests from the buyer (e.g., UL labels) will be borne by the buyer.",
            false,
            0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("9. Non-Cancellation / Non-Reschedule", true, 0));
    b.add(
        new TcLine(
            "a) Non-Cancellation: Once a purchase order is placed, it may only be canceled within "
                + nonCancelDays
                + " working days.",
            false,
            ind));
    b.add(
        new TcLine(
            "b) Non-Reschedule: Once a purchase order delivery date is confirmed, it may only be rescheduled within "
                + nonReschedWeeks
                + " weeks.",
            false,
            ind));
    b.add(new TcLine("All requests must be submitted in writing.", false, 0, true));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("10. Claims", true, 0));
    b.add(new TcLine("a) Packaging Damage Upon Receipt:", true, ind));
    b.add(
        new TcLine(
            "Any damage to packaging must be reported within "
                + claimsDays
                + " days of receiving the goods. Claims submitted after this period may not be accepted.",
            false,
            ind));
    b.add(new TcLine("b) Quality Issues During Operation:", true, ind));
    b.add(
        new TcLine(
            "Any quality issues identified during operation or use will be evaluated separately for potential replacement.",
            false,
            ind));
    b.add(new TcLine("c) Rejections During Final Assembly:", true, ind));
    b.add(
        new TcLine(
            "Any rejection or failure occurring during final assembly is not the responsibility of SDM.",
            false,
            ind));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("11. Forecast", true, 0));
    b.add(
        new TcLine(
            "When the buyer provides a firm forecast, SDM will support a "
                + forecastLt
                + " lead time. Without a forecast, SDM will operate strictly based on component lead times plus transit/shipment.",
            false,
            0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("12. LMW Compliance", true, 0));
    b.add(
        new TcLine(
            "All Malaysian local customers are subject to LMW company regulations. The buyer acknowledges that without LMW compliance, the company is responsible for all customs duty, sales tax, and related import costs.",
            false,
            0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("13. Payment & Financial Terms:", true, 0));
    b.add(
        new TcLine(
            "Late payment penalties: " + latePayment + "% per month on overdue amounts.",
            false,
            0));
    return b;
  }

  private List<TcLine> buildNonDysonTcBlocks(TermsAndConditionsNonDysonEntity tc) {
    String validity = String.valueOf(tc.getValidityNumberOfDays());
    String deliveryCharges =
        NumberUtils.formatDoubleToTwoDigits(tc.getDeliveryCharges().doubleValue());
    String storageMonths = String.valueOf(tc.getStorageAcceptDeliveryNumberMonths());
    String storageFee =
        NumberUtils.formatDoubleToTwoDigits(tc.getStorageMinimumStorageFee().doubleValue());
    String nonCancelDays = String.valueOf(tc.getNonCancellationNumberWorkingDays());
    String nonReschedWeeks = String.valueOf(tc.getNonRescheduledNumberWeeks());
    String claimsDays =
        NumberUtils.formatDoubleToTwoDigits(tc.getClaimsPackagingDamageNumberDays().doubleValue());
    String forecastLt = tc.getForecastLeadTime();
    String latePayment =
        NumberUtils.formatDoubleToTwoDigits(tc.getLatePaymentPenalty().doubleValue());

    float ind = 12f;
    List<TcLine> b = new ArrayList<>();
    b.add(new TcLine("Terms & Conditions (T&C)", true, 0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("1. Validity of Prices", true, 0));
    b.add(
        new TcLine(
            "The quoted prices are valid for orders delivered or invoiced within "
                + validity
                + " days of the quotation date.",
            false,
            0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("2. Price Changes", true, 0));
    b.add(new TcLine("Prices are subject to change in the event of:", false, 0));
    b.add(new TcLine("a) Design or drawing modifications", false, ind));
    b.add(new TcLine("b) Fluctuations in raw material costs", false, ind));
    b.add(new TcLine("c) Additional customer requirements", false, ind));
    b.add(
        new TcLine(
            "Any changes will be communicated in writing and agreed upon prior to implementation.",
            false,
            0,
            true));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("3. Delivery Charges", true, 0));
    b.add(
        new TcLine(
            "3.1 Delivery and transportation charges are included for " + deliveryCharges + ".",
            false,
            0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("4. Tools for Production", true, 0));
    b.add(
        new TcLine(
            "All costs for production tools will be borne by customer and quoted separately. Ownership of production tools remains with customer unless otherwise agreed.",
            false,
            0));
    b.add(
        new TcLine(
            "The supplier (SDM) will issue a formal exposure claim at the project's end-of-life",
            false,
            0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("5. Excess Material (Exposure Claims)", true, 0));
    b.add(
        new TcLine(
            "The buyer is responsible for any excess material arising from MOQ requirements or unused stock. (EOL) for settlement.",
            false,
            0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("6. Storage", true, 0));
    b.add(
        new TcLine(
            "If the buyer cannot accept delivery within "
                + storageMonths
                + " months of the agreed schedule, SDM may require full prepayment and charge a minimum storage fee of "
                + storageFee
                + "% per month of the goods' value.",
            false,
            0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("7. Packaging & Labeling", true, 0));
    b.add(
        new TcLine(
            "The packing method and materials will be designed by SDM. Any additional requests from the buyer (e.g., UL labels) will be borne by the buyer.",
            false,
            0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("8. Non-Cancellation / Non-Reschedule", true, 0));
    b.add(
        new TcLine(
            "a) Non-Cancellation: Once a purchase order is placed, it may only be canceled within "
                + nonCancelDays
                + " working days.",
            false,
            ind));
    b.add(
        new TcLine(
            "b) Non-Reschedule: Once a purchase order delivery date is confirmed, it may only be rescheduled within "
                + nonReschedWeeks
                + " weeks.",
            false,
            ind));
    b.add(new TcLine("All requests must be submitted in writing.", false, 0, true));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("9. Claims", true, 0));
    b.add(new TcLine("a) Packaging Damage Upon Receipt:", true, ind));
    b.add(
        new TcLine(
            "Any damage to packaging must be reported within "
                + claimsDays
                + " days of receiving the goods. Claims submitted after this period may not be accepted.",
            false,
            ind));
    b.add(new TcLine("b) Quality Issues During Operation:", true, ind));
    b.add(
        new TcLine(
            "Any quality issues identified during operation or use will be evaluated separately for potential replacement.",
            false,
            ind));
    b.add(new TcLine("c) Rejections During Final Assembly:", true, ind));
    b.add(
        new TcLine(
            "Any rejection or failure occurring during final assembly is not the responsibility of SDM.",
            false,
            ind));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("10. Forecast", true, 0));
    b.add(
        new TcLine(
            "When the buyer provides a firm forecast, SDM will support a "
                + forecastLt
                + " lead time. Without a forecast, SDM will operate strictly based on component lead times plus transit/shipment.",
            false,
            0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("11. LMW Compliance", true, 0));
    b.add(
        new TcLine(
            "All Malaysian local customers are subject to LMW company regulations. The buyer acknowledges that without LMW compliance, the company is responsible for all customs duty, sales tax, and related import costs.",
            false,
            0));
    b.add(new TcLine("", false, 0));
    b.add(new TcLine("12. Payment & Financial Terms:", true, 0));
    b.add(
        new TcLine(
            "Late payment penalties: " + latePayment + "% per month on overdue amounts.",
            false,
            0));
    return b;
  }

  /**
   * Draws the common page header on the given content stream and returns the y position just below
   * the header, ready for content.
   *
   * <p>Layout: logo centered at top; "To: [customer]" on the left; Ref. No / Revision / Date /
   * Payment Term / Currency / Page on the right.
   */
  private float drawPageHeader(PDPageContentStream cs, TcHeaderContext ctx, int pageNum)
      throws IOException {
    float y = ctx.pageHeight() - ctx.margin();

    // Logo centered
    if (ctx.logoBytes() != null) {
      PDImageXObject logo =
          PDImageXObject.createFromByteArray(ctx.document(), ctx.logoBytes(), "logo");
      float logoAspect = (float) logo.getWidth() / logo.getHeight();
      float logoWidth = ctx.logoHeight() * logoAspect;
      float logoX = ctx.margin() + (ctx.pageWidth() - 2 * ctx.margin() - logoWidth) / 2f;
      cs.drawImage(logo, logoX, y - ctx.logoHeight(), logoWidth, ctx.logoHeight());
    }
    y -= ctx.logoHeight() + 25f;

    float infoFs = ctx.infoFontSize();
    float lineH = ctx.infoLineHeight();

    // Right info block (6 rows) — anchored near the right side
    float rightColX = ctx.pageWidth() - ctx.margin() - 200f;
    float rightColonX = rightColX + 82f;

    // Left: "To: [customerName]" — wrap name so it never overlaps the right info block
    String toPrefix = "To: ";
    float toPrefixWidth = textWidth(toPrefix, ctx.fontBold(), infoFs);
    float maxNameWidth = rightColX - ctx.margin() - toPrefixWidth - 10f;
    String customerNameStr = ctx.customerName() != null ? ctx.customerName() : "";
    List<String> customerNameLines =
        wrapText(customerNameStr, ctx.fontRegular(), infoFs, maxNameWidth);
    cs.beginText();
    cs.setFont(ctx.fontBold(), infoFs);
    cs.newLineAtOffset(ctx.margin(), y);
    cs.showText(sanitizeTextForPdf(toPrefix));
    cs.endText();
    for (int i = 0; i < customerNameLines.size(); i++) {
      cs.beginText();
      cs.setFont(ctx.fontRegular(), infoFs);
      cs.newLineAtOffset(ctx.margin() + toPrefixWidth, y - i * lineH);
      cs.showText(sanitizeTextForPdf(customerNameLines.get(i)));
      cs.endText();
    }

    drawAlignedInfoLine(
        cs,
        ctx.fontBold(),
        ctx.fontRegular(),
        infoFs,
        rightColX,
        rightColonX,
        y,
        "Ref. No",
        ctx.refNumber());
    y -= lineH;
    drawAlignedInfoLine(
        cs,
        ctx.fontBold(),
        ctx.fontRegular(),
        infoFs,
        rightColX,
        rightColonX,
        y,
        "Revision",
        ctx.revision());
    y -= lineH;
    drawAlignedInfoLine(
        cs,
        ctx.fontBold(),
        ctx.fontRegular(),
        infoFs,
        rightColX,
        rightColonX,
        y,
        "Date",
        ctx.dateStr());
    y -= lineH;
    drawAlignedInfoLine(
        cs,
        ctx.fontBold(),
        ctx.fontRegular(),
        infoFs,
        rightColX,
        rightColonX,
        y,
        "Payment Term",
        ctx.paymentTerms());
    y -= lineH;
    if (ctx.currencyCode() != null) {
      drawAlignedInfoLine(
          cs,
          ctx.fontBold(),
          ctx.fontRegular(),
          infoFs,
          rightColX,
          rightColonX,
          y,
          "Currency",
          ctx.currencyCode());
      y -= lineH;
    }
    drawAlignedInfoLine(
        cs,
        ctx.fontBold(),
        ctx.fontRegular(),
        infoFs,
        rightColX,
        rightColonX,
        y,
        "Page",
        pageNum + " / " + ctx.totalPages());
    y -= lineH + 15f;

    return y;
  }

  /** Simulates rendering the T&C blocks to count how many pages they will occupy. */
  private int countTcPages(
      List<TcLine> tcBlocks,
      PDType1Font fontBold,
      PDType1Font fontRegular,
      float availableHeight,
      float contentWidth) {
    float fontSize = 9f;
    float lineSpacing = fontSize + 2f;
    float gapHeight = lineSpacing / 2f;

    int pages = 1;
    float y = availableHeight;

    for (TcLine block : tcBlocks) {
      if (block.text().isEmpty()) {
        y -= gapHeight;
        if (y < 0) {
          pages++;
          y = availableHeight - gapHeight;
        }
        continue;
      }
      PDType1Font font = block.bold() ? fontBold : fontRegular;
      float w = contentWidth - block.indent();
      List<String> wrapped = wrapText(block.text(), font, fontSize, w);
      for (int i = 0; i < wrapped.size(); i++) {
        if (y - lineSpacing < 0) {
          pages++;
          y = availableHeight;
        }
        y -= lineSpacing;
      }
    }

    return pages;
  }

  private void renderDysonTc(List<TcLine> tcBlocks, TcHeaderContext ctx, int firstPageNum)
      throws IOException {
    float fontSize = 9f;
    float lineSpacing = fontSize + 2f;
    float gapHeight = lineSpacing / 2f;
    PDType1Font fontItalic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    int currentPageNum = firstPageNum;
    PDPage currentPage = new PDPage(PDRectangle.A4);
    ctx.document().addPage(currentPage);

    PDPageContentStream cs = new PDPageContentStream(ctx.document(), currentPage);
    float y = drawPageHeader(cs, ctx, currentPageNum);
    try {
      for (TcLine block : tcBlocks) {
        if (block.text().isEmpty()) {
          y -= gapHeight;
          if (y < ctx.margin()) {
            cs.close();
            currentPageNum++;
            currentPage = new PDPage(PDRectangle.A4);
            ctx.document().addPage(currentPage);
            cs = new PDPageContentStream(ctx.document(), currentPage);
            y = drawPageHeader(cs, ctx, currentPageNum);
          }
          continue;
        }

        PDType1Font font =
            block.bold() ? ctx.fontBold() : block.italic() ? fontItalic : ctx.fontRegular();
        float contentWidth = ctx.pageWidth() - 2 * ctx.margin() - block.indent();
        List<String> wrapped = wrapText(block.text(), font, fontSize, contentWidth);

        for (String line : wrapped) {
          if (y - lineSpacing < ctx.margin()) {
            cs.close();
            currentPageNum++;
            currentPage = new PDPage(PDRectangle.A4);
            ctx.document().addPage(currentPage);
            cs = new PDPageContentStream(ctx.document(), currentPage);
            y = drawPageHeader(cs, ctx, currentPageNum);
          }
          cs.beginText();
          cs.setFont(font, fontSize);
          cs.newLineAtOffset(ctx.margin() + block.indent(), y - fontSize);
          cs.showText(sanitizeTextForPdf(line));
          cs.endText();
          y -= lineSpacing;
        }
      }
    } finally {
      cs.close();
    }
  }

  private record TcLine(String text, boolean bold, float indent, boolean italic) {
    TcLine(String text, boolean bold, float indent) {
      this(text, bold, indent, false);
    }
  }

  private record TcHeaderContext(
      PDDocument document,
      byte[] logoBytes,
      PDType1Font fontBold,
      PDType1Font fontRegular,
      float infoFontSize,
      float infoLineHeight,
      float logoHeight,
      float margin,
      float pageWidth,
      float pageHeight,
      String customerName,
      String refNumber,
      String dateStr,
      String currencyCode,
      String revision,
      String paymentTerms,
      int totalPages) {}
}
