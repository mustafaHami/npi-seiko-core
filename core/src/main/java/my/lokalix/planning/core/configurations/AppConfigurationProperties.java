package my.lokalix.planning.core.configurations;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Validated
@Getter
@Setter
@Configuration
@ConfigurationProperties
public class AppConfigurationProperties {
  @NotBlank private String appTimezone;
  @NotBlank private String licenseSecretKey;
  @NotBlank private String jwtSecretKey;
  @NotBlank private String usernameSuffixForTestsOnly;
  @NotBlank private String superAdminLogin;
  @NotBlank private String superAdminPassword;
  @NotBlank private String appBaseUrl;
  @NotNull private Integer startingYear;
  @NotBlank private String temporaryFilesPathDirectory;
  @NotBlank private String processLineFilesPathDirectory;

  private Smtp2go smtp2go = new Smtp2go();
  private ExcelTemplatePaths excelTemplatePaths = new ExcelTemplatePaths();

  @Valid
  @Getter
  @Setter
  public static class ExcelTemplatePaths {
    @NotBlank private String standardGenerationExcelFileTemplate;
  }

  @Valid
  @Getter
  @Setter
  public static class Smtp2go {
    @NotBlank private String baseUrl;
    @NotBlank private String sender;
    @NotBlank private String apiKey;
    @NotBlank private String apiKeyHeader;
    @NotBlank private String genericEmail;
  }
}
