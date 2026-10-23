package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleFilter;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.schema.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.schema.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.version.SchemaVersionRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.mapping.schema.SchemaMapper;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.mapping.version.SchemaVersionMapper;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.schema.SchemaOutput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.version.SchemaVersionOutput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.port.WorkspaceReferencePort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SchemaQueryService {

    private final SchemaRepository schemaRepository;
    private final SchemaVersionRepository versionRepository;
    private final WorkspaceReferencePort workspaceReferencePort;
    private final SchemaMapper schemaMapper;
    private final SchemaVersionMapper versionMapper;

    public SchemaQueryService(
            SchemaRepository schemaRepository,
            SchemaVersionRepository versionRepository,
            WorkspaceReferencePort workspaceReferencePort,
            SchemaMapper schemaMapper,
            SchemaVersionMapper versionMapper) {
        this.schemaRepository = schemaRepository;
        this.versionRepository = versionRepository;
        this.workspaceReferencePort = workspaceReferencePort;
        this.schemaMapper = schemaMapper;
        this.versionMapper = versionMapper;
    }

    @Transactional(readOnly = true)
    public SchemaOutput findPlatformByIdentifier(String identifier) {
        return output(requiredScoped(identifier, SchemaScopeTypeCode.platform(), null));
    }

    @Transactional(readOnly = true)
    public SchemaOutput findWorkspaceByIdentifier(String workspaceIdentifier, String identifier) {
        return output(
                requiredScoped(
                        identifier,
                        SchemaScopeTypeCode.workspace(),
                        workspaceReferencePort.resolveInternalId(workspaceIdentifier)));
    }

    @Transactional(readOnly = true)
    public List<SchemaOutput> findPlatform(String lifecycle) {
        return schemaRepository
                .findAllByScopeAndLifecycle(
                        SchemaScopeTypeCode.platform().value(),
                        null,
                        LifecycleFilter.resolve(lifecycle).value())
                .stream()
                .map(this::output)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchemaOutput> findWorkspace(String workspaceIdentifier, String lifecycle) {
        return schemaRepository
                .findAllByScopeAndLifecycle(
                        SchemaScopeTypeCode.workspace().value(),
                        workspaceReferencePort.resolveInternalId(workspaceIdentifier),
                        LifecycleFilter.resolve(lifecycle).value())
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
            String workspaceIdentifier, String schemaIdentifier) {
        Long workspaceId = workspaceReferencePort.resolveInternalId(workspaceIdentifier);
        Schema schema =
                requiredScoped(schemaIdentifier, SchemaScopeTypeCode.workspace(), workspaceId);
        return versions(schema);
    }

    private List<SchemaVersionOutput> versions(Schema schema) {
        return versionRepository.findBySchema_IdOrderBySchemaVersionDesc(schema.getId()).stream()
                .map(versionMapper::toOutput)
                .toList();
    }

    private Schema requiredScoped(String identifier, SchemaScopeTypeCode scope, Long workspaceId) {
        return schemaRepository
                .findByIdentifierAndScope(identifier, scope.value(), workspaceId)
                .orElseThrow(() -> new NotFoundException(SchemaMessageKeys.NOT_FOUND));
    }

    private SchemaOutput output(Schema schema) {
        String workspaceIdentifier =
                schema.getWorkspaceId() == null
                        ? null
                        : workspaceReferencePort.resolveIdentifier(schema.getWorkspaceId());
        return schemaMapper.toOutput(schema, workspaceIdentifier);
    }
}
