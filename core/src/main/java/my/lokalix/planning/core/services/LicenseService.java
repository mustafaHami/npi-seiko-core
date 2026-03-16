package my.lokalix.planning.core.services;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.models.entities.LicenseEntity;
import my.lokalix.planning.core.repositories.LicenseRepository;
import my.lokalix.planning.core.utils.TimeUtils;
import org.springframework.stereotype.Service;

@Service
public class LicenseService {

  private final LicenseRepository licenseRepository;
  private final Cipher encryptCipher;
  private final Cipher decryptCipher;
  private final AppConfigurationProperties appConfigurationProperties;

  public LicenseService(
      LicenseRepository licenseRepository, AppConfigurationProperties appConfigurationProperties)
      throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
    this.licenseRepository = licenseRepository;
    this.appConfigurationProperties = appConfigurationProperties;
    SecretKey secretKey =
        new SecretKeySpec(
            Base64.getDecoder().decode(appConfigurationProperties.getLicenseSecretKey()), "AES");
    this.encryptCipher = Cipher.getInstance("AES");
    this.encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);
    this.decryptCipher = Cipher.getInstance("AES");
    this.decryptCipher.init(Cipher.DECRYPT_MODE, secretKey);
  }

  public long retrieveCurrentLicenseMaxNumberOfActiveUsers()
      throws IllegalBlockSizeException, BadPaddingException {
    String encodedLicenseLimit = licenseRepository.findCurrentLicenseMaxNumberOfActiveUsers();
    byte[] decryptedBytes = decryptCipher.doFinal(Base64.getDecoder().decode(encodedLicenseLimit));
    return Long.parseLong(new String(decryptedBytes, StandardCharsets.UTF_8));
  }

  public void updateLicense(long activeUsersLimit)
      throws IllegalBlockSizeException, BadPaddingException {
    if (licenseRepository.count() > 0) {
      LicenseEntity license = licenseRepository.findAll().getFirst();
      byte[] encryptedBytes =
          encryptCipher.doFinal(String.valueOf(activeUsersLimit).getBytes(StandardCharsets.UTF_8));
      String encryptedText = Base64.getEncoder().encodeToString(encryptedBytes);
      license.setAllowedNumberOfActiveUsers(encryptedText);
      license.setLastUpdate(TimeUtils.nowOffsetDateTimeUTC());
      licenseRepository.save(license);
    } else {
      LicenseEntity license = new LicenseEntity();
      byte[] encryptedBytes =
          encryptCipher.doFinal(String.valueOf(activeUsersLimit).getBytes(StandardCharsets.UTF_8));
      String encryptedText = Base64.getEncoder().encodeToString(encryptedBytes);
      license.setAllowedNumberOfActiveUsers(encryptedText);
      license.setLastUpdate(TimeUtils.nowOffsetDateTimeUTC());
      licenseRepository.save(license);
    }
  }
}
