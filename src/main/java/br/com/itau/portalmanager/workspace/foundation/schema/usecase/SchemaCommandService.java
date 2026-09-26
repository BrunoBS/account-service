package br.com.itau.portalmanager.workspace.foundation.schema.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.Schema;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.CreateSchemaInput;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.SchemaOutput;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.UpdateSchemaInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SchemaCommandService {

    private final SchemaRepository repository;
    private final SchemaTypeService schemaTypeService;

    public SchemaCommandService(
            SchemaRepository repository,
            SchemaTypeService schemaTypeService
    ) {
        this.repository = repository;
        this.schemaTypeService = schemaTypeService;
    }

    @Transactional
    public SchemaOutput createPlatform(CreateSchemaInput input) {
        if (repository.findByTypeAndScope(input.schemaTypeCode(), "PLATFORM", null).isPresent()) {
            throw new IllegalArgumentException("Platform schema type already has a schema");
        }
        return create(input, SchemaScopeTypeCode.platform(), null, "PLATFORM");
    }

    @Transactional
    public SchemaOutput createWorkspace(CreateSchemaInput input) {
        if (input.workspaceIdentifier() == null || input.workspaceIdentifier().isBlank()) {
            throw new IllegalArgumentException("Workspace identifier is required");
        }
        return create(
                input,
                SchemaScopeTypeCode.workspace(),
                input.workspaceIdentifier().trim(),
                "WORKSPACE"
        );
    }

    @Transactional
    public SchemaOutput updatePlatform(String identifier, UpdateSchemaInput input) {
        return update(requiredScoped(identifier, "PLATFORM", null), input);
    }

    @Transactional
    public SchemaOutput updateWorkspace(
            String workspaceIdentifier,
            String identifier,
            UpdateSchemaInput input
    ) {
        return update(requiredScoped(identifier, "WORKSPACE", workspaceIdentifier), input);
    }

    @Transactional
    public SchemaOutput activatePlatform(String identifier) {
        return activate(requiredScoped(identifier, "PLATFORM", null));
    }

    @Transactional
    public SchemaOutput activateWorkspace(String workspaceIdentifier, String identifier) {
        return activate(requiredScoped(identifier, "WORKSPACE", workspaceIdentifier));
    }

    @Transactional
    public SchemaOutput inactivatePlatform(String identifier) {
        return inactivate(requiredScoped(identifier, "PLATFORM", null));
    }

    @Transactional
    public SchemaOutput inactivateWorkspace(String workspaceIdentifier, String identifier) {
        return inactivate(requiredScoped(identifier, "WORKSPACE", workspaceIdentifier));
    }

    @Transactional
    public SchemaOutput quarantinePlatform(String identifier) {
        return quarantine(requiredScoped(identifier, "PLATFORM", null));
    }

    @Transactional
    public SchemaOutput quarantineWorkspace(String workspaceIdentifier, String identifier) {
        return quarantine(requiredScoped(identifier, "WORKSPACE", workspaceIdentifier));
    }

    private SchemaOutput update(Schema schema, UpdateSchemaInput input) {
        if (input.version() == null || !input.version().equals(schema.getVersion())) {
            throw new IllegalStateException("Schema version conflict");
        }
        schema.update(input.name(), input.description(), LocalDateTime.now());
        return SchemaOutput.from(schema);
    }

    private SchemaOutput activate(Schema schema) {
        schema.activate(LocalDateTime.now());
        return SchemaOutput.from(schema);
    }

    private SchemaOutput inactivate(Schema schema) {
        schema.inactivate(LocalDateTime.now());
        return SchemaOutput.from(schema);
    }

    private SchemaOutput quarantine(Schema schema) {
        schema.quarantine(LocalDateTime.now());
        return SchemaOutput.from(schema);
    }

    private SchemaOutput create(
            CreateSchemaInput input,
            SchemaScopeTypeCode scope,
            String workspaceIdentifier,
            String scopeCode
    ) {
        if (!schemaTypeService.existsActive(input.schemaTypeCode())) {
            throw new IllegalArgumentException("Active schema type not found");
        }

        if (repository.findByTypeScopeAndCode(
                input.schemaTypeCode(),
                scopeCode,
                workspaceIdentifier,
                input.code()
        ).isPresent()) {
            throw new IllegalArgumentException("Schema code already exists in scope");
        }

        Schema schema = repository.save(new Schema(
                input.schemaTypeCode(),
                scope,
                workspaceIdentifier,
                input.code(),
                input.name(),
                input.description(),
                LocalDateTime.now()
        ));
        return SchemaOutput.from(schema);
    }

    private Schema requiredScoped(
            String identifier,
            String scopeCode,
            String workspaceIdentifier
    ) {
        return repository.findByIdentifierAndScope(identifier, scopeCode, workspaceIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found in requested scope"));
    }
}
