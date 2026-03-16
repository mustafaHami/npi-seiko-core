package my.lokalix.planning.core.services;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.mappers.TermsAndConditionsMapper;
import my.lokalix.planning.core.models.entities.admin.CustomerEntity;
import my.lokalix.planning.core.models.entities.admin.TermsAndConditionsDysonEntity;
import my.lokalix.planning.core.models.entities.admin.TermsAndConditionsNonDysonEntity;
import my.lokalix.planning.core.repositories.TermsAndConditionsDysonRepository;
import my.lokalix.planning.core.repositories.TermsAndConditionsNonDysonRepository;
import my.lokalix.planning.core.repositories.admin.CustomerRepository;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import my.zkonsulting.planning.generated.model.SWTermsAndConditionsDyson;
import my.zkonsulting.planning.generated.model.SWTermsAndConditionsDysonPatch;
import my.zkonsulting.planning.generated.model.SWTermsAndConditionsNonDyson;
import my.zkonsulting.planning.generated.model.SWTermsAndConditionsNonDysonPatch;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class TermsAndConditionsService {

  private final TermsAndConditionsMapper termsAndConditionsMapper;
  private final TermsAndConditionsNonDysonRepository termsAndConditionsNonDysonRepository;
  private final TermsAndConditionsDysonRepository termsAndConditionsDysonRepository;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final CustomerRepository customerRepository;

  @Transactional
  public SWTermsAndConditionsNonDyson getTermsAndConditionsNonDyson(UUID uid) {
    CustomerEntity customerEntity = entityRetrievalHelper.getMustExistCustomerById(uid);
    if (customerEntity.isDyson()) {
      throw new GenericWithMessageException("Dyson customer cannot retrieve non-Dyson T&C");
    }
    Optional<TermsAndConditionsNonDysonEntity> optEntity =
        termsAndConditionsNonDysonRepository.findById(uid);
    return optEntity
        .map(termsAndConditionsMapper::toSWTermsAndConditionsNonDyson)
        .orElse(new SWTermsAndConditionsNonDyson());
  }

  @Transactional
  public SWTermsAndConditionsNonDyson patchTermsAndConditionsNonDyson(
      UUID uid, SWTermsAndConditionsNonDysonPatch body) {
    CustomerEntity customerEntity = entityRetrievalHelper.getMustExistCustomerById(uid);
    if (customerEntity.isDyson()) {
      throw new GenericWithMessageException("Dyson customer cannot update non-Dyson T&C");
    }
    TermsAndConditionsNonDysonEntity entity =
        termsAndConditionsNonDysonRepository
            .findById(uid)
            .orElseGet(
                () -> {
                  TermsAndConditionsNonDysonEntity newEntity =
                      new TermsAndConditionsNonDysonEntity();
                  newEntity.setCustomer(customerEntity);
                  return newEntity;
                });
    termsAndConditionsMapper.updateTermsAndConditionsNonDysonEntityFromDto(body, entity);
    entity = termsAndConditionsNonDysonRepository.save(entity);
    return termsAndConditionsMapper.toSWTermsAndConditionsNonDyson(entity);
  }

  @Transactional
  public SWTermsAndConditionsDyson getTermsAndConditionsDyson(UUID uid) {
    CustomerEntity customerEntity = entityRetrievalHelper.getMustExistCustomerById(uid);
    if (!customerEntity.isDyson()) {
      throw new GenericWithMessageException("Non-Dyson customer cannot retrieve Dyson T&C");
    }
    Optional<TermsAndConditionsDysonEntity> optEntity =
        termsAndConditionsDysonRepository.findById(uid);
    return optEntity
        .map(termsAndConditionsMapper::toSWTermsAndConditionsDyson)
        .orElse(new SWTermsAndConditionsDyson());
  }

  @Transactional
  public SWTermsAndConditionsDyson patchTermsAndConditionsDyson(
      UUID uid, SWTermsAndConditionsDysonPatch body) {
    CustomerEntity customerEntity = entityRetrievalHelper.getMustExistCustomerById(uid);
    if (!customerEntity.isDyson()) {
      throw new GenericWithMessageException("Non-Dyson customer cannot retrieve Dyson T&C");
    }
    TermsAndConditionsDysonEntity entity =
        termsAndConditionsDysonRepository
            .findById(uid)
            .orElseGet(
                () -> {
                  TermsAndConditionsDysonEntity newEntity = new TermsAndConditionsDysonEntity();
                  newEntity.setCustomer(customerEntity);
                  return newEntity;
                });
    termsAndConditionsMapper.updateTermsAndConditionsDysonEntityFromDto(body, entity);
    entity = termsAndConditionsDysonRepository.save(entity);
    return termsAndConditionsMapper.toSWTermsAndConditionsDyson(entity);
  }

  @Transactional
  public int upsertDefaultTermsAndConditions() {
    List<CustomerEntity> customers =
        customerRepository.findAllByArchivedFalse(Sort.by(Sort.Direction.ASC, "code"));

    for (CustomerEntity customer : customers) {
      UUID customerId = customer.getCustomerId();
      if (customer.isDyson()) {
        TermsAndConditionsDysonEntity entity =
            termsAndConditionsDysonRepository
                .findById(customerId)
                .orElseGet(
                    () -> {
                      TermsAndConditionsDysonEntity newEntity = new TermsAndConditionsDysonEntity();
                      newEntity.setCustomer(customer);
                      return newEntity;
                    });
        entity.setValidityNumberOfDays(30);
        entity.setCurrencyExchangeRate(new BigDecimal("4.2"));
        entity.setMinimumDeliveryQuantity(500);
        entity.setStorageAcceptDeliveryNumberMonths(3);
        entity.setStorageMinimumStorageFee(new BigDecimal("3"));
        entity.setNonCancellationNumberWorkingDays(15);
        entity.setNonRescheduledNumberWeeks(2);
        entity.setClaimsPackagingDamageNumberDays(new BigDecimal("10"));
        entity.setForecastLeadTime("1-month");
        entity.setLatePaymentPenalty(new BigDecimal("5"));
        termsAndConditionsDysonRepository.save(entity);
      } else {
        TermsAndConditionsNonDysonEntity entity =
            termsAndConditionsNonDysonRepository
                .findById(customerId)
                .orElseGet(
                    () -> {
                      TermsAndConditionsNonDysonEntity newEntity =
                          new TermsAndConditionsNonDysonEntity();
                      newEntity.setCustomer(customer);
                      return newEntity;
                    });
        entity.setValidityNumberOfDays(30);
        entity.setDeliveryCharges(new BigDecimal("10"));
        entity.setStorageAcceptDeliveryNumberMonths(3);
        entity.setStorageMinimumStorageFee(new BigDecimal("3"));
        entity.setNonCancellationNumberWorkingDays(15);
        entity.setNonRescheduledNumberWeeks(2);
        entity.setClaimsPackagingDamageNumberDays(new BigDecimal("10"));
        entity.setForecastLeadTime("1-month");
        entity.setLatePaymentPenalty(new BigDecimal("5"));
        termsAndConditionsNonDysonRepository.save(entity);
      }
    }

    log.info("Upserted default terms and conditions for {} customers", customers.size());
    return customers.size();
  }
}
