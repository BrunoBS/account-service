package br.com.portalmanager.platform.workspace.core.workspace.repository;

import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface WorkspaceTagRepository extends JpaRepository<WorkspaceTag, Long> {

    @Query("""
            select t
              from WorkspaceTag t
             where t.workspace.id = :workspaceId
             order by t.name
            """)
    List<WorkspaceTag> findByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("""
            select t
              from WorkspaceTag t
             where t.workspace.identifier in :identifiers
               and t.originType = :originType
             order by t.workspace.identifier, t.name
            """)
    List<WorkspaceTag> findByWorkspaceIdentifiersAndOrigin(
            @Param("identifiers") Collection<String> identifiers,
            @Param("originType") TagOriginType originType
    );

    @Query("""
            select distinct t.workspace.identifier
              from WorkspaceTag t
             where t.name = :name
             order by t.workspace.identifier
            """)
    List<String> findWorkspaceIdentifiersByTag(@Param("name") String name);

    @Modifying(flushAutomatically = true)
    @Query("delete from WorkspaceTag t where t.workspace.id = :workspaceId")
    int deleteByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
