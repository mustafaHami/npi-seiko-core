package my.lokalix.planning.core.repositories;

import java.util.Optional;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.admin.GlobalConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalConfigRepository extends JpaRepository<GlobalConfigEntity, UUID> {
  Optional<GlobalConfigEntity> findFirstBy();
}
