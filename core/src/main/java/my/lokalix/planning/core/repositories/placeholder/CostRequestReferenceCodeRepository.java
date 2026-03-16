package my.lokalix.planning.core.repositories.placeholder;

import my.lokalix.planning.core.models.placeholder.CostRequestReferenceCode;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface CostRequestReferenceCodeRepository
    extends Repository<CostRequestReferenceCode, Long> {
  @Query(value = "SELECT nextval('cost_request_reference_code_seq')", nativeQuery = true)
  Long getNextCostRequestReferenceCodeSequenceValue();
}
