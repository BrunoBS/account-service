package br.com.portalmanager.platform.workspace.foundation.schema.repository;

import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaConfiguration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchemaConfigurationRepository extends JpaRepository<SchemaConfiguration, Long> {
    Optional<SchemaConfiguration> findByIdentifier(String identifier);
    Optional<SchemaConfiguration> findByResourceTypeAndResourceCode(String type, String code);
    boolean existsByResourceTypeAndResourceCode(String type, String code);
}
