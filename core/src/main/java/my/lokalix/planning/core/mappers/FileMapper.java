package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.FileInfoEntity;
import my.zkonsulting.planning.generated.model.SWFileInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FileMapper {
  List<SWFileInfo> toListFileMetadata(List<FileInfoEntity> files);

  @Mapping(source = "fileId", target = "uid")
  SWFileInfo toSwFileMetadata(FileInfoEntity file);
}
