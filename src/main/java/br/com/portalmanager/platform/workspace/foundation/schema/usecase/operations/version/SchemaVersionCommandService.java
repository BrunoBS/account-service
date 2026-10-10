package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.version;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaVersion;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaDefinitionValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaVersionRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaVersionInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaVersionOutput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaVersionOperationValidator;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
public class SchemaVersionCommandService {

    private static final String DEFINITION = "definition";

    private final SchemaRepository schemaRepository;
    private final SchemaVersionRepository versionRepository;
    private final SchemaDefinitionValidator validator;
    private final SchemaVersionOperationValidator operationValidator;
    private final WorkspaceReferenceResolver workspaceReferenceResolver;

    public SchemaVersionCommandService(
        SchemaRepository schemaRepository,
        SchemaVersionRepository versionRepository,
        SchemaDefinitionValidator validator,
        SchemaVersionOperationValidator operationValidator,
        WorkspaceReferenceResolver workspaceReferenceResolver
    ) {
        this.schemaRepository = schemaRepository;
        this.versionRepository = versionRepository;
        this.validator = validator;
        this.operationValidator = operationValidator;
        this.workspaceReferenceResolver = workspaceReferenceResolver;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "SCHEMA_VERSION_CREATED", resourceType = "SCHEMA_VERSION")
    public SchemaVersionOutput createPlatformDraft(String schemaIdentifier, CreateSchemaVersionInput input) {
        return createOrUpdateDraft(requiredScoped(schemaIdentifier, SchemaScopeTypeCode.platform(), null), input);
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "SCHEMA_VERSION_CREATED", resourceType = "SCHEMA_VERSION")
    public SchemaVersionOutput createWorkspaceDraft(
        String workspaceIdentifier,
        String schemaIdentifier,
        CreateSchemaVersionInput input
    ) {
        return createOrUpdateDraft(
            requiredScoped(
                schemaIdentifier,
                SchemaScopeTypeCode.workspace(),
                workspaceReferenceResolver.resolveInternalId(workspaceIdentifier)
            ),
            input
        );
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "SCHEMA_VERSION_PUBLISHED", resourceType = "SCHEMA_VERSION")
    public SchemaVersionOutput publishPlatform(String schemaIdentifier, String versionIdentifier) {
        return publish(requiredScoped(schemaIdentifier, SchemaScopeTypeCode.platform(), null), versionIdentifier);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "SCHEMA_VERSION_PUBLISHED", resourceType = "SCHEMA_VERSION")
    public SchemaVersionOutput publishWorkspace(
        String workspaceIdentifier,
        String schemaIdentifier,
        String versionIdentifier
    ) {
        return publish(
            requiredScoped(
                schemaIdentifier,
                SchemaScopeTypeCode.workspace(),
                workspaceReferenceResolver.resolveInternalId(workspaceIdentifier)
            ),
            versionIdentifier
        );
    }

    @Transactional
    public void deletePlatformDraft(String schemaIdentifier, String versionIdentifier) {
        deleteDraft(requiredScoped(schemaIdentifier, SchemaScopeTypeCode.platform(), null), versionIdentifier);
    }

    @Transactional
    public void deleteWorkspaceDraft(String workspaceIdentifier, String schemaIdentifier, String versionIdentifier) {
        deleteDraft(
            requiredScoped(
                schemaIdentifier,
                SchemaScopeTypeCode.workspace(),
                workspaceReferenceResolver.resolveInternalId(workspaceIdentifier)
            ),
            versionIdentifier
        );
    }

    private SchemaVersionOutput createOrUpdateDraft(Schema schema, CreateSchemaVersionInput input) {
        JsonNode definition = input == null ? null : input.definition();
        validator.requireValidSchemaSyntax(definition, DEFINITION);
        String serialized = validator.toJsonString(definition, DEFINITION);
        List<SchemaVersion> versions = versionRepository.findAllForUpdate(schema.getId());

        SchemaVersion existingDraft = versions.stream().filter(SchemaVersion::isDraft).findFirst().orElse(null);
        if (existingDraft != null) {
            operationValidator.validateDraftUpdate(existingDraft, serialized);
            existingDraft.updateDraft(input.versionName(), serialized);
            return SchemaVersionOutput.from(existingDraft);
        }

        SchemaVersion latestPublished = versions.stream().filter(SchemaVersion::isPublished).findFirst().orElse(null);
        int nextVersion = versions.isEmpty()
            ? 1
            : versions.stream().mapToInt(SchemaVersion::getSchemaVersion).max().orElse(0) + 1;
        String initialDefinition = latestPublished == null ? serialized : latestPublished.getDefinition();

        operationValidator.validateCreate(schema, nextVersion, initialDefinition, SchemaVersionStatusTypeCode.draft());
        SchemaVersion created = new SchemaVersion(
            schema,
            nextVersion,
            input.versionName(),
            initialDefinition,
            SchemaVersionStatusTypeCode.draft(),
            LocalDateTime.now()
        );

        if (!initialDefinition.equals(serialized)) {
            operationValidator.validateDraftUpdate(created, serialized);
            created.updateDraft(input.versionName(), serialized);
        }
        return SchemaVersionOutput.from(versionRepository.save(created));
    }

    private SchemaVersionOutput publish(Schema schema, String versionIdentifier) {
        versionRepository.findAllForUpdate(schema.getId());
        SchemaVersion version = versionRepository
            .findByIdentifierAndSchema_Id(versionIdentifier, schema.getId())
            .orElseThrow(() -> new NotFoundException(SchemaMessageKeys.VERSION_NOT_FOUND));
        version.publish();
        return SchemaVersionOutput.from(version);
    }

    private void deleteDraft(Schema schema, String versionIdentifier) {
        versionRepository.findAllForUpdate(schema.getId());
        SchemaVersion version = versionRepository
            .findByIdentifierAndSchema_Id(versionIdentifier, schema.getId())
            .orElseThrow(() -> new NotFoundException(SchemaMessageKeys.VERSION_NOT_FOUND));
        operationValidator.validateDraft(version);
        versionRepository.delete(version);
    }

    private Schema requiredScoped(String identifier, SchemaScopeTypeCode scope, Long workspaceId) {
        return schemaRepository
            .findByIdentifierAndScopeForUpdate(identifier, scope.value(), workspaceId)
            .orElseThrow(() -> new NotFoundException(SchemaMessageKeys.NOT_FOUND));
    }
}
