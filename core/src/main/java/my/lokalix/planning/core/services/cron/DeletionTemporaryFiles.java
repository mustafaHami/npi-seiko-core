package my.lokalix.planning.core.services.cron;

import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.models.entities.FileInfoEntity;
import my.lokalix.planning.core.repositories.FileRepository;
import my.lokalix.planning.core.services.TemporaryFileService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class DeletionTemporaryFiles {

  private final FileRepository fileRepository;
  private final TemporaryFileService temporaryFileService;

  @Transactional
  @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Singapore")
  public void deleteTemporaryFilesWithNoLinked() throws Exception {
    log.info("Starting deleting unlinked temporary files...");
    OffsetDateTime sevenDaysAgo = OffsetDateTime.now().minusDays(7);
    List<FileInfoEntity> fileInfoEntities =
        fileRepository.findAllFilesWithNoParentAndUploadedInLast7Days(sevenDaysAgo);
    if (fileInfoEntities.isEmpty()) {
      log.info("No unlinked files older than 7 days found.");
    } else {
      List<UUID> fileUids = fileInfoEntities.stream().map(FileInfoEntity::getFileId).toList();

      log.info("Found {} unlinked files to delete.", fileUids.size());
      temporaryFileService.deleteTemporaryFile(fileUids);
    }
    log.info("End unlinked temporary files deleted.");
  }
}
