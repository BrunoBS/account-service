package br.com.portalmanager.platform.workspace.foundation.schema.usecase.version;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaVersion;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaVersionRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaVersionInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaVersionOutput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.workspace.WorkspaceReferenceResolver;
import br.com.portalmanager.platform.messaging.exception.ValidationException;
import br.com.portalmanager.platform.messaging.validation.ValidationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SchemaVersionCommandService {

    private final SchemaRepository schemaRepository;
    private final SchemaVersionRepository versionRepository;
    private final SchemaValidator validator;
    private final WorkspaceReferenceResolver workspaceReferenceResolver;

    public SchemaVersionCommandService(
            SchemaRepository schemaRepository,
            SchemaVersionRepository versionRepository,
            SchemaValidator validator,
            WorkspaceReferenceResolver workspaceReferenceResolver
    ) {
        this.schemaRepository = schemaRepository;
        this.versionRepository = versionRepository;
        this.validator = validator;
        this.workspaceReferenceResolver = workspaceReferenceResolver;
    }

    @Transactional
    public SchemaVersionOutput createPlatformDraft(
            String schemaIdentifier,
            CreateSchemaVersionInput input
    ) {
        return createOrUpdateDraft(requiredScoped(schemaIdentifier, SchemaScopeTypeCode.platform(), null), input);
    }

    @Transactional
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
    public SchemaVersionOutput publishPlatform(
            String schemaIdentifier,
            String versionIdentifier
    ) {
        return publish(requiredScoped(schemaIdentifier, SchemaScopeTypeCode.platform(), null), versionIdentifier);
    }

    @Transactional
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
    public void deletePlatformDraft(
            String schemaIdentifier,
            String versionIdentifier
    ) {
        deleteDraft(requiredScoped(schemaIdentifier, SchemaScopeTypeCode.platform(), null), versionIdentifier);
    }

    @Transactional
    public void deleteWorkspaceDraft(
            String workspaceIdentifier,
            String schemaIdentifier,
            String versionIdentifier
    ) {
        deleteDraft(
                requiredScoped(
                        schemaIdentifier,
                        SchemaScopeTypeCode.workspace(),
                        workspaceReferenceResolver.resolveInternalId(workspaceIdentifier)
                ),
                versionIdentifier
        );
    }

    private SchemaVersionOutput createOrUpdateDraft(
            Schema schema,
            CreateSchemaVersionInput input
    ) {
        JsonNode definition = input == null ? null : input.definition();
        ValidationResult validation = new ValidationResult();
        validator.validateSchemaSyntax(definition, validation);
        if (validation.hasErrors()) {
            throw new ValidationException(validation);
        }

        String serialized = validator.toJsonString(definition);
        List<SchemaVersion> versions = versionRepository.findAllForUpdate(schema.getId());

        SchemaVersion existingDraft = versions.stream()
                .filter(SchemaVersion::isDraft)
                .findFirst()
                .orElse(null);

        if (existingDraft != null) {
            existingDraft.updateDraft(input.versionName(), serialized);
            return SchemaVersionOutput.from(existingDraft);
        }

        SchemaVersion latestPublished = versions.stream()
                .filter(SchemaVersion::isPublished)
                .findFirst()
                .orElse(null);

        int nextVersion = versions.isEmpty()
                ? 1
                : versions.stream()
                        .mapToInt(SchemaVersion::getSchemaVersion)
                        .max()
                        .orElse(0) + 1;

        String initialDefinition = latestPublished == null
                ? serialized
                : latestPublished.getDefinition();

        SchemaVersion created = new SchemaVersion(
                schema,
                nextVersion,
                input.versionName(),
                initialDefinition,
                SchemaVersionStatusTypeCode.draft(),
                LocalDateTime.now()
        );

        if (!initialDefinition.equals(serialized)) {
            created.updateDraft(input.versionName(), serialized);
        }

        return SchemaVersionOutput.from(versionRepository.save(created));
    }

    private SchemaVersionOutput publish(
            Schema schema,
            String versionIdentifier
    ) {
        versionRepository.findAllForUpdate(schema.getId());

        SchemaVersion version = versionRepository
                .findByIdentifierAndSchema_Id(versionIdentifier, schema.getId())
                .orElseThrow(() -> new IllegalArgumentException("Schema version not found"));

        version.publish();
        return SchemaVersionOutput.from(version);
    }

    private void deleteDraft(
            Schema schema,
            String versionIdentifier
    ) {
        versionRepository.findAllForUpdate(schema.getId());

        SchemaVersion version = versionRepository
                .findByIdentifierAndSchema_Id(versionIdentifier, schema.getId())
                .orElseThrow(() -> new IllegalArgumentException("Schema version not found"));

        if (!version.isDraft()) {
            throw new IllegalStateException("Published schema version cannot be physically deleted");
        }

        versionRepository.delete(version);
    }

    private Schema requiredScoped(
            String identifier,
            SchemaScopeTypeCode scope,
            Long workspaceId
    ) {
        return schemaRepository.findByIdentifierAndScopeForUpdate(identifier, scope.value(), workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found in requested scope"));
    }
}
