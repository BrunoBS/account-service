package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaResolutionPort;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaConfigurationRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaVersionRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaResolutionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class SchemaResolver implements SchemaResolutionPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaResolver.class);
    private static final String DEFAULT_CODE = "DEFAULT";
    private final SchemaConfigurationRepository configurations;
    private final SchemaVersionRepository versions;
    private final SchemaResolutionValidator validator = new SchemaResolutionValidator();

    public SchemaResolver(SchemaConfigurationRepository configurations, SchemaVersionRepository versions) {
        this.configurations = configurations;
        this.versions = versions;
    }

    @Override
    @Transactional(readOnly = true)
    public String resolve(String type, String resourceCode) {
        if (type == null || type.isBlank()) throw validator.typeNotFound();
        Optional<String> specific = definition(type, resourceCode);
        if (specific.isPresent()) return specific.get();
        if (!DEFAULT_CODE.equals(resourceCode)) {
            Optional<String> fallback = definition(type, DEFAULT_CODE);
            if (fallback.isPresent()) return fallback.get();
        }
        LOGGER.error("No published default schema configuration: type={}", type);
        throw validator.defaultNotFound();
    }

    private Optional<String> definition(String type, String code) {
        var binding = configurations.findByResourceTypeAndResourceCode(type, code);
        if (binding.isEmpty()) return Optional.empty();
        var configuration = binding.get();
        if (!configuration.isActive()) {
            LOGGER.warn("Inactive schema configuration: type={}, code={}", type, code);
            return Optional.empty();
        }
        Schema schema = configuration.getSchema();
        if (schema == null || !schema.isActive()) {
            LOGGER.warn("Inactive or missing configured schema: type={}, code={}", type, code);
            return Optional.empty();
        }
        var published = versions.findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(
                schema.getId(), SchemaVersionStatusTypeCode.published());
        if (published.isEmpty()) {
            LOGGER.warn("No published schema version: type={}, code={}, schema={}",
                    type, code, schema.getIdentifier());
            return Optional.empty();
        }
        return Optional.of(published.get().getDefinition());
    }

}
