package br.com.portalmanager.platform.workspace.core.applicationconfiguration.repository;

import br.com.portalmanager.platform.workspace.core.applicationconfiguration.domain.ApplicationEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationEnvironmentConfigurationRepository
        extends JpaRepository<ApplicationEnvironmentConfiguration, Long> {
    Optional<ApplicationEnvironmentConfiguration> findByIdentifierAndApplicationId(
            String identifier, Long applicationId);

    Optional<ApplicationEnvironmentConfiguration> findByApplicationIdAndEnvironmentId(
            Long applicationId, Long environmentId);

    boolean existsByApplicationIdAndEnvironmentId(Long applicationId, Long environmentId);

    Page<ApplicationEnvironmentConfiguration> findByApplicationIdAndLifecycle(
            Long applicationId, LifecycleTypeCode lifecycle, Pageable pageable);
}
