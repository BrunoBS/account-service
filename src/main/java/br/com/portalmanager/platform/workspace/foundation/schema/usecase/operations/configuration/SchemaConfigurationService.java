package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.configuration;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaConfiguration;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaResourceType;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaConfigurationRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaConfigurationInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaConfigurationOutput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaConfigurationInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class SchemaConfigurationService {
    private static final Pattern LOWER_CODE = Pattern.compile("[a-z][a-z0-9]*(?:-[a-z0-9]+)*");
    private static final Pattern PUBLISHER_CODE = Pattern.compile("[A-Z][A-Z0-9_]*");
    private final SchemaConfigurationRepository repository;
    private final SchemaRepository schemas;

    public SchemaConfigurationService(SchemaConfigurationRepository repository, SchemaRepository schemas) {
        this.repository = repository;
        this.schemas = schemas;
    }

    /** Also used by the resolver so administration and consumption agree on canonical codes. */
    public static SchemaResourceType resourceType(String value) {
        try {
            return SchemaResourceType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw invalid("resourceType", "workspace-service.schema-configuration.resource-type.invalid");
        }
    }

    public static String resourceCode(SchemaResourceType type, String value) {
        if (value == null) throw invalid("resourceCode", "workspace-service.schema-configuration.resource-code.invalid");
        String code = value.trim();
        if (type == SchemaResourceType.PUBLISHER) {
            code = code.toUpperCase(Locale.ROOT);
        } else {
            code = code.toLowerCase(Locale.ROOT).replace('_', '-');
        }
        if (code.length() > 50 || !(type == SchemaResourceType.PUBLISHER
                ? PUBLISHER_CODE.matcher(code).matches() : LOWER_CODE.matcher(code).matches())) {
            throw invalid("resourceCode", "workspace-service.schema-configuration.resource-code.invalid");
        }
        return code;
    }

    @Transactional
    public SchemaConfigurationOutput create(CreateSchemaConfigurationInput input) {
        if (input == null) throw invalid("request", "workspace-service.schema-configuration.request.invalid");
        SchemaResourceType type = resourceType(input.resourceType());
        String code = resourceCode(type, input.resourceCode());
        if (repository.existsByResourceTypeAndResourceCode(type, code))
            throw new ConflictException("workspace-service.schema-configuration.duplicate");
        Schema schema = platformSchema(input.schemaIdentifier());
        return SchemaConfigurationOutput.from(repository.save(
                new SchemaConfiguration(type, code, schema, LocalDateTime.now())));
    }

    @Transactional
    public SchemaConfigurationOutput update(String identifier, UpdateSchemaConfigurationInput input) {
        SchemaConfiguration configuration = required(identifier);
        if (input == null || input.version() == null)
            throw invalid("version", "workspace-service.schema-configuration.version.required");
        if (!Objects.equals(configuration.getVersion(), input.version())) throw new ResourceVersionConflictException();
        configuration.update(platformSchema(input.schemaIdentifier()), LocalDateTime.now());
        return SchemaConfigurationOutput.from(configuration);
    }

    @Transactional
    public SchemaConfigurationOutput activate(String identifier) {
        SchemaConfiguration configuration = required(identifier);
        configuration.activate(LocalDateTime.now());
        return SchemaConfigurationOutput.from(configuration);
    }

    @Transactional
    public SchemaConfigurationOutput inactivate(String identifier) {
        SchemaConfiguration configuration = required(identifier);
        configuration.inactivate(LocalDateTime.now());
        return SchemaConfigurationOutput.from(configuration);
    }

    @Transactional
    public void delete(String identifier) {
        SchemaConfiguration configuration = required(identifier);
        if (configuration.isActive())
            throw new ConflictException("workspace-service.schema-configuration.delete.active");
        repository.delete(configuration);
    }

    @Transactional(readOnly = true)
    public SchemaConfigurationOutput findByIdentifier(String identifier) {
        return SchemaConfigurationOutput.from(required(identifier));
    }

    @Transactional(readOnly = true)
    public List<SchemaConfigurationOutput> findAll() {
        return repository.findAll().stream().map(SchemaConfigurationOutput::from).toList();
    }

    private Schema platformSchema(String identifier) {
        if (identifier == null || identifier.isBlank())
            throw invalid("schemaIdentifier", "workspace-service.schema-configuration.schema.required");
        Schema schema = schemas.findByIdentifier(identifier.trim())
                .orElseThrow(() -> new NotFoundException("workspace-service.schema-configuration.schema.not-found"));
        if (!SchemaScopeTypeCode.platform().equals(schema.getScope()))
            throw invalid("schemaIdentifier", "workspace-service.schema-configuration.schema.platform-required");
        return schema;
    }

    private SchemaConfiguration required(String identifier) {
        return repository.findByIdentifier(identifier)
                .orElseThrow(() -> new NotFoundException("workspace-service.schema-configuration.not-found"));
    }

    private static ValidationException invalid(String field, String key) {
        return new ValidationException(new ValidationResult(field, key));
    }
}
