package my.lokalix.planning.core.services;

import jakarta.persistence.EntityExistsException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.mappers.CustomerMapper;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.lokalix.planning.core.models.entities.admin.CustomerEntity;
import my.lokalix.planning.core.models.entities.admin.CustomerShipmentLocationEntity;
import my.lokalix.planning.core.models.entities.admin.ShipmentLocationEntity;
import my.lokalix.planning.core.repositories.admin.CustomerRepository;
import my.lokalix.planning.core.repositories.admin.CustomerShipmentLocationRepository;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import my.lokalix.planning.core.services.validator.CustomerValidator;
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
public class CustomerService {

  private final CustomerMapper customerMapper;
  private final CustomerRepository customerRepository;
  private final CustomerShipmentLocationRepository customerShipmentLocationRepository;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final CustomerValidator customerValidator;

  @Transactional
  public SWCustomer createCustomer(SWCustomerCreate body) {
    if (customerRepository.existsByCodeIgnoreCaseAndArchivedFalse(body.getCode())) {
      throw new EntityExistsException(
          "A customer with the same code '" + body.getCode() + "' already exists");
    }
    if (customerRepository.existsByNameIgnoreCaseAndArchivedFalse(body.getName())) {
      throw new EntityExistsException(
          "A customer with the same name '" + body.getName() + "' already exists");
    }
    CustomerEntity customerEntity = customerMapper.toAdminCustomer(body);
    return customerMapper.toSWCustomer(customerRepository.save(customerEntity));
  }

  @Transactional
  public SWCustomer updateCustomer(UUID uid, SWCustomerUpdate body) {
    CustomerEntity customerEntity = entityRetrievalHelper.getMustExistCustomerById(uid);
    if (customerRepository.existsByCodeIgnoreCaseAndCustomerIdNotAndArchivedFalse(
        body.getCode(), uid)) {
      throw new EntityExistsException(
          "A customer with the same code '" + body.getCode() + "' already exists");
    }
    if (customerRepository.existsByNameIgnoreCaseAndCustomerIdNotAndArchivedFalse(
        body.getName(), uid)) {
      throw new EntityExistsException(
          "A customer with the same name '" + body.getName() + "' already exists");
    }
    customerMapper.updateAdminCustomerEntityFromDto(body, customerEntity);
    return customerMapper.toSWCustomer(customerRepository.save(customerEntity));
  }

  @Transactional
  public List<SWCustomerShipmentLocation> retrieveCustomerShipmentLocations(UUID uid) {
    CustomerEntity customer = entityRetrievalHelper.getMustExistCustomerById(uid);
    return customerMapper.toListSWCustomerShipmentLocation(customer.getShipmentLocations());
  }

  @Transactional
  public List<SWCustomerShipmentLocation> createCustomerShipmentLocation(
      UUID uid, SWCustomerShipmentLocationCreate body) {
    CustomerEntity customer = entityRetrievalHelper.getMustExistCustomerById(uid);
    ShipmentLocationEntity shipmentLocation =
        entityRetrievalHelper.getMustExistShipmentLocationById(body.getShipmentLocationId());
    CurrencyEntity currency = entityRetrievalHelper.getMustExistCurrencyById(body.getCurrencyId());
    customerValidator.validateMaxShipmentLocations(customer);
    customerValidator.validateCurrencyAcceptedByShipmentLocation(currency, shipmentLocation);
    customerValidator.validateNoDuplicateShipmentLocation(
        customer, shipmentLocation, currency, null);

    CustomerShipmentLocationEntity entity = new CustomerShipmentLocationEntity();
    entity.setShipmentLocation(shipmentLocation);
    entity.setCurrency(currency);
    customer.addShipmentLocation(entity);
    customerRepository.save(customer);
    return customerMapper.toListSWCustomerShipmentLocation(customer.getShipmentLocations());
  }

  @Transactional
  public SWCustomerShipmentLocation retrieveCustomerShipmentLocation(
      UUID uid, UUID shipmentLocationUid) {
    CustomerEntity customer = entityRetrievalHelper.getMustExistCustomerById(uid);
    CustomerShipmentLocationEntity entity =
        entityRetrievalHelper.getMustExistCustomerShipmentLocationById(
            shipmentLocationUid, customer);
    return customerMapper.toSWCustomerShipmentLocation(entity);
  }

  @Transactional
  public List<SWCustomerShipmentLocation> updateCustomerShipmentLocation(
      UUID uid, UUID shipmentLocationUid, SWCustomerShipmentLocationCreate body) {
    CustomerEntity customer = entityRetrievalHelper.getMustExistCustomerById(uid);
    CustomerShipmentLocationEntity entity =
        entityRetrievalHelper.getMustExistCustomerShipmentLocationById(
            shipmentLocationUid, customer);
    ShipmentLocationEntity shipmentLocation =
        entityRetrievalHelper.getMustExistShipmentLocationById(body.getShipmentLocationId());
    CurrencyEntity currency = entityRetrievalHelper.getMustExistCurrencyById(body.getCurrencyId());
    customerValidator.validateCurrencyAcceptedByShipmentLocation(currency, shipmentLocation);
    customerValidator.validateNoDuplicateShipmentLocation(
        customer, shipmentLocation, currency, shipmentLocationUid);

    entity.setShipmentLocation(shipmentLocation);
    entity.setCurrency(currency);
    customerShipmentLocationRepository.save(entity);
    return customerMapper.toListSWCustomerShipmentLocation(customer.getShipmentLocations());
  }

  @Transactional
  public List<SWCustomerShipmentLocation> deleteCustomerShipmentLocation(
      UUID uid, UUID shipmentLocationUid) {
    CustomerEntity customer = entityRetrievalHelper.getMustExistCustomerById(uid);
    CustomerShipmentLocationEntity entity =
        entityRetrievalHelper.getMustExistCustomerShipmentLocationById(
            shipmentLocationUid, customer);
    customer.removeShipmentLocation(entity);
    customerRepository.save(customer);
    return customerMapper.toListSWCustomerShipmentLocation(customer.getShipmentLocations());
  }

  @Transactional
  public void archiveCustomer(UUID uid) {
    CustomerEntity entity = entityRetrievalHelper.getMustExistCustomerById(uid);
    customerValidator.validateNotInUse(entity);
    entity.setArchived(true);
    customerRepository.save(entity);
  }

  @Transactional
  public SWCustomer retrieveCustomer(UUID uid) {
    CustomerEntity entity = entityRetrievalHelper.getMustExistCustomerById(uid);
    return customerMapper.toSWCustomer(entity);
  }

  @Transactional
  public List<SWCustomer> listCustomers() {
    Sort sort = Sort.by(Sort.Direction.ASC, "code");
    List<CustomerEntity> allCustomers = customerRepository.findAllByArchivedFalse(sort);
    return customerMapper.toListSwCustomer(allCustomers);
  }

  @Transactional
  public SWCustomersPaginated searchCustomers(int offset, int limit, SWBasicSearch search) {
    Sort sort = Sort.by(Sort.Direction.ASC, "code");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);
    Page<CustomerEntity> paginatedCustomers;

    if (StringUtils.isBlank(search.getSearchText())) {
      paginatedCustomers = customerRepository.findByArchivedFalse(pageable);
    } else {
      paginatedCustomers =
          customerRepository.findBySearchAndArchivedFalse(pageable, search.getSearchText());
    }

    return populateCustomersPaginatedResults(paginatedCustomers);
  }

  @Transactional
  public List<String> retrieveRequestorNamesFromCustomer(UUID uid) {
    CustomerEntity entity = entityRetrievalHelper.getMustExistCustomerById(uid);
    if (StringUtils.isBlank(entity.getRequestorNames())) {
      return Collections.emptyList();
    }
    return TextUtils.splitConcatenatedListWithSeparator(
            entity.getRequestorNames(), Function.identity())
        .stream()
        .sorted()
        .toList();
  }

  @Transactional
  public void setRequestorNamesToCustomer(UUID uid, List<String> names) {
    CustomerEntity entity = entityRetrievalHelper.getMustExistCustomerById(uid);
    entity.setRequestorNames(appendUniqueValues(null, names));
    customerRepository.save(entity);
  }

  @Transactional
  public void addRequestorNamesToCustomer(UUID uid, List<String> names) {
    CustomerEntity entity = entityRetrievalHelper.getMustExistCustomerById(uid);
    entity.setRequestorNames(appendUniqueValues(entity.getRequestorNames(), names));
    customerRepository.save(entity);
  }

  @Transactional
  public List<String> retrieveCustomerEmailsFromCustomer(UUID uid) {
    CustomerEntity entity = entityRetrievalHelper.getMustExistCustomerById(uid);
    if (StringUtils.isBlank(entity.getCustomerEmails())) {
      return Collections.emptyList();
    }
    return TextUtils.splitConcatenatedListWithSeparator(
            entity.getCustomerEmails(), Function.identity())
        .stream()
        .sorted()
        .toList();
  }

  @Transactional
  public void setCustomerEmailsToCustomer(UUID uid, List<String> emails) {
    CustomerEntity entity = entityRetrievalHelper.getMustExistCustomerById(uid);
    entity.setCustomerEmails(appendUniqueValues(null, emails));
    customerRepository.save(entity);
  }

  @Transactional
  public void addCustomerEmailsToCustomer(UUID uid, List<String> emails) {
    CustomerEntity entity = entityRetrievalHelper.getMustExistCustomerById(uid);
    entity.setCustomerEmails(appendUniqueValues(entity.getCustomerEmails(), emails));
    customerRepository.save(entity);
  }

  @Transactional
  public int uploadCustomersFromExcel(MultipartFile file) throws IOException {
    List<CustomerEntity> customersToCreate = new ArrayList<>();

    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream)) {

      Sheet sheet = workbook.getSheetAt(0);
      log.info("Processing customers sheet: {}", sheet.getSheetName());

      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
          continue;
        }

        String name = ExcelUtils.loadStringCell(row.getCell(1));
        if (StringUtils.isBlank(name)) {
          log.warn("Row {}: name is blank for customer '{}', skipping", rowIndex, name);
          continue;
        }
        name = name.trim();

        String code = ExcelUtils.loadStringCell(row.getCell(2));
        if (StringUtils.isBlank(code)) {
          log.warn("Row {}: code is blank for customer '{}', skipping", rowIndex, code);
          continue;
        }
        code = code.trim();

        if (customerRepository.existsByCodeIgnoreCaseAndArchivedFalse(code)) {
          log.warn("Row {}: customer with code '{}' already exists, skipping", rowIndex, code);
          continue;
        }
        if (customerRepository.existsByNameIgnoreCaseAndArchivedFalse(name)) {
          log.warn("Row {}: customer with name '{}' already exists, skipping", rowIndex, name);
          continue;
        }

        CustomerEntity entity = new CustomerEntity();
        entity.setName(name);
        entity.setCode(code);
        entity.setDyson(StringUtils.startsWithIgnoreCase(name, "dyson"));
        customersToCreate.add(entity);
      }
    }

    customerRepository.saveAll(customersToCreate);
    log.info("Uploaded {} customers from Excel", customersToCreate.size());
    return customersToCreate.size();
  }

  private String appendUniqueValues(String existing, List<String> newValues) {
    LinkedHashMap<String, String> values = new LinkedHashMap<>();
    if (StringUtils.isNotBlank(existing)) {
      for (String v : TextUtils.splitConcatenatedListWithSeparator(existing, Function.identity())) {
        values.put(v.trim().toLowerCase(), v.trim());
      }
    }
    if (CollectionUtils.isNotEmpty(newValues)) {
      for (String value : newValues) {
        if (StringUtils.isNotBlank(value)) {
          values.putIfAbsent(value.trim().toLowerCase(), value.trim());
        }
      }
    }
    if (values.isEmpty()) {
      return null;
    }
    return TextUtils.concatenateListWithSeparator(values.values().stream().toList());
  }

  private SWCustomersPaginated populateCustomersPaginatedResults(
      Page<CustomerEntity> paginatedCustomers) {
    SWCustomersPaginated customersPaginated = new SWCustomersPaginated();
    customersPaginated.setResults(customerMapper.toListSwCustomer(paginatedCustomers.getContent()));
    customersPaginated.setPage(paginatedCustomers.getNumber());
    customersPaginated.setPerPage(paginatedCustomers.getSize());
    customersPaginated.setTotal((int) paginatedCustomers.getTotalElements());
    customersPaginated.setHasPrev(paginatedCustomers.hasPrevious());
    customersPaginated.setHasNext(paginatedCustomers.hasNext());
    return customersPaginated;
  }
}
