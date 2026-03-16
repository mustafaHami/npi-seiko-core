package my.lokalix.planning.core.repositories.admin;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.admin.ShipmentMethodEntity;
import my.lokalix.planning.core.models.entities.admin.SupplierManufacturerEntity;
import my.lokalix.planning.core.models.enums.SupplierAndManufacturerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierAndManufacturerRepository
    extends JpaRepository<SupplierManufacturerEntity, UUID> {
  boolean existsByCodeIgnoreCaseAndArchivedFalse(String code);

  Optional<SupplierManufacturerEntity> findByTypeInAndNameIgnoreCaseAndArchivedFalse(
      List<SupplierAndManufacturerType> manufacturerTypes, @NotNull String manufacturerName);

  Optional<SupplierManufacturerEntity> findFirstByTypeInAndNameIgnoreCaseAndArchivedFalse(
      List<SupplierAndManufacturerType> types, String name);

  Optional<SupplierManufacturerEntity> findFirstByTypeInAndCodeIgnoreCaseAndArchivedFalse(
      List<SupplierAndManufacturerType> types, String code);

  Optional<SupplierManufacturerEntity>
      findByTypeInAndCodeIgnoreCaseAndNameIgnoreCaseAndArchivedTrueAndShipmentMethod(
          List<SupplierAndManufacturerType> types,
          String code,
          String name,
          ShipmentMethodEntity archivedShipmentMethod);

  @Query(
      """
      SELECT m FROM SupplierManufacturerEntity m
      WHERE LOWER(m.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
      AND m.archived = false
      """)
  Page<SupplierManufacturerEntity> findBySearchAndArchivedFalse(
      Pageable pageable, @Param("searchText") String searchText);

  Optional<SupplierManufacturerEntity>
      findByTypeInAndCodeIgnoreCaseAndNameIgnoreCaseAndArchivedTrue(
          List<SupplierAndManufacturerType> types, String code, String name);

  Optional<SupplierManufacturerEntity>
      findByTypeInAndNameIgnoreCaseAndArchivedTrueAndShipmentMethod(
          List<SupplierAndManufacturerType> types,
          String name,
          ShipmentMethodEntity archivedShipmentMethod);

  Page<SupplierManufacturerEntity> findByArchivedFalse(Pageable pageable);

  List<SupplierManufacturerEntity> findAllByTypeInAndArchivedFalse(
      List<SupplierAndManufacturerType> types, Sort sort);

  Optional<SupplierManufacturerEntity> findByTypeInAndCodeIgnoreCaseAndArchivedFalse(
      List<SupplierAndManufacturerType> types, @NotNull String code);

  @Query(
      """
      SELECT COUNT(m) > 0 FROM MaterialEntity m
      WHERE m.manufacturer = :manufacturer AND m.archived = false
      """)
  boolean isUsedByNonArchivedMaterial(
      @Param("manufacturer") SupplierManufacturerEntity manufacturer);

  @Query(
      """
      SELECT COUNT(d) > 0 FROM MaterialLineDraftEntity d
      WHERE d.manufacturer = :manufacturer AND d.costRequestLine.costRequest.archived = false
      """)
  boolean isUsedByNonArchivedDraftMaterialLine(
      @Param("manufacturer") SupplierManufacturerEntity manufacturer);

  @Query(
      """
          SELECT COUNT(ms) > 0 FROM MaterialSupplierEntity ms
          WHERE ms.supplier = :supplier AND ms.material.archived = false
          """)
  boolean isSupplierUsedByNonArchivedMaterial(
      @Param("supplier") SupplierManufacturerEntity supplier);

  boolean existsByNameIgnoreCaseAndArchivedFalse(@NotNull String name);

  Optional<SupplierManufacturerEntity> findByNameIgnoreCaseAndArchivedFalse(@NotNull String name);

  @Query(
      """
      SELECT m.code FROM SupplierManufacturerEntity m
      WHERE m.code LIKE 'SP%'
      ORDER BY m.code DESC
      """)
  List<String> findHighestSpCode(Pageable pageable);

  boolean existsByCodeIgnoreCaseAndSupplierManufacturerIdNotAndArchivedFalse(
      @NotNull String code, UUID uid);

  boolean existsByNameIgnoreCaseAndSupplierManufacturerIdNotAndArchivedFalse(
      @NotNull String name, UUID uid);

  Optional<SupplierManufacturerEntity>
      findFirstByTypeInAndNameIgnoreCaseAndCodeIgnoreCaseAndArchivedFalse(
          List<SupplierAndManufacturerType> supplier, String name, String code);
}
