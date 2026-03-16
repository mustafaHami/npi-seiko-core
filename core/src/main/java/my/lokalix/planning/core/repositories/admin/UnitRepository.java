package my.lokalix.planning.core.repositories.admin;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.admin.UnitEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnitRepository extends JpaRepository<UnitEntity, UUID> {
  boolean existsByNameIgnoreCaseAndArchivedFalse(String name);

  boolean existsByNameIgnoreCaseAndUnitIdNotAndArchivedFalse(String name, UUID uid);

  @Query(
      """
      SELECT u FROM UnitEntity u
      WHERE LOWER(u.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
      AND u.archived = false
      """)
  Page<UnitEntity> findBySearchAndArchivedFalse(
      Pageable pageable, @Param("searchText") String searchText);

  Page<UnitEntity> findByArchivedFalse(Pageable pageable);

  Optional<UnitEntity> findByNameIgnoreCaseAndArchivedTrue(@NotBlank String name);

  Optional<UnitEntity> findByNameIgnoreCaseAndArchivedFalse(@NotBlank String name);

  Optional<UnitEntity> findFirstByNameIgnoreCaseAndArchivedFalse(String name);

  List<UnitEntity> findAllByArchivedFalse(Sort name);

  @Query(
      """
      SELECT COUNT(m) > 0 FROM MaterialEntity m
      WHERE m.unit = :unit AND m.archived = false
      """)
  boolean isUsedByNonArchivedMaterial(@Param("unit") UnitEntity unit);

  @Query(
      """
      SELECT COUNT(d) > 0 FROM MaterialLineDraftEntity d
      WHERE d.unit = :unit AND d.costRequestLine.costRequest.archived = false
      """)
  boolean isUsedByNonArchivedDraftMaterialLine(@Param("unit") UnitEntity unit);
}
