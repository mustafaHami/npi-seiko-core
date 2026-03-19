package my.lokalix.planning.core.repositories;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.CustomerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {
  boolean existsByCodeIgnoreCaseAndArchivedFalse(String code);

  boolean existsByCodeIgnoreCaseAndCustomerIdNotAndArchivedFalse(String code, UUID uid);

  Optional<CustomerEntity> findByCodeIgnoreCaseAndArchivedFalse(String customerCode);

  @Query(
      """
      SELECT c FROM CustomerEntity c
      WHERE LOWER(c.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
      AND c.archived = false
      """)
  Page<CustomerEntity> findBySearchAndArchivedFalse(
      Pageable pageable, @Param("searchText") String searchText);

  Page<CustomerEntity> findByArchivedFalse(Pageable pageable);

  boolean existsByNameIgnoreCaseAndArchivedFalse(@NotNull String name);

  boolean existsByNameIgnoreCaseAndCustomerIdNotAndArchivedFalse(@NotNull String name, UUID uid);

  List<CustomerEntity> findAllByArchivedFalse(Sort sort);

  List<CustomerEntity> findAllByCustomerIdInAndArchivedFalse(List<UUID> customerIds, Sort sort);

  @Query(
      """
      SELECT COUNT(cr) > 0
      FROM NpiOrderEntity cr
      WHERE cr.customer = :customer AND cr.archived = false
      """)
  boolean isUsedByNonArchivedCostRequest(@Param("customer") CustomerEntity customer);
}
