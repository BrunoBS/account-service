package br.com.portalmanager.platform.workspace.core.workspace.repository;

import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    @EntityGraph(attributePaths = "approvers")
    @Query("select w from Workspace w where w.id = :id")
    Optional<Workspace> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = "approvers")
    Optional<Workspace> findByIdentifier(String identifier);

    @EntityGraph(attributePaths = "approvers")
    @Query("select w from Workspace w where w.identifier = :identifier and w.lifecycle.value = :lifecycleCode")
    Optional<Workspace> findByIdentifierAndLifecycleValue(
            @Param("identifier") String identifier,
            @Param("lifecycleCode") String lifecycleCode
    );

    @EntityGraph(attributePaths = "approvers")
    @Query("""
            select distinct w
              from Workspace w
             where w.lifecycle.value = :lifecycleCode
               and (:workspaceTypeCode is null or w.workspaceType.value = :workspaceTypeCode)
             order by w.id
            """)
    List<Workspace> findFiltered(
            @Param("lifecycleCode") String lifecycleCode,
            @Param("workspaceTypeCode") String workspaceTypeCode
    );

    @EntityGraph(attributePaths = "approvers")
    @Query("""
            select distinct w
              from Workspace w
             where w.lifecycle.value = :lifecycleCode
               and (:workspaceTypeCode is null or w.workspaceType.value = :workspaceTypeCode)
               and w.identifier in :identifiers
             order by w.id
            """)
    List<Workspace> findFilteredByIdentifiers(
            @Param("lifecycleCode") String lifecycleCode,
            @Param("workspaceTypeCode") String workspaceTypeCode,
            @Param("identifiers") Collection<String> identifiers
    );

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    @Modifying(flushAutomatically = true)
    @Query(value = "delete from workspace_approvers where workspace_id = :workspaceId", nativeQuery = true)
    void deleteApproversByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
