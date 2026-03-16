package my.lokalix.planning.core.repositories;

import java.util.UUID;
import my.lokalix.planning.core.models.entities.CostRequestFrozenShipmentLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostRequestFrozenShipmentLocationRepository
    extends JpaRepository<CostRequestFrozenShipmentLocationEntity, UUID> {}
