package my.lokalix.planning.core.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

  // Mandatory since Spring Boot 4
  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
