package my.lokalix.planning.core.services.helper;

import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.models.entities.*;
import my.lokalix.planning.core.models.entities.admin.*;
import my.lokalix.planning.core.models.enums.MaterialStatus;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.repositories.MaterialLineRepository;
import my.lokalix.planning.core.repositories.admin.*;
import my.lokalix.planning.core.services.EmailService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialHelper {

  private final MaterialLineRepository materialLineRepository;
  private final UserHelper userHelper;
  private final EmailService emailService;
  private final AppConfigurationProperties appConfigurationProperties;

  public void refreshMaterialLinesUsing(MaterialEntity material) {
    // Recalculate all material lines using this material
    List<MaterialLineEntity> materialLines = materialLineRepository.findByMaterial(material);
    for (MaterialLineEntity materialLine : materialLines) {
      materialLine.buildCalculatedFields(
          materialLine.getCostRequestLine().getCostingMethodType(),
          appConfigurationProperties.getTargetCurrencyCode());
    }
    materialLineRepository.saveAll(materialLines);

    // Notify engineering if all materials of an affected cost request line are now estimated
    notifyIfAllMaterialsEstimated(materialLines);
  }

  private void notifyIfAllMaterialsEstimated(List<MaterialLineEntity> affectedMaterialLines) {
    Set<CostRequestLineEntity> costRequestLines =
        affectedMaterialLines.stream()
            .map(MaterialLineEntity::getCostRequestLine)
            .collect(Collectors.toSet());

    for (CostRequestLineEntity costRequestLine : costRequestLines) {
      List<MaterialLineEntity> allLinesForCrl =
          costRequestLine.getOnlyMaterialLinesUsedForQuotation();
      if (CollectionUtils.isEmpty(allLinesForCrl)) continue;
      boolean allEstimated =
          allLinesForCrl.stream()
              .allMatch(ml -> ml.getMaterial().getStatus() == MaterialStatus.ESTIMATED);
      if (!allEstimated) continue;

      List<UserEntity> engineers = userHelper.getAllActiveUsersByRole(UserRole.ENGINEERING);
      if (CollectionUtils.isNotEmpty(engineers)) {
        List<String> emails = engineers.stream().map(UserEntity::getLogin).toList();
        var costRequest = costRequestLine.getCostRequest();
        emailService.sendAllMaterialEstimatedEmail(
            emails,
            costRequestLine.getCustomerPartNumber(),
            costRequestLine.getCustomerPartNumberRevision(),
            costRequest.getCostRequestReferenceNumber(),
            String.valueOf(costRequest.getCostRequestRevision()));
      }
    }
  }
}
