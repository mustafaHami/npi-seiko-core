package my.lokalix.planning.core.services;

import jakarta.persistence.EntityExistsException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.mappers.ProductNameMapper;
import my.lokalix.planning.core.models.entities.admin.ProductNameEntity;
import my.lokalix.planning.core.repositories.admin.ProductNameRepository;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import my.lokalix.planning.core.services.validator.ProductNameValidator;
import my.zkonsulting.planning.generated.model.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductNameService {

  private final ProductNameMapper productNameMapper;
  private final ProductNameRepository productNameRepository;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final ProductNameValidator productNameValidator;

  @Transactional
  public SWProductName createProductName(SWProductNameCreate body) {
    if (productNameRepository.existsByCodeIgnoreCaseAndArchivedFalse(body.getCode())) {
      throw new EntityExistsException(
          "A product name with the same code '" + body.getCode() + "' already exists");
    }
    return productNameMapper.toSwProductName(
        productNameRepository.save(productNameMapper.toAdminProductName(body)));
  }

  @Transactional
  public SWProductName updateProductName(UUID uid, SWProductNameUpdate body) {
    ProductNameEntity entity = entityRetrievalHelper.getMustExistProductNameById(uid);
    if (productNameRepository.existsByCodeIgnoreCaseAndProductNameIdNotAndArchivedFalse(
        body.getCode(), uid)) {
      throw new EntityExistsException(
          "A product name with the same code '" + body.getCode() + "' already exists");
    }
    productNameMapper.updateAdminProductNameEntityFromDto(body, entity);
    return productNameMapper.toSwProductName(productNameRepository.save(entity));
  }

  @Transactional
  public void archiveProductName(UUID uid) {
    ProductNameEntity entity = entityRetrievalHelper.getMustExistProductNameById(uid);
    productNameValidator.validateNotInUse(entity);
    entity.setArchived(true);
    productNameRepository.save(entity);
  }

  @Transactional
  public SWProductName retrieveProductName(UUID uid) {
    ProductNameEntity entity = entityRetrievalHelper.getMustExistProductNameById(uid);
    return productNameMapper.toSwProductName(entity);
  }

  @Transactional
  public List<SWProductName> listProductNames() {
    Sort sort = Sort.by(Sort.Direction.ASC, "name");
    List<ProductNameEntity> allProductNames = productNameRepository.findAllByArchivedFalse(sort);
    return productNameMapper.toListSwProductName(allProductNames);
  }

  @Transactional
  public SWProductNamesPaginated searchProductNames(int offset, int limit, SWBasicSearch search) {
    Sort sort = Sort.by(Sort.Direction.ASC, "name");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);
    Page<ProductNameEntity> paginatedProductNames;

    if (StringUtils.isBlank(search.getSearchText())) {
      paginatedProductNames = productNameRepository.findByArchivedFalse(pageable);
    } else {
      paginatedProductNames =
          productNameRepository.findBySearchAndArchivedFalse(pageable, search.getSearchText());
    }

    return populateProductNamesPaginatedResults(paginatedProductNames);
  }

  private SWProductNamesPaginated populateProductNamesPaginatedResults(
      Page<ProductNameEntity> paginatedProductNames) {
    SWProductNamesPaginated productNamesPaginated = new SWProductNamesPaginated();
    productNamesPaginated.setResults(
        productNameMapper.toListSwProductName(paginatedProductNames.getContent()));
    productNamesPaginated.setPage(paginatedProductNames.getNumber());
    productNamesPaginated.setPerPage(paginatedProductNames.getSize());
    productNamesPaginated.setTotal((int) paginatedProductNames.getTotalElements());
    productNamesPaginated.setHasPrev(paginatedProductNames.hasPrevious());
    productNamesPaginated.setHasNext(paginatedProductNames.hasNext());
    return productNamesPaginated;
  }
}
