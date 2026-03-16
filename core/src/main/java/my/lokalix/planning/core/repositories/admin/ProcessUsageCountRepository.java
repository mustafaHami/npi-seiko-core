package my.lokalix.planning.core.repositories.admin;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.admin.ProcessEntity;
import my.lokalix.planning.core.models.entities.admin.ProcessUsageCountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessUsageCountRepository extends JpaRepository<ProcessUsageCountEntity, UUID> {
  @Modifying
  @Query(
      """
      UPDATE ProcessUsageCountEntity p
      SET p.usageCount = p.usageCount + 1
      WHERE p.process = :process
      """)
  void incrementProcessUsageCount(@NotNull @Param("process") ProcessEntity process);

  @Query(
      """
      SELECT p.process FROM ProcessUsageCountEntity p
      WHERE p.process.archived = false
      AND p.usageCount > 0
      ORDER BY p.usageCount DESC, p.process.name ASC
      """)
  List<ProcessEntity> findProcessesOrderedByUsageCountDesc();
}
