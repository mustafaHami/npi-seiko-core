package my.lokalix.planning.core.services;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.exceptions.ShouldNeverHappenException;
import my.lokalix.planning.core.exceptions.user.CannotDeactivateOneselfException;
import my.lokalix.planning.core.exceptions.user.LicenseLimitExceededException;
import my.lokalix.planning.core.exceptions.user.UserInvalidTokenException;
import my.lokalix.planning.core.mappers.UserMapper;
import my.lokalix.planning.core.models.entities.AuthTokenEntity;
import my.lokalix.planning.core.models.entities.PasswordResetTokenEntity;
import my.lokalix.planning.core.models.entities.UserCreationTokenEntity;
import my.lokalix.planning.core.models.entities.UserEntity;
import my.lokalix.planning.core.models.enums.ConnectionType;
import my.lokalix.planning.core.models.enums.DisconnectionReasonType;
import my.lokalix.planning.core.models.enums.UserType;
import my.lokalix.planning.core.repositories.AuthTokenRepository;
import my.lokalix.planning.core.repositories.PasswordResetTokenRepository;
import my.lokalix.planning.core.repositories.UserCreationTokenRepository;
import my.lokalix.planning.core.repositories.UserRepository;
import my.lokalix.planning.core.security.JwtUtil;
import my.lokalix.planning.core.security.LoggedUserDetailsService;
import my.lokalix.planning.core.utils.TimeUtils;
import my.zkonsulting.planning.generated.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class UserService {

  private final EmailService emailService;
  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final AuthTokenRepository authTokenRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final BCryptPasswordEncoder encoder;
  private final AppConfigurationProperties appConfigurationProperties;
  private final UserCreationTokenRepository userCreationTokenRepository;
  private final LicenseService licenseService;
  private final LoggedUserDetailsService loggedUserDetailsService;

  @Transactional
  public SWLoggedUser login(@Valid SWLoginDetails body, ConnectionType connectionType) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(body.getLogin(), body.getPassword()));
    String login = authentication.getName();
    UserEntity user =
        userRepository.findByLoginIgnoreCase(login).orElseThrow(EntityNotFoundException::new);
    invalidateUserTokens(
        user.getLogin(), connectionType, DisconnectionReasonType.CONNECTED_OTHER_DEVICE);
    String token = jwtUtil.createToken(user);
    AuthTokenEntity authTokenEntity = new AuthTokenEntity();
    authTokenEntity.setConnectionType(connectionType);
    authTokenEntity.setUserLogin(user.getLogin());
    authTokenEntity.setToken(token);
    authTokenEntity.setCreationDate(
        TimeUtils.nowLocalDate(appConfigurationProperties.getAppTimezone()));
    authTokenRepository.save(authTokenEntity);
    return new SWLoggedUser()
        .uid(user.getUserId())
        .login(login)
        .active(user.isActive())
        .role(
            authentication.getAuthorities().stream()
                .map(
                    grantedAuthority ->
                        SWUserRole.fromValue(
                            grantedAuthority.getAuthority().replaceFirst("ROLE_", "")))
                .findFirst()
                .orElseThrow(EntityNotFoundException::new))
        .jwtToken(token);
  }

  public void invalidateUserTokens(
      String userLogin, ConnectionType type, DisconnectionReasonType cause) {
    List<AuthTokenEntity> authTokens =
        authTokenRepository.findAllByUserLoginAndConnectionType(userLogin, type);
    if (CollectionUtils.isNotEmpty(authTokens)) {
      for (AuthTokenEntity authToken : authTokens) {
        authToken.setTokenValid(false);
        authToken.setDisconnectionReasonType(cause);
        authTokenRepository.save(authToken);
      }
      log.info("{} previous session(s) invalid for {}", authTokens.size(), userLogin);
    }
  }

  @Transactional
  public SWUsersPaginated searchUsers(
      int offset, int limit, SWActiveFilter activeFilter, SWBasicSearch search) {
    Sort sort = Sort.by(Sort.Direction.ASC, "login");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);
    Page<UserEntity> paginatedUsers;
    if (StringUtils.isBlank(search.getSearchText()) && activeFilter.equals(SWActiveFilter.ALL)) {
      paginatedUsers =
          userRepository.findAllExceptTestOnlyAccounts(
              "%" + appConfigurationProperties.getUsernameSuffixForTestsOnly(), pageable);
    } else if (StringUtils.isBlank(search.getSearchText())) {
      paginatedUsers =
          userRepository.findByActiveExceptTestOnlyAccounts(
              pageable,
              activeFilter.equals(SWActiveFilter.ACTIVE_ONLY),
              "%" + appConfigurationProperties.getUsernameSuffixForTestsOnly());
    } else if (activeFilter.equals(SWActiveFilter.ALL)) {
      paginatedUsers =
          userRepository.findBySearchExceptTestOnlyAccounts(
              pageable,
              search.getSearchText(),
              "%" + appConfigurationProperties.getUsernameSuffixForTestsOnly());
    } else {
      paginatedUsers =
          userRepository.findByActiveAndSearchExceptTestOnlyAccounts(
              pageable,
              search.getSearchText(),
              activeFilter.equals(SWActiveFilter.ACTIVE_ONLY),
              "%" + appConfigurationProperties.getUsernameSuffixForTestsOnly());
    }
    return populatePaginatedResults(paginatedUsers);
  }

  private SWUsersPaginated populatePaginatedResults(Page<UserEntity> paginatedUsers) {
    SWUsersPaginated usersPaginated = new SWUsersPaginated();
    usersPaginated.setResults(userMapper.toListSwUser(paginatedUsers.getContent()));
    usersPaginated.setPage(paginatedUsers.getNumber());
    usersPaginated.setPerPage(paginatedUsers.getSize());
    usersPaginated.setTotal((int) paginatedUsers.getTotalElements());
    usersPaginated.setHasPrev(paginatedUsers.hasPrevious());
    usersPaginated.hasNext(paginatedUsers.hasNext());
    return usersPaginated;
  }

  public boolean existsByLoginIgnoreCase(String login) {
    return userRepository.existsByLoginIgnoreCase(login);
  }

  public boolean existsRegisteredEmailUserByEmailIgnoreCase(String email) {
    return userRepository.existsByTypeAndRegisteredEmailTrueAndLoginIgnoreCase(
        UserType.EMAIL_ADDRESS, email);
  }

  @Transactional
  public void manageUserForgotPasswordRequest(String email) {
    if (existsRegisteredEmailUserByEmailIgnoreCase(email)) {
      String token = UUID.randomUUID().toString();
      PasswordResetTokenEntity tokenEntity = new PasswordResetTokenEntity();
      tokenEntity.setToken(token);
      tokenEntity.setEmail(email.toLowerCase());
      tokenEntity.setCreationDate(TimeUtils.nowOffsetDateTimeUTC());
      passwordResetTokenRepository.save(tokenEntity);
      emailService.sendPasswordResetEmail(email, token);
    }
  }

  @Transactional
  public void resetUserPassword(SWUserResetPassword body) throws UserInvalidTokenException {
    PasswordResetTokenEntity passwordResetTokenEntity =
        passwordResetTokenRepository
            .findById(body.getToken())
            .orElseThrow(() -> new UserInvalidTokenException("Invalid reset password token"));
    checkTokenIsValid(passwordResetTokenEntity.getCreationDate(), 30);
    UserEntity user =
        userRepository
            .findByLoginIgnoreCase(passwordResetTokenEntity.getEmail())
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
    user.setPassword(encoder.encode(body.getPassword()));
    userRepository.save(user);
    passwordResetTokenRepository.delete(passwordResetTokenEntity);
  }

  @Transactional
  public void setUserPassword(SWUserResetPassword body) throws UserInvalidTokenException {
    UserCreationTokenEntity userCreationTokenEntity =
        userCreationTokenRepository
            .findById(body.getToken())
            .orElseThrow(() -> new UserInvalidTokenException("Invalid account creation token"));
    checkTokenIsValid(userCreationTokenEntity.getCreationDate(), 1440); // 24h
    UserEntity user =
        userRepository
            .findByLoginIgnoreCase(userCreationTokenEntity.getEmail())
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
    user.setPassword(encoder.encode(body.getPassword()));
    user.setRegisteredEmail(true);
    userRepository.save(user);
    userCreationTokenRepository.delete(userCreationTokenEntity);
  }

  private void checkTokenIsValid(OffsetDateTime tokenCreation, long minutes)
      throws UserInvalidTokenException {
    if (tokenCreation.isBefore(TimeUtils.nowOffsetDateTimeUTC().minusMinutes(minutes))) {
      throw new UserInvalidTokenException("Your token has expired");
    }
  }

  public UserEntity getUserById(UUID uid) {
    return userRepository.findById(uid).orElseThrow(EntityNotFoundException::new);
  }

  public UserEntity getUserByLogin(String login) {
    return userRepository.findByLoginIgnoreCase(login).orElseThrow(EntityNotFoundException::new);
  }

  public SWUser retrieveUser(UUID uid) {
    return userMapper.toSwUser(getUserById(uid));
  }

  @Transactional
  public void resendUserCreationEmail(UUID uid) throws BadRequestException {
    UserEntity user = getUserById(uid);
    if (!user.getType().equals(UserType.EMAIL_ADDRESS)) {
      throw new BadRequestException();
    }
    if (user.isRegisteredEmail()) {
      throw new BadRequestException();
    }
    String token = UUID.randomUUID().toString();
    UserCreationTokenEntity tokenEntity = new UserCreationTokenEntity();
    tokenEntity.setToken(token);
    tokenEntity.setEmail(user.getLogin().toLowerCase());
    tokenEntity.setCreationDate(TimeUtils.nowOffsetDateTimeUTC());
    userCreationTokenRepository.save(tokenEntity);
    emailService.sendAccountCreationEmail(user.getLogin().toLowerCase(), token);
  }

  @Transactional
  public UUID createUserWithEmail(@Valid SWUserWithEmailCreate body) {
    checkLicenseLimit();
    if (existsByLoginIgnoreCase(body.getEmail())) {
      throw new EntityExistsException("A user with email " + body.getEmail() + " already exists");
    }
    UserEntity user = new UserEntity();
    user.setLogin(body.getEmail().toLowerCase());
    user.setRole(userMapper.asUserRole(body.getRole()));
    user.setType(UserType.EMAIL_ADDRESS);
    user.setActive(true);
    user = userRepository.save(user);

    String token = UUID.randomUUID().toString();
    UserCreationTokenEntity tokenEntity = new UserCreationTokenEntity();
    tokenEntity.setToken(token);
    tokenEntity.setEmail(body.getEmail().toLowerCase());
    tokenEntity.setCreationDate(TimeUtils.nowOffsetDateTimeUTC());
    userCreationTokenRepository.save(tokenEntity);
    emailService.sendAccountCreationEmail(body.getEmail().toLowerCase(), token);
    return user.getUserId();
  }

  private void checkLicenseLimit() {
    long maxActiveUsers;
    try {
      maxActiveUsers = licenseService.retrieveCurrentLicenseMaxNumberOfActiveUsers();
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new ShouldNeverHappenException(e);
    }
    if (userRepository.countByActiveTrueExceptTestOnlyAccounts(
            "%" + appConfigurationProperties.getUsernameSuffixForTestsOnly())
        >= maxActiveUsers) {
      throw new LicenseLimitExceededException(maxActiveUsers);
    }
  }

  @Transactional
  public UUID createUserWithUsername(@Valid SWUserWithUsernameCreate body) {
    checkLicenseLimit();
    if (existsByLoginIgnoreCase(body.getUsername())) {
      throw new EntityExistsException(
          "A user with username " + body.getUsername() + " already exists");
    }
    UserEntity user = new UserEntity();
    user.setLogin(body.getUsername().toLowerCase());
    user.setPassword(encoder.encode(body.getPassword()));
    user.setRole(userMapper.asUserRole(body.getRole()));
    user.setType(UserType.USERNAME);
    user.setActive(true);
    return userRepository.save(user).getUserId();
  }

  @Transactional
  public void activateUser(UUID uid) {
    checkLicenseLimit();
    UserEntity user = getUserById(uid);
    user.setActive(true);
    userRepository.save(user);
    handleUserTokenValidityAfterActivateOrDeactivate(user.getLogin(), true);
  }

  @Transactional
  public void deactivateUser(UUID uid) {
    UserEntity user = getUserById(uid);
    if (loggedUserDetailsService.getLoggedUserId().equals(uid)) {
      throw new CannotDeactivateOneselfException("Cannot deactivate currently logged in user");
    }
    user.setActive(false);
    userRepository.save(user);
    handleUserTokenValidityAfterActivateOrDeactivate(user.getLogin(), false);
  }

  @Transactional
  public SWUser updateUserWithEmail(UUID uid, SWUserWithEmailUpdate body)
      throws BadRequestException {
    UserEntity user = getUserById(uid);
    if (!user.getType().equals(UserType.EMAIL_ADDRESS)) {
      throw new BadRequestException();
    }
    if (loggedUserDetailsService.getLoggedUserId().equals(user.getUserId())
        && !user.getRole().equals(userMapper.asUserRole(body.getRole()))) {
      throw new BadRequestException("Cannot update own role");
    }
    if (!userMapper.asUserRole(body.getRole()).equals(user.getRole())) {
      handleUserTokenValidityAfterChangedRole(user.getLogin());
    }
    user.setRole(userMapper.asUserRole(body.getRole()));
    return userMapper.toSwUser(userRepository.save(user));
  }

  @Transactional
  public SWUser updateUserWithUsername(UUID uid, SWUserWithUsernameUpdate body)
      throws BadRequestException {
    UserEntity user = getUserById(uid);
    if (!user.getType().equals(UserType.USERNAME)) {
      throw new BadRequestException();
    }
    if (loggedUserDetailsService.getLoggedUserId().equals(user.getUserId())
        && !user.getRole().equals(userMapper.asUserRole(body.getRole()))) {
      throw new BadRequestException("Cannot update own role");
    }
    if (!userMapper.asUserRole(body.getRole()).equals(user.getRole())) {
      handleUserTokenValidityAfterChangedRole(user.getLogin());
    }
    user.setRole(userMapper.asUserRole(body.getRole()));
    return userMapper.toSwUser(userRepository.save(user));
  }

  @Transactional
  public SWUser updateUserPassword(UUID uid, SWUserUsernameResetPassword body)
      throws BadRequestException {
    UserEntity user = getUserById(uid);
    if (!user.getType().equals(UserType.USERNAME)) {
      throw new BadRequestException();
    }
    user.setPassword(encoder.encode(body.getPassword()));
    return userMapper.toSwUser(userRepository.save(user));
  }

  @Transactional
  public Boolean checkLicenseValidity() {
    try {
      checkLicenseLimit();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public void handleUserTokenValidityAfterChangedRole(String userLogin) {
    List<AuthTokenEntity> authTokens = authTokenRepository.findByUserLogin(userLogin);
    if (CollectionUtils.isNotEmpty(authTokens)) {
      for (AuthTokenEntity authToken : authTokens) {
        if (authToken.getDisconnectionReasonType() != null) {
          continue;
        }
        authToken.setTokenValid(false);
        authToken.setDisconnectionReasonType(DisconnectionReasonType.ROLE_CHANGED);

        authTokenRepository.save(authToken);
      }
    }
  }

  public void handleUserTokenValidityAfterActivateOrDeactivate(String userLogin, boolean activate) {
    List<AuthTokenEntity> authTokens = authTokenRepository.findByUserLogin(userLogin);
    if (CollectionUtils.isNotEmpty(authTokens)) {
      for (AuthTokenEntity authToken : authTokens) {
        if (!activate) {
          authToken.setTokenValid(false);
          authToken.setDisconnectionReasonType(DisconnectionReasonType.DEACTIVATE);
        } else if (authToken.getDisconnectionReasonType() == null
            || authToken.getDisconnectionReasonType().equals(DisconnectionReasonType.DEACTIVATE)) {
          authToken.setTokenValid(true);
          authToken.setDisconnectionReasonType(null);
        }

        authTokenRepository.save(authToken);
      }
    }
  }

  @Transactional
  public void logoutUser() {
    invalidateUserTokens(
        loggedUserDetailsService.getLoggedUserLogin(),
        ConnectionType.WEBSITE,
        DisconnectionReasonType.LOGOUT);
  }
}
