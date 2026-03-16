package my.lokalix.planning.core.services;

import jakarta.persistence.EntityExistsException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.mappers.UnitMapper;
import my.lokalix.planning.core.models.entities.MaterialEntity;
import my.lokalix.planning.core.models.entities.MaterialLineDraftEntity;
import my.lokalix.planning.core.models.entities.admin.UnitEntity;
import my.lokalix.planning.core.repositories.MaterialLineDraftRepository;
import my.lokalix.planning.core.repositories.MaterialRepository;
import my.lokalix.planning.core.repositories.admin.UnitRepository;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import my.lokalix.planning.core.services.validator.UnitValidator;
import my.zkonsulting.planning.generated.model.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UnitService {

  private final UnitMapper unitMapper;
  private final UnitRepository unitRepository;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final UnitValidator unitValidator;
  private final MaterialRepository materialRepository;
  private final MaterialLineDraftRepository materialLineDraftRepository;

  @Transactional
  public void createUnit(SWUnitCreate body) {
    if (unitRepository.existsByNameIgnoreCaseAndArchivedFalse(body.getName())) {
      throw new EntityExistsException(
          "A unit with the same name '" + body.getName() + "' already exists");
    }
    UnitEntity entity = unitRepository.save(unitMapper.toUnitEntity(body));

    syncMaterialAndMaterialLineDraft(entity);
  }

  @Transactional
  public SWUnit updateUnit(UUID uid, SWUnitUpdate body) {
    UnitEntity entity = entityRetrievalHelper.getMustExistUnitById(uid);
    if (unitRepository.existsByNameIgnoreCaseAndUnitIdNotAndArchivedFalse(body.getName(), uid)) {
      throw new EntityExistsException(
          "A unit with the same name '" + body.getName() + "' already exists");
    }
    unitMapper.updateUnitEntityFromDto(body, entity);
    entity = unitRepository.save(entity);

    syncMaterialAndMaterialLineDraft(entity);

    return unitMapper.toSWUnit(entity);
  }

  private void syncMaterialAndMaterialLineDraft(UnitEntity entity) {
    List<MaterialEntity> unlinkedMaterials =
        materialRepository.findByUnitNullAndDraftUnitNameIgnoreCaseAndArchivedFalse(
            entity.getName());
    for (MaterialEntity material : unlinkedMaterials) {
      material.setUnit(entity);
      materialRepository.save(material);
    }

    List<MaterialLineDraftEntity> unlinkedDraftMaterialLines =
        materialLineDraftRepository.findByUnitNullAndDraftUnitNameIgnoreCase(entity.getName());
    for (MaterialLineDraftEntity materialLineDraft : unlinkedDraftMaterialLines) {
      materialLineDraft.setUnit(entity);
      materialLineDraftRepository.save(materialLineDraft);
    }
  }

  @Transactional
  public void archiveUnit(UUID uid) {
    UnitEntity entity = entityRetrievalHelper.getMustExistUnitById(uid);
    unitValidator.validateNotInUse(entity);
    entity.setArchived(true);
    unitRepository.save(entity);
  }

  @Transactional
  public SWUnit retrieveUnit(UUID uid) {
    UnitEntity entity = entityRetrievalHelper.getMustExistUnitById(uid);
    return unitMapper.toSWUnit(entity);
  }

  @Transactional
  public List<SWUnit> listUnits() {
    Sort sort = Sort.by(Sort.Direction.ASC, "name");
    List<UnitEntity> allUnits = unitRepository.findAllByArchivedFalse(sort);
    return unitMapper.toListSwUnit(allUnits);
  }

  @Transactional
  public SWUnitsPaginated searchUnits(int offset, int limit, SWBasicSearch search) {
    Sort sort = Sort.by(Sort.Direction.ASC, "name");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);
    Page<UnitEntity> paginatedUnits;

    if (StringUtils.isBlank(search.getSearchText())) {
      paginatedUnits = unitRepository.findByArchivedFalse(pageable);
    } else {
      paginatedUnits =
          unitRepository.findBySearchAndArchivedFalse(pageable, search.getSearchText());
    }

    return populateUnitsPaginatedResults(paginatedUnits);
  }

  @Transactional
  public UUID existUnitByName(SWStringBody body) {
    return unitRepository
        .findByNameIgnoreCaseAndArchivedFalse(body.getValue())
        .map(UnitEntity::getUnitId)
        .orElse(null);
  }

  private SWUnitsPaginated populateUnitsPaginatedResults(Page<UnitEntity> paginatedUnits) {
    SWUnitsPaginated unitsPaginated = new SWUnitsPaginated();
    unitsPaginated.setResults(unitMapper.toListSwUnit(paginatedUnits.getContent()));
    unitsPaginated.setPage(paginatedUnits.getNumber());
    unitsPaginated.setPerPage(paginatedUnits.getSize());
    unitsPaginated.setTotal((int) paginatedUnits.getTotalElements());
    unitsPaginated.setHasPrev(paginatedUnits.hasPrevious());
    unitsPaginated.setHasNext(paginatedUnits.hasNext());
    return unitsPaginated;
  }
}
