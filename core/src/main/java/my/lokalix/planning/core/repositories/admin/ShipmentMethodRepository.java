package my.lokalix.planning.core.repositories.admin;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.admin.ShipmentMethodEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentMethodRepository extends JpaRepository<ShipmentMethodEntity, UUID> {
  Optional<ShipmentMethodEntity> findByShipmentMethodIdAndArchivedFalse(UUID uid);

  @Query(
      """
      SELECT s FROM ShipmentMethodEntity s
      WHERE LOWER(s.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
      AND s.archived = false
      """)
  Page<ShipmentMethodEntity> findBySearchAndArchivedFalse(
      Pageable pageable, @Param("searchText") String searchText);

  Page<ShipmentMethodEntity> findByArchivedFalse(Pageable pageable);

  Optional<ShipmentMethodEntity> findByNameIgnoreCaseAndArchivedFalse(String name);

  Optional<ShipmentMethodEntity> findByNameIgnoreCaseAndArchivedTrue(@NotBlank String name);

  List<ShipmentMethodEntity> findAllByArchivedFalse(Sort sort);

  @Query(
      """
      SELECT COUNT(s) > 0 FROM SupplierManufacturerEntity s
      WHERE s.shipmentMethod = :shipmentMethod AND s.archived = false
      """)
  boolean isUsedByNonArchivedSupplier(@Param("shipmentMethod") ShipmentMethodEntity shipmentMethod);
}
