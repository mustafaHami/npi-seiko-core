package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.*;
import my.zkonsulting.planning.generated.model.*;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {
      EnumMapper.class,
      CurrencyMapper.class,
      ExchangeRateMapper.class,
      SupplierAndManufacturerMapper.class,
      MaterialMapper.class,
      MaterialCategoryMapper.class,
      MaterialSupplierMapper.class,
      MaterialSupplierMoqLineMapper.class,
      ProductNameMapper.class
    })
public abstract class MaterialLineMapper {

  // Base material (for CR qty=1)
  @IterableMapping(qualifiedByName = "toSwMaterialCostLineBase")
  public abstract List<SWMaterialCostLine> toListSwMaterialCostLineBase(
      List<MaterialLineEntity> entity);

  @Named("toSwMaterialCostLineBase")
  @Mapping(source = "materialLineId", target = "materialCostLineId")
  @Mapping(source = "material", target = ".")
  @Mapping(source = "material.materialId", target = "uid")
  @Mapping(source = "material.unit.name", target = "unit")
  @Mapping(source = "chosenMaterialSupplier", target = "chosenSupplierAndMoq.chosenSupplier")
  @Mapping(
      source = "minimumOrderQuantity",
      target = "chosenSupplierAndMoq.chosenMoq.minimumOrderQuantity")
  @Mapping(
      source = "unitPurchasingPriceInPurchasingCurrency",
      target = "chosenSupplierAndMoq.chosenMoq.unitPurchasingPriceInPurchasingCurrency")
  @Mapping(source = "leadTime", target = "chosenSupplierAndMoq.chosenMoq.leadTime")
  public abstract SWMaterialCostLine toSwMaterialCostLineBase(MaterialLineEntity entity);

  @Mapping(source = "materialLineId", target = "materialSubstituteCostLineId")
  @Mapping(source = "material", target = ".")
  @Mapping(source = "material.materialId", target = "uid")
  @Mapping(source = "material.unit.name", target = "unit")
  @Mapping(source = "chosenMaterialSupplier", target = "chosenSupplierAndMoq.chosenSupplier")
  @Mapping(
      source = "minimumOrderQuantity",
      target = "chosenSupplierAndMoq.chosenMoq.minimumOrderQuantity")
  @Mapping(
      source = "unitPurchasingPriceInPurchasingCurrency",
      target = "chosenSupplierAndMoq.chosenMoq.unitPurchasingPriceInPurchasingCurrency")
  @Mapping(source = "leadTime", target = "chosenSupplierAndMoq.chosenMoq.leadTime")
  public abstract SWMaterialSubstituteCostLine toSwMaterialSubstituteCostLine(
      MaterialLineEntity entity);

  // Material for CR quantity
  @Named("toSwMaterialCostLinePerQuantity")
  @Mapping(source = "material", target = ".")
  @Mapping(source = "material.materialId", target = "uid")
  @Mapping(source = "material.unit.name", target = "unit")
  public abstract SWMaterialCostLine toSwMaterialCostLinePerQuantity(MaterialLineEntity entity);

  public abstract List<SWMaterialCostLine> toListSwMaterialCostLineFromDraft(
      List<MaterialLineDraftEntity> entity);

  @Mapping(source = "materialLineDraftId", target = "uid")
  @Mapping(source = "unit.name", target = "unit")
  public abstract SWMaterialCostLine toSwMaterialCostLineFromDraft(MaterialLineDraftEntity entity);

  @AfterMapping
  protected void applyDraftFallbacks(
      MaterialLineDraftEntity source, @MappingTarget SWMaterialCostLine target) {
    if (target.getUnit() == null && StringUtils.isNotBlank(source.getDraftUnitName())) {
      target.setUnit(source.getDraftUnitName());
    }
    if (target.getManufacturer() == null
        && StringUtils.isNotBlank(source.getDraftManufacturerName())) {
      SWSupplierAndManufacturer draftManufacturer = new SWSupplierAndManufacturer();
      draftManufacturer.setName(source.getDraftManufacturerName());
      target.setManufacturer(draftManufacturer);
    }
    if (target.getCategory() == null && StringUtils.isNotBlank(source.getDraftCategoryName())) {
      SWMaterialCategory draftCategory = new SWMaterialCategory();
      draftCategory.setName(source.getDraftCategoryName());
      target.setCategory(draftCategory);
    }
  }

  @AfterMapping
  protected void applyBaseFallbacks(
      MaterialLineEntity source, @MappingTarget SWMaterialCostLine target) {
    if (target.getUnit() == null
        && StringUtils.isNotBlank(source.getMaterial().getDraftUnitName())) {
      target.setUnit(source.getMaterial().getDraftUnitName());
    }
    if (target.getManufacturer() == null
        && StringUtils.isNotBlank(source.getMaterial().getDraftManufacturerName())) {
      SWSupplierAndManufacturer draftManufacturer = new SWSupplierAndManufacturer();
      draftManufacturer.setName(source.getMaterial().getDraftManufacturerName());
      target.setManufacturer(draftManufacturer);
    }
    if (target.getCategory() == null
        && StringUtils.isNotBlank(source.getMaterial().getDraftCategoryName())) {
      SWMaterialCategory draftCategory = new SWMaterialCategory();
      draftCategory.setName(source.getMaterial().getDraftCategoryName());
      target.setCategory(draftCategory);
    }
  }
}
