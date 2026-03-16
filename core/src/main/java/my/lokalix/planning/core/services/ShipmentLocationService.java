package my.lokalix.planning.core.services;

import jakarta.persistence.EntityExistsException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.mappers.ShipmentLocationMapper;
import my.lokalix.planning.core.models.entities.admin.ShipmentLocationEntity;
import my.lokalix.planning.core.repositories.admin.ShipmentLocationRepository;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import my.lokalix.planning.core.services.validator.ShipmentLocationValidator;
import my.zkonsulting.planning.generated.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ShipmentLocationService {

  private final ShipmentLocationMapper shipmentLocationMapper;
  private final ShipmentLocationRepository shipmentLocationRepository;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final ShipmentLocationValidator shipmentLocationValidator;

  @Transactional
  public List<SWShipmentLocation> listShipmentLocations() {
    Sort sort = Sort.by(Sort.Direction.ASC, "name");
    List<ShipmentLocationEntity> all = shipmentLocationRepository.findAllByArchivedFalse(sort);
    return shipmentLocationMapper.toListSWShipmentLocation(all);
  }

  @Transactional
  public SWShipmentLocationsPaginated searchShipmentLocations(
      int offset, int limit, SWBasicSearch body) {
    Sort sort = Sort.by(Sort.Direction.ASC, "name");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);
    Page<ShipmentLocationEntity> paginatedResults;

    if (StringUtils.isBlank(body.getSearchText())) {
      paginatedResults = shipmentLocationRepository.findByArchivedFalse(pageable);
    } else {
      paginatedResults =
          shipmentLocationRepository.findBySearchAndArchivedFalse(pageable, body.getSearchText());
    }

    return populatePaginatedResults(paginatedResults);
  }

  @Transactional
  public SWShipmentLocation createShipmentLocation(SWShipmentLocationCreate body) {
    if (shipmentLocationRepository.existsByNameIgnoreCaseAndArchivedFalse(body.getName())) {
      throw new EntityExistsException(
          "A shipment location with the same name '" + body.getName() + "' already exists");
    }
    ShipmentLocationEntity entity = shipmentLocationMapper.toShipmentLocationEntity(body);

    if (CollectionUtils.isNotEmpty(body.getAcceptedCurrencyIds())) {
      for (UUID acceptedCurrencyId : body.getAcceptedCurrencyIds()) {
        entity.addAcceptedCurrency(
            entityRetrievalHelper.getMustExistCurrencyById(acceptedCurrencyId));
      }
    }

    return shipmentLocationMapper.toSWShipmentLocation(shipmentLocationRepository.save(entity));
  }

  @Transactional
  public SWShipmentLocation retrieveShipmentLocation(UUID uid) {
    ShipmentLocationEntity entity = entityRetrievalHelper.getMustExistShipmentLocationById(uid);
    return shipmentLocationMapper.toSWShipmentLocation(entity);
  }

  @Transactional
  public SWShipmentLocation updateShipmentLocation(UUID uid, SWShipmentLocationUpdate body) {
    ShipmentLocationEntity entity = entityRetrievalHelper.getMustExistShipmentLocationById(uid);
    if (shipmentLocationRepository.existsByNameIgnoreCaseAndShipmentLocationIdNotAndArchivedFalse(
        body.getName(), uid)) {
      throw new EntityExistsException(
          "A shipment location with the same name '" + body.getName() + "' already exists");
    }
    shipmentLocationMapper.updateEntityFromDto(body, entity);

    entity.clearAcceptedCurrencies();
    if (CollectionUtils.isNotEmpty(body.getAcceptedCurrencyIds())) {
      for (UUID acceptedCurrencyId : body.getAcceptedCurrencyIds()) {
        entity.addAcceptedCurrency(
            entityRetrievalHelper.getMustExistCurrencyById(acceptedCurrencyId));
      }
    }

    return shipmentLocationMapper.toSWShipmentLocation(shipmentLocationRepository.save(entity));
  }

  @Transactional
  public void archiveShipmentLocation(UUID uid) {
    ShipmentLocationEntity entity = entityRetrievalHelper.getMustExistShipmentLocationById(uid);
    shipmentLocationValidator.validateNotInUse(entity);
    entity.setArchived(true);
    shipmentLocationRepository.save(entity);
  }

  private SWShipmentLocationsPaginated populatePaginatedResults(
      Page<ShipmentLocationEntity> paginatedResults) {
    SWShipmentLocationsPaginated result = new SWShipmentLocationsPaginated();
    result.setResults(
        shipmentLocationMapper.toListSWShipmentLocation(paginatedResults.getContent()));
    result.setPage(paginatedResults.getNumber());
    result.setPerPage(paginatedResults.getSize());
    result.setTotal((int) paginatedResults.getTotalElements());
    result.setHasPrev(paginatedResults.hasPrevious());
    result.setHasNext(paginatedResults.hasNext());
    return result;
  }
}
