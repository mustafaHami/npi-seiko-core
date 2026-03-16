package my.lokalix.planning.core.repositories;

import java.util.UUID;
import my.lokalix.planning.core.models.entities.MaterialSupplierMoqLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialSupplierMoqLineRepository
    extends JpaRepository<MaterialSupplierMoqLineEntity, UUID> {}
