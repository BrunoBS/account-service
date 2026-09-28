package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.configuration;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaConfiguration;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaConfigurationRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaConfigurationInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaConfigurationOutput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaConfigurationInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class SchemaConfigurationService {
    private final SchemaConfigurationRepository repository;
    private final SchemaRepository schemas;

    public SchemaConfigurationService(SchemaConfigurationRepository repository, SchemaRepository schemas) {
        this.repository = repository;
        this.schemas = schemas;
    }

    @Transactional
    public SchemaConfigurationOutput create(CreateSchemaConfigurationInput input) {
        if (input == null) throw invalid("request", "workspace-service.schema-configuration.request.invalid");
        String type = input.resourceType();
        String code = input.resourceCode();
        if (type == null || type.isBlank() || type.length() > 100)
            throw invalid("resourceType", "workspace-service.schema-configuration.resource-type.invalid");
        if (code == null || code.isBlank() || code.length() > 50)
            throw invalid("resourceCode", "workspace-service.schema-configuration.resource-code.invalid");
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
