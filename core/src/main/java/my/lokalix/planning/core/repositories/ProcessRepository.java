package my.lokalix.planning.core.repositories;

import java.util.List;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.ProcessEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessRepository extends JpaRepository<ProcessEntity, UUID> {

  List<ProcessEntity> findAllByOrderByCreationDateAsc();
}
