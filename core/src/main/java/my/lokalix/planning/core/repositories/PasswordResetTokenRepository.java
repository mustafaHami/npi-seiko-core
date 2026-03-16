package my.lokalix.planning.core.repositories;

import java.time.OffsetDateTime;
import my.lokalix.planning.core.models.entities.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PasswordResetTokenRepository
    extends JpaRepository<PasswordResetTokenEntity, String> {

  @Modifying
  @Query("DELETE FROM PasswordResetTokenEntity a WHERE a.creationDate < :date")
  int deleteAllTokensOlderThan(OffsetDateTime date);
}
