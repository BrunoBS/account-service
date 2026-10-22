package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.repository;

import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain.WorkspaceEnvironmentConfigurationPublisher;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface WorkspaceEnvironmentConfigurationPublisherRepository
        extends JpaRepository<WorkspaceEnvironmentConfigurationPublisher, Long> {
    List<WorkspaceEnvironmentConfigurationPublisher> findByConfigurationIdOrderBySortOrderAsc(
            Long configurationId);

    List<WorkspaceEnvironmentConfigurationPublisher> findByConfigurationIdInOrderBySortOrderAsc(
            Collection<Long> configurationIds);

    @Modifying(flushAutomatically = true)
    @Query(
            "delete from WorkspaceEnvironmentConfigurationPublisher association where"
                    + " association.configuration.id = :configurationId")
    void deleteAssociations(@Param("configurationId") Long configurationId);
}
