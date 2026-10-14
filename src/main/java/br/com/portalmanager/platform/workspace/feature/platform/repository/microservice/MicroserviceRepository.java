package br.com.portalmanager.platform.workspace.feature.platform.repository.microservice;

import br.com.portalmanager.platform.workspace.feature.platform.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MicroserviceRepository extends JpaRepository<Microservice, Long> {
    Page<Microservice> findByLifecycle(
        LifecycleTypeCode lifecycle, Pageable pageable);

    Optional<Microservice> findByIdentifierAndLifecycle(String identifier, LifecycleTypeCode lifecycle);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select microservice.id from Microservice microservice where microservice.identifier = :identifier")
    Optional<Long> findInternalIdByIdentifierForUpdate(@Param("identifier") String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select microservice from Microservice microservice where microservice.identifier = :identifier")
    Optional<Microservice> findByIdentifierForUpdate(@Param("identifier") String identifier);

    @Query("select microservice.id from Microservice microservice where microservice.identifier = :identifier")
    Optional<Long> findInternalIdByIdentifier(@Param("identifier") String identifier);

    @Query("select microservice.id from Microservice microservice where microservice.identifier = :identifier and microservice.lifecycle.value = 'ACTIVE'")
    Optional<Long> findActiveInternalIdByIdentifier(@Param("identifier") String identifier);

    @Query("select microservice.identifier from Microservice microservice where microservice.id = :id")
    Optional<String> findIdentifierByInternalId(@Param("id") Long id);

    boolean existsByCodeAndLifecycle(String code, LifecycleTypeCode lifecycle);

    boolean existsByIdentifierAndLifecycle(String identifier, LifecycleTypeCode lifecycle);

    boolean existsByNameAndIdNot(String name, Long excludedId);

    Optional<Microservice> findByIdentifier(String identifier);

    Optional<Microservice> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select microservice.id from Microservice microservice where microservice.identifier = :identifier and microservice.lifecycle.value = 'ACTIVE'")
    Optional<Long> findActiveIdForReference(@Param("identifier") String identifier);

    @Query("select microservice.id as id, microservice.identifier as identifier from Microservice microservice where microservice.id in :ids")
    List<IdentifierReference> findIdentifierReferences(@Param("ids") Collection<Long> ids);

    interface IdentifierReference {
        Long getId();
        String getIdentifier();
    }

    boolean existsByIdentifier(String identifier);

    boolean existsByCode(String code);

    boolean existsByName(String name);
}
