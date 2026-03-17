package my.lokalix.planning.core.services.helper;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.FileInfoEntity;
import my.lokalix.planning.core.models.enums.FileType;
import my.lokalix.planning.core.models.interfaces.FileInterface;
import my.lokalix.planning.core.repositories.FileRepository;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.core.io.UrlResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class FileHelper {

  @Resource private AppConfigurationProperties appConfigurationProperties;
  @Resource private FileRepository fileRepository;
  @Resource private EntityRetrievalHelper entityRetrievalHelper;

  private static String getFileExtension(String fileName) {
    return fileName.substring(fileName.lastIndexOf(".") + 1);
  }

  private Path getFilePath(String directoryName, String fileName, boolean isNew) throws Exception {
    Path filePath = Paths.get(directoryName).resolve(fileName.replace(" ", "_")).normalize();
    // because the file does not exist in the download process
    if (!isNew && !Files.exists(filePath)) {
      throw new GenericWithMessageException(
          "File " + fileName + " not found", SWCustomErrorCode.GENERIC_ERROR);
    }
    if (isNew && Files.exists(filePath)) {
      throw new GenericWithMessageException(
          "File " + fileName + " is already exist", SWCustomErrorCode.GENERIC_ERROR);
    }
    return filePath;
  }

  @Transactional
  public List<String> uploadFiles(
      MultipartFile[] files, String directoryName, List<String> allowedExtensions)
      throws Exception {
    List<String> filesAdded = new ArrayList<>();
    try {
      // Step 1: Create Directory
      Files.createDirectories(Path.of(directoryName));
      // Step 2: Browse all files
      for (MultipartFile file : files) {
        // Step 3: Check that the file is empty and has its extension  is valid
        if (file.isEmpty()) {
          throw new GenericWithMessageException(
              "File" + file.getOriginalFilename() + " is empty", SWCustomErrorCode.GENERIC_ERROR);
        }
        String fileExtension =
            FileHelper.getFileExtension(file.getOriginalFilename()).toLowerCase();
        if (!allowedExtensions.contains(fileExtension)) {
          throw new GenericWithMessageException(
              "Unsupported file type: " + fileExtension, SWCustomErrorCode.GENERIC_ERROR);
        }
        // Step 4: Write file
        Path path = getFilePath(directoryName, file.getOriginalFilename(), true);
        try (BufferedOutputStream bufferedOutputStream =
            new BufferedOutputStream(Files.newOutputStream(path))) {
          bufferedOutputStream.write(file.getBytes());
        }
        filesAdded.add(path.getFileName().toString());
      }
    } catch (Exception e) {
      for (String file : filesAdded) {
        Files.deleteIfExists(getFilePath(directoryName, file, false));
      }
      throw e;
    }
    return filesAdded;
  }

  @Transactional
  public org.springframework.core.io.Resource downloadFile(
      String directoryName, List<String> fileNames, String zipFileName) throws Exception {
    if (!fileNames.isEmpty()) {
      if (fileNames.size() > 1) {
        // Step 1 : Check zipFileName if there is more than one file, and if is the string delete if
        // exists zip.
        if (!StringUtils.hasText(zipFileName)) {
          throw new GenericWithMessageException(
              "Zip file name is empty", SWCustomErrorCode.GENERIC_ERROR);
        } else {
          Files.deleteIfExists(
              Paths.get(directoryName).resolve(zipFileName.replace(" ", "_")).normalize());
        }
        // Step 2 : Create zip
        Path zipPath = getFilePath(directoryName, zipFileName, true);
        BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(zipPath));
        try (ZipOutputStream zipOut = new ZipOutputStream(bos)) {
          // Step 3 : Browse all file names and create zip
          for (String fileName : fileNames) {
            Path filePath = getFilePath(directoryName, fileName, false);
            ZipEntry zipEntry = new ZipEntry(fileName);
            zipOut.putNextEntry(zipEntry);
            Files.copy(filePath, zipOut);
            zipOut.closeEntry();
          }
        }
        // Step 4 : Return zip URI
        return new UrlResource(zipPath.toUri());
      } else {
        // OR Step 1 : Create and return File
        Path filePath = getFilePath(directoryName, fileNames.getFirst(), false);
        return new UrlResource(filePath.toUri());
      }
    } else {
      throw new GenericWithMessageException("File names is empty", SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  @Transactional()
  public org.springframework.core.io.Resource downloadFileByPath(String filePath) throws Exception {

    Path path = Paths.get(filePath);

    if (!Files.exists(path) || !Files.isRegularFile(path)) {
      throw new GenericWithMessageException("File not found", SWCustomErrorCode.GENERIC_ERROR);
    }

    return new UrlResource(path.toUri());
  }

  @Transactional
  public List<String> copyFiles(
      List<String> fileNames, String sourceDirectoryName, String targetDirectoryName)
      throws Exception {
    List<String> filesCopied = new ArrayList<>();
    try {
      // Step 1: Create target directory
      Files.createDirectories(Path.of(targetDirectoryName));
      // Step 2: Browse all filenames
      for (String filename : fileNames) {
        // Step 3: Copy file
        Path sourcePath = getFilePath(sourceDirectoryName, filename, false);
        Path targetPath = getFilePath(targetDirectoryName, filename, true);
        Files.copy(sourcePath, targetPath, REPLACE_EXISTING);
        filesCopied.add(filename);
      }
    } catch (Exception e) {
      for (String file : filesCopied) {
        Files.deleteIfExists(getFilePath(targetDirectoryName, file, false));
      }
      throw e;
    }
    return filesCopied;
  }

  @Transactional
  public List<Path> deleteMultipleFiles(String directoryName, List<String> fileNames)
      throws Exception {
    List<Path> validPaths = new ArrayList<>();
    if (!fileNames.isEmpty()) {
      for (String fileName : fileNames) {
        validPaths.add(deleteFile(directoryName, fileName));
      }
      return validPaths;
    } else {
      throw new GenericWithMessageException("File names is empty", SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  @Transactional
  public Path deleteFile(String directoryName, String fileName) throws Exception {
    if (org.apache.commons.lang3.StringUtils.isNotBlank(fileName)) {
      // Step 1: valid the file name
      Path filePath = getFilePath(directoryName, fileName, false);
      // Step 2: delete a file
      Files.deleteIfExists(filePath);
      return filePath;
    } else {
      throw new GenericWithMessageException(
          "File not found: " + fileName, SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public List<MultipartFile> convertResourcesToMultipartFiles(
      List<org.springframework.core.io.Resource> resources) throws IOException {
    List<MultipartFile> multipartFiles = new ArrayList<>();
    for (org.springframework.core.io.Resource resource : resources) {
      if (resource == null || !resource.exists() || !resource.isReadable()) {
        throw new IOException(
            "Resource invalide ou illisible : "
                + (resource != null ? resource.getFilename() : "null"));
      }

      String fileName = resource.getFilename();
      byte[] content = resource.getInputStream().readAllBytes();

      MultipartFile multipartFile =
          new MockMultipartFile(fileName, fileName, "application/octet-stream", content);

      multipartFiles.add(multipartFile);
    }

    return multipartFiles;
  }

  public List<FileInfoEntity> uploadTemporaryFiles(
      MultipartFile[] files,
      String directoryName,
      List<String> allowedExtensions,
      FileType fileType)
      throws Exception {
    List<FileInfoEntity> fileInfoEntities = new ArrayList<>();
    try {
      // Step 1: Browse all files
      for (MultipartFile file : files) {
        // Step 2: Check that the file is empty and has its extension  is valid
        if (file.isEmpty()) {
          throw new GenericWithMessageException(
              "File" + file.getOriginalFilename() + " is empty", SWCustomErrorCode.GENERIC_ERROR);
        }
        // Step 3: Create FileInfoEntity for get UUID
        String originalFileName =
            Objects.requireNonNull(file.getOriginalFilename()).replace(" ", "_");
        FileInfoEntity fileInfoEntity = createFileEntity(originalFileName, fileType);
        fileRepository.save(fileInfoEntity);

        // Step 4: Create Directory only on by file
        String directoryNameWithFileId = directoryName + fileInfoEntity.getFileId().toString();
        Files.createDirectories(Path.of(directoryNameWithFileId));
        String fileExtension =
            FileHelper.getFileExtension(Objects.requireNonNull(originalFileName)).toLowerCase();
        if (!allowedExtensions.contains(fileExtension)) {
          throw new GenericWithMessageException(
              "Unsupported file type: " + fileExtension, SWCustomErrorCode.GENERIC_ERROR);
        }

        // Step 5: Write file
        Path path = getFilePath(directoryNameWithFileId, originalFileName, true);
        try (BufferedOutputStream bufferedOutputStream =
            new BufferedOutputStream(Files.newOutputStream(path))) {
          bufferedOutputStream.write(file.getBytes());
        }
        fileInfoEntities.add(fileInfoEntity);
      }
    } catch (Exception e) {
      for (FileInfoEntity fileInfoEntity : fileInfoEntities) {
        Files.deleteIfExists(getFilePath(directoryName, fileInfoEntity.getFileName(), false));
      }
      throw e;
    }
    return fileInfoEntities;
  }

  public void deleteTemporaryFiles(@Valid List<UUID> fileUids, String path) throws Exception {
    for (UUID fileUid : fileUids) {
      FileInfoEntity fileInfoEntity = entityRetrievalHelper.getMustExistFileEntity(fileUid);
      deleteDirectory(path, fileInfoEntity.getFileId().toString());
      fileRepository.delete(fileInfoEntity);
    }
  }

  public Path deleteDirectory(String directoryName, String folderName) throws Exception {
    if (org.apache.commons.lang3.StringUtils.isBlank(folderName)) {
      throw new GenericWithMessageException(
          "Folder name is blank", SWCustomErrorCode.GENERIC_ERROR);
    }

    Path basePath = Paths.get(directoryName).normalize();
    Path targetDir = basePath.resolve(folderName).normalize();
    if (!targetDir.startsWith(basePath)) {
      throw new SecurityException("Unauthorized path access detected: " + targetDir);
    }

    if (!Files.exists(targetDir)) {
      throw new GenericWithMessageException(
          "Directory not found: " + targetDir, SWCustomErrorCode.GENERIC_ERROR);
    }
    try (Stream<Path> walk = Files.walk(targetDir)) {
      walk.sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException e) {
                  throw new RuntimeException("Failed to delete: " + path, e);
                }
              });
    }

    return targetDir;
  }

  private FileInfoEntity createFileEntity(String fileName, FileType fileType) {
    FileInfoEntity fileInfoEntity = new FileInfoEntity();
    fileInfoEntity.setType(fileType);
    fileInfoEntity.setFileName(fileName);
    return fileInfoEntity;
  }

  public void addFilesInAttachedFilesListInEntity(
      FileInterface entity, List<String> fileNames, FileType fileType) {
    for (String fileName : fileNames) {
      FileInfoEntity fileInfoEntity = new FileInfoEntity();
      fileInfoEntity.setType(fileType);
      fileInfoEntity.setFileName(fileName);
      entity.addAttachedFile(fileInfoEntity);
    }
  }

  public void deleteFilesInAttachedFilesListInEntity(
      FileInterface entity, List<Path> validPaths, FileType fileType) {
    if (entity.getAttachedFiles().isEmpty()) return;
    for (Path path : validPaths) {
      FileInfoEntity matchingFileInfo =
          entity.getAttachedFiles().stream()
              .filter(fileInfo -> fileInfo.getType().equals(fileType))
              .filter(fileInfo -> fileInfo.getFileName().equals(path.getFileName().toString()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new GenericWithMessageException(
                          "File not found: " + path.getFileName(),
                          SWCustomErrorCode.GENERIC_ERROR));
      entity.removeAttachedFile(matchingFileInfo);
    }
  }

  public List<String> fileUidsToFileNames(List<UUID> fileUids) {
    if (CollectionUtils.isEmpty(fileUids)) {
      return List.of();
    }
    return fileUids.stream()
        .map(uuid -> entityRetrievalHelper.getMustExistFileEntity(uuid).getFileName())
        .collect(Collectors.toList());
  }

  /**
   * Delete temporary files after successful transaction commit. Uses
   * TransactionSynchronizationManager to ensure files are only deleted after the transaction has
   * been successfully committed to the database. This prevents file loss if a rollback occurs.
   */
  public void deleteTemporaryFiles(List<UUID> fileIds) {
    if (CollectionUtils.isEmpty(fileIds)) {
      return;
    }

    // Register synchronization to delete files AFTER transaction commit
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            // This code executes ONLY after successful transaction commit
            for (UUID fileId : fileIds) {
              try {
                FileInfoEntity fileInfoEntity =
                    entityRetrievalHelper.getMustExistFileEntity(fileId);
                deleteDirectory(
                    appConfigurationProperties.getTemporaryFilesPathDirectory(),
                    fileInfoEntity.getFileId().toString());
              } catch (Exception e) {
                // Log error but don't throw - transaction is already committed
                log.error("Failed to delete temporary file with ID: {}", fileId, e);
              }
            }
          }
        });
  }
}
