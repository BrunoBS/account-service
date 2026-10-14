package br.com.portalmanager.platform.workspace.core.workspace.repository;

import br.com.portalmanager.platform.library.tagging.model.TagName;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.repository.projection.WorkspaceOnboardingReference;
import br.com.portalmanager.platform.workspace.core.workspace.repository.projection.WorkspaceReference;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    @EntityGraph(attributePaths = "approvers")
    Optional<Workspace> findByIdentifier(String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "approvers")
    @Query("select workspace from Workspace workspace where workspace.identifier = :identifier")
    Optional<Workspace> findByIdentifierForUpdate(@Param("identifier") String identifier);

    @Query(
            value = "select id from applications where workspace_id = :workspaceId limit 1 for share",
            nativeQuery = true
    )
    Optional<Long> findAssociatedApplicationId(@Param("workspaceId") Long workspaceId);

    @Query(
            value = """
                    select w from Workspace w
         where w.lifecycle.value = :lifecycleCode
           and (:workspaceTypeCode is null or w.workspaceType.value = :workspaceTypeCode)
                       and (:tagName is null or exists (
                           select 1 from WorkspaceTag tag where tag.owner = w and tag.name = :tagName
                       ))
                    """,
            countQuery = """
                    select count(w) from Workspace w
         where w.lifecycle.value = :lifecycleCode
           and (:workspaceTypeCode is null or w.workspaceType.value = :workspaceTypeCode)
                       and (:tagName is null or exists (
                           select 1 from WorkspaceTag tag where tag.owner = w and tag.name = :tagName
                       ))
        """
    )
    Page<Workspace> findFiltered(
        @Param("lifecycleCode") String lifecycleCode,
        @Param("workspaceTypeCode") String workspaceTypeCode,
        @Param("tagName") TagName tagName,
        Pageable pageable
    );

    @Query(
            "select w.id as id, w.identifier as identifier, w.workspaceType.value as workspaceType from Workspace w where w.identifier = :identifier and w.lifecycle.value = 'ACTIVE'"
    )
    Optional<WorkspaceReference> findActiveReferenceByIdentifier(@Param("identifier") String identifier);

    @Query("select w.id as id, w.workspaceType.value as workspaceType, w.onboarding as onboarding from Workspace w where w.identifier = :identifier and w.lifecycle.value = 'ACTIVE'")
    Optional<WorkspaceOnboardingReference> findActiveOnboardingReferenceByIdentifier(@Param("identifier") String identifier);

    @Query(
            "select w.id as id, w.identifier as identifier, w.workspaceType.value as workspaceType from Workspace w where w.id = :id and (:lifecycleCode is null or w.lifecycle.value = :lifecycleCode)"
    )
    Optional<WorkspaceReference> findReferenceById(@Param("id") Long id, @Param("lifecycleCode") String lifecycleCode);

    @Query(
            "select w.id as id, w.identifier as identifier, w.workspaceType.value as workspaceType from Workspace w where w.id in :ids and (:lifecycleCode is null or w.lifecycle.value = :lifecycleCode)"
    )
    List<WorkspaceReference> findReferencesByIds(
            @Param("ids") Collection<Long> ids,
            @Param("lifecycleCode") String lifecycleCode
    );

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
