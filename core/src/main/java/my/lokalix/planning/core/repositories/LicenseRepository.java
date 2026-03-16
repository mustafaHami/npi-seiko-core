package my.lokalix.planning.core.repositories;

import my.lokalix.planning.core.models.entities.LicenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LicenseRepository extends JpaRepository<LicenseEntity, String> {

  @Query("SELECT l.allowedNumberOfActiveUsers FROM LicenseEntity l")
  String findCurrentLicenseMaxNumberOfActiveUsers();
}
