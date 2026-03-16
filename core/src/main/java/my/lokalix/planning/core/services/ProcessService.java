package my.lokalix.planning.core.services;

import jakarta.persistence.EntityExistsException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.mappers.ProcessMapper;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.lokalix.planning.core.models.entities.admin.ProcessEntity;
import my.lokalix.planning.core.models.entities.admin.ProcessUsageCountEntity;
import my.lokalix.planning.core.repositories.admin.ProcessRepository;
import my.lokalix.planning.core.repositories.admin.ProcessUsageCountRepository;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import my.lokalix.planning.core.services.validator.ProcessValidator;
import my.lokalix.planning.core.utils.ExcelUtils;
import my.zkonsulting.planning.generated.model.*;
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
public class ProcessService {

  private final ProcessMapper processMapper;
  private final ProcessRepository processRepository;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final ProcessUsageCountRepository processUsageCountRepository;
  private final AppConfigurationProperties appConfigurationProperties;
  private final ProcessValidator processValidator;

  @Transactional
  public SWProcess createProcess(SWProcessCreate body) {
    ProcessEntity entity = processMapper.toAdminProcess(body);
    if (processRepository.existsByNameIgnoreCaseAndArchivedFalse(body.getName())) {
      throw new EntityExistsException(
          "A process with the same name '" + body.getName() + "' already exists");
    }
    entity.setCurrency(entityRetrievalHelper.getMustExistCurrencyById(body.getCurrencyId()));
    entity = processRepository.save(entity);
    ProcessUsageCountEntity processUsageCountEntity = new ProcessUsageCountEntity();
    processUsageCountEntity.setProcess(entity);
    processUsageCountRepository.save(processUsageCountEntity);
    return processMapper.toSWProcess(entity);
  }

  @Transactional
  public SWProcess updateProcess(UUID uid, SWProcessUpdate body) {
    ProcessEntity entity = entityRetrievalHelper.getMustExistProcessById(uid);
    if (processRepository.existsByNameIgnoreCaseAndProcessIdNotAndArchivedFalse(
        body.getName(), uid)) {
      throw new EntityExistsException(
          "A process with the same name '" + body.getName() + "' already exists");
    }
    processMapper.updateAdminProcessEntityFromDto(body, entity);
    entity.setCurrency(entityRetrievalHelper.getMustExistCurrencyById(body.getCurrencyId()));
    return processMapper.toSWProcess(processRepository.save(entity));
  }

  @Transactional
  public void archiveProcess(UUID uid) {
    ProcessEntity entity = entityRetrievalHelper.getMustExistProcessById(uid);
    processValidator.validateNotInUse(entity);
    entity.setArchived(true);
    processRepository.save(entity);
  }

  @Transactional
  public SWProcess retrieveProcess(UUID uid) {
    ProcessEntity entity = entityRetrievalHelper.getMustExistProcessById(uid);
    return processMapper.toSWProcess(entity);
  }

  @Transactional
  public List<SWProcess> retrieveHighestUsageCountProcesses() {
    List<ProcessEntity> processes =
        processUsageCountRepository.findProcessesOrderedByUsageCountDesc();
    int limit = appConfigurationProperties.getNumberHighestUsageProcessesToReturn();
    return processMapper.toListSwProcess(processes.subList(0, Math.min(limit, processes.size())));
  }

  @Transactional
  public List<SWProcess> listProcesses() {
    Sort sort = Sort.by(Sort.Direction.ASC, "name");
    List<ProcessEntity> allProcesses = processRepository.findAllByArchivedFalse(sort);
    return processMapper.toListSwProcess(allProcesses);
  }

  @Transactional
  public SWProcessesPaginated searchProcesses(int offset, int limit, SWBasicSearch search) {
    Sort sort = Sort.by(Sort.Direction.ASC, "name");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);
    Page<ProcessEntity> paginatedProcesses;

    if (StringUtils.isBlank(search.getSearchText())) {
      paginatedProcesses = processRepository.findByArchivedFalse(pageable);
    } else {
      paginatedProcesses =
          processRepository.findBySearchAndArchivedFalse(pageable, search.getSearchText());
    }

    return populateProcessesPaginatedResults(paginatedProcesses);
  }

  private SWProcessesPaginated populateProcessesPaginatedResults(
      Page<ProcessEntity> paginatedProcesses) {
    SWProcessesPaginated processesPaginated = new SWProcessesPaginated();
    processesPaginated.setResults(processMapper.toListSwProcess(paginatedProcesses.getContent()));
    processesPaginated.setPage(paginatedProcesses.getNumber());
    processesPaginated.setPerPage(paginatedProcesses.getSize());
    processesPaginated.setTotal((int) paginatedProcesses.getTotalElements());
    processesPaginated.setHasPrev(paginatedProcesses.hasPrevious());
    processesPaginated.setHasNext(paginatedProcesses.hasNext());
    return processesPaginated;
  }

  @Transactional
  public int uploadProcessesFromExcel(MultipartFile file) throws IOException {
    List<ProcessEntity> processesCreated = new ArrayList<>();
    CurrencyEntity myrCurrency = entityRetrievalHelper.getMustExistCurrencyByCode("MYR");

    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream)) {

      Sheet sheet = workbook.getSheetAt(0);
      int rowIndex = 0;

      for (Row row : sheet) {
        if (rowIndex < 1) {
          rowIndex++;
          continue;
        }

        // Get name from first column (index 0)
        String name = ExcelUtils.loadStringCell(row.getCell(0));
        if (StringUtils.isBlank(name)) {
          continue;
        }

        if (processRepository.existsByNameIgnoreCaseAndArchivedFalse(name)) {
          log.warn("Process with name '{}' already exists, skipping", name);
          continue;
        }

        // Get cycleTimeInMinutes from second column (index 1)
        String cycleTimeStr = ExcelUtils.loadStringCell(row.getCell(1));
        BigDecimal cycleTimeInMinutes;
        if (StringUtils.isBlank(cycleTimeStr)) {
          log.warn(
              "Row {}: cycleTimeInMinutes is blank for process '{}', skipping", rowIndex, name);
          rowIndex++;
          continue;
        }
        try {
          cycleTimeInMinutes = new BigDecimal(cycleTimeStr);
        } catch (NumberFormatException e) {
          log.warn(
              "Row {}: invalid cycleTimeInMinutes '{}' for process '{}', skipping",
              rowIndex,
              cycleTimeStr,
              name);
          rowIndex++;
          continue;
        }

        // Get hourlyCost from third column (index 2)
        String hourlyCostStr = ExcelUtils.loadStringCell(row.getCell(2));
        BigDecimal hourlyCost;
        if (StringUtils.isBlank(hourlyCostStr)) {
          log.warn("Row {}: hourlyCost is blank for process '{}', skipping", rowIndex, name);
          rowIndex++;
          continue;
        }
        try {
          hourlyCost = new BigDecimal(hourlyCostStr);
        } catch (NumberFormatException e) {
          log.warn(
              "Row {}: invalid hourlyCost '{}' for process '{}', skipping",
              rowIndex,
              hourlyCostStr,
              name);
          rowIndex++;
          continue;
        }

        ProcessEntity process = new ProcessEntity();
        process.setName(name);
        process.setCurrency(myrCurrency);
        process.setDysonCycleTimeInSeconds(cycleTimeInMinutes);
        process.setNonDysonCycleTimeInSeconds(cycleTimeInMinutes);
        process.setCostPerMinute(hourlyCost);
        processesCreated.add(processRepository.save(process));
        rowIndex++;
      }

      String machineSetupName = "MACHINE SETUP";
      if (!processRepository.existsByNameIgnoreCaseAndArchivedFalse(machineSetupName)) {
        ProcessEntity process = new ProcessEntity();
        process.setName(machineSetupName);
        process.setCurrency(myrCurrency);
        process.setCostPerMinute(BigDecimal.valueOf(0.286));
        process.setDysonCycleTimeInSeconds(BigDecimal.valueOf(1800));
        process.setNonDysonCycleTimeInSeconds(BigDecimal.valueOf(1800));
        process.setSetupProcess(true);
        processesCreated.add(processRepository.save(process));
      }

      if (!processesCreated.isEmpty()) {
        for (ProcessEntity processEntity : processesCreated) {
          ProcessUsageCountEntity processUsageCountEntity = new ProcessUsageCountEntity();
          processUsageCountEntity.setProcess(processEntity);
          processUsageCountRepository.save(processUsageCountEntity);
        }
        log.info("Created {} processes from Excel upload", processesCreated.size());
      }
    }

    return processesCreated.size();
  }
}
