package my.lokalix.planning.core.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.models.entities.ProcessEntity;
import my.lokalix.planning.core.repositories.ProcessRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultProcessInitializer implements ApplicationRunner {

  private final ProcessRepository processRepository;

  @Override
  public void run(ApplicationArguments args) {
    if (processRepository.count() > 0) {
      return;
    }

    log.info("Initializing default processes...");

    processRepository.save(buildMaterialPurchase());
    processRepository.save(buildProcess("Material Receiving"));
    processRepository.save(buildProduction());
    processRepository.save(buildTesting());
    processRepository.save(buildShipment());
    processRepository.save(buildProcess("Customer Approval"));

    log.info("Default processes initialized successfully.");
  }

  private ProcessEntity buildMaterialPurchase() {
    ProcessEntity p = buildProcess("Material Purchase");
    p.setIsMaterialPurchase(true);
    return p;
  }

  private ProcessEntity buildProduction() {
    ProcessEntity p = buildProcess("Production");
    p.setHasPlanTime(true);
    p.setIsProduction(true);
    return p;
  }

  private ProcessEntity buildTesting() {
    ProcessEntity p = buildProcess("Testing");
    p.setHasPlanTime(true);
    p.setIsTesting(true);
    return p;
  }

  private ProcessEntity buildShipment() {
    ProcessEntity p = buildProcess("Shipping");
    p.setIsShipment(true);
    return p;
  }

  private ProcessEntity buildProcess(String name) {
    ProcessEntity process = new ProcessEntity();
    process.setName(name);
    return process;
  }
}
