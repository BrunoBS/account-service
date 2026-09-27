package br.com.portalmanager.platform.workspace.core.environment.repository;

import br.com.portalmanager.platform.workspace.core.environment.domain.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {
    Optional<Environment> findByIdentifierAndWorkspaceId(String identifier, Long workspaceId);
    Optional<Environment> findByIdentifierAndWorkspaceIdIsNull(String identifier);
    boolean existsByNameAndWorkspaceId(String name, Long workspaceId);
    boolean existsByNameAndWorkspaceIdAndIdNot(String name, Long workspaceId, Long id);
    boolean existsByNameAndWorkspaceIdIsNull(String name);
    boolean existsByNameAndWorkspaceIdIsNullAndIdNot(String name, Long id);

    @Query("""
            select e from Environment e where e.workspaceId = :workspaceId and e.lifecycle.value = :lifecycle
              order by e.sortOrder, e.id
            """)
    List<Environment> findByWorkspaceAndLifecycle(@Param("workspaceId") Long workspaceId,
                                                   @Param("lifecycle") String lifecycle);

    @Query("""
            select e from Environment e where e.workspaceId is null and e.lifecycle.value = :lifecycle
              order by e.sortOrder, e.id
            """)
    List<Environment> findDefaultsByLifecycle(@Param("lifecycle") String lifecycle);

    @Query("select max(e.sortOrder) from Environment e where e.workspaceId = :workspaceId")
    Integer maxCustomSortOrder(@Param("workspaceId") Long workspaceId);
    @Query("select max(e.sortOrder) from Environment e where e.workspaceId is null")
    Integer maxDefaultSortOrder();
}
