package br.com.itau.portalmanager.workspace.foundation.schema.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.Schema;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaType;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.CreateSchemaInput;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.SchemaOutput;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.UpdateSchemaInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SchemaCommandService {

    private final SchemaRepository repository;
    private final SchemaTypeRepository typeRepository;

    public SchemaCommandService(
            SchemaRepository repository,
            SchemaTypeRepository typeRepository
    ) {
        this.repository = repository;
        this.typeRepository = typeRepository;
    }

    @Transactional
    public SchemaOutput createPlatform(CreateSchemaInput input) {
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
    public SchemaOutput update(String identifier, UpdateSchemaInput input) {
        Schema schema = required(identifier);
        if (input.version() == null || !input.version().equals(schema.getVersion())) {
            throw new IllegalStateException("Schema version conflict");
        }
        schema.update(input.name(), input.description(), LocalDateTime.now());
        return SchemaOutput.from(schema);
    }

    @Transactional
    public SchemaOutput activate(String identifier) {
        Schema schema = required(identifier);
        schema.activate(LocalDateTime.now());
        return SchemaOutput.from(schema);
    }

    @Transactional
    public SchemaOutput inactivate(String identifier) {
        Schema schema = required(identifier);
        schema.inactivate(LocalDateTime.now());
        return SchemaOutput.from(schema);
    }

    @Transactional
    public SchemaOutput quarantine(String identifier) {
        Schema schema = required(identifier);
        schema.quarantine(LocalDateTime.now());
        return SchemaOutput.from(schema);
    }

    private SchemaOutput create(
            CreateSchemaInput input,
            SchemaScopeTypeCode scope,
            String workspaceIdentifier,
            String scopeCode
    ) {
        SchemaType type = typeRepository.findByCode(input.schemaTypeCode())
                .filter(SchemaType::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Active schema type not found"));

        if (repository.findByTypeScopeAndCode(
                input.schemaTypeCode(),
                scopeCode,
                workspaceIdentifier,
                input.code()
        ).isPresent()) {
            throw new IllegalArgumentException("Schema code already exists in scope");
        }

        Schema schema = repository.save(new Schema(
                type,
                scope,
                workspaceIdentifier,
                input.code(),
                input.name(),
                input.description(),
                LocalDateTime.now()
        ));
        return SchemaOutput.from(schema);
    }

    private Schema required(String identifier) {
        return repository.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found"));
    }
}
