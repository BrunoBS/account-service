package br.com.portalmanager.platform.workspace.core.application.repository;

import br.com.portalmanager.platform.library.tagging.model.TagName;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.repository.projection.ApplicationReference;
import br.com.portalmanager.platform.workspace.core.application.repository.projection.ApplicationScopeReference;
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
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    @Query(
        """
        select application.id as id, application.workspaceId as workspaceId,
               application.applicationScope.value as applicationScope
        from Application application
        where application.id = :applicationId and application.workspaceId = :workspaceId
          and application.lifecycle.value = 'ACTIVE'
        """
    )
    Optional<ApplicationScopeReference> findActiveScope(
        @Param("workspaceId") Long workspaceId,
        @Param("applicationId") Long applicationId
    );

    @Query(
        """
        select application.id as id, application.workspaceId as workspaceId,
               application.identifier as identifier, application.name as name,
               application.alias as alias, application.acronym as acronym,
               application.applicationScope.value as applicationScope,
               application.authorizerGroup as authorizerGroup
        from Application application
        where application.id in :ids and (:lifecycle is null or application.lifecycle.value = :lifecycle)
        """
    )
    List<ApplicationReference> findReferences(@Param("ids") Collection<Long> ids, @Param("lifecycle") String lifecycle);

    @Query(
        """
        select application.id as id, application.workspaceId as workspaceId,
               application.identifier as identifier, application.name as name,
               application.alias as alias, application.acronym as acronym,
               application.applicationScope.value as applicationScope,
               application.authorizerGroup as authorizerGroup
        from Application application
        where application.workspaceId = :workspaceId and application.lifecycle.value = 'ACTIVE'
        order by application.id
        """
    )
    List<ApplicationReference> findActiveSummaries(@Param("workspaceId") Long workspaceId);

    @Query(
        "select application.identifier from Application application where application.id = :applicationId and application.workspaceId = :workspaceId"
    )
    Optional<String> findIdentifier(@Param("workspaceId") Long workspaceId, @Param("applicationId") Long applicationId);

    Optional<Application> findByIdentifierAndWorkspaceId(String identifier, Long workspaceId);

    boolean existsByWorkspaceId(Long workspaceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select application from Application application where application.identifier = :identifier and application.workspaceId = :workspaceId"
    )
    Optional<Application> findByIdentifierAndWorkspaceIdForUpdate(
        @Param("identifier") String identifier,
        @Param("workspaceId") Long workspaceId
    );

    boolean existsByWorkspaceIdAndName(Long workspaceId, String name);
    boolean existsByWorkspaceIdAndNameAndIdNot(Long workspaceId, String name, Long id);

    @Query(
        """
        select application from Application application
        where application.workspaceId = :workspaceId and application.lifecycle.value = :lifecycle
          and (:tagName is null or exists (
              select 1 from ApplicationTag tag where tag.owner = application and tag.name = :tagName
          ))
        """
    )
    Page<Application> findFiltered(
        @Param("workspaceId") Long workspaceId,
        @Param("lifecycle") String lifecycle,
        @Param("tagName") TagName tagName,
        Pageable pageable
    );
}
