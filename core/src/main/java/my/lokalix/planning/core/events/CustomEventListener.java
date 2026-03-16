package my.lokalix.planning.core.events;

import static my.lokalix.planning.core.models.enums.UserRole.SUPER_ADMINISTRATOR;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.models.entities.UserEntity;
import my.lokalix.planning.core.models.enums.UserType;
import my.lokalix.planning.core.repositories.GlobalConfigRepository;
import my.lokalix.planning.core.repositories.UserRepository;
import my.lokalix.planning.core.services.GlobalConfigService;
import my.lokalix.planning.core.services.UserService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomEventListener {

  @Resource private UserService userService;
  @Resource private AppConfigurationProperties appConfigurationProperties;
  @Resource private BCryptPasswordEncoder encoder;
  @Resource private UserRepository userRepository;
  @Resource private GlobalConfigRepository globalConfigRepository;
  @Resource private GlobalConfigService globalConfigService;

  @EventListener(ApplicationReadyEvent.class)
  public void afterStartup() {
    createGlobalConfigIfNotExist();
    createSuperAdminUserIfNotExist();
    log.info("Application successfully started...");
  }

  private void createGlobalConfigIfNotExist() {
    if (globalConfigRepository.findFirstBy().isEmpty()) {
      globalConfigRepository.save(globalConfigService.buildDefaultGlobalConfig());
    }
  }

  private void createSuperAdminUserIfNotExist() {
    String superAdminLogin =
        appConfigurationProperties.getSuperAdminLogin()
            + appConfigurationProperties.getUsernameSuffixForTestsOnly();
    if (!userService.existsByLoginIgnoreCase(superAdminLogin)) {
      UserEntity user = new UserEntity();
      user.setLogin(superAdminLogin);
      user.setPassword(encoder.encode(appConfigurationProperties.getSuperAdminPassword()));
      user.setRole(SUPER_ADMINISTRATOR);
      user.setType(UserType.USERNAME);
      user.setActive(true);
      userRepository.save(user);
    }
    String superAdmin2Login =
        appConfigurationProperties.getSuperAdminLogin()
            + "2"
            + appConfigurationProperties.getUsernameSuffixForTestsOnly();
    if (!userService.existsByLoginIgnoreCase(superAdmin2Login)) {
      UserEntity user = new UserEntity();
      user.setLogin(superAdmin2Login);
      user.setPassword(encoder.encode(appConfigurationProperties.getSuperAdminPassword()));
      user.setRole(SUPER_ADMINISTRATOR);
      user.setType(UserType.USERNAME);
      user.setActive(true);
      userRepository.save(user);
    }
  }
}
