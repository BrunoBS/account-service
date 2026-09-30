package br.com.portalmanager.platform.workspace.core.workspace.repository;

import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.storage.TagPersistence;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface WorkspaceTagRepository extends JpaRepository<WorkspaceTag, Long>,
        TagPersistence<WorkspaceTag, Workspace, Long, String> {

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

    @Override
    default Long ownerId(Workspace owner) {
        return owner.getId();
    }

    @Override
    default String ownerKey(WorkspaceTag tag) {
        return tag.getWorkspace().getIdentifier();
    }

    @Override
    default WorkspaceTag newTag(Workspace owner, String name, TagOriginType originType) {
        return new WorkspaceTag(owner, name, originType);
    }

    @Override
    default List<WorkspaceTag> findByOwnerId(Long ownerId) {
        return findByWorkspaceId(ownerId);
    }

    @Override
    default List<WorkspaceTag> findByOwnerKeysAndOrigin(Collection<String> ownerKeys, TagOriginType originType) {
        return findByWorkspaceIdentifiersAndOrigin(ownerKeys, originType);
    }

    @Override
    default List<String> findOwnerKeysByTag(String normalizedTag) {
        return findWorkspaceIdentifiersByTag(normalizedTag);
    }

    @Override
    default void saveAllTags(Collection<WorkspaceTag> tags) {
        saveAll(tags);
    }

    @Override
    default void deleteAllTags(Collection<WorkspaceTag> tags) {
        deleteAll(tags);
    }

    @Override
    default void deleteByOwnerId(Long ownerId) {
        deleteByWorkspaceId(ownerId);
    }
}
