package my.lokalix.planning.core.repositories;

import java.util.List;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.NpiOrderEntity;
import my.lokalix.planning.core.models.entities.ProcessLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessLineRepository extends JpaRepository<ProcessLineEntity, UUID> {

  List<ProcessLineEntity> findAllByNpiOrderOrderByIndexIdAsc(NpiOrderEntity npiOrder);
}
