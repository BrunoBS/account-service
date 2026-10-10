package br.com.portalmanager.platform.workspace.core.environment.repository;

import br.com.portalmanager.platform.workspace.core.environment.domain.Environment;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {
    Optional<Environment> findByIdentifierAndWorkspaceId(String identifier, Long workspaceId);
    Optional<Environment> findByIdentifierAndWorkspaceIdIsNull(String identifier);
    Optional<Environment> findByIdentifier(String identifier);
    boolean existsByEnvironmentTypeId(Long typeId);
    boolean existsByEnvironmentTypeIdAndParentIsNull(Long typeId);
    boolean existsByParentId(Long parentId);

    @Query(
        """
        select (count(e) > 0) from Environment e
          where e.parent.environmentType.id = :parentTypeId and e.environmentType.id = :childTypeId
        """
    )
    boolean existsByTypePair(@Param("parentTypeId") Long parentTypeId, @Param("childTypeId") Long childTypeId);

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
    List<Environment> findByWorkspaceAndLifecycle(
        @Param("workspaceId") Long workspaceId,
        @Param("lifecycle") LifecycleTypeCode lifecycle
    );

    @Query(
        """
        select e from Environment e where e.workspaceId is null and e.lifecycle = :lifecycle
          order by e.sortOrder, e.id
        """
    )
    List<Environment> findDefaultsByLifecycle(@Param("lifecycle") LifecycleTypeCode lifecycle);

    @Query(
        """
        select e from Environment e where e.workspaceId = :workspaceId and e.parent is null
          and e.lifecycle = :lifecycle order by e.sortOrder, e.id
        """
    )
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
