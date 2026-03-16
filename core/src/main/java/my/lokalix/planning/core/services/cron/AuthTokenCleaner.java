package my.lokalix.planning.core.services.cron;

import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.repositories.AuthTokenRepository;
import my.lokalix.planning.core.utils.TimeUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthTokenCleaner {

  @Resource private AuthTokenRepository authTokenRepository;

  @Transactional
  @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Singapore")
  public void purgeOldAuthTokens() {
    LocalDate today = TimeUtils.nowLocalDate("Asia/Singapore");
    int nbOfDeletedTokens = authTokenRepository.deleteAllTokensOlderThan(today.minusDays(7));
    log.info("Deleted {} old authentication tokens", nbOfDeletedTokens);
  }
}
