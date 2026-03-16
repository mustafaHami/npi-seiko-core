package my.lokalix.planning.core.repositories.admin;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.admin.ProcessEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessRepository extends JpaRepository<ProcessEntity, UUID> {
  boolean existsByNameIgnoreCaseAndArchivedFalse(String code);

  boolean existsByNameIgnoreCaseAndProcessIdNotAndArchivedFalse(String code, UUID uid);

  @Query(
      """
      SELECT p FROM ProcessEntity p
      WHERE p.archived = false
      AND LOWER(p.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
      """)
  Page<ProcessEntity> findBySearchAndArchivedFalse(
      Pageable pageable, @Param("searchText") String searchText);

  Page<ProcessEntity> findByArchivedFalse(Pageable pageable);

  Optional<ProcessEntity> findByNameIgnoreCaseAndArchivedFalse(@NotBlank String name);

  Optional<ProcessEntity> findFirstBySetupProcessTrueAndArchivedFalse();

  List<ProcessEntity> findAllByArchivedFalse(Sort sort);

  @Query(
      """
      SELECT COUNT(pl) > 0 FROM ProcessLineEntity pl
      WHERE pl.process = :process AND pl.costRequestLine.costRequest.archived = false
      """)
  boolean isUsedByNonArchivedProcessLine(@Param("process") ProcessEntity process);
}
