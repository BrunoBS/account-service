package br.com.portalmanager.platform.workspace.core.environment.repository;

import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentTypeRepository extends JpaRepository<EnvironmentType, Long> {
    Optional<EnvironmentType> findByCodeAndLifecycle(String code, LifecycleTypeCode lifecycle);
    Optional<EnvironmentType> findByIdentifier(String identifier);
    boolean existsByCode(String code);
    List<EnvironmentType> findAllByOrderByDisplayOrderAscIdAsc();
}
