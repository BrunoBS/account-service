package br.com.portalmanager.platform.workspace.core.environment.repository;

import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentType;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EnvironmentTypeRepository extends JpaRepository<EnvironmentType, Long> {
    Optional<EnvironmentType> findByCodeAndLifecycle(String code, LifecycleTypeCode lifecycle);
    Optional<EnvironmentType> findByIdentifier(String identifier);
    boolean existsByCode(String code);
    List<EnvironmentType> findAllByOrderByDisplayOrderAscIdAsc();
}
