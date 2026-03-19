package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.CustomerEntity;
import my.zkonsulting.planning.generated.model.SWCustomer;
import my.zkonsulting.planning.generated.model.SWCustomerCreate;
import my.zkonsulting.planning.generated.model.SWCustomerUpdate;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class CustomerMapper {

  public abstract CustomerEntity toAdminCustomer(SWCustomerCreate customerConfigCreate);

  public abstract List<SWCustomer> toListSwCustomer(List<CustomerEntity> customerEntities);

  @Mapping(source = "customerId", target = "uid")
  public abstract SWCustomer toSWCustomer(CustomerEntity customerEntity);

  public abstract void updateAdminCustomerEntityFromDto(
      SWCustomerUpdate dto, @MappingTarget CustomerEntity entity);

  public abstract CustomerEntity toCopyCostumerEntity(CustomerEntity customer);
}
