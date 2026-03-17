package my.lokalix.planning.core.services.helper;

import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.models.entities.*;
import my.lokalix.planning.core.repositories.*;
import my.lokalix.planning.core.repositories.ProcessLineRepository;
import my.lokalix.planning.core.repositories.ProcessRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityRetrievalHelper {

  private final FileRepository fileRepository;
  private final NpiOrderRepository npiOrderRepository;
  private final ProcessRepository processRepository;
  private final ProcessLineRepository processLineRepository;


  public FileInfoEntity getMustExistFileEntity(UUID uuid) {
    return fileRepository
        .findById(uuid)
        .orElseThrow(() -> new EntityNotFoundException("File not found"));
  }
  public NpiOrderEntity getMustExistNpiOrderById(UUID uuid) {
    return npiOrderRepository
        .findById(uuid)
        .orElseThrow(() -> new EntityNotFoundException("NPI order not found"));
  }

  public ProcessEntity getMustExistProcessById(UUID uuid) {
    return processRepository
        .findById(uuid)
        .orElseThrow(() -> new EntityNotFoundException("Process not found"));
  }

  public ProcessLineEntity getMustExistProcessLineById(UUID uuid) {
    return processLineRepository
        .findById(uuid)
        .orElseThrow(() -> new EntityNotFoundException("Process line not found"));
  }
}
