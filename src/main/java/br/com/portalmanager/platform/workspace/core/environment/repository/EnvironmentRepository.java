package br.com.portalmanager.platform.workspace.core.environment.repository;

import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {
    @Query(
        "select environment from Environment environment where environment.identifier in :identifiers and (environment.workspaceId = :workspaceId or environment.workspaceId is null)"
    )
    @EntityGraph(attributePaths = "environmentType")
    List<Environment> findSharedReferences(
        @Param("workspaceId") Long workspaceId,
        @Param("identifiers") Collection<String> identifiers
    );

    Optional<Environment> findByIdentifierAndWorkspaceId(String identifier, Long workspaceId);
    Optional<Environment> findByIdentifierAndWorkspaceIdIsNull(String identifier);
    Optional<Environment> findByIdentifier(String identifier);

    @Query("select environment.environmentType.code from Environment environment where environment.identifier = :identifier")
    Optional<String> findTypeCodeByIdentifier(@Param("identifier") String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select environment from Environment environment where environment.identifier = :identifier")
    Optional<Environment> findByIdentifierForUpdate(@Param("identifier") String identifier);

    @Query("select environment from Environment environment where environment.id = :id")
    Optional<Environment> findVisibleById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select environment from Environment environment where environment.id = :id and environment.lifecycle = :lifecycle")
    Optional<Environment> findVisibleByIdAndLifecycleForRead(
        @Param("id") Long id,
        @Param("lifecycle") LifecycleTypeCode lifecycle
    );

    @Query("select environment.id as id, environment.parent.id as parentId, environment.environmentType.code as typeCode, " +
        "environment.lifecycle.value as lifecycle from Environment environment " +
        "where environment.workspaceId = :workspaceId or environment.workspaceId is null")
    List<HierarchyReference> findVisibleHierarchyInWorkspace(@Param("workspaceId") Long workspaceId);

    @Query("select environment from Environment environment where environment.workspaceId is null and environment.parent is null " +
        "and environment.lifecycle = :lifecycle order by environment.sortOrder, environment.id")
    @EntityGraph(attributePaths = "environmentType")
    List<Environment> findDefaultRoots(@Param("lifecycle") LifecycleTypeCode lifecycle);

    @Query(value = """
        with recursive hierarchy as (
            select id, parent_environment_id, environment_type_code, lifecycle_code
            from environments where identifier in (:identifiers) and (workspace_id = :workspaceId or workspace_id is null)
            union distinct
            select parent.id, parent.parent_environment_id, parent.environment_type_code, parent.lifecycle_code
            from environments parent join hierarchy child on parent.id = child.parent_environment_id
            where parent.workspace_id = :workspaceId or parent.workspace_id is null
        )
        select id, parent_environment_id as parentId, environment_type_code as typeCode, lifecycle_code as lifecycle from hierarchy
        """, nativeQuery = true)
    List<HierarchyReference> findHierarchyForSharedReferences(
        @Param("workspaceId") Long workspaceId, @Param("identifiers") Collection<String> identifiers
    );

    @Query("select environment.id as id, environment.identifier as identifier from Environment environment where environment.id in :ids")
    List<IdentifierReference> findIdentifierReferences(@Param("ids") Collection<Long> ids);

    interface IdentifierReference {
        Long getId();
        String getIdentifier();
    }

    @Query(value = "select id, parent_environment_id as parentId, environment_type_code as typeCode, lifecycle_code as lifecycle from environments where id = :id", nativeQuery = true)
    Optional<HierarchyReference> findHierarchyReference(@Param("id") Long id);

    @Query(value = "select id, parent_environment_id as parentId, environment_type_code as typeCode, lifecycle_code as lifecycle from environments where id = :id for share", nativeQuery = true)
    Optional<HierarchyReference> findHierarchyReferenceForRead(@Param("id") Long id);

    @Query(value = "select id from environments where parent_environment_id = :parentId order by id limit 1 for share", nativeQuery = true)
    Optional<Long> findChildForEnvironmentValidation(@Param("parentId") Long parentId);

    interface HierarchyReference {
        Long getId();
        Long getParentId();
        String getTypeCode();
        String getLifecycle();
    }

    @Query(value = "select id from environments where environment_type_code = :typeCode limit 1 for share", nativeQuery = true)
    Optional<Long> findUsageForTypeValidation(@Param("typeCode") String typeCode);

    @Query(value = "select id from environments where environment_type_code = :typeCode and parent_environment_id is null limit 1 for share", nativeQuery = true)
    Optional<Long> findRootForTypeValidation(@Param("typeCode") String typeCode);

    @Query(value = "select child.id from environments parent join environments child on child.parent_environment_id = parent.id where parent.environment_type_code = :typeCode limit 1 for share", nativeQuery = true)
    Optional<Long> findChildForTypeValidation(@Param("typeCode") String typeCode);

    @Query(value = "select child.id from environments child join environments parent on parent.id = child.parent_environment_id " +
        "where parent.environment_type_code = :parentTypeCode and child.environment_type_code = :childTypeCode limit 1 for share", nativeQuery = true)
    Optional<Long> findUsageForCompatibilityValidation(@Param("parentTypeCode") String parentTypeCode, @Param("childTypeCode") String childTypeCode);

    @Query(
        """
        select (count(e) > 0) from Environment e where e.name = :name
          and ((:workspaceId is null and e.workspaceId is null) or e.workspaceId = :workspaceId)
          and ((:parentId is null and e.parent is null) or e.parent.id = :parentId)
          and (:excludedId is null or e.id <> :excludedId)
        """
    )
    boolean existsSibling(
        @Param("name") String name,
        @Param("workspaceId") Long workspaceId,
        @Param("parentId") Long parentId,
        @Param("excludedId") Long excludedId
    );

    @Query(
        """
        select e from Environment e where e.workspaceId = :workspaceId and e.lifecycle = :lifecycle
          order by e.sortOrder, e.id
        """
    )
    @EntityGraph(attributePaths = "environmentType")
    List<Environment> findByWorkspaceAndLifecycle(
        @Param("workspaceId") Long workspaceId,
        @Param("lifecycle") LifecycleTypeCode lifecycle
    );

    @Query(
        """
        select e from Environment e where e.lifecycle = :lifecycle
          and (e.workspaceId = :workspaceId or e.workspaceId is null)
          order by e.sortOrder, e.id
        """
    )
    @EntityGraph(attributePaths = "environmentType")
    List<Environment> findVisibleInWorkspace(
        @Param("workspaceId") Long workspaceId,
        @Param("lifecycle") LifecycleTypeCode lifecycle
    );

    @Query(
        """
        select e from Environment e where e.workspaceId is null and e.lifecycle = :lifecycle
          order by e.sortOrder, e.id
        """
    )
    @EntityGraph(attributePaths = "environmentType")
    List<Environment> findDefaultsByLifecycle(@Param("lifecycle") LifecycleTypeCode lifecycle);

    @Query(
        """
        select e from Environment e where e.workspaceId = :workspaceId and e.parent is null
          and e.lifecycle = :lifecycle order by e.sortOrder, e.id
        """
    )
    @EntityGraph(attributePaths = "environmentType")
    List<Environment> findWorkspaceRoots(
        @Param("workspaceId") Long workspaceId,
        @Param("lifecycle") LifecycleTypeCode lifecycle
    );

    @Query(
        """
        select e from Environment e where e.parent.id = :parentId and e.workspaceId = :workspaceId
          and e.lifecycle = :lifecycle order by e.sortOrder, e.id
        """
    )
    @EntityGraph(attributePaths = "environmentType")
    List<Environment> findChildren(
        @Param("parentId") Long parentId,
        @Param("workspaceId") Long workspaceId,
        @Param("lifecycle") LifecycleTypeCode lifecycle
    );

    @Query(
        """
        select max(e.sortOrder) from Environment e
        where ((:workspaceId is null and e.workspaceId is null) or e.workspaceId = :workspaceId)
          and ((:parentId is null and e.parent is null) or e.parent.id = :parentId)
        """
    )
    Integer maxSiblingSortOrder(@Param("workspaceId") Long workspaceId, @Param("parentId") Long parentId);
}
