package my.lokalix.planning.core.services;

import jakarta.persistence.EntityExistsException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.mappers.EnumMapper;
import my.lokalix.planning.core.mappers.SupplierAndManufacturerMapper;
import my.lokalix.planning.core.models.entities.MaterialEntity;
import my.lokalix.planning.core.models.entities.MaterialLineDraftEntity;
import my.lokalix.planning.core.models.entities.admin.ShipmentMethodEntity;
import my.lokalix.planning.core.models.entities.admin.SupplierManufacturerEntity;
import my.lokalix.planning.core.models.enums.MaterialStatus;
import my.lokalix.planning.core.models.enums.SupplierAndManufacturerType;
import my.lokalix.planning.core.repositories.MaterialLineDraftRepository;
import my.lokalix.planning.core.repositories.MaterialRepository;
import my.lokalix.planning.core.repositories.admin.SupplierAndManufacturerRepository;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import my.lokalix.planning.core.services.helper.MaterialHelper;
import my.lokalix.planning.core.services.helper.SystemIdHelper;
import my.lokalix.planning.core.services.validator.SupplierManufacturerValidator;
import my.lokalix.planning.core.utils.ExcelUtils;
import my.lokalix.planning.core.utils.TextUtils;
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
public class SupplierAndManufacturerService {

  private final SupplierAndManufacturerMapper supplierAndManufacturerMapper;
  private final SupplierAndManufacturerRepository supplierAndManufacturerRepository;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final SupplierManufacturerValidator supplierManufacturerValidator;
  private final EnumMapper enumMapper;
  private final MaterialRepository materialRepository;
  private final SystemIdHelper systemIdHelper;
  private final MaterialLineDraftRepository materialLineDraftRepository;
  private final MaterialHelper materialHelper;

  @Transactional
  public SWSupplierAndManufacturer createSupplierManufacturer(
      SWSupplierAndManufacturerCreate body) {
    if (supplierAndManufacturerRepository.existsByNameIgnoreCaseAndArchivedFalse(body.getName())) {
      throw new EntityExistsException(
          "A supplier/manufacturer with the same name '" + body.getName() + "' already exists");
    }
    if (supplierAndManufacturerRepository.existsByCodeIgnoreCaseAndArchivedFalse(body.getCode())) {
      throw new EntityExistsException(
          "A supplier/manufacturer with the same code '" + body.getCode() + "' already exists");
    }

    SupplierManufacturerEntity entity =
        supplierAndManufacturerMapper.toSupplierManufacturerEntity(body);
    if (body.getShipmentMethodId() != null) {
      ShipmentMethodEntity shipmentMethod =
          entityRetrievalHelper.getMustExistShipmentMethodById(body.getShipmentMethodId());
      entity.setShipmentMethod(shipmentMethod);
    } else {
      entity.setShipmentMethod(null);
    }
    entity = supplierAndManufacturerRepository.save(entity);

    syncMaterialAndMaterialLineDraft(entity);

    return supplierAndManufacturerMapper.toSwSupplierManufacturer(entity);
  }

  private void syncMaterialAndMaterialLineDraft(SupplierManufacturerEntity entity) {
    boolean anyMatch = false;
    if (entity.getType() == SupplierAndManufacturerType.MANUFACTURER
        || entity.getType() == SupplierAndManufacturerType.BOTH
        || entity.getType() == SupplierAndManufacturerType.SUPPLIER) {
      List<MaterialEntity> unlinkedMaterials =
          materialRepository
              .findByManufacturerNullAndDraftManufacturerNameIgnoreCaseAndArchivedFalse(
                  entity.getName());
      for (MaterialEntity material : unlinkedMaterials) {
        anyMatch = true;
        material.setManufacturer(entity);
        String systemId =
            systemIdHelper.generateSystemId(
                entity, material.getCategory(), material.getManufacturerPartNumber());
        material.setSystemId(systemId);

        // set its status to ESTIMATED if supplier has already been provided and all data is OK
        if (material.getCategory() != null && CollectionUtils.isNotEmpty(material.getSuppliers())) {
          material.setStatus(MaterialStatus.ESTIMATED);
          material = materialRepository.save(material);
          // Notify engineering if all materials of an affected cost request line are now estimated
          materialHelper.refreshMaterialLinesUsing(material);
        } else {
          materialRepository.save(material);
        }
      }

      List<MaterialLineDraftEntity> unlinkedDraftMaterialLines =
          materialLineDraftRepository.findByManufacturerNullAndDraftManufacturerNameIgnoreCase(
              entity.getName());
      for (MaterialLineDraftEntity materialLineDraft : unlinkedDraftMaterialLines) {
        anyMatch = true;
        materialLineDraft.setManufacturer(entity);
        materialLineDraftRepository.save(materialLineDraft);
      }
    }
    if (anyMatch && entity.getType() == SupplierAndManufacturerType.SUPPLIER) {
      entity.setType(SupplierAndManufacturerType.BOTH);
      supplierAndManufacturerRepository.save(entity);
    }
  }

  @Transactional
  public SWSupplierAndManufacturer updateSupplierManufacturer(
      UUID uid, SWSupplierAndManufacturerUpdate body) {
    SupplierManufacturerEntity entity =
        entityRetrievalHelper.getMustExistSupplierManufacturerById(uid);
    if (supplierAndManufacturerRepository
        .existsByCodeIgnoreCaseAndSupplierManufacturerIdNotAndArchivedFalse(body.getCode(), uid)) {
      throw new EntityExistsException(
          "A supplier/manufacturer with the same code '" + body.getCode() + "' already exists");
    }
    if (supplierAndManufacturerRepository
        .existsByNameIgnoreCaseAndSupplierManufacturerIdNotAndArchivedFalse(body.getName(), uid)) {
      throw new EntityExistsException(
          "A supplier/manufacturer with the same name '" + body.getName() + "' already exists");
    }
    // Check the case when changing the type from SUPPLIER to MANUFACTURER and vice versa
    if (body.getType() != SWSupplierAndManufacturerType.BOTH
        && enumMapper.asSupplierAndManufacturerType(body.getType()) != entity.getType()) {
      if (entity.getType() == SupplierAndManufacturerType.SUPPLIER
          || entity.getType() == SupplierAndManufacturerType.BOTH) {
        supplierManufacturerValidator.validateSupplierNotInUse(entity);
      }
      if (entity.getType() == SupplierAndManufacturerType.MANUFACTURER
          || entity.getType() == SupplierAndManufacturerType.BOTH) {
        supplierManufacturerValidator.validateManufacturerNotInUse(entity);
      }
    }

    supplierAndManufacturerMapper.updateAdminSupplierManufacturerEntityFromDto(body, entity);
    if (body.getShipmentMethodId() != null) {
      ShipmentMethodEntity shipmentMethod =
          entityRetrievalHelper.getMustExistShipmentMethodById(body.getShipmentMethodId());
      entity.setShipmentMethod(shipmentMethod);
    } else {
      entity.setShipmentMethod(null);
    }
    entity = supplierAndManufacturerRepository.save(entity);

    syncMaterialAndMaterialLineDraft(entity);

    return supplierAndManufacturerMapper.toSwSupplierManufacturer(entity);
  }

  @Transactional
  public void archiveSupplierManufacturer(UUID uid) {
    SupplierManufacturerEntity entity =
        entityRetrievalHelper.getMustExistSupplierManufacturerById(uid);
    supplierManufacturerValidator.validateNotInUse(entity);
    entity.setArchived(true);
    supplierAndManufacturerRepository.save(entity);
  }

  @Transactional
  public SWSupplierAndManufacturer retrieveSupplierManufacturer(UUID uid) {
    SupplierManufacturerEntity entity =
        entityRetrievalHelper.getMustExistSupplierManufacturerById(uid);
    return supplierAndManufacturerMapper.toSwSupplierManufacturer(entity);
  }

  @Transactional
  public List<SWSupplierAndManufacturer> listSuppliersOrManufacturers(
      SWSupplierAndManufacturerType type) {
    List<SupplierManufacturerEntity> allSupplierManufacturers =
        supplierAndManufacturerRepository.findAllByTypeInAndArchivedFalse(
            List.of(
                enumMapper.asSupplierAndManufacturerType(type), SupplierAndManufacturerType.BOTH),
            Sort.unsorted());
    allSupplierManufacturers.sort(
        (a, b) -> {
          if (a.getCode() == null) return 1;
          if (b.getCode() == null) return -1;
          return TextUtils.compareNaturally(a.getCode(), b.getCode());
        });
    return supplierAndManufacturerMapper.toListSwSupplierManufacturer(allSupplierManufacturers);
  }

  @Transactional
  public SWSuppliersAndManufacturersPaginated searchSuppliersAndManufacturers(
      int offset, int limit, SWBasicSearch search) {
    Sort sort = Sort.by(Sort.Direction.ASC, "code");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);
    Page<SupplierManufacturerEntity> paginatedManufacturers;

    if (StringUtils.isBlank(search.getSearchText())) {
      paginatedManufacturers = supplierAndManufacturerRepository.findByArchivedFalse(pageable);
    } else {
      paginatedManufacturers =
          supplierAndManufacturerRepository.findBySearchAndArchivedFalse(
              pageable, search.getSearchText());
    }

    return populateManufacturersPaginatedResults(paginatedManufacturers);
  }

  @Transactional
  public UUID existSupplierManufacturerByName(SWStringBody body) {
    return supplierAndManufacturerRepository
        .findByNameIgnoreCaseAndArchivedFalse(body.getValue())
        .map(SupplierManufacturerEntity::getSupplierManufacturerId)
        .orElse(null);
  }

  private SWSuppliersAndManufacturersPaginated populateManufacturersPaginatedResults(
      Page<SupplierManufacturerEntity> paginatedManufacturers) {
    SWSuppliersAndManufacturersPaginated manufacturersPaginated =
        new SWSuppliersAndManufacturersPaginated();
    List<SupplierManufacturerEntity> content = new ArrayList<>(paginatedManufacturers.getContent());
    content.sort(
        (a, b) -> {
          if (a.getCode() == null) return 1;
          if (b.getCode() == null) return -1;
          return TextUtils.compareNaturally(a.getCode(), b.getCode());
        });
    manufacturersPaginated.setResults(
        supplierAndManufacturerMapper.toListSwSupplierManufacturer(content));
    manufacturersPaginated.setPage(paginatedManufacturers.getNumber());
    manufacturersPaginated.setPerPage(paginatedManufacturers.getSize());
    manufacturersPaginated.setTotal((int) paginatedManufacturers.getTotalElements());
    manufacturersPaginated.setHasPrev(paginatedManufacturers.hasPrevious());
    manufacturersPaginated.setHasNext(paginatedManufacturers.hasNext());
    return manufacturersPaginated;
  }

  @Transactional
  public int uploadManufacturersFromExcel(MultipartFile file) throws IOException {
    List<SupplierManufacturerEntity> manufacturersToCreate = new ArrayList<>();
    Set<String> seenCodes = new HashSet<>();
    Set<String> seenNames = new HashSet<>();

    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream)) {

      // Process all 26 sheets
      int numberOfSheets = Math.min(workbook.getNumberOfSheets(), 26);

      for (int sheetIndex = 0; sheetIndex < numberOfSheets; sheetIndex++) {
        Sheet sheet = workbook.getSheetAt(sheetIndex);
        log.info(
            "Processing sheet {} of {}: {}", sheetIndex + 1, numberOfSheets, sheet.getSheetName());

        // Process each row starting from row 0
        for (Row row : sheet) {
          // Get code from first column (index 0)
          String code = ExcelUtils.loadStringCell(row.getCell(0));

          // Skip if code is blank
          if (StringUtils.isBlank(code)) {
            continue;
          }

          // Get name from second column (index 1)
          String name = ExcelUtils.loadStringCell(row.getCell(1));

          // Skip if name is blank
          if (StringUtils.isBlank(name)) {
            continue;
          }

          // Deduplicate within the file
          if (!seenCodes.add(code.toUpperCase())) {
            log.debug("Duplicate code '{}' in file, skipping", code);
            continue;
          }
          if (!seenNames.add(name.toUpperCase())) {
            log.debug("Duplicate name '{}' in file, skipping", name);
            continue;
          }

          // Check if manufacturer with this code already exists
          if (supplierAndManufacturerRepository.existsByCodeIgnoreCaseAndArchivedFalse(code)) {
            log.warn("Manufacturer with code '{}' already exists, skipping", code);
            continue;
          }

          if (StringUtils.isNotBlank(name)
              && supplierAndManufacturerRepository.existsByNameIgnoreCaseAndArchivedFalse(name)) {
            log.warn("Manufacturer with name '{}' already exists, skipping", name);
            continue;
          }

          // Create new manufacturer entity
          SupplierManufacturerEntity manufacturer = new SupplierManufacturerEntity();
          manufacturer.setType(SupplierAndManufacturerType.MANUFACTURER);
          manufacturer.setCode(code);
          manufacturer.setName(StringUtils.isNotBlank(name) ? name : "");
          manufacturersToCreate.add(manufacturer);
        }
      }

      // Save all manufacturers in batch
      if (!manufacturersToCreate.isEmpty()) {
        supplierAndManufacturerRepository.saveAll(manufacturersToCreate);
        log.info(
            "Successfully created {} manufacturers from Excel file", manufacturersToCreate.size());
      } else {
        log.info("No new manufacturers to create from Excel file");
      }
    }

    return manufacturersToCreate.size();
  }

  @Transactional
  public int uploadSuppliersFromExcel(MultipartFile file) throws IOException {
    List<SupplierManufacturerEntity> suppliersToCreate = new ArrayList<>();
    List<SupplierManufacturerEntity> suppliersToUpdate = new ArrayList<>();
    Set<String> seenNames = new HashSet<>();
    Set<String> seenCodes = new HashSet<>();

    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream)) {

      Sheet sheet = workbook.getSheetAt(0);
      log.info("Processing suppliers sheet: {}", sheet.getSheetName());

      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
          continue;
        }

        // Name is at column index 1
        String name = ExcelUtils.loadStringCell(row.getCell(1));

        if (StringUtils.isBlank(name)) {
          log.warn("Row {}: name is blank, skipping", rowIndex);
          continue;
        }

        // Name is at column index 1
        String code = ExcelUtils.loadStringCell(row.getCell(2));

        if (StringUtils.isBlank(code)) {
          log.warn("Row {}: code is blank, skipping", rowIndex);
          continue;
        }

        // Deduplicate within the file
        if (!seenNames.add(name.toUpperCase())) {
          log.debug("Row {}: duplicate name '{}' in file, skipping", rowIndex, name);
          continue;
        }

        // Deduplicate within the file
        if (!seenCodes.add(code.toUpperCase())) {
          log.debug("Row {}: duplicate code '{}' in file, skipping", rowIndex, code);
          continue;
        }

        // If already exists, mark as BOTH; otherwise create as new SUPPLIER
        Optional<SupplierManufacturerEntity> existing =
            supplierAndManufacturerRepository
                .findFirstByTypeInAndNameIgnoreCaseAndCodeIgnoreCaseAndArchivedFalse(
                    List.of(
                        SupplierAndManufacturerType.SUPPLIER,
                        SupplierAndManufacturerType.MANUFACTURER,
                        SupplierAndManufacturerType.BOTH),
                    name,
                    code);
        if (existing.isPresent()) {
          SupplierManufacturerEntity entity = existing.get();
          if (entity.getType() == SupplierAndManufacturerType.MANUFACTURER) {
            entity.setType(SupplierAndManufacturerType.BOTH);
            log.debug("Marked '{}' as BOTH", name);
          }
          suppliersToUpdate.add(entity);
          continue;
        }

        // If already exists, mark as BOTH; otherwise create as new SUPPLIER
        Optional<SupplierManufacturerEntity> existingByName =
            supplierAndManufacturerRepository.findFirstByTypeInAndNameIgnoreCaseAndArchivedFalse(
                List.of(
                    SupplierAndManufacturerType.SUPPLIER,
                    SupplierAndManufacturerType.MANUFACTURER,
                    SupplierAndManufacturerType.BOTH),
                name);
        if (existingByName.isPresent()) {
          SupplierManufacturerEntity entity = existingByName.get();
          if (entity.getType() == SupplierAndManufacturerType.MANUFACTURER) {
            entity.setType(SupplierAndManufacturerType.BOTH);
            entity.setCode(code);
          }
          suppliersToUpdate.add(entity);
          continue;
        }

        SupplierManufacturerEntity supplier = new SupplierManufacturerEntity();
        supplier.setType(SupplierAndManufacturerType.SUPPLIER);
        supplier.setCode(code);
        supplier.setName(name);
        suppliersToCreate.add(supplier);
        log.debug("Prepared supplier '{}'", name);
      }

      if (!suppliersToUpdate.isEmpty()) {
        supplierAndManufacturerRepository.saveAll(suppliersToUpdate);
        log.info("Updated {} entities to BOTH from Excel file", suppliersToUpdate.size());
      }
      if (!suppliersToCreate.isEmpty()) {
        supplierAndManufacturerRepository.saveAll(suppliersToCreate);
        log.info("Successfully created {} suppliers from Excel file", suppliersToCreate.size());
      }
      if (suppliersToUpdate.isEmpty() && suppliersToCreate.isEmpty()) {
        log.info("No new suppliers to create from Excel file");
      }
    }

    return suppliersToCreate.size();
  }
}
