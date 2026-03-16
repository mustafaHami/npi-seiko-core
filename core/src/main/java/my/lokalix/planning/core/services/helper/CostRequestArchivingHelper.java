package my.lokalix.planning.core.services.helper;

import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.mappers.*;
import my.lokalix.planning.core.models.entities.*;
import my.lokalix.planning.core.models.entities.admin.*;
import my.lokalix.planning.core.repositories.*;
import my.lokalix.planning.core.repositories.admin.*;
import my.lokalix.planning.core.utils.TimeUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostRequestArchivingHelper {

  private final CurrencyRepository currencyRepository;
  private final CostRequestHelper costRequestHelper;
  private final MaterialMapper materialMapper;
  private final MaterialRepository materialRepository;
  private final CustomerRepository customerRepository;
  private final ProcessMapper processMapper;
  private final CustomerMapper customerMapper;
  private final MaterialSupplierMapper materialSupplierMapper;
  private final ProcessRepository processRepository;
  private final MaterialSupplierMoqLineRepository materialSupplierMoqLineRepository;
  private final MaterialSupplierRepository materialSupplierRepository;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final TermsAndConditionsDysonRepository termsAndConditionsDysonRepository;
  private final TermsAndConditionsMapper termsAndConditionsMapper;
  private final TermsAndConditionsNonDysonRepository termsAndConditionsNonDysonRepository;

  /**
   * Archive all data for a cost request and its lines in order to take a snapshot.
   *
   * <p>This method should be called when a cost request is being finalized (ESTIMATED or ABORTED)
   * to freeze all dependent data at that point in time.
   *
   * @param costRequest The cost request entity to archive data for
   */
  public void archiveCostRequestDataFreeze(CostRequestEntity costRequest) {
    //     Freeze the global config used for this cost request
    GlobalConfigEntity globalConfigEntity = entityRetrievalHelper.getMustExistGlobalConfig();
    freezeGlobalConfig(costRequest, globalConfigEntity);

    // Create archived versions of all active currencies with their exchange rates, to get full
    // snapshot of currencies & exchange rates, and for reusability purpose
    Map<UUID, CurrencyEntity> archivedCurrenciesByOriginalId = archiveAllCurrencies();

    // Set CR currency to its archived counterpart
    costRequest.setCurrency(
        retrieveArchivedCurrency(archivedCurrenciesByOriginalId, costRequest.getCurrency()));

    // This Map is used as a local cache of archived shipment methods, whenever encountering an
    // active one not yet having an archived counterpart, for reusability purpose
    Map<UUID, ShipmentMethodEntity> archivedShipmentMethodsByOriginalId = new HashMap<>();

    // This Map is used as a local cache of archived DYSON shipment locations, whenever encountering
    // an
    // active one not yet having an archived counterpart, for reusability purpose
    Map<UUID, ShipmentLocationEntity> archivedShipmentLocationByOriginalId = new HashMap<>();

    // Archive the CR customer
    archiveCustomerForCostRequest(
        costRequest, archivedShipmentLocationByOriginalId, archivedCurrenciesByOriginalId);

    // Archive data for freezing for each line
    if (CollectionUtils.isNotEmpty(costRequest.getLines())) {
      for (CostRequestLineEntity line : costRequest.getLines()) {
        // Archive line product name
        archiveCostRequestLineProductName(line);
        // Archive line materials
        archiveCostRequestLineMaterials(
            line, archivedShipmentMethodsByOriginalId, archivedCurrenciesByOriginalId);
        // Archive line processes
        archiveCostRequestLineProcesses(line, archivedCurrenciesByOriginalId);
        // Archive line toolings
        archiveCostRequestLineToolings(line, archivedCurrenciesByOriginalId);
        // Archive line others
        archiveCostRequestLineOthers(
            line, archivedCurrenciesByOriginalId, archivedShipmentLocationByOriginalId);
      }
    }
  }

  private void freezeGlobalConfig(
      CostRequestEntity costRequest, GlobalConfigEntity globalConfigEntity) {
    log.info("Freezing global config for request for quotation: {} ", costRequest);
    costRequest.getArchivedGlobalConfig().setLaborCost(globalConfigEntity.getLaborCost());
    costRequest.getArchivedGlobalConfig().setOverheadCost(globalConfigEntity.getOverheadCost());
    costRequest
        .getArchivedGlobalConfig()
        .setInternalTransportation(globalConfigEntity.getInternalTransportation());
    costRequest
        .getArchivedGlobalConfig()
        .setDepreciationCost(globalConfigEntity.getDepreciationCost());
    costRequest
        .getArchivedGlobalConfig()
        .setAdministrationCost(globalConfigEntity.getAdministrationCost());
    costRequest
        .getArchivedGlobalConfig()
        .setStandardJigsAndFixturesCost(globalConfigEntity.getStandardJigsAndFixturesCost());
    costRequest
        .getArchivedGlobalConfig()
        .setSmallPackagingCost(globalConfigEntity.getSmallPackagingCost());
    costRequest
        .getArchivedGlobalConfig()
        .setLargePackagingCost(globalConfigEntity.getLargePackagingCost());
    costRequest
        .getArchivedGlobalConfig()
        .setMarkupApprovalStrategy(globalConfigEntity.getMarkupApprovalStrategy());
    costRequest.getArchivedGlobalConfig().setBaseMarkup(globalConfigEntity.getBaseMarkup());
    costRequest.getArchivedGlobalConfig().setMarkupRange(globalConfigEntity.getMarkupRange());
    costRequest
        .getArchivedGlobalConfig()
        .setBudgetaryAdditionalRate(globalConfigEntity.getBudgetaryAdditionalRate());
    costRequest
        .getArchivedGlobalConfig()
        .setNpiProcessesAdditionalRate(globalConfigEntity.getNpiProcessesAdditionalRate());
    costRequest
        .getArchivedGlobalConfig()
        .setYieldPercentage(globalConfigEntity.getYieldPercentage());
  }

  /**
   * Archive all active currencies and their exchange rates. Creates an archived copy of every
   * non-archived currency, then creates exchange rates between the archived copies so that the
   * archived currencies form a self-contained snapshot.
   *
   * @return A map from original currency id to the archived currency entity
   */
  private Map<UUID, CurrencyEntity> archiveAllCurrencies() {
    List<CurrencyEntity> activeCurrencies =
        currencyRepository.findAllByArchivedFalse(Sort.unsorted());
    if (CollectionUtils.isEmpty(activeCurrencies)) {
      return Collections.emptyMap();
    }

    // First pass: create archived currency shells (without exchange rates)
    // Map: original currency ID -> archived currency entity
    Map<UUID, CurrencyEntity> originalIdToArchived = new HashMap<>();

    for (CurrencyEntity activeCurrency : activeCurrencies) {
      CurrencyEntity archivedCopy = new CurrencyEntity();
      archivedCopy.setCode(activeCurrency.getCode());
      archivedCopy.setArchived(true);
      archivedCopy.setArchivedAt(TimeUtils.nowOffsetDateTimeUTC());
      archivedCopy = currencyRepository.save(archivedCopy);
      originalIdToArchived.put(activeCurrency.getCurrencyId(), archivedCopy);
    }

    // Second pass: create exchange rates between archived currencies
    for (CurrencyEntity activeCurrency : activeCurrencies) {
      CurrencyEntity archivedFrom = originalIdToArchived.get(activeCurrency.getCurrencyId());
      if (CollectionUtils.isNotEmpty(activeCurrency.getExchangeRates())) {
        for (ExchangeRateEntity exchangeRate : activeCurrency.getExchangeRates()) {
          CurrencyEntity archivedTo =
              originalIdToArchived.get(exchangeRate.getToCurrency().getCurrencyId());
          if (archivedTo != null) {
            ExchangeRateEntity archivedExchangeRate = new ExchangeRateEntity();
            archivedExchangeRate.setFromCurrency(archivedFrom);
            archivedExchangeRate.setToCurrency(archivedTo);
            archivedExchangeRate.setRate(exchangeRate.getRate());
            archivedFrom.getExchangeRates().add(archivedExchangeRate);
          }
        }
        currencyRepository.save(archivedFrom);
      }
    }

    return originalIdToArchived;
  }

  private void archiveCostRequestLineOthers(
      CostRequestLineEntity line,
      Map<UUID, CurrencyEntity> archivedCurrenciesByOriginalId,
      Map<UUID, ShipmentLocationEntity> archivedDysonShipmentLocationByOriginalId) {
    if (CollectionUtils.isNotEmpty(line.getOtherCostLines())) {
      for (OtherCostLineEntity otherCostLine : line.getOtherCostLines()) {
        otherCostLine.setCurrency(
            retrieveArchivedCurrency(archivedCurrenciesByOriginalId, otherCostLine.getCurrency()));
        otherCostLine.setShipmentLocation(
            retrieveArchivedShipmentLocation(
                archivedDysonShipmentLocationByOriginalId, otherCostLine.getShipmentLocation()));
      }
    }
  }

  private void archiveCostRequestLineToolings(
      CostRequestLineEntity line, Map<UUID, CurrencyEntity> archivedCurrenciesByOriginalId) {
    if (CollectionUtils.isNotEmpty(line.getToolingCostLines())) {
      for (ToolingCostLineEntity toolingCostLine : line.getToolingCostLines()) {
        toolingCostLine.setCurrency(
            retrieveArchivedCurrency(
                archivedCurrenciesByOriginalId, toolingCostLine.getCurrency()));
      }
    }
  }

  private void archiveCostRequestLineProcesses(
      CostRequestLineEntity line, Map<UUID, CurrencyEntity> archivedCurrenciesByOriginalId) {
    if (CollectionUtils.isNotEmpty(line.getProcessLines())) {
      for (ProcessLineEntity processCostLine : line.getProcessLines()) {
        ProcessEntity archivedProcess =
            processMapper.toCopyProcessEntity(processCostLine.getProcess());
        archivedProcess.setCurrency(
            retrieveArchivedCurrency(
                archivedCurrenciesByOriginalId, processCostLine.getProcess().getCurrency()));
        archivedProcess.setArchived(true);
        processCostLine.setProcess(processRepository.save(archivedProcess));
      }
    }
  }

  private void archiveCostRequestLineMaterials(
      CostRequestLineEntity line,
      Map<UUID, ShipmentMethodEntity> archivedShipmentMethodsByOriginalId,
      Map<UUID, CurrencyEntity> archivedCurrenciesByOriginalId) {
    // This Map is used as a local cache of archived units, and it is updated whenever
    // encountering an active unit not yet having an archived counterpart, for reusability
    // purpose
    Map<UUID, UnitEntity> archivedUnitsByOriginalId = new HashMap<>();
    // This Map is used as a local cache of archived suppliers and manufacturers, and it is
    // updated whenever encountering an active supplier/manufacturer not yet having an archived
    // counterpart, for reusability purpose
    Map<UUID, SupplierManufacturerEntity> archivedSuppliersManufacturersByOriginalId =
        new HashMap<>();
    // This Map is used as a local cache of archived categories, and it is updated whenever
    // encountering an active category not yet having an archived counterpart, for reusability
    // purpose
    Map<UUID, MaterialCategoryEntity> archivedMaterialCategoriesByOriginalId = new HashMap<>();
    // This Map is used as a local cache of archived material suppliers, and it is updated
    // whenever encountering an active material supplier not yet having an archived
    // counterpart, for
    // reusability purpose
    Map<UUID, MaterialSupplierEntity> archivedMaterialSupplierByOriginalId = new HashMap<>();
    if (CollectionUtils.isNotEmpty(line.getMaterialLines())) {
      for (MaterialLineEntity ml : line.getMaterialLines()) {
        // Used to map the correct MOQ line to material line chosen one if any
        MaterialEntity activeMaterial = ml.getMaterial();
        MaterialEntity archivedMaterial = materialMapper.toCopyMaterial(activeMaterial);
        archivedMaterial.setArchived(true);
        archivedMaterial.setManufacturer(
            retrieveArchivedSupplierManufacturer(
                archivedSuppliersManufacturersByOriginalId,
                activeMaterial.getManufacturer(),
                null));
        archivedMaterial.setCategory(
            retrieveArchivedMaterialCategory(
                archivedMaterialCategoriesByOriginalId, activeMaterial.getCategory()));
        archivedMaterial.setUnit(
            retrieveArchivedUnit(archivedUnitsByOriginalId, activeMaterial.getUnit()));
        archivedMaterial = materialRepository.save(archivedMaterial);

        if (CollectionUtils.isNotEmpty(activeMaterial.getSuppliers())) {
          for (MaterialSupplierEntity activeSupplier : activeMaterial.getSuppliers()) {
            MaterialSupplierEntity archivedSupplier =
                materialSupplierMapper.toCopyMaterialSupplier(activeSupplier);
            archivedSupplier.setMaterial(archivedMaterial);
            archivedSupplier.setSupplier(
                retrieveArchivedSupplierManufacturer(
                    archivedSuppliersManufacturersByOriginalId,
                    activeSupplier.getSupplier(),
                    archivedShipmentMethodsByOriginalId));
            archivedSupplier.setPurchasingCurrency(
                retrieveArchivedCurrency(
                    archivedCurrenciesByOriginalId, activeSupplier.getPurchasingCurrency()));
            archivedSupplier = materialSupplierRepository.save(archivedSupplier);
            archivedMaterialSupplierByOriginalId.put(
                activeSupplier.getMaterialSupplierId(), archivedSupplier);

            // Copy all MOQ lines for this supplier
            if (CollectionUtils.isNotEmpty(activeSupplier.getMoqLines())) {
              for (MaterialSupplierMoqLineEntity moqLine : activeSupplier.getMoqLines()) {
                MaterialSupplierMoqLineEntity archivedMoqLine = new MaterialSupplierMoqLineEntity();
                archivedMoqLine.setMaterialSupplier(archivedSupplier);
                archivedMoqLine.setMinimumOrderQuantity(moqLine.getMinimumOrderQuantity());
                archivedMoqLine.setUnitPurchasingPriceInPurchasingCurrency(
                    moqLine.getUnitPurchasingPriceInPurchasingCurrency());
                archivedMoqLine.setLeadTime(moqLine.getLeadTime());
                archivedMoqLine.setStandardPackagingQuantity(
                    moqLine.getStandardPackagingQuantity());
                archivedMoqLine = materialSupplierMoqLineRepository.save(archivedMoqLine);
                archivedSupplier.addMoqLine(archivedMoqLine);
              }
            }
            archivedMaterial.addMaterialSupplier(archivedSupplier);
          }
          if (ml.getChosenMaterialSupplier() != null) {
            ml.setChosenMaterialSupplier(
                archivedMaterialSupplierByOriginalId.get(
                    ml.getChosenMaterialSupplier().getMaterialSupplierId()));
          }
        }
        ml.setMaterial(materialRepository.save(archivedMaterial));
      }
    }
    if (CollectionUtils.isNotEmpty(line.getDraftMaterialLines())) {
      for (MaterialLineDraftEntity ml : line.getDraftMaterialLines()) {
        ml.setManufacturer(
            retrieveArchivedSupplierManufacturer(
                archivedSuppliersManufacturersByOriginalId, ml.getManufacturer(), null));
        ml.setCategory(
            retrieveArchivedMaterialCategory(
                archivedMaterialCategoriesByOriginalId, ml.getCategory()));
        ml.setUnit(retrieveArchivedUnit(archivedUnitsByOriginalId, ml.getUnit()));
      }
    }
  }

  private ShipmentLocationEntity retrieveArchivedShipmentLocation(
      Map<UUID, ShipmentLocationEntity> archivedShipmentLocationsByOriginalId,
      ShipmentLocationEntity shipmentLocation) {
    if (shipmentLocation == null) {
      return null;
    }
    if (shipmentLocation.isArchived()) {
      return shipmentLocation;
    }
    if (!archivedShipmentLocationsByOriginalId.containsKey(
        shipmentLocation.getShipmentLocationId())) {
      ShipmentLocationEntity archivedShipmentLocation =
          costRequestHelper.getOrCreateArchivedShipmentLocation(shipmentLocation);
      archivedShipmentLocationsByOriginalId.put(
          shipmentLocation.getShipmentLocationId(), archivedShipmentLocation);
      return archivedShipmentLocation;
    } else {
      return archivedShipmentLocationsByOriginalId.get(shipmentLocation.getShipmentLocationId());
    }
  }

  private CurrencyEntity retrieveArchivedCurrency(
      Map<UUID, CurrencyEntity> archivedCurrenciesByOriginalId, CurrencyEntity currency) {
    if (currency == null) {
      return null;
    }
    if (currency.isArchived()) {
      return currency;
    }
    return archivedCurrenciesByOriginalId.get(currency.getCurrencyId());
  }

  private MaterialCategoryEntity retrieveArchivedMaterialCategory(
      Map<UUID, MaterialCategoryEntity> archivedMaterialCategoriesByOriginalId,
      MaterialCategoryEntity materialCategory) {
    if (materialCategory == null) {
      return null;
    }
    if (materialCategory.isArchived()) {
      return materialCategory;
    }
    if (!archivedMaterialCategoriesByOriginalId.containsKey(
        materialCategory.getMaterialCategoryId())) {
      MaterialCategoryEntity archivedMaterialCategory =
          costRequestHelper.getOrCreateArchivedMaterialCategory(materialCategory);
      archivedMaterialCategoriesByOriginalId.put(
          materialCategory.getMaterialCategoryId(), archivedMaterialCategory);
      return archivedMaterialCategory;
    } else {
      return archivedMaterialCategoriesByOriginalId.get(materialCategory.getMaterialCategoryId());
    }
  }

  private UnitEntity retrieveArchivedUnit(
      Map<UUID, UnitEntity> archivedUnitsByOriginalId, UnitEntity unit) {
    if (unit == null) {
      return null;
    }
    if (unit.isArchived()) {
      return unit;
    }
    if (!archivedUnitsByOriginalId.containsKey(unit.getUnitId())) {
      UnitEntity archivedUnit = costRequestHelper.getOrCreateArchivedUnit(unit);
      archivedUnitsByOriginalId.put(unit.getUnitId(), archivedUnit);
      return archivedUnit;
    } else {
      return archivedUnitsByOriginalId.get(unit.getUnitId());
    }
  }

  private SupplierManufacturerEntity retrieveArchivedSupplierManufacturer(
      Map<UUID, SupplierManufacturerEntity> archivedSuppliersManufacturersByOriginalId,
      SupplierManufacturerEntity supplierManufacturer,
      Map<UUID, ShipmentMethodEntity> archivedShipmentMethodsByOriginalId) {
    if (supplierManufacturer == null) {
      return null;
    }
    if (supplierManufacturer.isArchived()) {
      return supplierManufacturer;
    }
    if (!archivedSuppliersManufacturersByOriginalId.containsKey(
        supplierManufacturer.getSupplierManufacturerId())) {
      SupplierManufacturerEntity archivedSupplierManufacturer =
          costRequestHelper.getOrCreateArchivedSupplierManufacturer(
              supplierManufacturer, archivedShipmentMethodsByOriginalId);
      archivedSuppliersManufacturersByOriginalId.put(
          supplierManufacturer.getSupplierManufacturerId(), archivedSupplierManufacturer);
      return archivedSupplierManufacturer;
    } else {
      return archivedSuppliersManufacturersByOriginalId.get(
          supplierManufacturer.getSupplierManufacturerId());
    }
  }

  /**
   * Archive the customer for a cost request if not already archived. Checks database first for
   * existing archived version.
   *
   * @param costRequest The cost request entity
   * @param archivedShipmentLocationByOriginalId
   * @param archivedCurrenciesByOriginalId
   */
  private void archiveCustomerForCostRequest(
      CostRequestEntity costRequest,
      Map<UUID, ShipmentLocationEntity> archivedShipmentLocationByOriginalId,
      Map<UUID, CurrencyEntity> archivedCurrenciesByOriginalId) {
    CustomerEntity customer = costRequest.getCustomer();
    if (customer != null && !customer.isArchived()) {
      CustomerEntity archivedCustomer =
          createArchivedCustomer(
              customer, archivedShipmentLocationByOriginalId, archivedCurrenciesByOriginalId);
      costRequest.setCustomer(archivedCustomer);
    }
  }

  /**
   * creates a newly created customer archived copy.
   *
   * @param customer The customer entity to archive
   * @param archivedShipmentLocationByOriginalId
   * @param archivedCurrenciesByOriginalId
   * @return The archived customer newly created
   */
  private CustomerEntity createArchivedCustomer(
      CustomerEntity customer,
      Map<UUID, ShipmentLocationEntity> archivedShipmentLocationByOriginalId,
      Map<UUID, CurrencyEntity> archivedCurrenciesByOriginalId) {
    CustomerEntity archivedCustomer = customerMapper.toCopyCostumerEntity(customer);
    archivedCustomer.setArchived(true);
    archivedCustomer = customerRepository.save(archivedCustomer);

    if (CollectionUtils.isNotEmpty(customer.getShipmentLocations())) {
      List<CustomerShipmentLocationEntity> archivedCustomerShipmentLocations =
          createArchivedCustomerShipmentLocationEntity(
              customer.getShipmentLocations(),
              archivedShipmentLocationByOriginalId,
              archivedCurrenciesByOriginalId);
      for (CustomerShipmentLocationEntity csl : archivedCustomerShipmentLocations) {
        archivedCustomer.addShipmentLocation(csl);
      }
      archivedCustomer = customerRepository.save(archivedCustomer);
    }

    if (customer.isDyson()) {
      Optional<TermsAndConditionsDysonEntity> termsAndConditionsDysonEntity =
          termsAndConditionsDysonRepository.findById(customer.getCustomerId());
      if (termsAndConditionsDysonEntity.isPresent()) {
        TermsAndConditionsDysonEntity termsAndConditionsDysonEntityArchived =
            termsAndConditionsMapper.toCopyTermsAndConditionsDysonEntity(
                termsAndConditionsDysonEntity.get());
        termsAndConditionsDysonEntityArchived.setCustomer(archivedCustomer);
        termsAndConditionsDysonRepository.save(termsAndConditionsDysonEntityArchived);
      }
    } else {
      TermsAndConditionsNonDysonEntity termsAndConditionsNonDysonEntity =
          termsAndConditionsNonDysonRepository.findById(customer.getCustomerId()).orElse(null);
      if (termsAndConditionsNonDysonEntity != null) {
        TermsAndConditionsNonDysonEntity termsAndConditionsNonDysonEntityArchived =
            termsAndConditionsMapper.toCopyTermsAndConditionsNonDysonEntity(
                termsAndConditionsNonDysonEntity);
        termsAndConditionsNonDysonEntityArchived.setCustomer(archivedCustomer);
        termsAndConditionsNonDysonRepository.save(termsAndConditionsNonDysonEntityArchived);
      }
    }
    return archivedCustomer;
  }

  private List<CustomerShipmentLocationEntity> createArchivedCustomerShipmentLocationEntity(
      List<CustomerShipmentLocationEntity> customerShipmentLocations,
      Map<UUID, ShipmentLocationEntity> archivedShipmentLocationByOriginalId,
      Map<UUID, CurrencyEntity> archivedCurrenciesByOriginalId) {
    if (CollectionUtils.isNotEmpty(customerShipmentLocations)) {
      List<CustomerShipmentLocationEntity> archivedCustomerShipmentLocations = new ArrayList<>();
      for (CustomerShipmentLocationEntity csl : customerShipmentLocations) {
        CustomerShipmentLocationEntity archivedCsl = new CustomerShipmentLocationEntity();
        archivedCsl.setShipmentLocation(
            retrieveArchivedShipmentLocation(
                archivedShipmentLocationByOriginalId, csl.getShipmentLocation()));
        archivedCsl.setCurrency(
            retrieveArchivedCurrency(archivedCurrenciesByOriginalId, csl.getCurrency()));
        archivedCsl.setArchived(true);
        archivedCustomerShipmentLocations.add(archivedCsl);
      }
      return archivedCustomerShipmentLocations;
    }
    return List.of();
  }

  public List<CostRequestFrozenShipmentLocationEntity>
      createCostRequestFrozenShipmentLocationEntity(
          List<CustomerShipmentLocationEntity> customerShipmentLocations,
          Map<UUID, ShipmentLocationEntity> archivedShipmentLocationByOriginalId) {
    if (CollectionUtils.isNotEmpty(customerShipmentLocations)) {
      List<CostRequestFrozenShipmentLocationEntity> archivedCustomerShipmentLocations =
          new ArrayList<>();
      for (CustomerShipmentLocationEntity csl : customerShipmentLocations) {
        CostRequestFrozenShipmentLocationEntity archivedCsl =
            new CostRequestFrozenShipmentLocationEntity();
        archivedCsl.setShipmentLocation(
            retrieveArchivedShipmentLocation(
                archivedShipmentLocationByOriginalId, csl.getShipmentLocation()));
        archivedCsl.setCurrencyCode(csl.getCurrency().getCode());
        archivedCsl.setArchived(true);
        archivedCustomerShipmentLocations.add(archivedCsl);
      }
      return archivedCustomerShipmentLocations;
    }
    return List.of();
  }

  /**
   * Archive the product name for a cost request line if not already archived. Checks in database
   * first if an archived version exists.
   *
   * @param line The cost request line entity
   */
  private void archiveCostRequestLineProductName(CostRequestLineEntity line) {
    ProductNameEntity productName = line.getProductName();
    if (productName != null && !productName.isArchived()) {
      ProductNameEntity archivedProductName =
          costRequestHelper.getOrCreateArchivedProductName(productName);
      line.setProductName(archivedProductName);
    }
  }
}
