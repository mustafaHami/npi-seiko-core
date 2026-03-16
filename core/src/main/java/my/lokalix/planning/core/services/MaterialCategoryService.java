package my.lokalix.planning.core.services;

import jakarta.persistence.EntityExistsException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.mappers.MaterialCategoryMapper;
import my.lokalix.planning.core.models.entities.MaterialEntity;
import my.lokalix.planning.core.models.entities.MaterialLineDraftEntity;
import my.lokalix.planning.core.models.entities.admin.MaterialCategoryEntity;
import my.lokalix.planning.core.models.enums.MaterialStatus;
import my.lokalix.planning.core.repositories.MaterialLineDraftRepository;
import my.lokalix.planning.core.repositories.MaterialRepository;
import my.lokalix.planning.core.repositories.admin.MaterialCategoryRepository;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import my.lokalix.planning.core.services.helper.MaterialHelper;
import my.lokalix.planning.core.services.helper.SystemIdHelper;
import my.lokalix.planning.core.services.validator.MaterialCategoryValidator;
import my.lokalix.planning.core.utils.ExcelUtils;
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
public class MaterialCategoryService {

  private final MaterialCategoryMapper materialCategoryMapper;
  private final MaterialCategoryRepository materialCategoryRepository;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final MaterialCategoryValidator materialCategoryValidator;
  private final MaterialRepository materialRepository;
  private final SystemIdHelper systemIdHelper;
  private final MaterialLineDraftRepository materialLineDraftRepository;
  private final MaterialHelper materialHelper;

  @Transactional
  public SWMaterialCategory createMaterialCategory(SWMaterialCategoryCreate body) {
    if (materialCategoryRepository.existsByNameIgnoreCaseAndArchivedFalse(body.getName())) {
      throw new EntityExistsException(
          "A material category with the same name '" + body.getName() + "' already exists");
    }
    MaterialCategoryEntity entity = materialCategoryMapper.toAdminMaterialCategory(body);
    entity = materialCategoryRepository.save(entity);

    syncMaterialAndMaterialLineDraft(entity);

    return materialCategoryMapper.toSwMaterialCategory(entity);
  }

  @Transactional
  public SWMaterialCategory updateMaterialCategory(UUID uid, SWMaterialCategoryUpdate body) {
    MaterialCategoryEntity entity = entityRetrievalHelper.getMustExistMaterialCategoryById(uid);
    if (materialCategoryRepository.existsByNameIgnoreCaseAndMaterialCategoryIdNotAndArchivedFalse(
        body.getName(), uid)) {
      throw new EntityExistsException(
          "A material category with the same name '" + body.getName() + "' already exists");
    }
    materialCategoryMapper.updateAdminMaterialCategoryEntityFromDto(body, entity);
    entity = materialCategoryRepository.save(entity);

    syncMaterialAndMaterialLineDraft(entity);

    return materialCategoryMapper.toSwMaterialCategory(entity);
  }

  private void syncMaterialAndMaterialLineDraft(MaterialCategoryEntity entity) {
    List<MaterialEntity> unlinkedMaterials =
        materialRepository.findByCategoryNullAndDraftCategoryNameIgnoreCaseAndArchivedFalse(
            entity.getName());
    for (MaterialEntity material : unlinkedMaterials) {
      material.setCategory(entity);
      String systemId =
          systemIdHelper.generateSystemId(
              material.getManufacturer(), entity, material.getManufacturerPartNumber());
      material.setSystemId(systemId);

      // set its status to ESTIMATED if supplier has already been provided and all data is OK
      if (material.getManufacturer() != null
          && CollectionUtils.isNotEmpty(material.getSuppliers())) {
        material.setStatus(MaterialStatus.ESTIMATED);
        material = materialRepository.save(material);
        // Notify engineering if all materials of an affected cost request line are now estimated
        materialHelper.refreshMaterialLinesUsing(material);
      } else {
        materialRepository.save(material);
      }
    }

    List<MaterialLineDraftEntity> unlinkedDraftMaterialLines =
        materialLineDraftRepository.findByCategoryNullAndDraftCategoryNameIgnoreCase(
            entity.getName());
    for (MaterialLineDraftEntity materialLineDraft : unlinkedDraftMaterialLines) {
      materialLineDraft.setCategory(entity);
      materialLineDraftRepository.save(materialLineDraft);
    }
  }

  @Transactional
  public void archiveMaterialCategory(UUID uid) {
    MaterialCategoryEntity entity = entityRetrievalHelper.getMustExistMaterialCategoryById(uid);
    materialCategoryValidator.validateNotInUse(entity);
    entity.setArchived(true);
    materialCategoryRepository.save(entity);
  }

  @Transactional
  public SWMaterialCategory retrieveMaterialCategory(UUID uid) {
    MaterialCategoryEntity entity = entityRetrievalHelper.getMustExistMaterialCategoryById(uid);
    return materialCategoryMapper.toSwMaterialCategory(entity);
  }

  @Transactional
  public List<SWMaterialCategory> listMaterialCategories() {
    Sort sort = Sort.by(Sort.Direction.ASC, "name");
    List<MaterialCategoryEntity> allMaterialCategories =
        materialCategoryRepository.findAllByArchivedFalse(sort);
    return materialCategoryMapper.toListSwMaterialCategory(allMaterialCategories);
  }

  @Transactional
  public SWMaterialCategoriesPaginated searchMaterialCategories(
      int offset, int limit, SWBasicSearch search) {
    Sort sort = Sort.by(Sort.Direction.ASC, "name");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);
    Page<MaterialCategoryEntity> paginatedMaterialCategories;

    if (StringUtils.isBlank(search.getSearchText())) {
      paginatedMaterialCategories = materialCategoryRepository.findByArchivedFalse(pageable);
    } else {
      paginatedMaterialCategories =
          materialCategoryRepository.findBySearchAndArchivedFalse(pageable, search.getSearchText());
    }

    return populateMaterialCategoriesPaginatedResults(paginatedMaterialCategories);
  }

  @Transactional
  public UUID existMaterialCategoryByName(SWStringBody body) {
    return materialCategoryRepository
        .findByNameIgnoreCaseAndArchivedFalse(body.getValue())
        .map(MaterialCategoryEntity::getMaterialCategoryId)
        .orElse(null);
  }

  private SWMaterialCategoriesPaginated populateMaterialCategoriesPaginatedResults(
      Page<MaterialCategoryEntity> paginatedMaterialCategories) {
    SWMaterialCategoriesPaginated materialCategoriesPaginated = new SWMaterialCategoriesPaginated();
    materialCategoriesPaginated.setResults(
        materialCategoryMapper.toListSwMaterialCategory(paginatedMaterialCategories.getContent()));
    materialCategoriesPaginated.setPage(paginatedMaterialCategories.getNumber());
    materialCategoriesPaginated.setPerPage(paginatedMaterialCategories.getSize());
    materialCategoriesPaginated.setTotal((int) paginatedMaterialCategories.getTotalElements());
    materialCategoriesPaginated.setHasPrev(paginatedMaterialCategories.hasPrevious());
    materialCategoriesPaginated.setHasNext(paginatedMaterialCategories.hasNext());
    return materialCategoriesPaginated;
  }

  @Transactional
  public int uploadMaterialCategoriesFromExcel(MultipartFile file) throws IOException {
    int totalCreated = 0;

    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream)) {

      // First sheet: Material Categories
      Sheet categoriesSheet = workbook.getSheetAt(0);
      log.info("Processing categories sheet: {}", categoriesSheet.getSheetName());

      // Process each row starting from row 1 (index 1)
      for (int rowIndex = 1; rowIndex <= categoriesSheet.getLastRowNum(); rowIndex++) {
        Row row = categoriesSheet.getRow(rowIndex);
        if (row == null) {
          continue;
        }

        // Get name from first column (index 0)
        String categoryName = ExcelUtils.loadStringCell(row.getCell(0));

        // Skip if name is blank
        if (StringUtils.isBlank(categoryName)) {
          continue;
        }

        // Get abbreviation from second column (index 1)
        String abbreviation = ExcelUtils.loadStringCell(row.getCell(1));

        // Skip if abbreviation is blank
        if (StringUtils.isBlank(abbreviation)) {
          log.warn(
              "Row {}: abbreviation is blank for category '{}', skipping", rowIndex, categoryName);
          continue;
        }

        // Check if category already exists by name
        Optional<MaterialCategoryEntity> existingCategory =
            materialCategoryRepository.findByNameIgnoreCaseAndArchivedFalse(categoryName);
        if (existingCategory.isPresent()) {
          log.info("Material category '{}' already exists, skipping", categoryName);
          continue;
        }

        // Create new category
        MaterialCategoryEntity category = new MaterialCategoryEntity();
        category.setName(categoryName);
        category.setAbbreviation(abbreviation);
        materialCategoryRepository.save(category);
        totalCreated++;
        log.info(
            "Created material category '{}' with abbreviation '{}'", categoryName, abbreviation);
      }

      log.info("Successfully created {} material categories", totalCreated);
    }
    return totalCreated;
  }
}
