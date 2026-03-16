package my.lokalix.planning.core.services;

import jakarta.persistence.EntityExistsException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.mappers.BomConfigurationMapper;
import my.lokalix.planning.core.models.entities.admin.BomConfigurationEntity;
import my.lokalix.planning.core.repositories.admin.BomConfigurationRepository;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import my.zkonsulting.planning.generated.model.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class BomConfigurationService {

  private final BomConfigurationMapper bomConfigurationMapper;
  private final BomConfigurationRepository bomConfigurationRepository;
  private final EntityRetrievalHelper entityRetrievalHelper;

  @Transactional
  public List<SWBomConfiguration> listBomConfigurations() {
    return bomConfigurationMapper.toListSwBomConfiguration(
        bomConfigurationRepository.findAllByArchivedFalse());
  }

  @Transactional
  public SWBomConfigurationsPaginated searchBomConfigurations(
      int offset, int limit, SWBasicSearch search) {
    Sort sort = Sort.by(Sort.Direction.ASC, "name");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);
    Page<BomConfigurationEntity> paginatedResults;

    if (StringUtils.isBlank(search.getSearchText())) {
      paginatedResults = bomConfigurationRepository.findByArchivedFalse(pageable);
    } else {
      paginatedResults =
          bomConfigurationRepository.findBySearchAndArchivedFalse(pageable, search.getSearchText());
    }

    return populateBomConfigurationsPaginatedResults(paginatedResults);
  }

  @Transactional
  public SWBomConfiguration createBomConfiguration(SWBomConfigurationCreate body) {
    if (bomConfigurationRepository.existsByNameIgnoreCaseAndArchivedFalse(body.getName())) {
      throw new EntityExistsException(
          "A BOM configuration with the name '" + body.getName() + "' already exists");
    }
    BomConfigurationEntity entity = bomConfigurationMapper.toBomConfigurationEntity(body);
    return bomConfigurationMapper.toSwBomConfiguration(bomConfigurationRepository.save(entity));
  }

  @Transactional
  public SWBomConfiguration retrieveBomConfiguration(UUID uid) {
    return bomConfigurationMapper.toSwBomConfiguration(
        entityRetrievalHelper.getMustExistBomConfigurationById(uid));
  }

  @Transactional
  public SWBomConfiguration updateBomConfiguration(UUID uid, SWBomConfigurationUpdate body) {
    BomConfigurationEntity entity = entityRetrievalHelper.getMustExistBomConfigurationById(uid);
    if (bomConfigurationRepository.existsByNameIgnoreCaseAndBomConfigurationIdNotAndArchivedFalse(
        body.getName(), uid)) {
      throw new EntityExistsException(
          "A BOM configuration with the name '" + body.getName() + "' already exists");
    }
    bomConfigurationMapper.updateBomConfigurationEntityFromDto(body, entity);
    return bomConfigurationMapper.toSwBomConfiguration(bomConfigurationRepository.save(entity));
  }

  @Transactional
  public void archiveBomConfiguration(UUID uid) {
    BomConfigurationEntity entity = entityRetrievalHelper.getMustExistBomConfigurationById(uid);
    entity.setArchived(true);
    bomConfigurationRepository.save(entity);
  }

  private SWBomConfigurationsPaginated populateBomConfigurationsPaginatedResults(
      Page<BomConfigurationEntity> paginatedResults) {
    SWBomConfigurationsPaginated result = new SWBomConfigurationsPaginated();
    result.setResults(
        bomConfigurationMapper.toListSwBomConfiguration(paginatedResults.getContent()));
    result.setPage(paginatedResults.getNumber());
    result.setPerPage(paginatedResults.getSize());
    result.setTotal((int) paginatedResults.getTotalElements());
    result.setHasPrev(paginatedResults.hasPrevious());
    result.setHasNext(paginatedResults.hasNext());
    return result;
  }
}
