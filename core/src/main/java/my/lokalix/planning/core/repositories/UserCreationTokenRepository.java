package my.lokalix.planning.core.repositories;

import java.time.OffsetDateTime;
import my.lokalix.planning.core.models.entities.UserCreationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserCreationTokenRepository
    extends JpaRepository<UserCreationTokenEntity, String> {

  @Modifying
  @Query("DELETE FROM UserCreationTokenEntity a WHERE a.creationDate < :date")
  int deleteAllTokensOlderThan(OffsetDateTime date);
}
