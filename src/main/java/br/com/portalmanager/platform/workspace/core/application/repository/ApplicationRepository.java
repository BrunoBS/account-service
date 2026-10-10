package br.com.portalmanager.platform.workspace.core.application.repository;

import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<Application> findByIdentifierAndWorkspaceId(String identifier, Long workspaceId);
    boolean existsByWorkspaceIdAndName(Long workspaceId, String name);
    boolean existsByWorkspaceIdAndNameAndIdNot(Long workspaceId, String name, Long id);

    @Query(
        """
        select a from Application a where a.workspaceId = :workspaceId
          and a.lifecycle.value = :lifecycle order by a.id
        """
    )
    List<Application> findByWorkspaceAndLifecycle(
        @Param("workspaceId") Long workspaceId,
        @Param("lifecycle") String lifecycle
    );

    @Query(
        """
        select a from Application a where a.workspaceId = :workspaceId
          and a.lifecycle.value = :lifecycle and a.identifier in :identifiers order by a.id
        """
    )
    List<Application> findByWorkspaceLifecycleAndIdentifiers(
        @Param("workspaceId") Long workspaceId,
        @Param("lifecycle") String lifecycle,
        @Param("identifiers") Collection<String> identifiers
    );
}
