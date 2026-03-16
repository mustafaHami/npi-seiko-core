package my.lokalix.planning.core.mappers;

import my.lokalix.planning.core.models.entities.admin.TermsAndConditionsDysonEntity;
import my.lokalix.planning.core.models.entities.admin.TermsAndConditionsNonDysonEntity;
import my.zkonsulting.planning.generated.model.SWTermsAndConditionsDyson;
import my.zkonsulting.planning.generated.model.SWTermsAndConditionsDysonPatch;
import my.zkonsulting.planning.generated.model.SWTermsAndConditionsNonDyson;
import my.zkonsulting.planning.generated.model.SWTermsAndConditionsNonDysonPatch;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class TermsAndConditionsMapper {

  public abstract SWTermsAndConditionsNonDyson toSWTermsAndConditionsNonDyson(
      TermsAndConditionsNonDysonEntity entity);

  public abstract void updateTermsAndConditionsNonDysonEntityFromDto(
      SWTermsAndConditionsNonDysonPatch dto,
      @MappingTarget TermsAndConditionsNonDysonEntity entity);

  public abstract SWTermsAndConditionsDyson toSWTermsAndConditionsDyson(
      TermsAndConditionsDysonEntity entity);

  public abstract void updateTermsAndConditionsDysonEntityFromDto(
      SWTermsAndConditionsDysonPatch dto, @MappingTarget TermsAndConditionsDysonEntity entity);

  @Mapping(target = "customer", ignore = true)
  public abstract TermsAndConditionsDysonEntity toCopyTermsAndConditionsDysonEntity(
      TermsAndConditionsDysonEntity termsAndConditionsDysonEntity);

  @Mapping(target = "customer", ignore = true)
  public abstract TermsAndConditionsNonDysonEntity toCopyTermsAndConditionsNonDysonEntity(
      TermsAndConditionsNonDysonEntity termsAndConditionsDysonEntity);
}
