package my.lokalix.planning.core.services;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.mappers.NpiOrderMapper;
import my.lokalix.planning.core.models.entities.NpiOrderEntity;
import my.lokalix.planning.core.models.enums.NpiOrderStatus;
import my.lokalix.planning.core.repositories.NpiOrderRepository;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import my.lokalix.planning.core.utils.TimeUtils;
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
public class NpiOrderService {

  private final NpiOrderMapper npiOrderMapper;
  private final NpiOrderRepository npiOrderRepository;
  private final EntityRetrievalHelper entityRetrievalHelper;

  @Transactional
  public SWNpiOrder createNpiOrder(SWNpiOrderCreate body) {
    NpiOrderEntity entity = npiOrderMapper.toNpiOrderEntity(body);
    return npiOrderMapper.toSWNpiOrder(npiOrderRepository.save(entity));
  }

  @Transactional
  public SWNpiOrder retrieveNpiOrder(UUID uid) {
    NpiOrderEntity entity = entityRetrievalHelper.getMustExistNpiOrderById(uid);
    return npiOrderMapper.toSWNpiOrder(entity);
  }

  @Transactional
  public SWNpiOrder updateNpiOrder(UUID uid, SWNpiOrderUpdate body) {
    NpiOrderEntity entity = entityRetrievalHelper.getMustExistNpiOrderById(uid);
    if (entity.getStatus().isFinalStatus()) {
      throw new GenericWithMessageException(
          "Cannot update an NPI order with status " + entity.getStatus().getValue(),
          SWCustomErrorCode.GENERIC_ERROR);
    }
    npiOrderMapper.updateNpiOrderEntityFromDto(body, entity);
    return npiOrderMapper.toSWNpiOrder(npiOrderRepository.save(entity));
  }

  @Transactional
  public SWNpiOrder archiveNpiOrder(UUID uid) {
    NpiOrderEntity entity = entityRetrievalHelper.getMustExistNpiOrderById(uid);
    entity.setArchived(true);
    entity.setArchivedAt(TimeUtils.nowOffsetDateTimeUTC());
    return npiOrderMapper.toSWNpiOrder(npiOrderRepository.save(entity));
  }

  @Transactional
  public SWNpiOrder abortNpiOrder(UUID uid) {
    NpiOrderEntity entity = entityRetrievalHelper.getMustExistNpiOrderById(uid);
    if (!entity.getStatus().isAbortable()) {
      throw new GenericWithMessageException(
          "Cannot abort an NPI order with status " + entity.getStatus().getValue(),
          SWCustomErrorCode.GENERIC_ERROR);
    }
    entity.setStatus(NpiOrderStatus.ABORTED);
    entity.setStatusDate(TimeUtils.nowOffsetDateTimeUTC());
    return npiOrderMapper.toSWNpiOrder(npiOrderRepository.save(entity));
  }

  @Transactional
  public SWNpiOrdersPaginated searchNpiOrders(
      int offset, int limit, SWArchivedFilter archivedFilter, SWNpiOrderSearch body) {
    Sort sort = Sort.by(Sort.Direction.DESC, "creationDate");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);

    boolean hasStatusFilter = CollectionUtils.isNotEmpty(body.getStatuses());
    boolean hasSearchText = StringUtils.isNotBlank(body.getSearchText());

    List<NpiOrderStatus> statuses =
        hasStatusFilter
            ? body.getStatuses().stream()
                .map(s -> NpiOrderStatus.fromValue(s.getValue()))
                .toList()
            : null;

    Page<NpiOrderEntity> paginatedResults =
        switch (archivedFilter) {
          case NON_ARCHIVED_ONLY ->
              hasStatusFilter
                  ? hasSearchText
                      ? npiOrderRepository.findBySearchAndArchivedFalseAndStatusIn(
                          pageable, body.getSearchText(), statuses)
                      : npiOrderRepository.findAllByArchivedFalseAndStatusIn(pageable, statuses)
                  : hasSearchText
                      ? npiOrderRepository.findBySearchAndArchivedFalse(
                          pageable, body.getSearchText())
                      : npiOrderRepository.findAllByArchivedFalse(pageable);
          case ARCHIVED_ONLY ->
              hasSearchText
                  ? npiOrderRepository.findBySearchAndArchivedTrue(
                      pageable, body.getSearchText())
                  : npiOrderRepository.findAllByArchivedTrue(pageable);
          default ->
              hasSearchText
                  ? npiOrderRepository.findBySearch(pageable, body.getSearchText())
                  : npiOrderRepository.findAll(pageable);
        };

    return populateNpiOrdersPaginatedResults(paginatedResults);
  }

  private SWNpiOrdersPaginated populateNpiOrdersPaginatedResults(
      Page<NpiOrderEntity> paginatedResults) {
    SWNpiOrdersPaginated result = new SWNpiOrdersPaginated();
    result.setResults(npiOrderMapper.toListSWNpiOrder(paginatedResults.getContent()));
    result.setPage(paginatedResults.getNumber());
    result.setPerPage(paginatedResults.getSize());
    result.setTotal((int) paginatedResults.getTotalElements());
    result.setHasPrev(paginatedResults.hasPrevious());
    result.setHasNext(paginatedResults.hasNext());
    return result;
  }
}
