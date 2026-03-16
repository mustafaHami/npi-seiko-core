package my.lokalix.planning.core.repositories.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.admin.MaterialCategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialCategoryRepository extends JpaRepository<MaterialCategoryEntity, UUID> {
  boolean existsByNameIgnoreCaseAndArchivedFalse(String name);

  boolean existsByNameIgnoreCaseAndMaterialCategoryIdNotAndArchivedFalse(String name, UUID uid);

  Optional<MaterialCategoryEntity> findByNameIgnoreCaseAndArchivedFalse(String name);

  Optional<MaterialCategoryEntity> findFirstByNameIgnoreCaseAndArchivedFalse(String name);

  @Query(
      """
      SELECT mc FROM MaterialCategoryEntity mc
      WHERE mc.archived = false
      AND LOWER(mc.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
      """)
  Page<MaterialCategoryEntity> findBySearchAndArchivedFalse(
      Pageable pageable, @Param("searchText") String searchText);

  Page<MaterialCategoryEntity> findByArchivedFalse(Pageable pageable);

  Optional<MaterialCategoryEntity> findByNameIgnoreCaseAndArchivedTrue(String name);

  List<MaterialCategoryEntity> findAllByArchivedFalse(Sort sort);

  @Query(
      """
      SELECT COUNT(m) > 0 FROM MaterialEntity m
      WHERE m.category = :category AND m.archived = false
      """)
  boolean isUsedByNonArchivedMaterial(@Param("category") MaterialCategoryEntity category);

  @Query(
      """
      SELECT COUNT(d) > 0 FROM MaterialLineDraftEntity d
      WHERE d.category = :category AND d.costRequestLine.costRequest.archived = false
      """)
  boolean isUsedByNonArchivedDraftMaterialLine(@Param("category") MaterialCategoryEntity category);
}
