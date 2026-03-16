package my.lokalix.planning.core.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import my.lokalix.planning.core.models.entities.AuthTokenEntity;
import my.lokalix.planning.core.models.enums.ConnectionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AuthTokenRepository extends JpaRepository<AuthTokenEntity, String> {

  @Modifying
  @Query("DELETE FROM AuthTokenEntity a WHERE a.creationDate < :date")
  int deleteAllTokensOlderThan(LocalDate date);

  List<AuthTokenEntity> findAllByUserLoginAndConnectionType(
      String userLogin, ConnectionType connectionType);

  List<AuthTokenEntity> findByUserLogin(String userLogin);

  Optional<AuthTokenEntity> findByToken(String accessToken);
}
