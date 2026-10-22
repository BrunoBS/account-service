package br.com.portalmanager.platform.workspace.foundation.schema.integration;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.binding.SchemaBinding;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.schema.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.version.SchemaVersion;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.binding.SchemaBindingRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.version.SchemaVersionRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.schema.SchemaOperationValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class SchemaResolutionAdapter implements SchemaResolutionPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaResolutionAdapter.class);
    private static final String DEFAULT_CODE = "DEFAULT";
    private final SchemaBindingRepository bindings;
    private final SchemaVersionRepository versions;
    private final SchemaOperationValidator schemaOperationValidator;

    public SchemaResolutionAdapter(
            SchemaBindingRepository bindings,
            SchemaVersionRepository versions,
            SchemaOperationValidator schemaOperationValidator) {
        this.bindings = bindings;
        this.versions = versions;
        this.schemaOperationValidator = schemaOperationValidator;
    }

    @Override
    @Transactional(readOnly = true)
    public String resolve(String type, String resourceCode) {
        schemaOperationValidator.validateResolutionType(type);
        Optional<String> specific = definition(type, resourceCode);
        if (specific.isPresent()) return specific.get();

        Optional<String> fallback = Optional.empty();
        if (!DEFAULT_CODE.equals(resourceCode)) {
            fallback = definition(type, DEFAULT_CODE);
        }
        if (fallback.isEmpty()) LOGGER.error("No published default schema binding: type={}", type);
        return schemaOperationValidator.requireDefaultDefinition(fallback);
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveRequired(String type, String resourceCode) {
        schemaOperationValidator.validateResolutionType(type);
        return definition(type, resourceCode)
                .orElseThrow(
                        () ->
                                new ValidationException(
                                        new ValidationResult(
                                                "schema", SchemaMessageKeys.RESOLUTION_NOT_FOUND)));
    }

    private Optional<String> definition(String type, String code) {
        Optional<SchemaBinding> binding = bindings.findByResourceTypeAndResourceCode(type, code);
        if (binding.isEmpty()) return Optional.empty();
        SchemaBinding schemaBinding = binding.get();
        if (!schemaBinding.isActive()) {
            LOGGER.warn("Inactive schema binding: type={}, code={}", type, code);
            return Optional.empty();
        }
        Schema schema = schemaBinding.getSchema();
        if (schema == null || !schema.isActive()) {
            LOGGER.warn("Inactive or missing bound schema: type={}, code={}", type, code);
            return Optional.empty();
        }
        Optional<SchemaVersion> published =
                versions.findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(
                        schema.getId(), SchemaVersionStatusTypeCode.published());
        if (published.isEmpty()) {
            LOGGER.warn(
                    "No published schema version: type={}, code={}, schema={}",
                    type,
                    code,
                    schema.getIdentifier());
            return Optional.empty();
        }
        return Optional.of(published.get().getDefinition());
    }
}
