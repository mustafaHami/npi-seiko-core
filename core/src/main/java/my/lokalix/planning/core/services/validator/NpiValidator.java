package my.lokalix.planning.core.services.validator;

import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.NpiOrderEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NpiValidator {
  public void validateNpiUpdatable(NpiOrderEntity npiOrder) {
    if (npiOrder == null) {
      throw new GenericWithMessageException("NPI order cannot be null");
    }
    if (npiOrder.getStatus().isFinalStatus())
      throw new GenericWithMessageException("NPI order cannot be updated after final status");
  }
}
