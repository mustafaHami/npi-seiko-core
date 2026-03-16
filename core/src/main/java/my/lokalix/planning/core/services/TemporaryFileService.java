package my.lokalix.planning.core.services;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.mappers.FileMapper;
import my.lokalix.planning.core.models.entities.FileInfoEntity;
import my.lokalix.planning.core.models.enums.FileType;
import my.lokalix.planning.core.services.helper.FileHelper;
import my.lokalix.planning.core.utils.GlobalConstants;
import my.zkonsulting.planning.generated.model.SWFileInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Slf4j
@Service
public class TemporaryFileService {
  private final FileHelper fileHelper;
  private final AppConfigurationProperties appConfigurationProperties;
  private final FileMapper fileMapper;

  @Transactional
  public List<SWFileInfo> uploadTemporaryFile(MultipartFile[] files) throws Exception {
    List<FileInfoEntity> fileInfoEntities =
        fileHelper.uploadTemporaryFiles(
            files,
            appConfigurationProperties.getTemporaryFilesPathDirectory(),
            GlobalConstants.ALLOWED_FILE_EXTENSIONS,
            FileType.ANY);
    return fileMapper.toListFileMetadata(fileInfoEntities);
  }

  @Transactional
  public void deleteTemporaryFile(@Valid List<UUID> fileUids) throws Exception {
    fileHelper.deleteTemporaryFiles(
        fileUids, appConfigurationProperties.getTemporaryFilesPathDirectory());
  }

  @Transactional
  public org.springframework.core.io.Resource downloadTemporaryFiles(@Valid List<UUID> fileUids)
      throws Exception {
    if (CollectionUtils.isEmpty(fileUids)) {
      throw new GenericWithMessageException("File not selected");
    }
    return fileHelper.downloadFile(
        appConfigurationProperties.getTemporaryFilesPathDirectory() + "/" + fileUids.getFirst(),
        fileHelper.fileUidsToFileNames(fileUids),
        GlobalConstants.ZIP_FILE);
  }
}
