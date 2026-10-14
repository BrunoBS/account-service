package br.com.portalmanager.platform.workspace.feature.platform.repository.context;

import br.com.portalmanager.platform.workspace.feature.platform.domain.context.Context;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContextRepository extends JpaRepository<Context, Long> {
    Optional<Context> findByIdentifierAndLifecycle(String identifier, LifecycleTypeCode lifecycle);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select context from Context context where context.identifier = :identifier")
    Optional<Context> findByIdentifierForUpdate(@Param("identifier") String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select context.id from Context context where context.identifier = :identifier")
    Optional<Long> findInternalIdByIdentifierForUpdate(@Param("identifier") String identifier);

    Optional<Context> findByIdentifier(String identifier);

    Optional<Context> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByIdentifier(String identifier);

    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long excludedId);

    @Query("select context from Context context where context.lifecycle.value = :lifecycle " +
        "and (:featureIdentifier is null or exists (select relation from FeatureContextRelation relation " +
        "where relation.context = context and relation.feature.identifier = :featureIdentifier))")
    Page<Context> findAll(@Param("featureIdentifier") String featureIdentifier,
        @Param("lifecycle") String lifecycle, Pageable pageable);
}
