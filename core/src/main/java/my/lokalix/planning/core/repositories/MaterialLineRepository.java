package my.lokalix.planning.core.repositories;

import java.util.List;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.MaterialEntity;
import my.lokalix.planning.core.models.entities.MaterialLineEntity;
import my.lokalix.planning.core.models.entities.MaterialSupplierEntity;
import my.lokalix.planning.core.models.enums.CostRequestStatus;
import my.lokalix.planning.core.models.enums.MaterialStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialLineRepository extends JpaRepository<MaterialLineEntity, UUID> {
  List<MaterialLineEntity> findByMaterial(MaterialEntity material);

  List<MaterialLineEntity> findByChosenMaterialSupplier(MaterialSupplierEntity materialSupplier);

  // --- Dashboard queries ---

  @Query(
      """
      SELECT COUNT(ml) FROM MaterialLineEntity ml
      WHERE ml.material.status = :materialStatus
      AND ml.isSubstituteMaterial = false
      AND ml.costRequestLine.costRequest.archived = false
      AND ml.costRequestLine.costRequest.status NOT IN :excludedStatuses
      """)
  long dashboardCountByMaterialStatusAndActiveCostRequest(
      @Param("materialStatus") MaterialStatus materialStatus,
      @Param("excludedStatuses") List<CostRequestStatus> excludedStatuses);
}
