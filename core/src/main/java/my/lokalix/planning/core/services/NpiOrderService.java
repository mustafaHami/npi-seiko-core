package my.lokalix.planning.core.services;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.mappers.NpiOrderMapper;
import my.lokalix.planning.core.mappers.ProcessLineMapper;
import my.lokalix.planning.core.mappers.ProcessLineStatusHistoryMapper;
import my.lokalix.planning.core.models.entities.NpiOrderEntity;
import my.lokalix.planning.core.models.entities.ProcessEntity;
import my.lokalix.planning.core.models.entities.ProcessLineEntity;
import my.lokalix.planning.core.models.entities.ProcessLineStatusHistoryEntity;
import my.lokalix.planning.core.models.enums.NpiOrderStatus;
import my.lokalix.planning.core.models.enums.ProcessLineStatus;
import my.lokalix.planning.core.repositories.NpiOrderRepository;
import my.lokalix.planning.core.repositories.ProcessLineRepository;
import my.lokalix.planning.core.repositories.ProcessLineStatusHistoryRepository;
import my.lokalix.planning.core.repositories.ProcessRepository;
import my.lokalix.planning.core.security.LoggedUserDetailsService;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import my.lokalix.planning.core.services.validator.NpiValidator;
import my.lokalix.planning.core.services.validator.ProcessLineValidator;
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
  private final ProcessLineMapper processLineMapper;
  private final ProcessLineStatusHistoryMapper processLineStatusHistoryMapper;
  private final NpiOrderRepository npiOrderRepository;
  private final ProcessRepository processRepository;
  private final ProcessLineRepository processLineRepository;
  private final ProcessLineStatusHistoryRepository processLineStatusHistoryRepository;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final ProcessLineValidator processLineValidator;
  private final AppConfigurationProperties appConfigurationProperties;
  private final NpiValidator npiValidator;
  private final LoggedUserDetailsService loggedUserDetailsService;

  @Transactional
  public SWNpiOrder createNpiOrder(SWNpiOrderCreate body) {
    List<ProcessEntity> processes = processRepository.findAllByOrderByCreationDateAsc();
    if (CollectionUtils.isEmpty(processes)) {
      throw new GenericWithMessageException(
          "Default processes have not been initialized", SWCustomErrorCode.GENERIC_ERROR);
    }

    NpiOrderEntity entity = npiOrderMapper.toNpiOrderEntity(body);
    calculateAndSetDeliveryDates(entity, body.getProductionPlanTime(), body.getTestingPlanTime());

    buildProcessLines(entity, processes, body.getProductionPlanTime(), body.getTestingPlanTime());
    NpiOrderEntity savedEntity = npiOrderRepository.save(entity);

    return npiOrderMapper.toSWNpiOrder(savedEntity);
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

    List<ProcessLineEntity> lines = entity.getProcessLines();
    lines.forEach(
        line -> {
          if (line.getIsProduction() && body.getProductionPlanTime() != null) {
            line.setPlanTime(body.getProductionPlanTime());
          } else if (line.getIsTesting() && body.getTestingPlanTime() != null) {
            line.setPlanTime(body.getTestingPlanTime());
          }
        });

    calculateAndSetDeliveryDates(entity, body.getProductionPlanTime(), body.getTestingPlanTime());

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
  public SWProcess retrieveNpiOrderProcess(UUID npiOrderUid) {
    NpiOrderEntity npiOrder = entityRetrievalHelper.getMustExistNpiOrderById(npiOrderUid);
    List<ProcessLineEntity> lines =
        processLineRepository.findAllByNpiOrderOrderByIndexIdAsc(npiOrder);

    SWProcess process = new SWProcess();
    process.setUid(npiOrder.getNpiOrderId());
    process.setPartNumber(npiOrder.getPartNumber());
    process.setLines(processLineMapper.toListSWProcessLine(lines));
    return process;
  }

  @Transactional
  public SWOutputProcessLineUpdate updateNpiOrderProcessLineStatus(
      UUID npiOrderUid, UUID lineUid, SWProcessLineStatusUpdateBody body) {
    NpiOrderEntity npiOrder = entityRetrievalHelper.getMustExistNpiOrderById(npiOrderUid);
    ProcessLineEntity line = entityRetrievalHelper.getMustExistProcessLineById(lineUid);
    npiValidator.validateNpiUpdatable(npiOrder);
    processLineValidator.validateStatusUpdate(line, body);

    ProcessLineStatus newStatus = ProcessLineStatus.fromValue(body.getStatus().getValue());

    if (newStatus == ProcessLineStatus.IN_PROGRESS) {
      if (line.getIsMaterialPurchase()) {
        line.setMaterialLatestDeliveryDate(body.getMaterialLatestDeliveryDate());
      }
      if (line.getIsProduction() || line.getIsTesting()) {
        line.setRemainingDuration(body.getRemainingTime());
        recalculateForecastDeliveryDate(npiOrder, line);
      }
    }

    if (newStatus == ProcessLineStatus.COMPLETED && line.getIsShipment()) {
      npiOrder.setShippingDate(TimeUtils.nowLocalDate(appConfigurationProperties.getAppTimezone()));
      npiOrderRepository.save(npiOrder);
    }

    line.addStatus(
        TimeUtils.nowOffsetDateTimeUTC(),
        null,
        newStatus,
        loggedUserDetailsService.getLoggedUserReference());
    ProcessLineEntity savedLine = processLineRepository.save(line);

    List<ProcessLineEntity> allLines =
        processLineRepository.findAllByNpiOrderOrderByIndexIdAsc(npiOrder);
    boolean processIsCompleted = allLines.stream().allMatch(l -> l.getStatus().isFinalStatus());

    SWOutputProcessLineUpdate output = new SWOutputProcessLineUpdate();
    output.setUpdatedProcessLine(processLineMapper.toSWProcessLine(savedLine));
    output.setProcessIsCompleted(processIsCompleted);
    return output;
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
            ? body.getStatuses().stream().map(s -> NpiOrderStatus.fromValue(s.getValue())).toList()
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
                  ? npiOrderRepository.findBySearchAndArchivedTrue(pageable, body.getSearchText())
                  : npiOrderRepository.findAllByArchivedTrue(pageable);
          default ->
              hasSearchText
                  ? npiOrderRepository.findBySearch(pageable, body.getSearchText())
                  : npiOrderRepository.findAll(pageable);
        };

    return populateNpiOrdersPaginatedResults(paginatedResults);
  }

  private void buildProcessLines(
      NpiOrderEntity npiOrder,
      List<ProcessEntity> processes,
      BigDecimal productionPlanTime,
      BigDecimal testingPlanTime) {
    for (ProcessEntity process : processes) {
      ProcessLineEntity line = new ProcessLineEntity();
      line.setProcessName(process.getName());
      line.setIsMaterialPurchase(process.getIsMaterialPurchase());
      line.setIsProduction(process.getIsProduction());
      line.setIsTesting(process.getIsTesting());
      line.setIsShipment(process.getIsShipment());
      if (process.getHasPlanTime()) {
        if (process.getIsProduction()) {
          line.setPlanTime(productionPlanTime);
        } else if (process.getIsTesting()) {
          line.setPlanTime(testingPlanTime);
        }
      }
      npiOrder.addProcessLine(line);
    }
  }

  private void recalculateForecastDeliveryDate(
      NpiOrderEntity npiOrder, ProcessLineEntity updatedLine) {
    List<ProcessLineEntity> allLines =
        processLineRepository.findAllByNpiOrderOrderByIndexIdAsc(npiOrder);

    LocalDate today = TimeUtils.nowLocalDate(appConfigurationProperties.getAppTimezone());

    double totalForecastDays = 0;
    for (ProcessLineEntity l : allLines) {
      boolean isCurrentLine = l.getProcessLineId().equals(updatedLine.getProcessLineId());
      ProcessLineStatus status = isCurrentLine ? updatedLine.getStatus() : l.getStatus();
      BigDecimal remainingDuration =
          isCurrentLine ? updatedLine.getRemainingDuration() : l.getRemainingDuration();
      BigDecimal planTime = l.getPlanTime();

      if (status == ProcessLineStatus.IN_PROGRESS && remainingDuration != null) {
        totalForecastDays += remainingDuration.doubleValue();
      } else if (status == ProcessLineStatus.NOT_STARTED && planTime != null) {
        totalForecastDays += planTime.doubleValue();
      }
    }

    npiOrder.setForecastDeliveryDate(today.plusDays(Math.round(totalForecastDays)));
    npiOrderRepository.save(npiOrder);
  }

  private void calculateAndSetDeliveryDates(
      NpiOrderEntity entity, BigDecimal productionPlanTime, BigDecimal testingPlanTime) {
    if (entity.getOrderDate() == null || productionPlanTime == null || testingPlanTime == null) {
      return;
    }
    long totalDays = Math.round(productionPlanTime.add(testingPlanTime).doubleValue());
    LocalDate plannedDate = entity.getOrderDate().plusDays(totalDays);
    entity.setPlannedDeliveryDate(plannedDate);
    entity.setForecastDeliveryDate(plannedDate);
  }

  @Transactional
  public List<SWProcessLineStatusHistory> retrieveNpiOrderProcessLineStatusesHistory(
      UUID npiOrderUid, UUID lineUid) {
    entityRetrievalHelper.getMustExistNpiOrderById(npiOrderUid);
    ProcessLineEntity line = entityRetrievalHelper.getMustExistProcessLineById(lineUid);
    List<ProcessLineStatusHistoryEntity> history =
        processLineStatusHistoryRepository.findAllByProcessLineOrderByStartDateAsc(line);
    return processLineStatusHistoryMapper.toListSWProcessLineStatusHistory(history);
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
