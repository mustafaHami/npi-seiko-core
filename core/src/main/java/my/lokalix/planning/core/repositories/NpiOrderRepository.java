package my.lokalix.planning.core.repositories;

import java.util.List;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.NpiOrderEntity;
import my.lokalix.planning.core.models.enums.NpiOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NpiOrderRepository extends JpaRepository<NpiOrderEntity, UUID> {

  List<NpiOrderEntity> findAllByArchivedFalse();

  List<NpiOrderEntity> findByArchivedTrue(Sort sort);

  List<NpiOrderEntity> findByArchivedFalseAndStatusNotIn(List<NpiOrderStatus> statuses, Sort sort);

  @Query(
      """
          SELECT n FROM NpiOrderEntity n
          WHERE n.archived = false
          AND LOWER(n.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
          """)
  Page<NpiOrderEntity> findBySearchAndArchivedFalse(
      Pageable pageable, @Param("searchText") String searchText);

  @Query(
      """
          SELECT n FROM NpiOrderEntity n
          WHERE n.archived = false
          """)
  Page<NpiOrderEntity> findAllByArchivedFalse(Pageable pageable);

  @Query(
      """
          SELECT n FROM NpiOrderEntity n
          WHERE n.archived = true
          AND LOWER(n.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
          """)
  Page<NpiOrderEntity> findBySearchAndArchivedTrue(
      Pageable pageable, @Param("searchText") String searchText);

  @Query(
      """
          SELECT n FROM NpiOrderEntity n
          WHERE n.archived = true
          """)
  Page<NpiOrderEntity> findAllByArchivedTrue(Pageable pageable);

  @Query(
      """
          SELECT n FROM NpiOrderEntity n
          WHERE LOWER(n.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
          """)
  Page<NpiOrderEntity> findBySearch(Pageable pageable, @Param("searchText") String searchText);

  @Query(
      """
          SELECT n FROM NpiOrderEntity n
          WHERE n.archived = false
          AND n.status IN :statuses
          AND LOWER(n.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
          """)
  Page<NpiOrderEntity> findBySearchAndArchivedFalseAndStatusIn(
      Pageable pageable,
      @Param("searchText") String searchText,
      @Param("statuses") List<NpiOrderStatus> statuses);

  @Query(
      """
          SELECT n FROM NpiOrderEntity n
          WHERE n.archived = false
          AND n.status IN :statuses
          """)
  Page<NpiOrderEntity> findAllByArchivedFalseAndStatusIn(
      Pageable pageable, @Param("statuses") List<NpiOrderStatus> statuses);
}
