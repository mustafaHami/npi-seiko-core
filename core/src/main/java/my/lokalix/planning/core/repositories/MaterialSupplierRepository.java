package my.lokalix.planning.core.repositories;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.MaterialEntity;
import my.lokalix.planning.core.models.entities.MaterialSupplierEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialSupplierRepository extends JpaRepository<MaterialSupplierEntity, UUID> {

  Page<MaterialSupplierEntity> findByMaterial(MaterialEntity material, Pageable pageable);

  @Query(
      """
          SELECT DISTINCT s FROM MaterialSupplierEntity s
          LEFT JOIN FETCH s.moqLines
          WHERE s.materialSupplierId IN :supplierIds
          """)
  List<MaterialSupplierEntity> findByIdInWithMoqLines(
      @Param("supplierIds") Set<UUID> supplierIds);

  @Query(
      """
              SELECT ms FROM MaterialSupplierEntity ms
              WHERE ms.material = :material
              AND LOWER(ms.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
              """)
  Page<MaterialSupplierEntity> findByMaterialAndSearch(
      @Param("material") MaterialEntity material,
      @Param("searchText") String searchText,
      Pageable pageable);
}
