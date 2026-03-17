package my.lokalix.planning.core.repositories;

import java.util.List;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.NpiOrderEntity;
import my.lokalix.planning.core.models.entities.ProcessLineEntity;
import my.lokalix.planning.core.models.enums.NpiOrderStatus;
import my.lokalix.planning.core.models.enums.ProcessLineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessLineRepository extends JpaRepository<ProcessLineEntity, UUID> {

  List<ProcessLineEntity> findAllByNpiOrderOrderByIndexIdAsc(NpiOrderEntity npiOrder);

  @Query(
      """
          SELECT COUNT(pl) FROM ProcessLineEntity pl
          WHERE pl.isCustomerApproval = true
          AND pl.status = :status
          AND pl.npiOrder.archived = false
          AND pl.npiOrder.status NOT IN :finalStatuses
          """)
  long countCustomerApprovalByStatus(
      @Param("status") ProcessLineStatus status,
      @Param("finalStatuses") List<NpiOrderStatus> finalStatuses);

  @Query(
      """
          SELECT pl.processName, pl.status, COUNT(pl) FROM ProcessLineEntity pl
          WHERE pl.npiOrder.archived = false
          AND pl.npiOrder.status NOT IN :finalStatuses
          AND pl.status IN :activeStatuses
          GROUP BY pl.processName, pl.status
          ORDER BY pl.processName
          """)
  List<Object[]> dashboardCountProcessLinesByProcessNameAndStatus(
      @Param("finalStatuses") List<NpiOrderStatus> finalStatuses,
      @Param("activeStatuses") List<ProcessLineStatus> activeStatuses);
}
