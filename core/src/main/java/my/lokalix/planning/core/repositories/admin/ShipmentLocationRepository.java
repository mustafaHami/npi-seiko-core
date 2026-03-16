package my.lokalix.planning.core.repositories.admin;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.admin.ShipmentLocationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentLocationRepository extends JpaRepository<ShipmentLocationEntity, UUID> {

  boolean existsByNameIgnoreCaseAndArchivedFalse(String name);

  boolean existsByNameIgnoreCaseAndShipmentLocationIdNotAndArchivedFalse(String name, UUID uid);

  List<ShipmentLocationEntity> findAllByArchivedFalse(Sort sort);

  List<ShipmentLocationEntity> findAllByArchivedFalse();

  Page<ShipmentLocationEntity> findByArchivedFalse(Pageable pageable);

  @Query(
      """
      SELECT d FROM ShipmentLocationEntity d
      WHERE LOWER(d.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
      AND d.archived = false
      """)
  Page<ShipmentLocationEntity> findBySearchAndArchivedFalse(
      Pageable pageable, @Param("searchText") String searchText);

  @Query(
      """
          SELECT COUNT(cr) > 0 FROM OtherCostLineEntity cr
          WHERE cr.shipmentLocation = :entity
          """)
  boolean isUsedByNonArchivedOtherCostLine(ShipmentLocationEntity entity);

  @Query(
      """
              SELECT COUNT(csl) > 0 FROM CustomerShipmentLocationEntity csl
              WHERE csl.shipmentLocation = :entity
              """)
  boolean isUsedByNonArchivedCustomer(ShipmentLocationEntity entity);

  Optional<ShipmentLocationEntity> findByNameIgnoreCaseAndArchivedTrue(@NotBlank String name);

  Optional<ShipmentLocationEntity> findByNameIgnoreCaseAndArchivedFalse(@NotBlank String name);

  ShipmentLocationEntity findFirstByArchivedFalse();

  Optional<ShipmentLocationEntity> findFirstBy();
}
