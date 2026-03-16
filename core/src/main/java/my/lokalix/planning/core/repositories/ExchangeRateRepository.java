package my.lokalix.planning.core.repositories;

import java.util.Optional;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.lokalix.planning.core.models.entities.admin.ExchangeRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, UUID> {
  Optional<ExchangeRateEntity> findByFromCurrencyAndToCurrency(
      CurrencyEntity fromCurrency, CurrencyEntity toCurrency);
}
