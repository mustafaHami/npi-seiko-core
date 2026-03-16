package my.lokalix.planning.core.repositories.admin;

import java.util.Optional;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.admin.CustomerEntity;
import my.lokalix.planning.core.models.entities.admin.CustomerShipmentLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerShipmentLocationRepository
    extends JpaRepository<CustomerShipmentLocationEntity, UUID> {

  Optional<CustomerShipmentLocationEntity>
      findByCustomerShipmentLocationIdAndCustomer(UUID id, CustomerEntity customer);
}
