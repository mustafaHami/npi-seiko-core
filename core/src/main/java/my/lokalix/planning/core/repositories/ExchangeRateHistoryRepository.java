package my.lokalix.planning.core.repositories;

import java.util.List;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.admin.ExchangeRateHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExchangeRateHistoryRepository
    extends JpaRepository<ExchangeRateHistoryEntity, UUID> {

  @Query(
      """
      SELECT h FROM ExchangeRateHistoryEntity h
      WHERE h.fromCurrency = :fromCurrency
      ORDER BY h.changeDate DESC
      """)
  List<ExchangeRateHistoryEntity> findByFromCurrencyOrderByChangeDateDesc(
      @Param("fromCurrency") String fromCurrency);
}
