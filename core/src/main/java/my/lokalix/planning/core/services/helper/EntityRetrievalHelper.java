package my.lokalix.planning.core.services.helper;

import io.micrometer.common.util.StringUtils;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.models.entities.*;
import my.lokalix.planning.core.models.entities.admin.*;
import my.lokalix.planning.core.models.entities.admin.BomConfigurationEntity;
import my.lokalix.planning.core.models.entities.admin.SupplierManufacturerEntity;
import my.lokalix.planning.core.models.entities.admin.UnitEntity;
import my.lokalix.planning.core.models.enums.SupplierAndManufacturerType;
import my.lokalix.planning.core.repositories.*;
import my.lokalix.planning.core.repositories.admin.BomConfigurationRepository;
import my.lokalix.planning.core.repositories.admin.CustomerRepository;
import my.lokalix.planning.core.repositories.admin.CustomerShipmentLocationRepository;
import my.lokalix.planning.core.repositories.admin.MaterialCategoryRepository;
import my.lokalix.planning.core.repositories.admin.ProcessRepository;
import my.lokalix.planning.core.repositories.admin.ProductNameRepository;
import my.lokalix.planning.core.repositories.admin.ShipmentLocationRepository;
import my.lokalix.planning.core.repositories.admin.ShipmentMethodRepository;
import my.lokalix.planning.core.repositories.admin.SupplierAndManufacturerRepository;
import my.lokalix.planning.core.repositories.admin.UnitRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityRetrievalHelper {

  private final CustomerRepository customerRepository;
  private final CustomerShipmentLocationRepository customerShipmentLocationRepository;
  private final MaterialCategoryRepository materialCategoryRepository;
  private final SupplierAndManufacturerRepository supplierAndManufacturerRepository;
  private final ProcessRepository processRepository;
  private final ProductNameRepository productNameRepository;
  private final ShipmentMethodRepository shipmentMethodRepository;
  private final CurrencyRepository currencyRepository;
  private final CostRequestRepository costRequestRepository;
  private final CostRequestLineRepository costRequestLineRepository;
  private final GlobalConfigRepository globalConfigRepository;
  private final TermsAndConditionsNonDysonRepository termsAndConditionsNonDysonRepository;
  private final TermsAndConditionsDysonRepository termsAndConditionsDysonRepository;
  private final FileRepository fileRepository;
  private final MaterialRepository materialRepository;
  private final MaterialSupplierRepository materialSupplierRepository;
  private final MaterialLineRepository materialLineRepository;
  private final UnitRepository unitRepository;
  private final ToolingCostLineRepository toolingCostLineRepository;
  private final BomConfigurationRepository bomConfigurationRepository;
  private final ShipmentLocationRepository shipmentLocationRepository;

  public ShipmentLocationEntity getMustExistShipmentLocationById(UUID uid) {
    return shipmentLocationRepository
        .findById(uid)
        .orElseThrow(() -> new EntityNotFoundException("Dyson shipment location not found"));
  }

  public BomConfigurationEntity getMustExistBomConfigurationById(UUID uid) {
    return bomConfigurationRepository
        .findById(uid)
        .orElseThrow(() -> new EntityNotFoundException("BOM configuration not found"));
  }

  public CustomerShipmentLocationEntity getMustExistCustomerShipmentLocationById(
      UUID uid, CustomerEntity customer) {
    return customerShipmentLocationRepository
        .findByCustomerShipmentLocationIdAndCustomer(uid, customer)
        .orElseThrow(() -> new EntityNotFoundException("Customer shipment location not found"));
  }

  public CustomerEntity getMustExistCustomerById(UUID uid) {
    return customerRepository
        .findById(uid)
        .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
  }

  public SupplierManufacturerEntity getMustExistSupplierManufacturerById(UUID uid) {
    return supplierAndManufacturerRepository
        .findById(uid)
        .orElseThrow(() -> new EntityNotFoundException("Supplier/Manufacturer not found"));
  }

  public ProductNameEntity getMustExistProductNameById(UUID uid) {
    return productNameRepository
        .findById(uid)
        .orElseThrow(() -> new EntityNotFoundException("Product name not found"));
  }

  public ProcessEntity getMustExistProcessById(UUID uid) {
    return processRepository
        .findById(uid)
        .orElseThrow(() -> new EntityNotFoundException("Process not found"));
  }

  public ProcessEntity getMustExistSetupProcess() {
    return processRepository
        .findFirstBySetupProcessTrueAndArchivedFalse()
        .orElseThrow(
            () -> new EntityNotFoundException("No active setup process found in the system"));
  }

  public MaterialCategoryEntity getMustExistMaterialCategoryById(UUID uid) {
    return materialCategoryRepository
        .findById(uid)
        .orElseThrow(() -> new EntityNotFoundException("Material category not found"));
  }

  public CurrencyEntity getMustExistCurrencyById(UUID uid) {
    return currencyRepository
        .findById(uid)
        .orElseThrow(() -> new EntityNotFoundException("Currency not found"));
  }

  public CurrencyEntity getMustExistCurrencyByCode(String code) {
    return currencyRepository
        .findByCodeAndArchivedFalse(code)
        .orElseThrow(
            () -> new EntityNotFoundException("Currency with code '" + code + "' not found"));
  }

  public CostRequestEntity getMustExistCostRequestById(UUID uid) {
    return costRequestRepository
        .findById(uid)
        .orElseThrow(() -> new EntityNotFoundException("Cost request not found"));
  }

  public CostRequestLineEntity getMustExistCostRequestLineEntity(UUID uid, UUID lineUid) {
    CostRequestEntity costRequest = getMustExistCostRequestById(uid);
    return costRequestLineRepository
        .findByCostRequestAndCostRequestLineId(costRequest, lineUid)
        .orElseThrow(() -> new EntityNotFoundException("Cost request line not found"));
  }

  public CostRequestLineEntity getMustExistCostRequestLineById(UUID uid) {
    return costRequestLineRepository
        .findById(uid)
        .orElseThrow(() -> new EntityNotFoundException("Cost request line not found"));
  }

  public GlobalConfigEntity getMustExistGlobalConfig() {
    return globalConfigRepository
        .findFirstBy()
        .orElseThrow(() -> new EntityNotFoundException("Global config not found"));
  }

  public TermsAndConditionsNonDysonEntity getMustExistTermsAndConditionsNonDysonById(UUID uid) {
    return termsAndConditionsNonDysonRepository
        .findById(uid)
        .orElseThrow(() -> new EntityNotFoundException("Terms and conditions non dyson not found"));
  }

  public TermsAndConditionsDysonEntity getMustExistTermsAndConditionsDysonById(UUID uid) {
    return termsAndConditionsDysonRepository
        .findById(uid)
        .orElseThrow(() -> new EntityNotFoundException("Terms and conditions dyson not found"));
  }

  public FileInfoEntity getMustExistFileEntity(UUID uuid) {
    return fileRepository
        .findById(uuid)
        .orElseThrow(() -> new EntityNotFoundException("File not found"));
  }

  public MaterialEntity getMustExistMaterialById(UUID uid) {
    return materialRepository
        .findById(uid)
        .orElseThrow(() -> new EntityNotFoundException("Material not found"));
  }

  public MaterialSupplierEntity getMustExistMaterialSupplierById(UUID uid) {
    return materialSupplierRepository
        .findById(uid)
        .orElseThrow(() -> new EntityNotFoundException("Material supplier not found"));
  }

  public MaterialLineEntity getMustExistMaterialLineById(UUID uid) {
    return materialLineRepository
        .findById(uid)
        .orElseThrow(() -> new EntityNotFoundException("Material line not found"));
  }

  public ShipmentMethodEntity getMustExistShipmentMethodById(UUID uid) {
    return shipmentMethodRepository
        .findByShipmentMethodIdAndArchivedFalse(uid)
        .orElseThrow(() -> new EntityNotFoundException("Shipment method not found"));
  }

  public ShipmentMethodEntity getMustExistShipmentMethodByName(String name) {
    return shipmentMethodRepository
        .findByNameIgnoreCaseAndArchivedFalse(name)
        .orElseThrow(
            () ->
                new EntityNotFoundException("Shipment method with name '" + name + "' not found"));
  }

  public UnitEntity getMustExistUnitById(UUID uid) {
    return unitRepository
        .findById(uid)
        .orElseThrow(() -> new EntityNotFoundException("Unit not found"));
  }

  public ToolingCostLineEntity getMustExistToolingCostLineById(UUID uid) {
    return toolingCostLineRepository
        .findById(uid)
        .orElseThrow(() -> new EntityNotFoundException("Tooling cost line not found"));
  }

  public Optional<SupplierManufacturerEntity> getOptionalManufacturerByName(String name) {
    if (StringUtils.isBlank(name)) return Optional.empty();
    Optional<SupplierManufacturerEntity> opt =
        supplierAndManufacturerRepository.findByTypeInAndNameIgnoreCaseAndArchivedFalse(
            SupplierAndManufacturerType.getManufacturerTypes(), name);
    // If we are looking for a manufacturer and we find a match that is only a supplier, mark as
    // BOTH and return it
    if (opt.isPresent() && opt.get().getType() == SupplierAndManufacturerType.SUPPLIER) {
      opt.get().setType(SupplierAndManufacturerType.BOTH);
      return Optional.of(supplierAndManufacturerRepository.save(opt.get()));
    }
    return opt;
  }

  public Optional<MaterialCategoryEntity> getOptionalCategoryByName(String name) {
    if (StringUtils.isBlank(name)) return Optional.empty();
    return materialCategoryRepository.findByNameIgnoreCaseAndArchivedFalse(name);
  }

  public Optional<UnitEntity> getOptionalUnitByName(String name) {
    if (StringUtils.isBlank(name)) return Optional.empty();
    return unitRepository.findByNameIgnoreCaseAndArchivedFalse(name);
  }

  public Optional<SupplierManufacturerEntity> getOptionalSupplierByName(String name) {
    if (StringUtils.isBlank(name)) return Optional.empty();
    return supplierAndManufacturerRepository.findByTypeInAndNameIgnoreCaseAndArchivedFalse(
        SupplierAndManufacturerType.getSupplierTypes(), name);
  }
}
