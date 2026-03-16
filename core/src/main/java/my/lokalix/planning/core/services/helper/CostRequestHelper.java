package my.lokalix.planning.core.services.helper;

import jakarta.validation.constraints.NotBlank;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.*;
import my.lokalix.planning.core.models.entities.admin.*;
import my.lokalix.planning.core.models.enums.CostRequestStatus;
import my.lokalix.planning.core.models.enums.MaterialStatus;
import my.lokalix.planning.core.models.enums.SupplierAndManufacturerType;
import my.lokalix.planning.core.models.excel.CellColorEnum;
import my.lokalix.planning.core.models.excel.CellStyleFormatEnum;
import my.lokalix.planning.core.models.excel.ExcelCellStyles;
import my.lokalix.planning.core.repositories.CostRequestRepository;
import my.lokalix.planning.core.repositories.CurrencyRepository;
import my.lokalix.planning.core.repositories.MaterialRepository;
import my.lokalix.planning.core.repositories.admin.*;
import my.lokalix.planning.core.repositories.placeholder.CostRequestReferenceCodeRepository;
import my.lokalix.planning.core.utils.ExcelUtils;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostRequestHelper {
  private static final List<CostRequestStatus> FINALIZED_STATUSES =
      List.of(CostRequestStatus.READY_TO_QUOTE, CostRequestStatus.ABORTED);

  private final CostRequestRepository costRequestRepository;
  private final CostRequestReferenceCodeRepository costRequestReferenceCodeRepository;
  private final ShipmentMethodRepository shipmentMethodRepository;
  private final UnitRepository unitRepository;
  private final MaterialCategoryRepository materialCategoryRepository;
  private final SupplierAndManufacturerRepository supplierAndManufacturerRepository;
  private final ProductNameRepository productNameRepository;
  private final ProcessRepository processRepository;
  private final CurrencyRepository currencyRepository;
  private final MaterialRepository materialRepository;
  private final SystemIdHelper systemIdHelper;
  private final AppConfigurationProperties appConfigurationProperties;
  private final ShipmentLocationRepository shipmentLocationRepository;

  public byte[] buildOpenCostRequests() throws IOException {
    Sort sort =
        Sort.by(
            Sort.Order.asc("costRequestReferenceNumber"), Sort.Order.asc("costRequestRevision"));
    List<CostRequestEntity> entities =
        costRequestRepository.findByArchivedFalseAndStatusNotIn(FINALIZED_STATUSES, sort);
    String[] headers = {
      "Reference",
      "Rev.",
      "Customer Code",
      "Customer Name",
      "Project Name",
      "Status",
      "Requestor",
      "Currency",
      "PO Expected Date",
      "Creation Date",
      "Total Cost"
    };
    return buildCostRequestsWorkbook(headers, entities, false);
  }

  public byte[] buildArchivedCostRequests() throws IOException {
    Sort sort =
        Sort.by(
            Sort.Order.asc("costRequestReferenceNumber"), Sort.Order.asc("costRequestRevision"));
    List<CostRequestEntity> entities = costRequestRepository.findByArchivedTrue(sort);
    String[] headers = {
      "Reference",
      "Rev.",
      "Customer Code",
      "Customer Name",
      "Project Name",
      "Status",
      "Requestor",
      "Currency",
      "PO Expected Date",
      "Creation Date",
      "Finalization Date",
      "Total Cost"
    };
    return buildCostRequestsWorkbook(headers, entities, true);
  }

  public byte[] buildProductionBomOfCostRequestLine(CostRequestLineEntity costRequestLine)
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

      int rowIndex = 0;

      // --- Info section ---
      Row infoHeaderRow = sheet.createRow(rowIndex++);
      ExcelUtils.createAndStyleCellLeftAlignment(
          infoHeaderRow,
          0,
          "Customer Part Number",
          CellStyleFormatEnum.STRING,
          CellColorEnum.LIGHT_GREEN,
          styles);
      ExcelUtils.createAndStyleCellLeftAlignment(
          infoHeaderRow,
          1,
          "Revision",
          CellStyleFormatEnum.STRING,
          CellColorEnum.LIGHT_GREEN,
          styles);
      ExcelUtils.createAndStyleCellLeftAlignment(
          infoHeaderRow,
          2,
          "Description",
          CellStyleFormatEnum.STRING,
          CellColorEnum.LIGHT_GREEN,
          styles);

      Row infoDataRow = sheet.createRow(rowIndex++);
      ExcelUtils.createAndStyleCellLeftAlignment(
          infoDataRow,
          0,
          costRequestLine.getCustomerPartNumber(),
          CellStyleFormatEnum.STRING,
          CellColorEnum.WHITE,
          styles);
      ExcelUtils.createAndStyleCellLeftAlignment(
          infoDataRow,
          1,
          costRequestLine.getCustomerPartNumberRevision(),
          CellStyleFormatEnum.STRING,
          CellColorEnum.WHITE,
          styles);
      ExcelUtils.createAndStyleCellLeftAlignment(
          infoDataRow,
          2,
          costRequestLine.getDescription(),
          CellStyleFormatEnum.STRING,
          CellColorEnum.WHITE,
          styles);

      // empty separator row
      rowIndex++;

      // --- BOM lines section (processes + materials) ---
      String[] bomHeaders = {"Type", "Name", "Quantity", "Time (min)", "UoM"};
      Row bomHeaderRow = sheet.createRow(rowIndex++);
      for (int i = 0; i < bomHeaders.length; i++) {
        ExcelUtils.createAndStyleCellLeftAlignment(
            bomHeaderRow,
            i,
            bomHeaders[i],
            CellStyleFormatEnum.STRING,
            CellColorEnum.LIGHT_GREEN,
            styles);
      }

      for (ProcessLineEntity processLine : costRequestLine.getProcessLines()) {
        Row dataRow = sheet.createRow(rowIndex++);
        ExcelUtils.createAndStyleCellLeftAlignment(
            dataRow, 0, "Process", CellStyleFormatEnum.STRING, CellColorEnum.WHITE, styles);
        ExcelUtils.createAndStyleCellLeftAlignment(
            dataRow,
            1,
            processLine.getProcess().getName(),
            CellStyleFormatEnum.STRING,
            CellColorEnum.WHITE,
            styles);
        ExcelUtils.createAndStyleCellLeftAlignment(
            dataRow,
            2,
            processLine.getQuantity() != null ? processLine.getQuantity().doubleValue() : null,
            CellStyleFormatEnum.DOUBLE,
            CellColorEnum.WHITE,
            styles);
        ExcelUtils.createAndStyleCellLeftAlignment(
            dataRow,
            3,
            processLine.getProcessCycleTimeInSeconds().doubleValue(),
            CellStyleFormatEnum.DOUBLE,
            CellColorEnum.WHITE,
            styles);
        ExcelUtils.createAndStyleCellLeftAlignment(
            dataRow, 4, null, CellStyleFormatEnum.STRING, CellColorEnum.WHITE, styles);
      }

      for (MaterialLineEntity materialLine :
          costRequestLine.getOnlyMaterialLinesUsedForQuotation()) {
        Row dataRow = sheet.createRow(rowIndex++);
        String materialType =
            materialLine.getMaterial().getMaterialType() != null
                ? "Material " + materialLine.getMaterial().getMaterialType().getHumanReadableValue()
                : "Material";
        ExcelUtils.createAndStyleCellLeftAlignment(
            dataRow, 0, materialType, CellStyleFormatEnum.STRING, CellColorEnum.WHITE, styles);
        ExcelUtils.createAndStyleCellLeftAlignment(
            dataRow,
            1,
            materialLine.getMaterial().getManufacturerPartNumber(),
            CellStyleFormatEnum.STRING,
            CellColorEnum.WHITE,
            styles);
        ExcelUtils.createAndStyleCellLeftAlignment(
            dataRow,
            2,
            materialLine.getQuantity() != null ? materialLine.getQuantity().doubleValue() : null,
            CellStyleFormatEnum.DOUBLE,
            CellColorEnum.WHITE,
            styles);
        ExcelUtils.createAndStyleCellLeftAlignment(
            dataRow, 3, null, CellStyleFormatEnum.STRING, CellColorEnum.WHITE, styles);
        ExcelUtils.createAndStyleCellLeftAlignment(
            dataRow,
            4,
            materialLine.getMaterial().getUnit() != null
                ? materialLine.getMaterial().getUnit().getName()
                : null,
            CellStyleFormatEnum.STRING,
            CellColorEnum.WHITE,
            styles);
      }

      for (int i = 0; i < 6; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(bos);
      return bos.toByteArray();
    }
  }

  private byte[] buildCostRequestsWorkbook(
      String[] headers, List<CostRequestEntity> entities, boolean includeFinalizationDate)
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
      for (CostRequestEntity cr : entities) {
        col =
            writeCostRequestDataRow(
                sheet.createRow(rowIndex++), cr, styles, includeFinalizationDate);
      }
      for (int i = 0; i < col; i++) {
        sheet.autoSizeColumn(i);
      }
      workbook.write(bos);
      return bos.toByteArray();
    }
  }

  private int writeCostRequestDataRow(
      Row row, CostRequestEntity cr, ExcelCellStyles styles, boolean includeFinalizationDate) {
    int col = 0;
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        cr.getCostRequestReferenceNumber(),
        CellStyleFormatEnum.STRING,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        cr.getCostRequestRevision(),
        CellStyleFormatEnum.INTEGER,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        cr.getCustomer() != null ? cr.getCustomer().getCode() : null,
        CellStyleFormatEnum.STRING,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        cr.getCustomer() != null ? cr.getCustomer().getName() : null,
        CellStyleFormatEnum.STRING,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row, col++, cr.getProjectName(), CellStyleFormatEnum.STRING, CellColorEnum.WHITE, styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        cr.getStatus() != null ? cr.getStatus().getHumanReadableValue() : null,
        CellStyleFormatEnum.STRING,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row, col++, cr.getRequestorName(), CellStyleFormatEnum.STRING, CellColorEnum.WHITE, styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        cr.getCurrency() != null ? cr.getCurrency().getCode() : null,
        CellStyleFormatEnum.STRING,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        cr.getPurchaseOrderExpectedDate(),
        CellStyleFormatEnum.DATE,
        CellColorEnum.WHITE,
        styles);
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col++,
        cr.getCreationDate().toLocalDate(),
        CellStyleFormatEnum.DATE,
        CellColorEnum.WHITE,
        styles);
    if (includeFinalizationDate) {
      ExcelUtils.createAndStyleCellLeftAlignment(
          row,
          col++,
          cr.getFinalizationDate() != null ? cr.getFinalizationDate().toLocalDate() : null,
          CellStyleFormatEnum.DATE,
          CellColorEnum.WHITE,
          styles);
    }
    ExcelUtils.createAndStyleCellLeftAlignment(
        row,
        col,
        cr.getTotalLinesCostInSystemCurrency() != null
            ? cr.getTotalLinesCostInSystemCurrency().doubleValue()
            : null,
        CellStyleFormatEnum.DOUBLE,
        CellColorEnum.WHITE,
        styles);
    return col;
  }

  private void writeHeaderRow(Sheet sheet, String[] headers, ExcelCellStyles styles) {
    Row headerRow = sheet.createRow(0);
    for (int i = 0; i < headers.length; i++) {
      ExcelUtils.createAndStyleCellLeftAlignment(
          headerRow, i, headers[i], CellStyleFormatEnum.STRING, CellColorEnum.LIGHT_GREEN, styles);
    }
  }

  public @NotBlank String nextCostRequestReferenceNumber() {
    Long nextValue =
        costRequestReferenceCodeRepository.getNextCostRequestReferenceCodeSequenceValue();
    return String.format("%05d", nextValue);
  }

  /**
   * Convert MaterialLineDraftEntity to MaterialLineEntity for a cost request line. For each draft:
   * - Check if material exists in database - If exists: reuse it - If not: create new material with
   * generated system ID if possible - Create MaterialLineEntity linking to the material
   *
   * @param costRequestLine The cost request line entity
   */
  public boolean convertDraftsToMaterialLines(CostRequestLineEntity costRequestLine) {
    if (CollectionUtils.isEmpty(costRequestLine.getDraftMaterialLines())) {
      return false;
    }

    // Clear existing material lines (should be empty at this stage)
    costRequestLine.getMaterialLines().clear();

    boolean atLeastOneNewMaterialCreated = false;

    for (MaterialLineDraftEntity draft : costRequestLine.getDraftMaterialLines()) {
      if (StringUtils.isBlank(draft.getManufacturerPartNumber())
          || (draft.getManufacturer() == null
              && StringUtils.isBlank(draft.getDraftManufacturerName()))) {
        throw new GenericWithMessageException(
            "The manufacturer and manufacturer part number are required for all material lines",
            SWCustomErrorCode.GENERIC_ERROR);
      }

      SupplierManufacturerEntity manufacturer = draft.getManufacturer();
      if (manufacturer == null && StringUtils.isNotBlank(draft.getDraftManufacturerName())) {
        manufacturer =
            supplierAndManufacturerRepository
                .findFirstByTypeInAndNameIgnoreCaseAndArchivedFalse(
                    List.of(
                        SupplierAndManufacturerType.SUPPLIER,
                        SupplierAndManufacturerType.MANUFACTURER,
                        SupplierAndManufacturerType.BOTH),
                    draft.getDraftManufacturerName())
                .orElse(null);
        // If SUPPLIER only found, then mark it as BOTH
        if (manufacturer != null
            && manufacturer.getType() == SupplierAndManufacturerType.SUPPLIER) {
          manufacturer.setType(SupplierAndManufacturerType.BOTH);
          manufacturer = supplierAndManufacturerRepository.save(manufacturer);
        }
      }

      // Check if material exists in database
      Optional<MaterialEntity> existingMaterial = Optional.empty();
      if (manufacturer != null
          && draft.getCategory() != null
          && StringUtils.isNotBlank(draft.getManufacturerPartNumber())) {
        // Case 1: manufacturer + category + part number
        existingMaterial =
            materialRepository
                .findFirstByManufacturerAndCategoryAndPartNumberAndArchivedFalse(
                    manufacturer, draft.getCategory(), draft.getManufacturerPartNumber())
                .stream()
                .findFirst();
      } else if (manufacturer != null
          && StringUtils.isNotBlank(draft.getManufacturerPartNumber())) {
        // Case 2: manufacturer + part number (no category)
        existingMaterial =
            materialRepository
                .findFirstByManufacturerAndPartNumberAndArchivedFalse(
                    manufacturer, draft.getManufacturerPartNumber())
                .stream()
                .findFirst();
      } else if (StringUtils.isNotBlank(draft.getDraftManufacturerName())
          && StringUtils.isNotBlank(draft.getManufacturerPartNumber())) {
        // Case 3: no manufacturer found — match on draftManufacturerName + part number
        existingMaterial =
            materialRepository
                .findFirstByDraftManufacturerNameAndPartNumberAndArchivedFalse(
                    draft.getDraftManufacturerName(), draft.getManufacturerPartNumber())
                .stream()
                .findFirst();
      }

      MaterialEntity material;
      if (existingMaterial.isPresent()) {
        // Reuse existing material
        material = existingMaterial.get();
      } else {
        // Create new material from draft information
        atLeastOneNewMaterialCreated = true;
        material = new MaterialEntity();
        material.setManufacturer(manufacturer);
        material.setManufacturerPartNumber(draft.getManufacturerPartNumber());
        material.setDescription(draft.getDescription());
        material.setCategory(draft.getCategory());
        material.setUnit(draft.getUnit());
        material.setMaterialType(draft.getMaterialType());

        material.setDraftManufacturerName(draft.getDraftManufacturerName());
        material.setDraftCategoryName(draft.getDraftCategoryName());
        material.setDraftUnitName(draft.getDraftUnitName());

        // Generate system ID
        String systemId =
            systemIdHelper.generateSystemId(
                manufacturer, draft.getCategory(), draft.getManufacturerPartNumber());
        material.setSystemId(systemId);
        // Set status to TO_BE_ESTIMATED since no suppliers are provided
        material.setStatus(MaterialStatus.TO_BE_ESTIMATED);
        material = materialRepository.save(material);
      }

      // Create MaterialLineEntity
      MaterialLineEntity materialLine = new MaterialLineEntity();
      materialLine.setMarkedNotUsedForQuote(draft.isMarkedNotUsedForQuote());
      materialLine.setMaterial(material);
      materialLine.setQuantity(draft.getQuantity());
      costRequestLine.addMaterialLine(materialLine);
      materialLine.buildCalculatedFields(
          costRequestLine.getCostingMethodType(),
          appConfigurationProperties.getTargetCurrencyCode());
    }
    return atLeastOneNewMaterialCreated;
  }

  /**
   * Get the active (non-archived) version of a currency. If the currency is not archived, returns
   * it as-is. If archived, searches for the non-archived version in DB by code.
   *
   * @param currency The currency entity (may be archived or not)
   * @param activeCurrencyByOriginalCode volatile cache to avoid loading twice the same entity
   * @return The active currency, or null if not found
   */
  public CurrencyEntity getActiveVersionOfCurrency(
      CurrencyEntity currency, Map<String, CurrencyEntity> activeCurrencyByOriginalCode) {
    if (currency == null) {
      return null;
    }

    // If not archived, return as-is
    if (!currency.isArchived()) {
      return currency;
    }

    if (activeCurrencyByOriginalCode.containsKey(currency.getCode())) {
      return activeCurrencyByOriginalCode.get(currency.getCode());
    }
    // Search for active version in DB
    Optional<CurrencyEntity> activeVersion =
        currencyRepository.findByCodeAndArchivedFalse(currency.getCode());
    if (activeVersion.isPresent()) {
      log.debug("Found active version of archived currency {}", currency.getCode());
      activeCurrencyByOriginalCode.put(currency.getCode(), activeVersion.get());
      return activeVersion.get();
    }

    log.warn("No active version found for archived currency {}", currency.getCode());
    return null;
  }

  /**
   * Get the active (non-archived) version of a DYSON shipment location. If it is not archived,
   * returns it as-is. If archived, searches for the non-archived version in DB by code.
   *
   * @param entity The shipment location entity (may be archived or not)
   * @param activeShipmentLocationByOriginalName volatile cache to avoid loading twice the same
   *     entity
   * @return The active shipment location, or null if not found
   */
  public ShipmentLocationEntity getActiveVersionOfShipmentLocation(
      ShipmentLocationEntity entity,
      Map<String, ShipmentLocationEntity> activeShipmentLocationByOriginalName) {
    if (entity == null) {
      return null;
    }

    // If not archived, return as-is
    if (!entity.isArchived()) {
      return entity;
    }

    if (activeShipmentLocationByOriginalName.containsKey(entity.getName())) {
      return activeShipmentLocationByOriginalName.get(entity.getName());
    }

    // Search for active version in DB
    Optional<ShipmentLocationEntity> activeVersion =
        shipmentLocationRepository.findByNameIgnoreCaseAndArchivedFalse(entity.getName());
    if (activeVersion.isPresent()) {
      log.debug("Found active version of archived shipment location {}", entity.getName());
      activeShipmentLocationByOriginalName.put(entity.getName(), activeVersion.get());
      return activeVersion.get();
    }

    log.warn("No active version found for archived shipment location {}", entity.getName());
    return null;
  }

  /**
   * Get the active (non-archived) version of a product name. If the product name is not archived,
   * returns it as-is. If archived, searches for the non-archived version in DB by code.
   *
   * @param entity The product name entity (may be archived or not)
   * @param activeProductNameByOriginalCode volatile cache to avoid loading twice the same entity
   * @return The active product name, or null if not found
   */
  public ProductNameEntity getActiveVersionOfProductName(
      ProductNameEntity entity, Map<String, ProductNameEntity> activeProductNameByOriginalCode) {
    if (entity == null) {
      return null;
    }

    // If not archived, return as-is
    if (!entity.isArchived()) {
      return entity;
    }

    if (activeProductNameByOriginalCode.containsKey(entity.getCode())) {
      return activeProductNameByOriginalCode.get(entity.getCode());
    }

    // Search for active version in DB
    Optional<ProductNameEntity> activeVersion =
        productNameRepository.findByCodeIgnoreCaseAndArchivedFalse(entity.getCode());
    if (activeVersion.isPresent()) {
      log.debug(
          "Found active version of archived product name {} (code: {})",
          entity.getName(),
          entity.getCode());
      activeProductNameByOriginalCode.put(entity.getCode(), activeVersion.get());
      return activeVersion.get();
    }

    log.warn(
        "No active version found for archived product name {} (code: {})",
        entity.getName(),
        entity.getCode());
    return null;
  }

  /**
   * Get the active (non-archived) version of a supplier/manufacturer. If the supplier/manufacturer
   * is not archived, returns it as-is. If archived, searches for the non-archived version in DB by
   * code.
   *
   * @param manufacturer The manufacturer entity (may be archived or not)
   * @param activeManufacturerByOriginalCode volatile cache to avoid loading twice the same entity
   * @return The active manufacturer, or null if not found
   */
  public SupplierManufacturerEntity getActiveVersionOfManufacturer(
      SupplierManufacturerEntity manufacturer,
      Map<String, SupplierManufacturerEntity> activeManufacturerByOriginalCode) {
    if (manufacturer == null) {
      return null;
    }

    // If not archived, return as-is
    if (!manufacturer.isArchived()) {
      return manufacturer;
    }

    if (activeManufacturerByOriginalCode.containsKey(manufacturer.getCode())) {
      return activeManufacturerByOriginalCode.get(manufacturer.getCode());
    }
    // Search for active version in DB by code
    Optional<SupplierManufacturerEntity> activeVersion =
        supplierAndManufacturerRepository.findByTypeInAndCodeIgnoreCaseAndArchivedFalse(
            List.of(SupplierAndManufacturerType.MANUFACTURER, SupplierAndManufacturerType.BOTH),
            manufacturer.getCode());

    if (activeVersion.isPresent()) {
      log.debug(
          "Found active version of archived manufacturer {} (code: {})",
          manufacturer.getName(),
          manufacturer.getCode());
      activeManufacturerByOriginalCode.put(manufacturer.getCode(), activeVersion.get());
      return activeVersion.get();
    }

    log.warn(
        "No active version found for archived manufacturer {} (code: {})",
        manufacturer.getName(),
        manufacturer.getCode());
    return null;
  }

  /**
   * Get the active (non-archived) version of a material category. If the category is not archived,
   * returns it as-is. If archived, searches for the non-archived version in DB by name.
   *
   * @param category The material category entity (may be archived or not)
   * @param activeMaterialCategoryByOriginalName volatile cache to avoid loading twice the same
   *     entity
   * @return The active material category, or null if not found
   */
  public MaterialCategoryEntity getActiveVersionOfMaterialCategory(
      MaterialCategoryEntity category,
      Map<String, MaterialCategoryEntity> activeMaterialCategoryByOriginalName) {
    if (category == null) {
      return null;
    }

    // If not archived, return as-is
    if (!category.isArchived()) {
      return category;
    }

    if (activeMaterialCategoryByOriginalName.containsKey(category.getName())) {
      return activeMaterialCategoryByOriginalName.get(category.getName());
    }
    // Search for active version in DB by name
    Optional<MaterialCategoryEntity> activeVersion =
        materialCategoryRepository.findByNameIgnoreCaseAndArchivedFalse(category.getName());

    if (activeVersion.isPresent()) {
      log.debug("Found active version of archived material category {}", category.getName());
      activeMaterialCategoryByOriginalName.put(category.getName(), activeVersion.get());
      return activeVersion.get();
    }

    log.warn("No active version found for archived material category {}", category.getName());
    return null;
  }

  /**
   * Get the active (non-archived) version of a unit. If the unit is not archived, returns it as-is.
   * If archived, searches for the non-archived version in DB by name.
   *
   * @param unit The unit entity (may be archived or not)
   * @return The active unit, or null if not found
   */
  public UnitEntity getActiveVersionOfUnit(
      UnitEntity unit, Map<String, UnitEntity> activeUnitByOriginalName) {
    if (unit == null) {
      return null;
    }

    // If not archived, return as-is
    if (!unit.isArchived()) {
      return unit;
    }

    if (activeUnitByOriginalName.containsKey(unit.getName())) {
      return activeUnitByOriginalName.get(unit.getName());
    }
    // Search for active version in DB by name
    Optional<UnitEntity> activeVersion =
        unitRepository.findByNameIgnoreCaseAndArchivedFalse(unit.getName());

    if (activeVersion.isPresent()) {
      log.debug("Found active version of archived unit {}", unit.getName());
      activeUnitByOriginalName.put(unit.getName(), activeVersion.get());
      return activeVersion.get();
    }

    log.warn("No active version found for archived unit {}", unit.getName());
    return null;
  }

  /**
   * Get the active (non-archived) version of a process. If the process is not archived, returns it
   * as-is. If archived, searches for the non-archived version in DB by name.
   *
   * @param process The process entity (may be archived or not)
   * @return The active process, or null if not found
   */
  public ProcessEntity getActiveVersionOfProcess(
      ProcessEntity process, Map<String, ProcessEntity> activeProcessByOriginalName) {
    if (process == null) {
      return null;
    }

    // If not archived, return as-is
    if (!process.isArchived()) {
      return process;
    }

    if (activeProcessByOriginalName.containsKey(process.getName())) {
      return activeProcessByOriginalName.get(process.getName());
    }
    // Search for active version in DB by name
    Optional<ProcessEntity> activeVersion =
        processRepository.findByNameIgnoreCaseAndArchivedFalse(process.getName());

    if (activeVersion.isPresent()) {
      log.debug("Found active version of archived process {}", process.getName());
      activeProcessByOriginalName.put(process.getName(), activeVersion.get());
      return activeVersion.get();
    }

    log.warn("No active version found for archived process {}", process.getName());
    return null;
  }

  /**
   * Get or create an archived copy of a product name. First checks in database if an archived
   * version with the same code and name exists. If yes, reuses it. If no, creates a new archived
   * copy.
   *
   * @param productName The product name entity to archive
   * @return The archived product name (existing or newly created)
   */
  public ProductNameEntity getOrCreateArchivedProductName(ProductNameEntity productName) {
    // Check if archived version already exists in DB
    Optional<ProductNameEntity> existingArchived =
        productNameRepository.findByCodeIgnoreCaseAndNameIgnoreCaseAndArchivedTrue(
            productName.getCode(), productName.getName());

    if (existingArchived.isPresent()) {
      log.debug("Reusing existing archived product name {}", productName.getName());
      return existingArchived.get();
    }

    // Create new archived copy
    log.debug("Creating new archived product name {}", productName.getName());
    return createArchivedProductNameCopy(productName);
  }

  /**
   * Create an archived copy of a product name entity. The copy is marked as archived and saved to
   * the database.
   *
   * @param productName The product name entity to archive
   * @return The created archived product name copy
   */
  private ProductNameEntity createArchivedProductNameCopy(ProductNameEntity productName) {
    ProductNameEntity archivedProductName = new ProductNameEntity();
    archivedProductName.setCode(productName.getCode());
    archivedProductName.setName(productName.getName());
    archivedProductName.setArchived(true);
    return productNameRepository.save(archivedProductName);
  }

  /**
   * Get or create an archived copy of a supplier/manufacturer. First checks in database if an
   * archived version with the same name exists. If yes, reuses it. If no, creates a new archived
   * copy.
   *
   * @param supplierManufacturer The supplier/manufacturer entity to archive
   * @return The archived supplier/manufacturer (existing or newly created)
   */
  public SupplierManufacturerEntity getOrCreateArchivedSupplierManufacturer(
      SupplierManufacturerEntity supplierManufacturer,
      Map<UUID, ShipmentMethodEntity> archivedShipmentMethodsByOriginalId) {

    ShipmentMethodEntity archivedShipmentMethod =
        retrieveArchivedShipmentMethod(
            archivedShipmentMethodsByOriginalId, supplierManufacturer.getShipmentMethod());

    Optional<SupplierManufacturerEntity> existingArchived;
    switch (supplierManufacturer.getType()) {
      case SUPPLIER -> {
        // Check if archived version already exists in DB
        existingArchived =
            supplierAndManufacturerRepository
                .findByTypeInAndNameIgnoreCaseAndArchivedTrueAndShipmentMethod(
                    List.of(supplierManufacturer.getType(), SupplierAndManufacturerType.BOTH),
                    supplierManufacturer.getName(),
                    archivedShipmentMethod);
      }
      case MANUFACTURER -> {
        // Check if archived version already exists in DB
        existingArchived =
            supplierAndManufacturerRepository
                .findByTypeInAndCodeIgnoreCaseAndNameIgnoreCaseAndArchivedTrue(
                    List.of(supplierManufacturer.getType(), SupplierAndManufacturerType.BOTH),
                    supplierManufacturer.getCode(),
                    supplierManufacturer.getName());
      }
      case BOTH -> {
        // Check if archived version already exists in DB
        existingArchived =
            supplierAndManufacturerRepository
                .findByTypeInAndCodeIgnoreCaseAndNameIgnoreCaseAndArchivedTrueAndShipmentMethod(
                    List.of(supplierManufacturer.getType(), SupplierAndManufacturerType.BOTH),
                    supplierManufacturer.getCode(),
                    supplierManufacturer.getName(),
                    archivedShipmentMethod);
      }
      default ->
          throw new IllegalStateException("Unexpected value: " + supplierManufacturer.getType());
    }

    if (existingArchived.isPresent()) {
      log.debug(
          "Reusing existing archived supplier/manufacturer {}", supplierManufacturer.getName());
      return existingArchived.get();
    }

    // Create new archived copy
    log.debug("Creating new archived supplier/manufacturer {}", supplierManufacturer.getName());
    return createArchivedManufacturerSupplierCopy(supplierManufacturer, archivedShipmentMethod);
  }

  public ShipmentMethodEntity retrieveArchivedShipmentMethod(
      Map<UUID, ShipmentMethodEntity> archivedShipmentMethodsByOriginalId,
      ShipmentMethodEntity shipmentMethod) {
    if (archivedShipmentMethodsByOriginalId == null || shipmentMethod == null) {
      return null;
    }
    if (shipmentMethod.isArchived()) {
      return shipmentMethod;
    }
    if (!archivedShipmentMethodsByOriginalId.containsKey(shipmentMethod.getShipmentMethodId())) {
      ShipmentMethodEntity archivedShipmentMethod =
          getOrCreateArchivedShipmentMethod(shipmentMethod);
      archivedShipmentMethodsByOriginalId.put(
          shipmentMethod.getShipmentMethodId(), archivedShipmentMethod);
      return archivedShipmentMethod;
    } else {
      return archivedShipmentMethodsByOriginalId.get(shipmentMethod.getShipmentMethodId());
    }
  }

  /**
   * Create an archived copy of a manufacturer entity. The copy is marked as archived and saved to
   * the database.
   *
   * @param manufacturer The manufacturer entity to archive
   * @return The created archived manufacturer copy
   */
  private SupplierManufacturerEntity createArchivedManufacturerCopy(
      SupplierManufacturerEntity manufacturer) {
    SupplierManufacturerEntity archivedManufacturer = new SupplierManufacturerEntity();
    archivedManufacturer.setCode(manufacturer.getCode());
    archivedManufacturer.setName(manufacturer.getName());
    archivedManufacturer.setArchived(true);
    return supplierAndManufacturerRepository.save(archivedManufacturer);
  }

  /**
   * Create an archived copy of a supplierManufacturer entity. The copy is marked as archived and
   * saved to the database.
   *
   * @param supplierManufacturer The supplierManufacturer entity to archive
   * @return The created archived supplierManufacturer copy
   */
  private SupplierManufacturerEntity createArchivedManufacturerSupplierCopy(
      SupplierManufacturerEntity supplierManufacturer,
      ShipmentMethodEntity archivedShipmentMethod) {
    SupplierManufacturerEntity archivedSupplier = new SupplierManufacturerEntity();
    archivedSupplier.setType(supplierManufacturer.getType());
    archivedSupplier.setCode(supplierManufacturer.getCode());
    archivedSupplier.setName(supplierManufacturer.getName());
    archivedSupplier.setArchived(true);
    archivedSupplier.setShipmentMethod(archivedShipmentMethod);
    return supplierAndManufacturerRepository.save(archivedSupplier);
  }

  /**
   * Get or create an archived copy of a DYSON shipment location. First checks in database if an
   * archived version with the same name exists. If yes, reuses it. If no, creates a new archived
   * copy.
   *
   * @param shipmentLocation The entity to archive
   * @return The archived entity (existing or newly created)
   */
  public ShipmentLocationEntity getOrCreateArchivedShipmentLocation(
      ShipmentLocationEntity shipmentLocation) {
    // Check if archived version already exists in DB
    Optional<ShipmentLocationEntity> existingArchived =
        shipmentLocationRepository.findByNameIgnoreCaseAndArchivedTrue(shipmentLocation.getName());

    if (existingArchived.isPresent()) {
      log.debug("Reusing existing archived shipment location {}", shipmentLocation.getName());
      return existingArchived.get();
    }

    // Create new archived copy
    log.debug("Creating new archived shipment location {}", shipmentLocation.getName());
    return createArchivedShipmentLocationCopy(shipmentLocation);
  }

  /**
   * Create an archived copy of a shipment location entity. The copy is marked as archived and saved
   * to the database.
   *
   * @param shipmentLocation The entity to archive
   * @return The created archived copy
   */
  private ShipmentLocationEntity createArchivedShipmentLocationCopy(
      ShipmentLocationEntity shipmentLocation) {
    ShipmentLocationEntity archivedShipmentLocation = new ShipmentLocationEntity();
    archivedShipmentLocation.setName(shipmentLocation.getName());
    archivedShipmentLocation.setArchived(true);
    return shipmentLocationRepository.save(archivedShipmentLocation);
  }

  /**
   * Get or create an archived copy of a material category. First checks in database if an archived
   * version with the same name exists. If yes, reuses it. If no, creates a new archived copy.
   *
   * @param category The material category entity to archive
   * @return The archived material category (existing or newly created)
   */
  public MaterialCategoryEntity getOrCreateArchivedMaterialCategory(
      MaterialCategoryEntity category) {
    // Check if archived version already exists in DB
    Optional<MaterialCategoryEntity> existingArchived =
        materialCategoryRepository.findByNameIgnoreCaseAndArchivedTrue(category.getName());

    if (existingArchived.isPresent()) {
      log.debug("Reusing existing archived material category {}", category.getName());
      return existingArchived.get();
    }

    // Create new archived copy
    log.debug("Creating new archived material category {}", category.getName());
    return createArchivedMaterialCategoryCopy(category);
  }

  /**
   * Create an archived copy of a material category entity. The copy is marked as archived and saved
   * to the database.
   *
   * @param category The material category entity to archive
   * @return The created archived material category copy
   */
  private MaterialCategoryEntity createArchivedMaterialCategoryCopy(
      MaterialCategoryEntity category) {
    MaterialCategoryEntity archivedCategory = new MaterialCategoryEntity();
    archivedCategory.setName(category.getName());
    archivedCategory.setAbbreviation(category.getAbbreviation());
    archivedCategory.setArchived(true);
    return materialCategoryRepository.save(archivedCategory);
  }

  /**
   * Get or create an archived copy of a unit. First checks in database if an archived version with
   * the same name exists. If yes, reuses it. If no, creates a new archived copy.
   *
   * @param unit The unit entity to archive
   * @return The archived unit (existing or newly created)
   */
  public UnitEntity getOrCreateArchivedUnit(UnitEntity unit) {
    // Check if archived version already exists in DB
    Optional<UnitEntity> existingArchived =
        unitRepository.findByNameIgnoreCaseAndArchivedTrue(unit.getName());

    if (existingArchived.isPresent()) {
      log.debug("Reusing existing archived unit {}", unit.getName());
      return existingArchived.get();
    }

    // Create new archived copy
    log.debug("Creating new archived unit {}", unit.getName());
    return createArchivedUnitCopy(unit);
  }

  /**
   * Create an archived copy of a unit entity. The copy is marked as archived and saved to the
   * database.
   *
   * @param category The unit entity to archive
   * @return The created archived unit copy
   */
  private UnitEntity createArchivedUnitCopy(UnitEntity category) {
    UnitEntity archivedUnit = new UnitEntity();
    archivedUnit.setName(category.getName());
    archivedUnit.setArchived(true);
    return unitRepository.save(archivedUnit);
  }

  /**
   * Get or create an archived copy of a shipment method. First checks in database if an archived
   * version with the same name exists. If yes, reuses it. If no, creates a new archived copy.
   *
   * @param shipmentMethod The shipment method entity to archived
   * @return The archived shipment method (existing or newly created)
   */
  public ShipmentMethodEntity getOrCreateArchivedShipmentMethod(
      ShipmentMethodEntity shipmentMethod) {
    // Check if archived version already exists in DB
    Optional<ShipmentMethodEntity> existingArchived =
        shipmentMethodRepository.findByNameIgnoreCaseAndArchivedTrue(shipmentMethod.getName());

    if (existingArchived.isPresent()) {
      log.debug("Reusing existing archived shipment method {}", shipmentMethod.getName());
      return existingArchived.get();
    }

    // Create new archived copy
    log.debug("Creating new archived shipment method {}", shipmentMethod.getName());
    return createArchivedShipmentMethodCopy(shipmentMethod);
  }

  /**
   * Create an archived copy of a shipment method entity. The copy is marked as archived and saved
   * to the database.
   *
   * @param shipmentMethod The shipment method entity to archive
   * @return The created archived shipment method copy
   */
  private ShipmentMethodEntity createArchivedShipmentMethodCopy(
      ShipmentMethodEntity shipmentMethod) {
    ShipmentMethodEntity archivedShipmentMethod = new ShipmentMethodEntity();
    archivedShipmentMethod.setName(shipmentMethod.getName());
    archivedShipmentMethod.setPercentage(shipmentMethod.getPercentage());
    archivedShipmentMethod.setArchived(true);
    return shipmentMethodRepository.save(archivedShipmentMethod);
  }

  public void setStatusAllNonAbortedLinesOfCostRequestStatus(
      CostRequestEntity costRequest, CostRequestStatus status) {
    costRequest
        .getLines()
        .forEach(
            l -> {
              if (l.getStatus() != CostRequestStatus.ABORTED) l.setStatus(status);
            });
  }
}
