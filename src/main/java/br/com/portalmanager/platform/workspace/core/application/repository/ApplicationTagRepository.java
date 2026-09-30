package br.com.portalmanager.platform.workspace.core.application.repository;

import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.storage.TagPersistence;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApplicationTagRepository extends JpaRepository<ApplicationTag, Long>,
        TagPersistence<ApplicationTag, Application, Long, String> {

    @Query("""
            select t
              from ApplicationTag t
             where t.application.id = :applicationId
             order by t.name
            """)
    List<ApplicationTag> findByApplicationId(@Param("applicationId") Long applicationId);

    @Query("""
            select t
              from ApplicationTag t
             where t.application.identifier in :identifiers
               and t.originType = :originType
             order by t.application.identifier, t.name
            """)
    List<ApplicationTag> findByApplicationIdentifiersAndOrigin(
            @Param("identifiers") Collection<String> identifiers,
            @Param("originType") TagOriginType originType
    );

    @Query("""
            select distinct t.application.identifier
              from ApplicationTag t
             where t.name = :name
             order by t.application.identifier
            """)
    List<String> findApplicationIdentifiersByTag(@Param("name") String name);

    @Modifying(flushAutomatically = true)
    @Query("delete from ApplicationTag t where t.application.id = :applicationId")
    int deleteByApplicationId(@Param("applicationId") Long applicationId);

    @Override
    default Long ownerId(Application owner) {
        return owner.getId();
    }

    @Override
    default String ownerKey(ApplicationTag tag) {
        return tag.getApplication().getIdentifier();
    }

    @Override
    default ApplicationTag newTag(Application owner, String name, TagOriginType originType) {
        return new ApplicationTag(owner, name, originType);
    }

    @Override
    default List<ApplicationTag> findByOwnerId(Long ownerId) {
        return findByApplicationId(ownerId);
    }

    @Override
    default List<ApplicationTag> findByOwnerKeysAndOrigin(Collection<String> ownerKeys, TagOriginType originType) {
        return findByApplicationIdentifiersAndOrigin(ownerKeys, originType);
    }

    @Override
    default List<String> findOwnerKeysByTag(String normalizedTag) {
        return findApplicationIdentifiersByTag(normalizedTag);
    }

    @Override
    default void saveAllTags(Collection<ApplicationTag> tags) {
        saveAll(tags);
    }

    @Override
    default void deleteAllTags(Collection<ApplicationTag> tags) {
        deleteAll(tags);
    }

    @Override
    default void deleteByOwnerId(Long ownerId) {
        deleteByApplicationId(ownerId);
    }
}
