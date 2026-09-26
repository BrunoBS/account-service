package br.com.itau.portalmanager.workspace.foundation.schema.usecase.schema;

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
    public SchemaOutput findByIdentifier(String identifier) {
        return output(required(identifier));
    }

    @Transactional(readOnly = true)
    public SchemaOutput findPlatformByIdentifier(String identifier) {
        return output(requiredScoped(identifier, "PLATFORM", null));
    }

    @Transactional(readOnly = true)
    public SchemaOutput findWorkspaceByIdentifier(String workspaceIdentifier, String identifier) {
        return output(requiredScoped(identifier, "WORKSPACE", workspaceReferenceResolver.resolveInternalId(workspaceIdentifier)));
    }

    @Transactional(readOnly = true)
    public List<SchemaOutput> findAll() {
        return schemaRepository.findAll().stream().map(this::output).toList();
    }

    @Transactional(readOnly = true)
    public List<SchemaOutput> findPlatform() {
        return schemaRepository.findAllByScope("PLATFORM", null)
                .stream()
                .map(this::output)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchemaOutput> findWorkspace(String workspaceIdentifier) {
        return schemaRepository.findAllByScope("WORKSPACE", workspaceReferenceResolver.resolveInternalId(workspaceIdentifier))
                .stream()
                .map(this::output)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchemaVersionOutput> findVersions(String schemaIdentifier) {
        Schema schema = required(schemaIdentifier);
        return versionRepository.findBySchema_IdOrderBySchemaVersionDesc(schema.getId())
                .stream()
                .map(SchemaVersionOutput::from)
                .toList();
    }

    private Schema required(String identifier) {
        return schemaRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found"));
    }

    private Schema requiredScoped(
            String identifier,
            String scope,
            Long workspaceId
    ) {
        return schemaRepository.findByIdentifierAndScope(identifier, scope, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found in requested scope"));
    }
    private SchemaOutput output(Schema schema) {
        String workspaceIdentifier = schema.getWorkspaceId() == null
                ? null
                : workspaceReferenceResolver.resolveIdentifier(schema.getWorkspaceId());
        return SchemaOutput.from(schema, workspaceIdentifier);
    }
}

