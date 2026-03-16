package my.lokalix.planning.core.repositories;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.MaterialEntity;
import my.lokalix.planning.core.models.entities.admin.MaterialCategoryEntity;
import my.lokalix.planning.core.models.entities.admin.SupplierManufacturerEntity;
import my.lokalix.planning.core.models.enums.MaterialStatus;
import my.lokalix.planning.core.models.enums.MaterialType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialRepository extends JpaRepository<MaterialEntity, UUID> {

  @Query(
      """
        SELECT m FROM MaterialEntity m
        WHERE m.archived = false
          AND (:materialType IS NULL OR m.materialType = :materialType)
    """)
  Page<MaterialEntity> findByArchivedFalseAndOptionalMaterialType(
      Pageable pageable, @Param("materialType") MaterialType materialType);

  @Query(
      """
              SELECT m FROM MaterialEntity m
              WHERE m.archived = false
              AND LOWER(m.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
              AND (:materialType IS NULL OR m.materialType = :materialType)
              """)
  Page<MaterialEntity> findBySearchAndArchivedFalse(
      Pageable pageable,
      @Param("searchText") String searchText,
      @Param("materialType") MaterialType materialType);

  @Query(
      """
        SELECT m FROM MaterialEntity m
        WHERE m.archived = false
          AND m.status IN :statuses
          AND (:materialType IS NULL OR m.materialType = :materialType)
    """)
  Page<MaterialEntity> findByArchivedFalseAndStatusInAndOptionalType(
      Pageable pageable,
      @Param("statuses") List<MaterialStatus> statuses,
      @Param("materialType") MaterialType materialType);

  @Query(
      """
              SELECT m FROM MaterialEntity m
              WHERE m.archived = false
              AND m.status IN :statuses
              AND (:materialType is null or m.materialType = :materialType)
              AND LOWER(m.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
              """)
  Page<MaterialEntity> findBySearchAndArchivedFalseAndStatusInAndType(
      Pageable pageable,
      @Param("searchText") String searchText,
      @Param("statuses") List<MaterialStatus> statuses,
      @Param("materialType") MaterialType materialType);

  Optional<MaterialEntity> findBySystemIdAndArchivedFalse(String systemId);

  @Query(
      """
          SELECT DISTINCT m FROM MaterialEntity m
          LEFT JOIN FETCH m.suppliers
          WHERE m.systemId IN :systemIds AND m.archived = false
          """)
  List<MaterialEntity> findBySystemIdInWithSuppliers(@Param("systemIds") Set<String> systemIds);

  @Query(
      """
              SELECT m.systemId FROM MaterialEntity m
              WHERE m.systemId LIKE CONCAT(:prefix, '%')
              ORDER BY m.systemId DESC
              """)
  List<String> findHighestSystemIdByPrefix(@Param("prefix") String prefix, Pageable pageable);

  boolean existsByManufacturerAndManufacturerPartNumberAndArchivedFalse(
      SupplierManufacturerEntity manufacturer, @NotNull String manufacturerPartNumber);

  Optional<MaterialEntity> findByManufacturerAndManufacturerPartNumberAndArchivedFalse(
      SupplierManufacturerEntity manufacturer, String manufacturerPartNumber);

  @Query(
      """
      SELECT m FROM MaterialEntity m
      WHERE m.archived = false
        AND m.manufacturer = :manufacturer
        AND m.category = :category
        AND LOWER(m.manufacturerPartNumber) = LOWER(:manufacturerPartNumber)
      """)
  List<MaterialEntity> findFirstByManufacturerAndCategoryAndPartNumberAndArchivedFalse(
      @Param("manufacturer") SupplierManufacturerEntity manufacturer,
      @Param("category") MaterialCategoryEntity category,
      @Param("manufacturerPartNumber") String manufacturerPartNumber);

  @Query(
      """
      SELECT m FROM MaterialEntity m
      WHERE m.archived = false
        AND m.manufacturer = :manufacturer
        AND LOWER(m.manufacturerPartNumber) = LOWER(:manufacturerPartNumber)
      """)
  List<MaterialEntity> findFirstByManufacturerAndPartNumberAndArchivedFalse(
      @Param("manufacturer") SupplierManufacturerEntity manufacturer,
      @Param("manufacturerPartNumber") String manufacturerPartNumber);

  @Query(
      """
      SELECT m FROM MaterialEntity m
      WHERE m.archived = false
        AND m.manufacturer is null
        AND LOWER(m.draftManufacturerName) = LOWER(:draftManufacturerName)
        AND LOWER(m.manufacturerPartNumber) = LOWER(:manufacturerPartNumber)
      """)
  List<MaterialEntity> findFirstByDraftManufacturerNameAndPartNumberAndArchivedFalse(
      @Param("draftManufacturerName") String draftManufacturerName,
      @Param("manufacturerPartNumber") String manufacturerPartNumber);

  @Query(
      """
      SELECT m FROM MaterialEntity m
      WHERE m.archived = false
        AND m.manufacturer IS NULL
        AND LOWER(m.draftManufacturerName) = LOWER(:draftManufacturerName)
      """)
  List<MaterialEntity> findByManufacturerNullAndDraftManufacturerNameIgnoreCaseAndArchivedFalse(
      @Param("draftManufacturerName") String draftManufacturerName);

  @Query(
      """
      SELECT COUNT(ml) > 0 FROM MaterialLineEntity ml
      WHERE ml.material = :material AND ml.costRequestLine.costRequest.archived = false
      """)
  boolean isUsedByNonArchivedMaterialLine(@Param("material") MaterialEntity material);

  @Query(
      """
              SELECT m FROM MaterialEntity m
              WHERE m.manufacturer = :manufacturer
              AND m.archived = false
              AND m.materialType = :materialType
              AND (:manufacturer is NULL OR LOWER(m.manufacturerPartNumber) LIKE LOWER(CONCAT('%', :partNumber, '%')))
              """)
  List<MaterialEntity> findByManufacturerAndPartNumberContaining(
      @Param("manufacturer") SupplierManufacturerEntity manufacturer,
      @Param("partNumber") String partNumber,
      @Param("materialType") MaterialType materialType);

  List<MaterialEntity> findByCategoryNullAndDraftCategoryNameIgnoreCaseAndArchivedFalse(
      @NotBlank String name);

  List<MaterialEntity> findByUnitNullAndDraftUnitNameIgnoreCaseAndArchivedFalse(
      @NotBlank String name);
}
