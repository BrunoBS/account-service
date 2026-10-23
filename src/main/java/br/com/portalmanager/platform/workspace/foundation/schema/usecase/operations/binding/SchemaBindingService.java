package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.binding;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleFilter;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.binding.SchemaBinding;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.schema.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.binding.SchemaBindingRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.schema.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.mapping.binding.SchemaBindingMapper;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.binding.CreateSchemaBindingInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.binding.SchemaBindingOutput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.binding.UpdateSchemaBindingInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.binding.SchemaBindingValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SchemaBindingService {

    private final SchemaBindingRepository repository;
    private final SchemaRepository schemas;
    private final SchemaBindingValidator validator;
    private final SchemaBindingMapper mapper;

    public SchemaBindingService(
            SchemaBindingRepository repository,
            SchemaRepository schemas,
            SchemaBindingValidator validator,
            SchemaBindingMapper mapper) {
        this.repository = repository;
        this.schemas = schemas;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional
    @Auditable(
            action = AuditAction.CREATE,
            event = "SCHEMA_BINDING_CREATED",
            resourceType = "SCHEMA_BINDING")
    public SchemaBindingOutput create(CreateSchemaBindingInput input) {
        validator.validateCreate(input);
        String type = input.resourceType();
        String code = input.resourceCode();
        validator.validateAvailable(repository.existsByResourceTypeAndResourceCode(type, code));
        Schema schema = platformSchema(input.schemaIdentifier());
        return mapper.toOutput(
                repository.save(new SchemaBinding(type, code, schema, LocalDateTime.now())));
    }

    @Transactional
    @Auditable(
            action = AuditAction.UPDATE,
            event = "SCHEMA_BINDING_UPDATED",
            resourceType = "SCHEMA_BINDING")
    public SchemaBindingOutput update(String identifier, UpdateSchemaBindingInput input) {
        SchemaBinding binding = required(identifier);
        validator.validateUpdate(binding, input);
        binding.update(platformSchema(input.schemaIdentifier()), LocalDateTime.now());
        repository.flush();
        return mapper.toOutput(binding);
    }

    @Transactional
    @Auditable(
            action = AuditAction.ACTIVATE,
            event = "SCHEMA_BINDING_ACTIVATED",
            resourceType = "SCHEMA_BINDING")
    public SchemaBindingOutput activate(String identifier) {
        SchemaBinding binding = required(identifier);
        binding.activate(LocalDateTime.now());
        repository.flush();
        return mapper.toOutput(binding);
    }

    @Transactional
    @Auditable(
            action = AuditAction.DEACTIVATE,
            event = "SCHEMA_BINDING_DEACTIVATED",
            resourceType = "SCHEMA_BINDING")
    public SchemaBindingOutput inactivate(String identifier) {
        SchemaBinding binding = required(identifier);
        binding.inactivate(LocalDateTime.now());
        repository.flush();
        return mapper.toOutput(binding);
    }

    @Transactional
    @Auditable(
            action = AuditAction.PURGE,
            event = "SCHEMA_BINDING_DELETED",
            resourceType = "SCHEMA_BINDING")
    public SchemaBindingOutput delete(String identifier) {
        SchemaBinding binding = required(identifier);
        validator.validateDeletion(binding);
        SchemaBindingOutput snapshot = mapper.toOutput(binding);
        repository.delete(binding);
        repository.flush();
        return snapshot;
    }

    @Transactional(readOnly = true)
    public SchemaBindingOutput findByIdentifier(String identifier) {
        return mapper.toOutput(required(identifier));
    }

    @Transactional(readOnly = true)
    public List<SchemaBindingOutput> findAll(String lifecycle) {
        return repository.findByLifecycleOrderByIdAsc(LifecycleFilter.resolve(lifecycle)).stream()
                .map(mapper::toOutput)
                .toList();
    }

    private Schema platformSchema(String identifier) {
        Schema schema =
                schemas.findByIdentifier(validator.requireSchemaIdentifier(identifier))
                        .orElseThrow(validator::schemaNotFound);
        validator.validatePlatformSchema(schema);
        return schema;
    }

    private SchemaBinding required(String identifier) {
        return repository.findByIdentifier(identifier).orElseThrow(validator::bindingNotFound);
    }
}
