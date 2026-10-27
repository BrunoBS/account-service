package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.repository;

import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain.WorkspaceEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface WorkspaceEnvironmentConfigurationRepository extends JpaRepository<WorkspaceEnvironmentConfiguration, Long> {

    Optional<WorkspaceEnvironmentConfiguration> findByIdentifierAndWorkspaceId(String identifier, Long workspaceId);
    Optional<WorkspaceEnvironmentConfiguration> findByWorkspaceIdAndEnvironmentId(Long workspaceId, Long environmentId);
    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<WorkspaceEnvironmentConfiguration> findByWorkspaceIdAndEnvironmentIdAndLifecycle(
            Long workspaceId, Long environmentId, LifecycleTypeCode lifecycle);

    boolean existsByWorkspaceIdAndEnvironmentId(Long workspaceId, Long environmentId);
    Page<WorkspaceEnvironmentConfiguration> findByWorkspaceIdAndLifecycle(Long workspaceId, LifecycleTypeCode lifecycle, Pageable pageable);
}
