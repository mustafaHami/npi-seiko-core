package my.lokalix.planning.core.repositories.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.admin.ProductNameEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductNameRepository extends JpaRepository<ProductNameEntity, UUID> {
  boolean existsByCodeIgnoreCaseAndArchivedFalse(String code);

  boolean existsByCodeIgnoreCaseAndProductNameIdNotAndArchivedFalse(String code, UUID uid);

  Optional<ProductNameEntity> findByCodeIgnoreCaseAndArchivedFalse(String productNameCode);

  @Query(
      """
      SELECT p FROM ProductNameEntity p
      WHERE LOWER(p.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
      AND p.archived = false
      """)
  Page<ProductNameEntity> findBySearchAndArchivedFalse(
      Pageable pageable, @Param("searchText") String searchText);

  Optional<ProductNameEntity> findByCodeIgnoreCaseAndNameIgnoreCaseAndArchivedTrue(
      String code, String name);

  Page<ProductNameEntity> findByArchivedFalse(Pageable pageable);

  List<ProductNameEntity> findAllByArchivedFalse(Sort sort);

  @Query(
      """
      SELECT COUNT(l) > 0 FROM CostRequestLineEntity l
      WHERE l.productName = :productName AND l.costRequest.archived = false
      """)
  boolean isUsedByNonArchivedCostRequestLine(@Param("productName") ProductNameEntity productName);
}
