package br.com.portalmanager.platform.workspace.core.environment.repository;

import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentTypeCompatibility;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EnvironmentTypeCompatibilityRepository extends JpaRepository<EnvironmentTypeCompatibility, Long> {
    boolean existsByParentTypeIdAndChildTypeIdAndLifecycle(Long parent, Long child, String lifecycle);
    Optional<EnvironmentTypeCompatibility> findByParentTypeIdAndChildTypeId(Long parent, Long child);
    Optional<EnvironmentTypeCompatibility> findByIdentifier(String identifier);
    List<EnvironmentTypeCompatibility> findByLifecycle(String lifecycle);
    boolean existsByParentTypeIdOrChildTypeId(Long parent, Long child);
}
