package br.com.itau.portalmanager.workspace.foundation.schema.usecase.schema;

import br.com.itau.portalmanager.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.Schema;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaVersionRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.SchemaOutput;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.SchemaVersionOutput;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.workspace.WorkspaceReferenceResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SchemaQueryService {

    private final SchemaRepository schemaRepository;
    private final SchemaVersionRepository versionRepository;
    private final WorkspaceReferenceResolver workspaceReferenceResolver;

    public SchemaQueryService(
            SchemaRepository schemaRepository,
            SchemaVersionRepository versionRepository,
            WorkspaceReferenceResolver workspaceReferenceResolver
    ) {
        this.schemaRepository = schemaRepository;
        this.versionRepository = versionRepository;
        this.workspaceReferenceResolver = workspaceReferenceResolver;
    }

    @Transactional(readOnly = true)
    public SchemaOutput findPlatformByIdentifier(String identifier) {
        return output(requiredScoped(identifier, SchemaScopeTypeCode.platform(), null));
    }

    @Transactional(readOnly = true)
    public SchemaOutput findWorkspaceByIdentifier(String workspaceIdentifier, String identifier) {
        return output(requiredScoped(
                identifier,
                SchemaScopeTypeCode.workspace(),
                workspaceReferenceResolver.resolveInternalId(workspaceIdentifier)
        ));
    }

    @Transactional(readOnly = true)
    public List<SchemaOutput> findPlatform() {
        return schemaRepository.findAllByScope(SchemaScopeTypeCode.platform().value(), null)
                .stream()
                .map(this::output)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchemaOutput> findWorkspace(String workspaceIdentifier) {
        return schemaRepository.findAllByScope(
                        SchemaScopeTypeCode.workspace().value(),
                        workspaceReferenceResolver.resolveInternalId(workspaceIdentifier)
                )
                .stream()
                .map(this::output)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchemaVersionOutput> findPlatformVersions(String schemaIdentifier) {
        Schema schema = requiredScoped(schemaIdentifier, SchemaScopeTypeCode.platform(), null);
        return versions(schema);
    }

    @Transactional(readOnly = true)
    public List<SchemaVersionOutput> findWorkspaceVersions(
            String workspaceIdentifier,
            String schemaIdentifier
    ) {
        Long workspaceId = workspaceReferenceResolver.resolveInternalId(workspaceIdentifier);
        Schema schema = requiredScoped(schemaIdentifier, SchemaScopeTypeCode.workspace(), workspaceId);
        return versions(schema);
    }

    private List<SchemaVersionOutput> versions(Schema schema) {
        return versionRepository.findBySchema_IdOrderBySchemaVersionDesc(schema.getId())
                .stream()
                .map(SchemaVersionOutput::from)
                .toList();
    }

    private Schema requiredScoped(
            String identifier,
            SchemaScopeTypeCode scope,
            Long workspaceId
    ) {
        return schemaRepository.findByIdentifierAndScope(identifier, scope.value(), workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found in requested scope"));
    }
    private SchemaOutput output(Schema schema) {
        String workspaceIdentifier = schema.getWorkspaceId() == null
                ? null
                : workspaceReferenceResolver.resolveIdentifier(schema.getWorkspaceId());
        return SchemaOutput.from(schema, workspaceIdentifier);
    }
}

