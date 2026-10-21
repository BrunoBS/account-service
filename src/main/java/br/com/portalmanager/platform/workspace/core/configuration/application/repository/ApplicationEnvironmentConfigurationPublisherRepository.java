package br.com.portalmanager.platform.workspace.core.configuration.application.repository;

import br.com.portalmanager.platform.workspace.core.configuration.application.domain.ApplicationEnvironmentConfigurationPublisher;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApplicationEnvironmentConfigurationPublisherRepository
        extends JpaRepository<ApplicationEnvironmentConfigurationPublisher, Long> {
    List<ApplicationEnvironmentConfigurationPublisher> findByConfigurationIdOrderBySortOrderAsc(
            Long configurationId);

    List<ApplicationEnvironmentConfigurationPublisher> findByConfigurationIdInOrderBySortOrderAsc(
            Collection<Long> configurationIds);

    @Modifying(flushAutomatically = true)
    @Query(
            "delete from ApplicationEnvironmentConfigurationPublisher association where"
                + " association.configuration.id = :configurationId")
    void deleteAssociations(@Param("configurationId") Long configurationId);
}
