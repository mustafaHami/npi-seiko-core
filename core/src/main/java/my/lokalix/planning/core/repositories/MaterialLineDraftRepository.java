package my.lokalix.planning.core.repositories;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.MaterialLineDraftEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialLineDraftRepository extends JpaRepository<MaterialLineDraftEntity, UUID> {
  List<MaterialLineDraftEntity> findByManufacturerNullAndDraftManufacturerNameIgnoreCase(
      String name);

  List<MaterialLineDraftEntity> findByCategoryNullAndDraftCategoryNameIgnoreCase(
      @NotBlank String name);

  List<MaterialLineDraftEntity> findByUnitNullAndDraftUnitNameIgnoreCase(@NotBlank String name);
}
