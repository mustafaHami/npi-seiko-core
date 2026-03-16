package my.lokalix.planning.core.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CurrencyRepository extends JpaRepository<CurrencyEntity, UUID> {
  boolean existsByCodeAndArchivedFalse(String code);

  boolean existsByCodeAndCurrencyIdNotAndArchivedFalse(String code, UUID uid);

  Optional<CurrencyEntity> findByCodeAndArchivedFalse(String code);

  @Query(
      """
      SELECT c FROM CurrencyEntity c
      WHERE LOWER(c.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
      AND c.archived = false
      """)
  Page<CurrencyEntity> findBySearchAndArchivedFalse(
      Pageable pageable, @Param("searchText") String searchText);

  @Query(
      """
          SELECT c FROM CurrencyEntity c
          WHERE c.archived = false
          """)
  Page<CurrencyEntity> findByArchivedFalse(Pageable pageable);

  List<CurrencyEntity> findAllByArchivedFalse(Sort sort);

  @Query(
      """
      SELECT COUNT(cr) > 0 FROM CostRequestEntity cr
      WHERE cr.currency = :currency AND cr.archived = false
      """)
  boolean isUsedByNonArchivedCostRequest(@Param("currency") CurrencyEntity currency);

  @Query(
      """
      SELECT COUNT(p) > 0 FROM ProcessEntity p
      WHERE p.currency = :currency AND p.archived = false
      """)
  boolean isUsedByNonArchivedProcess(@Param("currency") CurrencyEntity currency);

  @Query(
      """
      SELECT COUNT(tl) > 0 FROM ToolingCostLineEntity tl
      WHERE tl.currency = :currency AND tl.costRequestLine.costRequest.archived = false
      """)
  boolean isUsedByNonArchivedToolingCostLine(@Param("currency") CurrencyEntity currency);

  @Query(
      """
      SELECT COUNT(ol) > 0 FROM OtherCostLineEntity ol
      WHERE ol.currency = :currency AND ol.costRequestLine.costRequest.archived = false
      """)
  boolean isUsedByNonArchivedOtherCostLine(@Param("currency") CurrencyEntity currency);

  @Query(
      """
      SELECT COUNT(ms) > 0 FROM MaterialSupplierEntity ms
      WHERE ms.purchasingCurrency = :currency AND ms.material.archived = false
      """)
  boolean isUsedByNonArchivedMaterialSupplier(@Param("currency") CurrencyEntity currency);

  @Query(
      """
      SELECT COUNT(sl) > 0 FROM ShipmentLocationEntity sl
      JOIN sl.acceptedCurrencies c
      WHERE c = :currency AND sl.archived = false
      """)
  boolean isUsedByNonArchivedShipmentLocation(@Param("currency") CurrencyEntity currency);

  @Query(
      """
      SELECT COUNT(csl) > 0 FROM CustomerShipmentLocationEntity csl
      WHERE csl.currency = :currency
      """)
  boolean isUsedByNonArchivedCustomerShipmentLocation(@Param("currency") CurrencyEntity currency);
}
