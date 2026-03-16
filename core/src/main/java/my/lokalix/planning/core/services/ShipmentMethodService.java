package my.lokalix.planning.core.services;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.mappers.ShipmentMethodMapper;
import my.lokalix.planning.core.models.entities.admin.ShipmentMethodEntity;
import my.lokalix.planning.core.repositories.admin.ShipmentMethodRepository;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import my.lokalix.planning.core.services.validator.ShipmentMethodValidator;
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
public class ShipmentMethodService {

  private final ShipmentMethodMapper shipmentMethodMapper;
  private final ShipmentMethodRepository shipmentMethodRepository;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final ShipmentMethodValidator shipmentMethodValidator;

  @Transactional
  public SWShipmentMethod createShipmentMethod(SWShipmentMethodCreate body) {
    ShipmentMethodEntity entity = shipmentMethodMapper.toShipmentMethodEntity(body);
    return shipmentMethodMapper.toSwShipmentMethod(shipmentMethodRepository.save(entity));
  }

  @Transactional
  public SWShipmentMethod updateShipmentMethod(UUID uid, SWShipmentMethodUpdate body) {
    ShipmentMethodEntity entity = entityRetrievalHelper.getMustExistShipmentMethodById(uid);
    shipmentMethodMapper.updateShipmentMethodEntityFromDto(body, entity);
    return shipmentMethodMapper.toSwShipmentMethod(shipmentMethodRepository.save(entity));
  }

  @Transactional
  public void archiveShipmentMethod(UUID uid) {
    ShipmentMethodEntity entity = entityRetrievalHelper.getMustExistShipmentMethodById(uid);
    shipmentMethodValidator.validateNotInUse(entity);
    entity.setArchived(true);
    shipmentMethodRepository.save(entity);
  }

  @Transactional
  public SWShipmentMethod retrieveShipmentMethod(UUID uid) {
    ShipmentMethodEntity entity = entityRetrievalHelper.getMustExistShipmentMethodById(uid);
    return shipmentMethodMapper.toSwShipmentMethod(entity);
  }

  @Transactional
  public List<SWShipmentMethod> listShipmentMethods() {
    Sort sort = Sort.by(Sort.Direction.ASC, "name");
    List<ShipmentMethodEntity> allShipmentMethods =
        shipmentMethodRepository.findAllByArchivedFalse(sort);
    return shipmentMethodMapper.toListSwShipmentMethod(allShipmentMethods);
  }

  @Transactional
  public SWShipmentMethodsPaginated searchShipmentMethods(
      int offset, int limit, SWBasicSearch search) {
    Sort sort = Sort.by(Sort.Direction.ASC, "name");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);
    Page<ShipmentMethodEntity> paginatedShipmentMethods;

    if (StringUtils.isBlank(search.getSearchText())) {
      paginatedShipmentMethods = shipmentMethodRepository.findByArchivedFalse(pageable);
    } else {
      paginatedShipmentMethods =
          shipmentMethodRepository.findBySearchAndArchivedFalse(pageable, search.getSearchText());
    }

    return populateShipmentMethodsPaginatedResults(paginatedShipmentMethods);
  }

  private SWShipmentMethodsPaginated populateShipmentMethodsPaginatedResults(
      Page<ShipmentMethodEntity> paginatedShipmentMethods) {
    SWShipmentMethodsPaginated shipmentMethodsPaginated = new SWShipmentMethodsPaginated();
    shipmentMethodsPaginated.setResults(
        shipmentMethodMapper.toListSwShipmentMethod(paginatedShipmentMethods.getContent()));
    shipmentMethodsPaginated.setPage(paginatedShipmentMethods.getNumber());
    shipmentMethodsPaginated.setPerPage(paginatedShipmentMethods.getSize());
    shipmentMethodsPaginated.setTotal((int) paginatedShipmentMethods.getTotalElements());
    shipmentMethodsPaginated.setHasPrev(paginatedShipmentMethods.hasPrevious());
    shipmentMethodsPaginated.setHasNext(paginatedShipmentMethods.hasNext());
    return shipmentMethodsPaginated;
  }

  @Transactional
  public int uploadShipmentMethodsFromExcel(MultipartFile file) throws IOException {
    List<ShipmentMethodEntity> shipmentMethodsToCreate = new ArrayList<>();

    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream)) {

      Sheet sheet = workbook.getSheetAt(0);
      log.info("Processing shipment methods sheet: {}", sheet.getSheetName());

      // Process each row starting from row 1 (index 1)
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
          continue;
        }

        // Get name from first column (index 0)
        String name = ExcelUtils.loadStringCell(row.getCell(0));

        // Skip if name is blank
        if (StringUtils.isBlank(name)) {
          continue;
        }

        // Get percentage from second column (index 1)
        String percentageStr = ExcelUtils.loadStringCell(row.getCell(1));

        // Skip if percentage is blank
        if (StringUtils.isBlank(percentageStr)) {
          log.warn(
              "Row {}: percentage is blank for shipment method '{}', skipping", rowIndex, name);
          continue;
        }

        // Parse percentage as BigDecimal
        BigDecimal percentage;
        try {
          percentage = new BigDecimal(percentageStr);
        } catch (NumberFormatException e) {
          log.warn(
              "Row {}: invalid percentage '{}' for shipment method '{}', skipping",
              rowIndex,
              percentageStr,
              name);
          continue;
        }

        // Check if shipment method already exists by name
        Optional<ShipmentMethodEntity> existingShipmentMethod =
            shipmentMethodRepository.findByNameIgnoreCaseAndArchivedFalse(name);
        if (existingShipmentMethod.isPresent()) {
          log.info("Shipment method '{}' already exists, skipping", name);
          continue;
        }

        // Create new shipment method
        ShipmentMethodEntity shipmentMethod = new ShipmentMethodEntity();
        shipmentMethod.setName(name);
        shipmentMethod.setPercentage(percentage);
        shipmentMethodsToCreate.add(shipmentMethod);
        log.info("Prepared shipment method '{}' with percentage {}", name, percentage);
      }

      // Save all shipment methods in batch
      if (!shipmentMethodsToCreate.isEmpty()) {
        shipmentMethodRepository.saveAll(shipmentMethodsToCreate);
        log.info(
            "Successfully created {} shipment methods from Excel file",
            shipmentMethodsToCreate.size());
      } else {
        log.info("No new shipment methods to create from Excel file");
      }
    }

    return shipmentMethodsToCreate.size();
  }
}
