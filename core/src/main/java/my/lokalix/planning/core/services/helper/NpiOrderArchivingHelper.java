package my.lokalix.planning.core.services.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.mappers.CustomerMapper;
import my.lokalix.planning.core.models.entities.CustomerEntity;
import my.lokalix.planning.core.models.entities.NpiOrderEntity;
import my.lokalix.planning.core.repositories.CustomerRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NpiOrderArchivingHelper {
  private final CustomerMapper customerMapper;
  private final CustomerRepository customerRepository;

  /**
   * Archive all data for a npi order and its lines in order to take a snapshot.
   *
   * <p>This method should be called when a npi order is being finalized (ESTIMATED or ABORTED) to
   * freeze all dependent data at that point in time.
   *
   * @param npiOrder The cost request entity to archive data for
   */
  public void archiveCostRequestDataFreeze(NpiOrderEntity npiOrder) {
    archiveCustomerForNpiOrder(npiOrder);
  }

  /**
   * Archive the customer for a npi order if not already archived. Checks database first for
   * existing archived version.
   *
   * @param npiOrder The npi order entity
   */
  private void archiveCustomerForNpiOrder(NpiOrderEntity npiOrder) {
    CustomerEntity customer = npiOrder.getCustomer();
    if (customer != null && !customer.isArchived()) {
      CustomerEntity archivedCustomer = createArchivedCustomer(customer);
      npiOrder.setCustomer(archivedCustomer);
    }
  }

  /**
   * creates a newly created customer archived copy.
   *
   * @param customer The customer entity to archive
   * @return The archived customer newly created
   */
  private CustomerEntity createArchivedCustomer(CustomerEntity customer) {
    CustomerEntity archivedCustomer = customerMapper.toCopyCostumerEntity(customer);
    archivedCustomer.setArchived(true);
    archivedCustomer = customerRepository.save(archivedCustomer);
    return archivedCustomer;
  }
}
