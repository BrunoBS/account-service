package br.com.itau.portalmanager.workspace.core.workspace.repository;

import br.com.itau.portalmanager.workspace.core.workspace.domain.Workspace;
import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycle.domain.LifecycleTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.workspace.domain.WorkspaceTypeEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
    Optional<Workspace> findByIdAndLifecycle(Long id, LifecycleTypeEnum lifecycle);

    @EntityGraph(attributePaths = "approvers")
    @Query("""
            select distinct w
              from Workspace w
             where w.lifecycle = :lifecycle
               and (:workspaceType is null or w.workspaceType = :workspaceType)
             order by w.id
            """)
    List<Workspace> findFiltered(
            @Param("lifecycle") LifecycleTypeEnum lifecycle,
            @Param("workspaceType") WorkspaceTypeEnum workspaceType
    );

    @EntityGraph(attributePaths = "approvers")
    @Query("""
            select distinct w
              from Workspace w
             where w.lifecycle = :lifecycle
               and (:workspaceType is null or w.workspaceType = :workspaceType)
               and w.identifier in :identifiers
             order by w.id
            """)
    List<Workspace> findFilteredByIdentifiers(
            @Param("lifecycle") LifecycleTypeEnum lifecycle,
            @Param("workspaceType") WorkspaceTypeEnum workspaceType,
            @Param("identifiers") Collection<String> identifiers
    );

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
