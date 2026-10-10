package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.configuration.SchemaConfiguration;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.schema.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.version.SchemaVersion;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaResolutionPort;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaConfigurationRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaVersionRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaOperationValidator;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SchemaResolver implements SchemaResolutionPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaResolver.class);
    private static final String DEFAULT_CODE = "DEFAULT";
    private final SchemaConfigurationRepository configurations;
    private final SchemaVersionRepository versions;
    private final SchemaOperationValidator validator;

    public SchemaResolver(
        SchemaConfigurationRepository configurations,
        SchemaVersionRepository versions,
        SchemaOperationValidator validator
    ) {
        this.configurations = configurations;
        this.versions = versions;
        this.validator = validator;
    }

    @Override
    @Transactional(readOnly = true)
    public String resolve(String type, String resourceCode) {
        validator.validateResolutionType(type);
        Optional<String> specific = definition(type, resourceCode);
        if (specific.isPresent()) return specific.get();

        Optional<String> fallback = Optional.empty();
        if (!DEFAULT_CODE.equals(resourceCode)) {
            fallback = definition(type, DEFAULT_CODE);
        }
        if (fallback.isEmpty()) LOGGER.error("No published default schema configuration: type={}", type);
        return validator.requireDefaultDefinition(fallback);
    }

    private Optional<String> definition(String type, String code) {
        Optional<SchemaConfiguration> binding = configurations.findByResourceTypeAndResourceCode(type, code);
        if (binding.isEmpty()) return Optional.empty();
        SchemaConfiguration configuration = binding.get();
        if (!configuration.isActive()) {
            LOGGER.warn("Inactive schema configuration: type={}, code={}", type, code);
            return Optional.empty();
        }
        Schema schema = configuration.getSchema();
        if (schema == null || !schema.isActive()) {
            LOGGER.warn("Inactive or missing configured schema: type={}, code={}", type, code);
            return Optional.empty();
        }
        Optional<SchemaVersion> published = versions.findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(
            schema.getId(),
            SchemaVersionStatusTypeCode.published()
        );
        if (published.isEmpty()) {
            LOGGER.warn("No published schema version: type={}, code={}, schema={}", type, code, schema.getIdentifier());
            return Optional.empty();
        }
        return Optional.of(published.get().getDefinition());
    }
}
