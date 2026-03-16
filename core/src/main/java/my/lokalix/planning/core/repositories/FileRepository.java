package my.lokalix.planning.core.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.FileInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FileRepository extends JpaRepository<FileInfoEntity, UUID> {

  @Query(
      """
    SELECT f
    FROM FileInfoEntity f
  """)
  List<FileInfoEntity> findAllFilesWithNoParentAndUploadedInLast7Days(OffsetDateTime sevenDaysAgo);
}
