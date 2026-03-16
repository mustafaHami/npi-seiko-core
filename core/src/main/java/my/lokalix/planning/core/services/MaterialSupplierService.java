package my.lokalix.planning.core.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.exceptions.ShouldNeverHappenException;
import my.lokalix.planning.core.mappers.MaterialSupplierMapper;
import my.lokalix.planning.core.mappers.MaterialSupplierMoqLineMapper;
import my.lokalix.planning.core.models.entities.MaterialEntity;
import my.lokalix.planning.core.models.entities.MaterialLineEntity;
import my.lokalix.planning.core.models.entities.MaterialLinePerCostRequestQuantityEntity;
import my.lokalix.planning.core.models.entities.MaterialSupplierEntity;
import my.lokalix.planning.core.models.entities.MaterialSupplierMoqLineEntity;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.lokalix.planning.core.models.entities.admin.SupplierManufacturerEntity;
import my.lokalix.planning.core.models.enums.MaterialStatus;
import my.lokalix.planning.core.repositories.MaterialLineRepository;
import my.lokalix.planning.core.repositories.MaterialRepository;
import my.lokalix.planning.core.repositories.MaterialSupplierRepository;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import my.lokalix.planning.core.services.helper.MaterialHelper;
import my.lokalix.planning.core.services.helper.UserHelper;
import my.lokalix.planning.core.services.validator.MaterialSupplierValidator;
import my.lokalix.planning.core.utils.NumberUtils;
import my.zkonsulting.planning.generated.model.SWBasicSearch;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import my.zkonsulting.planning.generated.model.SWMaterialSupplier;
import my.zkonsulting.planning.generated.model.SWMaterialSupplierCreate;
import my.zkonsulting.planning.generated.model.SWMaterialSupplierMoqLineCreate;
import my.zkonsulting.planning.generated.model.SWMaterialSupplierUpdate;
import my.zkonsulting.planning.generated.model.SWMaterialSuppliersPaginated;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MaterialSupplierService {

  private final EntityRetrievalHelper entityRetrievalHelper;
  private final MaterialSupplierRepository materialSupplierRepository;
  private final MaterialLineRepository materialLineRepository;
  private final MaterialSupplierMapper materialSupplierMapper;
  private final MaterialSupplierMoqLineMapper materialSupplierMoqLineMapper;
  private final MaterialSupplierValidator materialSupplierValidator;
  private final MaterialRepository materialRepository;
  private final EmailService emailService;
  private final UserHelper userHelper;
  private final MaterialHelper materialHelper;
  private final AppConfigurationProperties appConfigurationProperties;

  @Transactional
  public SWMaterialSupplier createMaterialSupplier(
      UUID materialUid, SWMaterialSupplierCreate body) {
    MaterialEntity material = entityRetrievalHelper.getMustExistMaterialById(materialUid);

    // Verify purchasing currency exists
    CurrencyEntity purchasingCurrency =
        entityRetrievalHelper.getMustExistCurrencyById(body.getPurchasingCurrencyId());
    SupplierManufacturerEntity supplier =
        entityRetrievalHelper.getMustExistSupplierManufacturerById(body.getSupplierId());
    // Verify at least one MOQ line is provided
    if (body.getMoqLines() == null || body.getMoqLines().isEmpty()) {
      throw new GenericWithMessageException(
          "At least one MOQ line must be provided for each supplier",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    MaterialSupplierEntity materialSupplierEntity =
        materialSupplierMapper.toMaterialSupplierEntity(body);

    materialSupplierEntity.setSupplier(supplier);
    materialSupplierEntity.setPurchasingCurrency(purchasingCurrency);
    materialSupplierEntity.setMaterial(material);
    boolean isFirstSupplier = CollectionUtils.isEmpty(material.getSuppliers());
    materialSupplierEntity.setDefaultSupplier(
        isFirstSupplier || BooleanUtils.isTrue(body.getDefaultSupplier()));
    // Create MOQ lines
    List<MaterialSupplierMoqLineEntity> moqLines = new ArrayList<>();
    for (SWMaterialSupplierMoqLineCreate moqLineCreate : body.getMoqLines()) {
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
      moqLineEntity.setMaterialSupplier(materialSupplierEntity);
      moqLines.add(moqLineEntity);
    }
    materialSupplierEntity.setMoqLines(moqLines);

    if (!isFirstSupplier && BooleanUtils.isTrue(body.getDefaultSupplier())) {
      unsetCurrentDefault(material);
    }
    materialSupplierEntity = materialSupplierRepository.save(materialSupplierEntity);
    material.addMaterialSupplier(materialSupplierEntity);

    // If material is OK, set its status to ESTIMATED since supplier is now provided
    if (StringUtils.isNotBlank(material.getSystemId())) {
      material.setStatus(MaterialStatus.ESTIMATED);
      material = materialRepository.save(material);
      materialHelper.refreshMaterialLinesUsing(material);
    } else {
      materialRepository.save(material);
    }

    return materialSupplierMapper.toSwMaterialSupplier(materialSupplierEntity);
  }

  @Transactional(readOnly = true)
  public SWMaterialSuppliersPaginated searchMaterialSuppliers(
      UUID materialUid, int offset, int limit, SWBasicSearch body) {
    MaterialEntity material = entityRetrievalHelper.getMustExistMaterialById(materialUid);
    Pageable pageable =
        PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.ASC, "indexId"));
    Page<MaterialSupplierEntity> page;
    if (StringUtils.isBlank(body.getSearchText())) {
      page = materialSupplierRepository.findByMaterial(material, pageable);
    } else {
      page =
          materialSupplierRepository.findByMaterialAndSearch(
              material, body.getSearchText(), pageable);
    }
    return populatePaginatedResults(page);
  }

  @Transactional(readOnly = true)
  public SWMaterialSupplier retrieveMaterialSupplier(UUID materialUid, UUID materialSupplierUid) {
    MaterialSupplierEntity entity =
        entityRetrievalHelper.getMustExistMaterialSupplierById(materialSupplierUid);
    verifyBelongsToMaterial(entity, materialUid);
    return materialSupplierMapper.toSwMaterialSupplier(entity);
  }

  @Transactional
  public SWMaterialSupplier updateMaterialSupplier(
      UUID materialUid, UUID materialSupplierUid, SWMaterialSupplierUpdate body) {
    MaterialEntity material = entityRetrievalHelper.getMustExistMaterialById(materialUid);
    MaterialSupplierEntity entity =
        entityRetrievalHelper.getMustExistMaterialSupplierById(materialSupplierUid);
    verifyBelongsToMaterial(entity, materialUid);
    materialSupplierValidator.validateMoqLines(body.getMoqLines());
    boolean isOnlySupplier = material.getSuppliers().size() == 1;
    boolean shouldBeDefault = isOnlySupplier || BooleanUtils.isTrue(body.getDefaultSupplier());
    if (shouldBeDefault && !entity.isDefaultSupplier()) {
      unsetCurrentDefault(material);
    }
    CurrencyEntity currency =
        entityRetrievalHelper.getMustExistCurrencyById(body.getPurchasingCurrencyId());
    SupplierManufacturerEntity supplier =
        entityRetrievalHelper.getMustExistSupplierManufacturerById(body.getSupplierId());
    entity.setSupplier(supplier);
    entity.setPurchasingCurrency(currency);
    entity.setDefaultSupplier(shouldBeDefault);
    entity.getMoqLines().clear();
    for (SWMaterialSupplierMoqLineCreate moqLineCreate : body.getMoqLines()) {
      MaterialSupplierMoqLineEntity moqLine =
          materialSupplierMoqLineMapper.toMaterialSupplierMoqLineEntity(moqLineCreate);
      entity.addMoqLine(moqLine);
    }
    MaterialSupplierEntity saved = materialSupplierRepository.save(entity);
    recalculateDependentMaterialLines(saved);
    List<MaterialLineEntity> materialLines = materialLineRepository.findByMaterial(material);
    return materialSupplierMapper.toSwMaterialSupplier(saved);
  }

  @Transactional
  public void archiveMaterialSupplier(UUID materialUid, UUID materialSupplierUid) {
    MaterialEntity material = entityRetrievalHelper.getMustExistMaterialById(materialUid);
    MaterialSupplierEntity entity =
        entityRetrievalHelper.getMustExistMaterialSupplierById(materialSupplierUid);
    verifyBelongsToMaterial(entity, materialUid);

    // 1) Ensure at least one other active supplier exists
    List<MaterialSupplierEntity> otherActiveSuppliers =
        material.getSuppliers().stream().filter(s -> !s.equals(entity)).toList();
    if (CollectionUtils.isEmpty(otherActiveSuppliers)) {
      throw new GenericWithMessageException(
          "Cannot archive the only active supplier for this material",
          SWCustomErrorCode.GENERIC_ERROR);
    }

    // 2) If this was the default, assign default to first other active supplier
    if (entity.isDefaultSupplier()) {
      MaterialSupplierEntity newDefault = otherActiveSuppliers.getFirst();
      newDefault.setDefaultSupplier(true);
      materialSupplierRepository.save(newDefault);
    }

    // 3) Reassign all dependent lines to the current default supplier
    MaterialSupplierEntity defaultSupplier =
        otherActiveSuppliers.stream()
            .filter(MaterialSupplierEntity::isDefaultSupplier)
            .findFirst()
            .orElseThrow(ShouldNeverHappenException::new);
    List<MaterialLineEntity> affectedLines =
        materialLineRepository.findByChosenMaterialSupplier(entity);
    for (MaterialLineEntity materialLine : affectedLines) {
      materialLine.setChosenMaterialSupplier(defaultSupplier);
      materialLine.buildCalculatedFields(
          materialLine.getCostRequestLine().getCostingMethodType(),
          appConfigurationProperties.getTargetCurrencyCode());
      for (MaterialLinePerCostRequestQuantityEntity perQty :
          materialLine.getMaterialLineForCostRequestQuantities()) {
        perQty.setChosenMaterialSupplier(defaultSupplier);
        perQty.buildCalculatedFields(
            materialLine.getCostRequestLine().getCostingMethodType(),
            appConfigurationProperties.getTargetCurrencyCode());
      }
      materialLineRepository.save(materialLine);
    }

    // 4) Remove from material's active supplier list
    material.removeMaterialSupplier(entity);
    materialRepository.save(material);
  }

  private void recalculateDependentMaterialLines(MaterialSupplierEntity supplier) {
    List<MaterialLineEntity> affectedLines =
        materialLineRepository.findByChosenMaterialSupplier(supplier);
    for (MaterialLineEntity materialLine : affectedLines) {
      materialLine.buildCalculatedFields(
          materialLine.getCostRequestLine().getCostingMethodType(),
          appConfigurationProperties.getTargetCurrencyCode());
      for (MaterialLinePerCostRequestQuantityEntity perQty :
          materialLine.getMaterialLineForCostRequestQuantities()) {
        perQty.buildCalculatedFields(
            materialLine.getCostRequestLine().getCostingMethodType(),
            appConfigurationProperties.getTargetCurrencyCode());
      }
      materialLineRepository.save(materialLine);
    }
  }

  private void unsetCurrentDefault(MaterialEntity material) {
    material.getSuppliers().stream()
        .filter(MaterialSupplierEntity::isDefaultSupplier)
        .forEach(
            s -> {
              s.setDefaultSupplier(false);
              materialSupplierRepository.save(s);
            });
  }

  private void verifyBelongsToMaterial(MaterialSupplierEntity entity, UUID materialUid) {
    if (!entity.getMaterial().getMaterialId().equals(materialUid)) {
      throw new GenericWithMessageException(
          "Material supplier does not belong to this material", SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  private SWMaterialSuppliersPaginated populatePaginatedResults(Page<MaterialSupplierEntity> page) {
    SWMaterialSuppliersPaginated result = new SWMaterialSuppliersPaginated();
    result.setResults(materialSupplierMapper.toListSwMaterialSupplier(page.getContent()));
    result.setPage(page.getNumber());
    result.setPerPage(page.getSize());
    result.setTotal((int) page.getTotalElements());
    result.setHasPrev(page.hasPrevious());
    result.setHasNext(page.hasNext());
    return result;
  }

  @Transactional
  public List<SWMaterialSupplier> listMaterialSuppliers(UUID uid) {
    MaterialEntity material = entityRetrievalHelper.getMustExistMaterialById(uid);
    return materialSupplierMapper.toListSwMaterialSupplier(material.getSuppliers());
  }
}
