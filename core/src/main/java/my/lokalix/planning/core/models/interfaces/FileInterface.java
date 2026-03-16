package my.lokalix.planning.core.models.interfaces;

import java.util.List;
import my.lokalix.planning.core.models.entities.FileInfoEntity;

public interface FileInterface {
  void addAttachedFile(FileInfoEntity fileInfoEntity);

  void removeAttachedFile(FileInfoEntity fileInfoEntity);

  List<FileInfoEntity> getAttachedFiles();
}
