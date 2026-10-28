package br.com.portalmanager.platform.workspace.feature.parameter.repository;

import br.com.portalmanager.platform.workspace.feature.parameter.domain.ParameterKey;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.projection.ParameterKeyReadProjection;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.projection.ParameterKeyRuntimeProjection;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParameterKeyRepository extends JpaRepository<ParameterKey, Long> {
    @Query(
            """
            select resource.id as id, resource.microserviceId as microserviceId, resource.key as key,
                resource.publishedType.value as type, resource.publishedValue as value
            from ParameterKey resource where resource.identifier = :identifier
            """)
    Optional<ParameterKeyRuntimeProjection> findRuntimeSnapshot(
            @Param("identifier") String identifier);

    @Query(
            """
            select resource.id as id, resource.microserviceId as microserviceId,
                resource.key as key, resource.publishedType.value as type, resource.publishedValue as value
            from ParameterKey resource
            where resource.active = true and resource.publishedValue is not null and resource.identifier in :identifiers
            order by resource.id
            """)
    List<ParameterKeyRuntimeProjection> findPublishedSnapshots(
            @Param("identifiers") Collection<String> identifiers);

    @Query(
            """
            select resource.id as id, resource.microserviceId as microserviceId,
                resource.key as key, resource.publishedType.value as type, resource.publishedValue as value
            from ParameterKey resource
            where resource.id > :afterId and resource.active = true and resource.publishedValue is not null
            order by resource.id
            """)
    Slice<ParameterKeyRuntimeProjection> findPublishedSnapshots(
            @Param("afterId") long afterId, Pageable pageable);

    boolean existsByMicroserviceIdAndKey(Long microserviceId, String key);

    boolean existsByMicroserviceId(Long microserviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select resource.id from ParameterKey resource where resource.identifier = :identifier")
    Optional<Long> findInternalIdForUpdate(@Param("identifier") String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select resource from ParameterKey resource where resource.identifier = :identifier")
    Optional<ParameterKey> findForUpdate(@Param("identifier") String identifier);

    @Query(
            """
            select resource.id as id, resource.version as version, resource.identifier as identifier, resource.microserviceId as microserviceId,
                resource.key as key, resource.description as description, resource.type.value as type, resource.value as value,
                resource.status.value as status, resource.active as active, resource.createdAt as createdAt, resource.updatedAt as updatedAt
            from ParameterKey resource where resource.identifier = :identifier
            """)
    Optional<ParameterKeyReadProjection> findProjection(@Param("identifier") String identifier);

    @Query(
            """
            select resource.id as id, resource.version as version, resource.identifier as identifier, resource.microserviceId as microserviceId,
                resource.key as key, resource.description as description, resource.type.value as type, resource.value as value,
                resource.status.value as status, resource.active as active, resource.createdAt as createdAt, resource.updatedAt as updatedAt
            from ParameterKey resource
            where (:microserviceId is null or resource.microserviceId = :microserviceId)
              and (:status is null or resource.status.value = :status)
              and (:type is null or resource.type.value = :type)
              and resource.active = :active
              and (:key is null or resource.key = :key)
            """)
    Page<ParameterKeyReadProjection> findPage(
            @Param("microserviceId") Long microserviceId,
            @Param("status") String status,
            @Param("type") String type,
            @Param("active") boolean active,
            @Param("key") String key,
            Pageable pageable);

    @Query("""
            select resource.id from ParameterKey resource
            where resource.id > :afterId and resource.microserviceId in :microserviceIds
              and (:all = true or resource.identifier in :identifiers)
            order by resource.id
            """)
    Slice<Long> findPublicationIds(long afterId, Collection<Long> microserviceIds,
            boolean all, Collection<String> identifiers, Pageable pageable);

    @Query("""
            select resource.id as id, resource.microserviceId as microserviceId, resource.key as key
            from ParameterKey resource where resource.id > :afterId and resource.active = false
            order by resource.id
            """)
    Slice<InactiveKey> findInactiveKeys(long afterId, Pageable pageable);

    interface InactiveKey {
        Long getId();
        Long getMicroserviceId();
        String getKey();
    }

    @Query("""
            select resource.id as id, resource.microserviceId as microserviceId, resource.key as key
            from ParameterKey resource where resource.id in :ids and resource.active = false
            order by resource.id
            """)
    List<InactiveKey> findInactiveKeys(Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select resource.id as id, resource.version as version, resource.identifier as identifier,
                resource.microserviceId as microserviceId, resource.key as key,
                resource.description as description, resource.type.value as type, resource.value as value,
                resource.status.value as status, resource.active as active,
                resource.createdAt as createdAt, resource.updatedAt as updatedAt
            from ParameterKey resource
            where resource.id in :ids and resource.microserviceId in :microserviceIds
              and resource.active = true and resource.status.value = :draft
            order by resource.id
            """)
    List<ParameterKeyReadProjection> findDraftsForPublication(
            Collection<Long> ids, Collection<Long> microserviceIds, String draft);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE parameter_keys
            SET published_value = value, published_type_code = type_code,
                status_code = :published, version = version + 1, updated_at = :now
            WHERE id IN (:ids) AND microservice_id IN (:microserviceIds)
                AND active = true AND status_code = :draft
            """, nativeQuery = true)
    int publishDrafts(Collection<Long> ids, Collection<Long> microserviceIds,
            String draft, String published, LocalDateTime now);

    @Query("""
            select resource.id as id, resource.microserviceId as microserviceId,
                resource.key as key,
                resource.publishedType.value as type, resource.publishedValue as value
            from ParameterKey resource
            where resource.active = true and resource.publishedValue is not null and resource.id in :ids
                and resource.status.value = :published
            order by resource.id
            """)
    List<ParameterKeyRuntimeProjection> findPublishedSnapshotsByIds(Collection<Long> ids, String published);
}
