package my.lokalix.planning.core.services;

import jakarta.transaction.Transactional;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.mappers.MaterialMapper;
import my.lokalix.planning.core.mappers.MaterialSupplierMapper;
import my.lokalix.planning.core.mappers.MaterialSupplierMoqLineMapper;
import my.lokalix.planning.core.models.MaterialUploadResult;
import my.lokalix.planning.core.models.entities.MaterialEntity;
import my.lokalix.planning.core.models.entities.MaterialSupplierEntity;
import my.lokalix.planning.core.models.entities.MaterialSupplierMoqLineEntity;
import my.lokalix.planning.core.models.entities.admin.*;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.lokalix.planning.core.models.enums.MaterialStatus;
import my.lokalix.planning.core.models.enums.MaterialType;
import my.lokalix.planning.core.models.enums.SupplierAndManufacturerType;
import my.lokalix.planning.core.repositories.*;
import my.lokalix.planning.core.repositories.admin.MaterialCategoryRepository;
import my.lokalix.planning.core.repositories.admin.SupplierAndManufacturerRepository;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import my.lokalix.planning.core.services.helper.MaterialHelper;
import my.lokalix.planning.core.services.helper.SystemIdHelper;
import my.lokalix.planning.core.services.validator.MaterialValidator;
import my.lokalix.planning.core.utils.ExcelUtils;
import my.lokalix.planning.core.utils.NumberUtils;
import my.lokalix.planning.core.utils.TimeUtils;
import my.zkonsulting.planning.generated.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Slf4j
@Service
public class MaterialService {

  private final MaterialMapper materialMapper;
  private final MaterialSupplierMapper materialSupplierMapper;
  private final MaterialSupplierMoqLineMapper materialSupplierMoqLineMapper;
  private final MaterialRepository materialRepository;
  private final MaterialValidator materialValidator;
  private final SupplierAndManufacturerRepository supplierAndManufacturerRepository;
  private final MaterialCategoryRepository materialCategoryRepository;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final AppConfigurationProperties appConfigurationProperties;
  private final MaterialSupplierMoqLineRepository materialSupplierMoqLineRepository;
  private final MaterialSupplierRepository materialSupplierRepository;
  private final CurrencyRepository currencyRepository;
  private final SystemIdHelper systemIdHelper;
  private final MaterialHelper materialHelper;

  @Transactional
  public SWMaterial createMaterial(SWMaterialCreate body) {
    // Verify manufacturer exists
    SupplierManufacturerEntity manufacturer =
        entityRetrievalHelper.getMustExistSupplierManufacturerById(body.getManufacturerId());

    // Verify category exists
    MaterialCategoryEntity category =
        entityRetrievalHelper.getMustExistMaterialCategoryById(body.getCategoryId());

    // Verify unit exists
    UnitEntity unit =
        body.getUnitId() != null
            ? entityRetrievalHelper.getMustExistUnitById(body.getUnitId())
            : null;

    // Verify at least one supplier is provided
    if (body.getSuppliers() == null || body.getSuppliers().isEmpty()) {
      throw new GenericWithMessageException(
          "At least one supplier must be provided", SWCustomErrorCode.GENERIC_ERROR);
    }

    // Verify that only one supplier is marked as chosen
    long defaultSuppliersCount =
        body.getSuppliers().stream().filter(SWMaterialSupplierCreate::getDefaultSupplier).count();
    if (defaultSuppliersCount != 1) {
      throw new GenericWithMessageException(
          "Exactly one supplier must be marked as default", SWCustomErrorCode.GENERIC_ERROR);
    }

    // Create material entity
    MaterialEntity materialEntity = materialMapper.toMaterialEntity(body);
    materialEntity.setManufacturer(manufacturer);
    materialEntity.setCategory(category);
    materialEntity.setUnit(unit);

    // Generate system ID
    String systemId =
        systemIdHelper.generateSystemId(manufacturer, category, body.getManufacturerPartNumber());
    materialEntity.setSystemId(systemId);

    // Set status to ESTIMATED since suppliers are provided
    materialEntity.setStatus(MaterialStatus.ESTIMATED);

    // Save material first to get the ID
    MaterialEntity savedMaterial = materialRepository.save(materialEntity);

    // Create suppliers with MOQ lines and add to collection
    for (SWMaterialSupplierCreate supplierCreate : body.getSuppliers()) {
      MaterialSupplierEntity supplierEntity = createSupplierEntity(supplierCreate, savedMaterial);
      savedMaterial.addMaterialSupplier(supplierEntity);
    }

    // Save again to persist suppliers
    savedMaterial = materialRepository.save(savedMaterial);

    return materialMapper.toSwMaterial(savedMaterial);
  }

  private MaterialSupplierEntity createSupplierEntity(
      SWMaterialSupplierCreate supplierCreate, MaterialEntity material) {
    // Verify purchasing currency exists
    CurrencyEntity purchasingCurrency =
        entityRetrievalHelper.getMustExistCurrencyById(supplierCreate.getPurchasingCurrencyId());
    SupplierManufacturerEntity supplier =
        entityRetrievalHelper.getMustExistSupplierManufacturerById(supplierCreate.getSupplierId());
    // Verify at least one MOQ line is provided
    if (supplierCreate.getMoqLines() == null || supplierCreate.getMoqLines().isEmpty()) {
      throw new GenericWithMessageException(
          "At least one MOQ line must be provided for each supplier",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    MaterialSupplierEntity supplierEntity =
        materialSupplierMapper.toMaterialSupplierEntity(supplierCreate);

    supplierEntity.setSupplier(supplier);
    supplierEntity.setPurchasingCurrency(purchasingCurrency);
    supplierEntity.setMaterial(material);
    supplierEntity.setDefaultSupplier(supplierCreate.getDefaultSupplier());
    // Create MOQ lines
    List<MaterialSupplierMoqLineEntity> moqLines = new ArrayList<>();
    for (SWMaterialSupplierMoqLineCreate moqLineCreate : supplierCreate.getMoqLines()) {
      if (NumberUtils.isNullOrNotStrictlyPositive(moqLineCreate.getMinimumOrderQuantity())) {
        throw new GenericWithMessageException(
            "Minimum order quantity must be greater than 0", SWCustomErrorCode.GENERIC_ERROR);
      }
      if (NumberUtils.isNullOrNotStrictlyPositive(
          moqLineCreate.getUnitPurchasingPriceInPurchasingCurrency())) {
        throw new GenericWithMessageException(
            "Unit price must be greater than 0", SWCustomErrorCode.GENERIC_ERROR);
      }
      MaterialSupplierMoqLineEntity moqLineEntity =
          materialSupplierMoqLineMapper.toMaterialSupplierMoqLineEntity(moqLineCreate);
      moqLineEntity.setMaterialSupplier(supplierEntity);
      moqLines.add(moqLineEntity);
    }
    supplierEntity.setMoqLines(moqLines);

    return supplierEntity;
  }

  @Transactional
  public SWMaterial updateMaterial(UUID uid, SWMaterialUpdate body) {
    MaterialEntity materialEntity = entityRetrievalHelper.getMustExistMaterialById(uid);

    // Verify manufacturer exists
    SupplierManufacturerEntity manufacturer =
        entityRetrievalHelper.getMustExistSupplierManufacturerById(body.getManufacturerId());

    // Verify category exists
    MaterialCategoryEntity category =
        entityRetrievalHelper.getMustExistMaterialCategoryById(body.getCategoryId());

    // Verify unit exists
    UnitEntity unit =
        body.getUnitId() != null
            ? entityRetrievalHelper.getMustExistUnitById(body.getUnitId())
            : null;

    // Update basic fields
    materialEntity.setManufacturer(manufacturer);
    materialEntity.setManufacturerPartNumber(body.getManufacturerPartNumber());
    materialEntity.setDescription(body.getDescription());
    materialEntity.setCategory(category);
    materialEntity.setUnit(unit);
    materialEntity.setMaterialType(MaterialType.fromValue(body.getMaterialType().getValue()));
    if (StringUtils.isBlank(materialEntity.getSystemId())) {
      // Generate system ID
      String systemId =
          systemIdHelper.generateSystemId(manufacturer, category, body.getManufacturerPartNumber());
      materialEntity.setSystemId(systemId);
    } else {
      // To avoid recreating a System ID with same prefix if it already exists with same prefix
      if (!materialEntity
          .getSystemId()
          .startsWith(
              systemIdHelper.buildSystemIdPrefix(
                  manufacturer, category, body.getManufacturerPartNumber()))) {
        materialEntity.setSystemId(
            systemIdHelper.generateSystemId(
                manufacturer, category, body.getManufacturerPartNumber()));
      }
    }

    // material is now OK, set its status to ESTIMATED if supplier has already been provided
    if (CollectionUtils.isNotEmpty(materialEntity.getSuppliers())) {
      materialEntity.setStatus(MaterialStatus.ESTIMATED);
      materialEntity = materialRepository.save(materialEntity);
      // Notify engineering if all materials of an affected cost request line are now estimated
      materialHelper.refreshMaterialLinesUsing(materialEntity);
    } else {
      materialEntity = materialRepository.save(materialEntity);
    }

    return materialMapper.toSwMaterial(materialEntity);
  }

  @Transactional
  public SWMaterial retrieveMaterial(UUID uid) {
    MaterialEntity materialEntity = entityRetrievalHelper.getMustExistMaterialById(uid);
    return materialMapper.toSwMaterial(materialEntity);
  }

  @Transactional
  public SWMaterialsPaginated searchMaterials(
      int offset,
      int limit,
      SWBasicSearch search,
      List<SWMaterialStatus> statuses,
      SWMaterialType type) {
    Sort sort = Sort.by(Sort.Direction.ASC, "systemId");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);
    Page<MaterialEntity> paginatedMaterials;

    boolean hasStatuses = CollectionUtils.isNotEmpty(statuses);
    boolean hasSearch = StringUtils.isNotBlank(search.getSearchText());
    MaterialType materialType = materialMapper.toMaterialType(type);
    if (hasStatuses) {
      List<MaterialStatus> mappedStatuses =
          statuses.stream()
              .map(s -> MaterialStatus.fromValue(s.getValue()))
              .collect(java.util.stream.Collectors.toList());
      if (hasSearch) {
        paginatedMaterials =
            materialRepository.findBySearchAndArchivedFalseAndStatusInAndType(
                pageable, search.getSearchText(), mappedStatuses, materialType);
      } else {
        paginatedMaterials =
            materialRepository.findByArchivedFalseAndStatusInAndOptionalType(
                pageable, mappedStatuses, materialType);
      }
    } else if (hasSearch) {
      paginatedMaterials =
          materialRepository.findBySearchAndArchivedFalse(
              pageable, search.getSearchText(), materialType);
    } else {
      paginatedMaterials =
          materialRepository.findByArchivedFalseAndOptionalMaterialType(pageable, materialType);
    }

    return populateMaterialsPaginatedResults(paginatedMaterials);
  }

  @Transactional
  public SWMaterial archiveMaterial(UUID uid) {
    MaterialEntity materialEntity = entityRetrievalHelper.getMustExistMaterialById(uid);
    materialValidator.validateNotInUse(materialEntity);
    materialEntity.setArchived(true);
    MaterialEntity savedMaterial = materialRepository.save(materialEntity);
    return materialMapper.toSwMaterial(savedMaterial);
  }

  @Transactional
  public List<SWMaterial> autoCompleteMaterial(SWAutoCompleteMaterialBody body) {
    // Get manufacturer
    SupplierManufacturerEntity manufacturer =
        entityRetrievalHelper.getMustExistSupplierManufacturerById(body.getManufacturerId());

    // Search for materials by manufacturer and part number (contains, case insensitive)
    List<MaterialEntity> materials =
        materialRepository.findByManufacturerAndPartNumberContaining(
            manufacturer,
            body.getManufacturerPartNumber(),
            materialMapper.toMaterialType(body.getMaterialType()));

    return materialMapper.toListSwMaterial(materials);
  }

  // Helper methods

  private SWMaterialsPaginated populateMaterialsPaginatedResults(
      Page<MaterialEntity> paginatedMaterials) {
    SWMaterialsPaginated materialsPaginated = new SWMaterialsPaginated();
    materialsPaginated.setResults(materialMapper.toListSwMaterial(paginatedMaterials.getContent()));
    materialsPaginated.setPage(paginatedMaterials.getNumber());
    materialsPaginated.setPerPage(paginatedMaterials.getSize());
    materialsPaginated.setTotal((int) paginatedMaterials.getTotalElements());
    materialsPaginated.setHasPrev(paginatedMaterials.hasPrevious());
    materialsPaginated.setHasNext(paginatedMaterials.hasNext());
    return materialsPaginated;
  }

  @Transactional
  public MaterialUploadResult uploadMaterialsFromExcel(MultipartFile file) throws IOException {
    int totalCreated = 0;
    int newSuppliersAdded = 0;
    int newMoqLinesAdded = 0;
    int updatedMoqLines = 0;
    List<String[]> manufacturerNotFoundRows = new ArrayList<>();
    List<String[]> manufacturerNotFoundOnSupplierNameRows = new ArrayList<>();
    List<String[]> suppliersNotFound = new ArrayList<>();
    Set<String> supplierNamesNotFound = new HashSet<>();
    List<String> manufacturerCodeExtractionFailedRows = new ArrayList<>();

    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream)) {

      // First sheet: Material Categories
      Sheet materialsSheet = workbook.getSheetAt(0);
      log.info("Processing materials sheet: {}", materialsSheet.getSheetName());

      // Pre-scan sheet to collect all systemIds for a single batch DB lookup
      Set<String> allSystemIds = new HashSet<>();
      for (int i = 1; i <= materialsSheet.getLastRowNum(); i++) {
        Row r = materialsSheet.getRow(i);
        if (r == null) continue;
        String sid = ExcelUtils.loadStringCell(r.getCell(0));
        if (StringUtils.isNotBlank(sid)) {
          allSystemIds.add(sid);
        }
      }

      // Load all existing materials with their suppliers in one query,
      // then a second query to populate moqLines on those suppliers.
      // Two queries avoid Hibernate's MultipleBagFetchException; the first-level cache
      // merges the second query's results into the same entity instances.
      Map<String, MaterialEntity> materialsBySystemId = new HashMap<>();
      if (!allSystemIds.isEmpty()) {
        List<MaterialEntity> existingMaterials =
            materialRepository.findBySystemIdInWithSuppliers(allSystemIds);
        existingMaterials.forEach(m -> materialsBySystemId.put(m.getSystemId(), m));

        Set<UUID> supplierIds =
            existingMaterials.stream()
                .flatMap(m -> m.getSuppliers().stream())
                .map(MaterialSupplierEntity::getMaterialSupplierId)
                .collect(Collectors.toSet());
        if (!supplierIds.isEmpty()) {
          materialSupplierRepository.findByIdInWithMoqLines(supplierIds);
        }
      }

      MaterialCategoryEntity noneCategory =
          materialCategoryRepository.findByNameIgnoreCaseAndArchivedFalse("NONE").get();

      List<SupplierManufacturerEntity> suppliers =
          supplierAndManufacturerRepository.findAllByTypeInAndArchivedFalse(
              List.of(SupplierAndManufacturerType.SUPPLIER, SupplierAndManufacturerType.BOTH),
              Sort.unsorted());
      Map<String, SupplierManufacturerEntity> mapSuppliers =
          suppliers.stream().collect(Collectors.toMap(s -> s.getName().toUpperCase(), s -> s));

      List<CurrencyEntity> currencies = currencyRepository.findAllByArchivedFalse(Sort.unsorted());
      Map<String, CurrencyEntity> mapCurrencies =
          currencies.stream().collect(Collectors.toMap(CurrencyEntity::getCode, s -> s));

      List<SupplierManufacturerEntity> manufacturers =
          supplierAndManufacturerRepository.findAllByTypeInAndArchivedFalse(
              List.of(SupplierAndManufacturerType.MANUFACTURER, SupplierAndManufacturerType.BOTH),
              Sort.unsorted());
      Map<String, SupplierManufacturerEntity> mapManufacturers =
          manufacturers.stream().collect(Collectors.toMap(s -> s.getCode().toUpperCase(), s -> s));

      List<MaterialCategoryEntity> materialCategories =
          materialCategoryRepository.findAllByArchivedFalse(Sort.unsorted());
      Map<String, MaterialCategoryEntity> mapMaterialCategories =
          materialCategories.stream()
              .collect(Collectors.toMap(s -> s.getName().toUpperCase(), s -> s));

      List<MaterialEntity> materialsToCreate = new ArrayList<>();
      // Process each row starting from row 2 (index 2)
      for (int rowIndex = 1; rowIndex <= materialsSheet.getLastRowNum(); rowIndex++) {
        Row row = materialsSheet.getRow(rowIndex);
        if (row == null) {
          continue;
        }

        // Get systemId from first column (index 0)
        String systemId = ExcelUtils.loadStringCell(row.getCell(0));

        // Skip if systemId is blank
        if (StringUtils.isBlank(systemId)) {
          continue;
        }
        // Read supplier and MOQ info early (needed for both new and duplicate systemId paths)

        // Get manufacturerPartNumber from second column (index 1)
        String manufacturerPartNumber = ExcelUtils.loadStringCell(row.getCell(1));

        // Skip if manufacturerPartNumber is blank
        if (StringUtils.isBlank(manufacturerPartNumber)) {
          log.warn("Row {}: manufacturerPartNumber is blank, skipping", rowIndex);
          continue;
        }

        // Read supplier and MOQ info early (needed for both new and duplicate systemId paths)
        String supplierName = ExcelUtils.loadStringCell(row.getCell(2));

        // Look up supplier by name
        SupplierManufacturerEntity supplier = null;
        if (StringUtils.isNotBlank(supplierName)) {
          supplier = mapSuppliers.get(supplierName.toUpperCase());
          if (supplier == null) {
            suppliersNotFound.add(new String[] {systemId, supplierName.toUpperCase()});
            supplierNamesNotFound.add(supplierName);
            continue;
          }
        } else {
          log.warn("Row {}: supplierName is blank, skipping", rowIndex);
          continue;
        }

        String manufacturerName = supplierName;

        // Look up manufacturer by name
        SupplierManufacturerEntity manufacturer = null;
        if (StringUtils.isNotBlank(manufacturerName)) {
          manufacturer = mapManufacturers.get(manufacturerName.toUpperCase());
          if (manufacturer == null) {
            manufacturerNotFoundOnSupplierNameRows.add(
                new String[] {systemId, manufacturerName.toUpperCase()});
          }
        } else {
          log.warn("Row {}: manufacturerName is blank, skipping", rowIndex);
        }

        String categoryName = ExcelUtils.loadStringCell(row.getCell(9));

        // Look up manufacturer by code
        MaterialCategoryEntity category =
            StringUtils.isNotBlank(categoryName)
                ? mapMaterialCategories.getOrDefault(categoryName.toUpperCase(), noneCategory)
                : noneCategory;

        String currencyCode = ExcelUtils.loadStringCell(row.getCell(3));
        if (currencyCode.equals("YEN")) {
          currencyCode = "JPY";
        }
        String moqQuantity = ExcelUtils.loadStringCell(row.getCell(6));
        String moqQuantityPrice = ExcelUtils.loadStringCell(row.getCell(4));
        String spqStr = ExcelUtils.loadStringCell(row.getCell(7));

        boolean isDirect = StringUtils.isNotBlank(ExcelUtils.loadStringCell(row.getCell(14)));
        boolean isIndirect = StringUtils.isNotBlank(ExcelUtils.loadStringCell(row.getCell(15)));

        if (!isDirect && !isIndirect) {
          log.warn("Row {}: isDirect and is Indirect are both blank, skipping", rowIndex);
          continue;
        }
        if (isDirect && isIndirect) {
          log.warn("Row {}: isDirect and is Indirect are both marked, skipping", rowIndex);
          continue;
        }

        BigDecimal standardPackagingQty =
            StringUtils.isNotBlank(spqStr) ? new BigDecimal(spqStr) : null;

        // Existing material with same systemId: add supplier/MOQ to existing material if not
        // already present
        if (materialsBySystemId.containsKey(systemId)) {
          MaterialEntity existingMaterial = materialsBySystemId.get(systemId);
          existingMaterial.setMaterialType(isDirect ? MaterialType.DIRECT : MaterialType.INDIRECT);
          existingMaterial.setCategory(category);
          existingMaterial.setManufacturerPartNumber(manufacturerPartNumber);
          existingMaterial.setManufacturer(manufacturer);

          if (StringUtils.isNotBlank(moqQuantity)
              && StringUtils.isNotBlank(moqQuantityPrice)
              && StringUtils.isNotBlank(supplierName)) {
            SupplierManufacturerEntity supplierEntity =
                mapSuppliers.get(supplierName.toUpperCase());
            if (supplierEntity != null) {
              MaterialSupplierEntity existingMaterialSupplier =
                  existingMaterial.getSuppliers().stream()
                      .filter(
                          ms -> ms.getSupplier() != null && ms.getSupplier().equals(supplierEntity))
                      .findFirst()
                      .orElse(null);
              String leadTime = ExcelUtils.loadStringCellForceNumericToLong(row.getCell(5));
              if (existingMaterialSupplier == null) {
                MaterialSupplierEntity newSupplier = new MaterialSupplierEntity();
                newSupplier.setMaterial(existingMaterial);
                newSupplier.setSupplier(supplierEntity);
                newSupplier.setPurchasingCurrency(mapCurrencies.get(currencyCode));
                newSupplier.setDefaultSupplier(false);
                MaterialSupplierMoqLineEntity newMoqLine = new MaterialSupplierMoqLineEntity();
                newMoqLine.setMinimumOrderQuantity(new BigDecimal(moqQuantity));
                newMoqLine.setUnitPurchasingPriceInPurchasingCurrency(
                    new BigDecimal(moqQuantityPrice));
                newMoqLine.setStandardPackagingQuantity(standardPackagingQty);
                if (StringUtils.isNotBlank(leadTime)) {
                  newMoqLine.setLeadTime(leadTime + " days");
                }
                newSupplier.addMoqLine(newMoqLine);
                existingMaterial.addMaterialSupplier(newSupplier);
                existingMaterial.setStatus(MaterialStatus.ESTIMATED);
                newSuppliersAdded++;
                newMoqLinesAdded++;
              } else {
                BigDecimal moqQty = new BigDecimal(moqQuantity).setScale(6, RoundingMode.HALF_UP);
                MaterialSupplierMoqLineEntity existingMoqLine =
                    existingMaterialSupplier.getMoqLines().stream()
                        .filter(m -> m.getMinimumOrderQuantity().compareTo(moqQty) == 0)
                        .findFirst()
                        .orElse(null);
                if (existingMoqLine == null) {
                  MaterialSupplierMoqLineEntity newMoqLine = new MaterialSupplierMoqLineEntity();
                  newMoqLine.setMinimumOrderQuantity(moqQty);
                  newMoqLine.setUnitPurchasingPriceInPurchasingCurrency(
                      new BigDecimal(moqQuantityPrice));
                  newMoqLine.setStandardPackagingQuantity(standardPackagingQty);
                  if (StringUtils.isNotBlank(leadTime)) {
                    newMoqLine.setLeadTime(leadTime + " days");
                  }
                  existingMaterialSupplier.addMoqLine(newMoqLine);
                  newMoqLinesAdded++;
                } else {
                  BigDecimal importedPrice =
                      new BigDecimal(moqQuantityPrice).setScale(6, RoundingMode.HALF_UP);
                  String importedLeadTime =
                      StringUtils.isNotBlank(leadTime) ? leadTime + " days" : null;
                  boolean changed =
                      existingMoqLine
                                  .getUnitPurchasingPriceInPurchasingCurrency()
                                  .compareTo(importedPrice)
                              != 0
                          || !Objects.equals(existingMoqLine.getLeadTime(), importedLeadTime)
                          || !Objects.equals(
                              existingMoqLine.getStandardPackagingQuantity(), standardPackagingQty);
                  existingMoqLine.setUnitPurchasingPriceInPurchasingCurrency(importedPrice);
                  existingMoqLine.setLeadTime(importedLeadTime);
                  existingMoqLine.setStandardPackagingQuantity(standardPackagingQty);
                  if (changed) {
                    updatedMoqLines++;
                  }
                }
              }
            }
          }
        } else {

          if (manufacturer == null) {
            // Extract manufacturer code from systemId (first character + following digits)
            String manufacturerCode = null;
            if (StringUtils.isNotBlank(systemId)) {
              StringBuilder codeBuilder = new StringBuilder();
              codeBuilder.append(systemId.charAt(0));

              // Add following characters if they are digits
              for (int i = 1; i < systemId.length(); i++) {
                char c = systemId.charAt(i);
                if (Character.isDigit(c)) {
                  codeBuilder.append(c);
                } else {
                  break;
                }
              }

              manufacturerCode = codeBuilder.toString();
            }

            // Look up manufacturer by code
            if (StringUtils.isNotBlank(manufacturerCode)) {
              manufacturer = mapManufacturers.get(manufacturerCode);
              if (manufacturer == null) {
                manufacturerNotFoundRows.add(new String[] {systemId, manufacturerCode});
                continue;
              }
            } else {
              manufacturerCodeExtractionFailedRows.add(systemId);
              continue;
            }
          }

          MaterialEntity material = new MaterialEntity();
          material.setSystemId(systemId);
          material.setManufacturerPartNumber(manufacturerPartNumber);
          material.setCategory(category);
          material.setManufacturer(manufacturer);
          material.setMaterialType(MaterialType.DIRECT);
          material.setStatus(MaterialStatus.TO_BE_ESTIMATED);

          MaterialSupplierEntity materialSupplier = null;
          MaterialSupplierMoqLineEntity moqLine = new MaterialSupplierMoqLineEntity();
          if (StringUtils.isNotBlank(moqQuantity) && StringUtils.isNotBlank(moqQuantityPrice)) {
            // Create new material entity

            materialSupplier = new MaterialSupplierEntity();
            materialSupplier.setMaterial(material);
            materialSupplier.setSupplier(supplier);
            materialSupplier.setPurchasingCurrency(mapCurrencies.get(currencyCode));
            materialSupplier.setDefaultSupplier(true);

            moqLine.setMinimumOrderQuantity(new BigDecimal(moqQuantity));
            moqLine.setUnitPurchasingPriceInPurchasingCurrency(new BigDecimal(moqQuantityPrice));
            moqLine.setStandardPackagingQuantity(standardPackagingQty);
            String leadTime = ExcelUtils.loadStringCellForceNumericToLong(row.getCell(5));
            if (StringUtils.isNotBlank(leadTime)) {
              moqLine.setLeadTime(leadTime + " days");
            }
            moqLine.setMaterialSupplier(materialSupplier);

            materialSupplier.addMoqLine(moqLine);
            material.addMaterialSupplier(materialSupplier);
            material.setStatus(MaterialStatus.ESTIMATED);
            newSuppliersAdded++;
            newMoqLinesAdded++;
          }
          materialsBySystemId.put(systemId, material);
          materialsToCreate.add(material);
        }
      }

      // Save all materials in batch
      if (!materialsToCreate.isEmpty()) {
        materialRepository.saveAll(
            materialsToCreate.stream()
                .filter(m -> m.getStatus() == MaterialStatus.ESTIMATED)
                .toList());
        totalCreated += materialsToCreate.size();
      }
      log.info(
          "Import complete: {} materials created, {} suppliers added, {} MOQ lines added, {} MOQ lines updated",
          totalCreated,
          newSuppliersAdded,
          newMoqLinesAdded,
          updatedMoqLines);
    }

    String timestamp =
        TimeUtils.nowOffsetDateTimeUTC().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    String manufacturerNotFoundCsvPath =
        writeCsvIfNotEmpty(
            manufacturerNotFoundRows,
            new String[] {"systemId", "manufacturerCode"},
            "manufacturer_not_found_" + timestamp + ".csv");
    String suppliersNotFoundCsvPath =
        writeSingleColumnCsvIfNotEmpty(
            supplierNamesNotFound, "supplierName", "suppliers_not_found_" + timestamp + ".csv");

    return new MaterialUploadResult(
        totalCreated,
        newSuppliersAdded,
        newMoqLinesAdded,
        updatedMoqLines,
        manufacturerNotFoundCsvPath,
        suppliersNotFoundCsvPath);
  }

  private String writeCsvIfNotEmpty(List<String[]> rows, String[] headers, String filename)
      throws IOException {
    if (rows.isEmpty()) {
      return null;
    }
    String filePath =
        Paths.get(appConfigurationProperties.getTemporaryFilesPathDirectory(), filename).toString();
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
      writer.write(String.join(";", headers));
      writer.newLine();
      for (String[] row : rows) {
        writer.write(String.join(";", row));
        writer.newLine();
      }
    }
    log.info("CSV written to: {}", filePath);
    return filePath;
  }

  private String writeSingleColumnCsvIfNotEmpty(Set<String> rows, String header, String filename)
      throws IOException {
    if (rows.isEmpty()) {
      return null;
    }
    String filePath =
        Paths.get(appConfigurationProperties.getTemporaryFilesPathDirectory(), filename).toString();
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
      writer.write(header);
      writer.newLine();
      for (String row : rows) {
        writer.write(row);
        writer.newLine();
      }
    }
    log.info("CSV written to: {}", filePath);
    return filePath;
  }
}
