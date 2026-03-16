package my.lokalix.planning.core.repositories.admin;

import java.util.List;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.admin.BomConfigurationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BomConfigurationRepository extends JpaRepository<BomConfigurationEntity, UUID> {

  List<BomConfigurationEntity> findAllByArchivedFalse();

  Page<BomConfigurationEntity> findByArchivedFalse(Pageable pageable);

  boolean existsByNameIgnoreCaseAndArchivedFalse(String name);

  boolean existsByNameIgnoreCaseAndBomConfigurationIdNotAndArchivedFalse(
      String name, UUID bomConfigurationId);

  @Query(
      """
      SELECT bc FROM BomConfigurationEntity bc
      WHERE LOWER(bc.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
      AND bc.archived = false
      """)
  Page<BomConfigurationEntity> findBySearchAndArchivedFalse(
      Pageable pageable, @Param("searchText") String searchText);
}
