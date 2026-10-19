package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferencePort;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.schema.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.version.SchemaVersion;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaDefinitionValidationAdapter;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.schema.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.version.SchemaVersionRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.schema.CreateSchemaInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.schema.SchemaOutput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.schema.UpdateSchemaInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.mapping.schema.SchemaMapper;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.schema.SchemaOperationValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.version.SchemaVersionOperationValidator;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchemaCommandService {

    private static final String DEFINITION = "definition";

    private final SchemaRepository repository;
    private final SchemaVersionRepository versionRepository;
    private final SchemaDefinitionValidationAdapter validator;
    private final SchemaOperationValidator operationValidator;
    private final SchemaVersionOperationValidator versionValidator;
    private final WorkspaceReferencePort workspaceReferencePort;
    private final SchemaMapper mapper;

    public SchemaCommandService(
        SchemaRepository repository,
        SchemaVersionRepository versionRepository,
        SchemaDefinitionValidationAdapter validator,
        SchemaOperationValidator operationValidator,
        SchemaVersionOperationValidator versionValidator,
        WorkspaceReferencePort workspaceReferencePort,
        SchemaMapper mapper
    ) {
        this.repository = repository;
        this.versionRepository = versionRepository;
        this.validator = validator;
        this.operationValidator = operationValidator;
        this.versionValidator = versionValidator;
        this.workspaceReferencePort = workspaceReferencePort;
        this.mapper = mapper;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "SCHEMA_CREATED", resourceType = "SCHEMA")
    public SchemaOutput createPlatform(CreateSchemaInput input) {
        operationValidator.validateCreateInput(input);
        return create(input, SchemaScopeTypeCode.platform(), null);
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "SCHEMA_CREATED", resourceType = "SCHEMA")
    public SchemaOutput createWorkspace(CreateSchemaInput input) {
        operationValidator.validateCreateInput(input);
        operationValidator.validateWorkspaceIdentifier(input.workspaceIdentifier());
        Long workspaceId = workspaceReferencePort.resolveInternalId(input.workspaceIdentifier().trim());
        return create(input, SchemaScopeTypeCode.workspace(), workspaceId);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "SCHEMA_UPDATED", resourceType = "SCHEMA")
    public SchemaOutput updatePlatform(String identifier, UpdateSchemaInput input) {
        return update(requiredScoped(identifier, SchemaScopeTypeCode.platform(), null), input);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "SCHEMA_UPDATED", resourceType = "SCHEMA")
    public SchemaOutput updateWorkspace(String workspaceIdentifier, String identifier, UpdateSchemaInput input) {
        return update(
            requiredScoped(
                identifier,
                SchemaScopeTypeCode.workspace(),
                workspaceReferencePort.resolveInternalId(workspaceIdentifier)
            ),
            input
        );
    }

    @Transactional
    public SchemaOutput activatePlatform(String identifier) {
        return activate(requiredScoped(identifier, SchemaScopeTypeCode.platform(), null));
    }

    @Transactional
    public SchemaOutput activateWorkspace(String workspaceIdentifier, String identifier) {
        return activate(
            requiredScoped(
                identifier,
                SchemaScopeTypeCode.workspace(),
                workspaceReferencePort.resolveInternalId(workspaceIdentifier)
            )
        );
    }

    @Transactional
    public SchemaOutput inactivatePlatform(String identifier) {
        return inactivate(requiredScoped(identifier, SchemaScopeTypeCode.platform(), null));
    }

    @Transactional
    public SchemaOutput inactivateWorkspace(String workspaceIdentifier, String identifier) {
        return inactivate(
            requiredScoped(
                identifier,
                SchemaScopeTypeCode.workspace(),
                workspaceReferencePort.resolveInternalId(workspaceIdentifier)
            )
        );
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE, event = "SCHEMA_DELETED", resourceType = "SCHEMA")
    public SchemaOutput quarantinePlatform(String identifier) {
        return quarantine(requiredScoped(identifier, SchemaScopeTypeCode.platform(), null));
    }

    @Transactional
    public SchemaOutput quarantineWorkspace(String workspaceIdentifier, String identifier) {
        return quarantine(
            requiredScoped(
                identifier,
                SchemaScopeTypeCode.workspace(),
                workspaceReferencePort.resolveInternalId(workspaceIdentifier)
            )
        );
    }

    private SchemaOutput update(Schema schema, UpdateSchemaInput input) {
        operationValidator.validateUpdate(schema, input);
        schema.update(input.name(), input.description(), LocalDateTime.now());
        return output(schema);
    }

    private SchemaOutput activate(Schema schema) {
        schema.activate(LocalDateTime.now());
        return output(schema);
    }

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
        operationValidator.validateCodeAvailable(
            repository.findByScopeAndCode(scope.value(), workspaceId, input.code()).isPresent()
        );

        validator.requireValidSchemaSyntax(input.definition(), DEFINITION);

        LocalDateTime now = LocalDateTime.now();
        Schema schema = repository.save(
            new Schema(scope, workspaceId, input.code(), input.name(), input.description(), now)
        );

        String definition = validator.toJsonString(input.definition(), DEFINITION);
        versionValidator.validateCreate(schema, 1, definition, SchemaVersionStatusTypeCode.draft());
        versionRepository.save(
            new SchemaVersion(schema, 1, input.versionName(), definition, SchemaVersionStatusTypeCode.draft(), now)
        );
        return output(schema);
    }

    private Schema requiredScoped(String identifier, SchemaScopeTypeCode scope, Long workspaceId) {
        return repository
            .findByIdentifierAndScope(identifier, scope.value(), workspaceId)
            .orElseThrow(() -> new NotFoundException(SchemaMessageKeys.NOT_FOUND));
    }

    private SchemaOutput output(Schema schema) {
        String workspaceIdentifier =
            schema.getWorkspaceId() == null
                ? null
                : workspaceReferencePort.resolveIdentifier(schema.getWorkspaceId());
        return mapper.toOutput(schema, workspaceIdentifier);
    }
}
