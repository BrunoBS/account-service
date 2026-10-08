package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.configuration;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;

import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaConfiguration;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaConfigurationRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaConfigurationInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaConfigurationOutput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaConfigurationInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaConfigurationValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SchemaConfigurationService {
    private final SchemaConfigurationRepository repository;
    private final SchemaRepository schemas;
    private final SchemaConfigurationValidator validator;

    public SchemaConfigurationService(SchemaConfigurationRepository repository, SchemaRepository schemas,
                                      SchemaConfigurationValidator validator) {
        this.repository = repository;
        this.schemas = schemas;
        this.validator = validator;
    }

    @Auditable(action = AuditAction.CREATE, event = "SCHEMA_CONFIGURATION_CREATED", resourceType = "SCHEMA_CONFIGURATION")
    @Transactional
    public SchemaConfigurationOutput create(CreateSchemaConfigurationInput input) {
        validator.validateCreate(input);
        String type = input.resourceType();
        String code = input.resourceCode();
        validator.validateAvailable(repository.existsByResourceTypeAndResourceCode(type, code));
        Schema schema = platformSchema(input.schemaIdentifier());
        return SchemaConfigurationOutput.from(repository.save(
                new SchemaConfiguration(type, code, schema, LocalDateTime.now())));
    }

    @Auditable(action = AuditAction.UPDATE, event = "SCHEMA_CONFIGURATION_UPDATED", resourceType = "SCHEMA_CONFIGURATION")
    @Transactional
    public SchemaConfigurationOutput update(String identifier, UpdateSchemaConfigurationInput input) {
        SchemaConfiguration configuration = required(identifier);
        validator.validateUpdate(configuration, input);
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

    @Auditable(action = AuditAction.DELETE, event = "SCHEMA_CONFIGURATION_DELETED", resourceType = "SCHEMA_CONFIGURATION")
    @Transactional
    public SchemaConfigurationOutput delete(String identifier) {
        SchemaConfiguration configuration = required(identifier);
        validator.validateDeletion(configuration);
        repository.delete(configuration);
        return SchemaConfigurationOutput.from(configuration);
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
        Schema schema = schemas.findByIdentifier(validator.requireSchemaIdentifier(identifier))
                .orElseThrow(validator::schemaNotFound);
        validator.validatePlatformSchema(schema);
        return schema;
    }

    private SchemaConfiguration required(String identifier) {
        return repository.findByIdentifier(identifier)
                .orElseThrow(validator::configurationNotFound);
    }
}
