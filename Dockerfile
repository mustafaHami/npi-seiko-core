FROM maven:3.9.12-eclipse-temurin-25 AS builder

# Set the working directory in the container
WORKDIR /app

# Copy the entire project to the container
COPY . .

# Build the app for production
RUN mvn clean install

###############################################################################
FROM eclipse-temurin:25-jre-alpine

# Copy the production-ready app to the container
COPY --from=builder /app/core/target/*.jar /usr/share/springboot/core.jar

EXPOSE 80

ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "-Dspring.profiles.active=production", "-Dspring.config.location=/usr/share/springboot/config/", "/usr/share/springboot/core.jar"]