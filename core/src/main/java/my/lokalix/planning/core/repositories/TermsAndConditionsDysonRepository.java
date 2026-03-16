package my.lokalix.planning.core.repositories;

import java.util.UUID;
import my.lokalix.planning.core.models.entities.admin.TermsAndConditionsDysonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TermsAndConditionsDysonRepository
    extends JpaRepository<TermsAndConditionsDysonEntity, UUID> {}
