package my.lokalix.planning.core.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.ProcessLineEntity;
import my.lokalix.planning.core.models.entities.ProcessLineStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessLineStatusHistoryRepository
    extends JpaRepository<ProcessLineStatusHistoryEntity, UUID> {

  List<ProcessLineStatusHistoryEntity> findAllByProcessLineOrderByStartDateAsc(
      ProcessLineEntity processLine);

  @Query(
      """
          SELECT h FROM ProcessLineStatusHistoryEntity h
          WHERE h.processLine = :processLine
          AND h.endDate IS NULL
          """)
  Optional<ProcessLineStatusHistoryEntity> findOpenEntryByProcessLine(
      @Param("processLine") ProcessLineEntity processLine);
}
