package my.lokalix.planning.core.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.UserEntity;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.models.enums.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
  Optional<UserEntity> findByLoginIgnoreCase(String login);

  boolean existsByLoginIgnoreCase(String email);

  boolean existsByTypeAndRegisteredEmailTrueAndLoginIgnoreCase(UserType userType, String email);

  @Query(
      """
                  SELECT u FROM UserEntity u
                  WHERE u.login not like :suffixedLoginTemplate
                  """)
  Page<UserEntity> findAllExceptTestOnlyAccounts(String suffixedLoginTemplate, Pageable pageable);

  Page<UserEntity> findByActive(Pageable pageable, boolean activeFilter);

  @Query(
      value =
          """
                    SELECT u FROM UserEntity u
                    WHERE u.active = :activeFilter
                    AND u.login not like :suffixedLoginTemplate
                    """)
  Page<UserEntity> findByActiveExceptTestOnlyAccounts(
      Pageable pageable, boolean activeFilter, String suffixedLoginTemplate);

  @Query(
      value =
          """
                  SELECT u FROM UserEntity u
                  WHERE LOWER(u.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
                  AND u.login not like :suffixedLoginTemplate
                  """)
  Page<UserEntity> findBySearchExceptTestOnlyAccounts(
      Pageable pageable, String searchText, String suffixedLoginTemplate);

  @Query(
      value =
          """
                  SELECT u FROM UserEntity u
                  WHERE u.active = :activeFilter
                  AND LOWER(u.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
                  AND u.login not like :suffixedLoginTemplate
                  """)
  Page<UserEntity> findByActiveAndSearchExceptTestOnlyAccounts(
      Pageable pageable, String searchText, boolean activeFilter, String suffixedLoginTemplate);

  @Query(
      value =
          """
                    SELECT COUNT(u) FROM UserEntity u
                    WHERE u.active = true
                    AND u.login not like :suffixedLoginTemplate
                    """)
  long countByActiveTrueExceptTestOnlyAccounts(String suffixedLoginTemplate);

  List<UserEntity> findByRoleAndTypeAndActiveTrue(UserRole userRole, UserType userType);

  List<UserEntity> findByRoleInAndTypeAndActiveTrue(List<UserRole> userRoles, UserType userType);
}
