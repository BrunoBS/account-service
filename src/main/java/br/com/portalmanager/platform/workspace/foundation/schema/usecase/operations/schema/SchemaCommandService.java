package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaVersion;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaVersionRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaOutput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaOperationValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaVersionOperationValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SchemaCommandService {

    private static final String DEFINITION = "definition";

    private final SchemaRepository repository;
    private final SchemaVersionRepository versionRepository;
    private final SchemaValidator validator;
    private final SchemaOperationValidator operationValidator = new SchemaOperationValidator();
    private final SchemaVersionOperationValidator versionValidator = new SchemaVersionOperationValidator();
    private final WorkspaceReferenceResolver workspaceReferenceResolver;

    public SchemaCommandService(SchemaRepository repository, SchemaVersionRepository versionRepository,
                                SchemaValidator validator,
                                WorkspaceReferenceResolver workspaceReferenceResolver) {
        this.repository = repository;
        this.versionRepository = versionRepository;
        this.validator = validator;
        this.workspaceReferenceResolver = workspaceReferenceResolver;
    }

    @Transactional
    public SchemaOutput createPlatform(CreateSchemaInput input) {
        operationValidator.validateCreateInput(input);
        return create(input, SchemaScopeTypeCode.platform(), null);
    }

    @Transactional
    public SchemaOutput createWorkspace(CreateSchemaInput input) {
        operationValidator.validateCreateInput(input);
        operationValidator.validateWorkspaceIdentifier(input.workspaceIdentifier());
        Long workspaceId = workspaceReferenceResolver.resolveInternalId(input.workspaceIdentifier().trim());
        return create(input, SchemaScopeTypeCode.workspace(), workspaceId);
    }

    @Transactional
    public SchemaOutput updatePlatform(String identifier, UpdateSchemaInput input) {
        return update(requiredScoped(identifier, SchemaScopeTypeCode.platform(), null), input);
    }

    @Transactional
    public SchemaOutput updateWorkspace(String workspaceIdentifier, String identifier, UpdateSchemaInput input) {
        return update(requiredScoped(identifier, SchemaScopeTypeCode.workspace(),
                workspaceReferenceResolver.resolveInternalId(workspaceIdentifier)), input);
    }

    @Transactional
    public SchemaOutput activatePlatform(String identifier) { return activate(requiredScoped(identifier, SchemaScopeTypeCode.platform(), null)); }

    @Transactional
    public SchemaOutput activateWorkspace(String workspaceIdentifier, String identifier) {
        return activate(requiredScoped(identifier, SchemaScopeTypeCode.workspace(), workspaceReferenceResolver.resolveInternalId(workspaceIdentifier)));
    }

    @Transactional
    public SchemaOutput inactivatePlatform(String identifier) { return inactivate(requiredScoped(identifier, SchemaScopeTypeCode.platform(), null)); }

    @Transactional
    public SchemaOutput inactivateWorkspace(String workspaceIdentifier, String identifier) {
        return inactivate(requiredScoped(identifier, SchemaScopeTypeCode.workspace(), workspaceReferenceResolver.resolveInternalId(workspaceIdentifier)));
    }

    @Transactional
    public SchemaOutput quarantinePlatform(String identifier) { return quarantine(requiredScoped(identifier, SchemaScopeTypeCode.platform(), null)); }

    @Transactional
    public SchemaOutput quarantineWorkspace(String workspaceIdentifier, String identifier) {
        return quarantine(requiredScoped(identifier, SchemaScopeTypeCode.workspace(), workspaceReferenceResolver.resolveInternalId(workspaceIdentifier)));
    }

    private SchemaOutput update(Schema schema, UpdateSchemaInput input) {
        operationValidator.validateUpdate(schema, input);
        schema.update(input.name(), input.description(), LocalDateTime.now());
        return output(schema);
    }

    private SchemaOutput activate(Schema schema) { schema.activate(LocalDateTime.now()); return output(schema); }
    private SchemaOutput inactivate(Schema schema) {
        schema.inactivate(LocalDateTime.now());
        return output(schema);
    }
    private SchemaOutput quarantine(Schema schema) {
        schema.quarantine(LocalDateTime.now());
        return output(schema);
    }

    private SchemaOutput create(CreateSchemaInput input, SchemaScopeTypeCode scope, Long workspaceId) {
        operationValidator.validateOwnership(scope, workspaceId);
        operationValidator.validateCodeAvailable(repository.findByScopeAndCode(
                scope.value(), workspaceId, input.code()).isPresent());

        validator.requireValidSchemaSyntax(input.definition(), DEFINITION);

        LocalDateTime now = LocalDateTime.now();
        Schema schema = repository.save(new Schema(scope, workspaceId,
                input.code(), input.name(), input.description(), now));

        String definition = validator.toJsonString(input.definition(), DEFINITION);
        versionValidator.validateCreate(schema, 1, definition, SchemaVersionStatusTypeCode.draft());
        versionRepository.save(new SchemaVersion(schema, 1, input.versionName(), definition,
                SchemaVersionStatusTypeCode.draft(), now));
        return output(schema);
    }

    private Schema requiredScoped(String identifier, SchemaScopeTypeCode scope, Long workspaceId) {
        return repository.findByIdentifierAndScope(identifier, scope.value(), workspaceId)
                .orElseThrow(() -> new NotFoundException(SchemaMessageKeys.NOT_FOUND));
    }

    private SchemaOutput output(Schema schema) {
        String workspaceIdentifier = schema.getWorkspaceId() == null ? null
                : workspaceReferenceResolver.resolveIdentifier(schema.getWorkspaceId());
        return SchemaOutput.from(schema, workspaceIdentifier);
    }
}
