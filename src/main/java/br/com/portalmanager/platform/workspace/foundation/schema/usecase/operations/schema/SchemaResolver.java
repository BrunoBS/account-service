package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaDefaults;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaVersion;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaVersionRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaResolution;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schematype.SchemaTypeQueryService;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaResolutionValidator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SchemaResolver {

    private final SchemaRepository schemaRepository;
    private final SchemaVersionRepository versionRepository;
    private final SchemaTypeQueryService schemaTypeQueryService;
    private final WorkspaceReferenceResolver workspaceReferenceResolver;
    private final SchemaResolutionValidator validator = new SchemaResolutionValidator();

    public SchemaResolver(
            SchemaRepository schemaRepository,
            SchemaVersionRepository versionRepository,
            SchemaTypeQueryService schemaTypeQueryService,
            WorkspaceReferenceResolver workspaceReferenceResolver
    ) {
        this.schemaRepository = schemaRepository;
        this.versionRepository = versionRepository;
        this.schemaTypeQueryService = schemaTypeQueryService;
        this.workspaceReferenceResolver = workspaceReferenceResolver;
    }

    @Transactional(readOnly = true)
    public SchemaResolution resolvePlatform(String schemaTypeCode) {
        String requestedType = validator.normalizeRequired(schemaTypeCode);
        String canonicalType = schemaTypeQueryService
                .requireActiveAllowed(requestedType, SchemaScopeTypeCode.platform())
                .getCode();

        var specific = schemaRepository.findByTypeAndScope(
                canonicalType,
                SchemaScopeTypeCode.platform().value(),
                null
        );

        if (specific.isPresent()) {
            return resolution(canonicalType, specific.get(), false);
        }

        Schema fallback = schemaRepository.findByTypeAndScope(
                        SchemaDefaults.DEFAULT_SCHEMA_TYPE_CODE,
                        SchemaScopeTypeCode.platform().value(),
                        null
                )
                .orElseThrow(validator::typeNotFound);

        return resolution(canonicalType, fallback, true);
    }

    @Transactional(readOnly = true)
    public SchemaResolution resolveWorkspace(
            String workspaceIdentifier,
            String schemaTypeCode,
            String schemaCode
    ) {
        String workspace = validator.normalizeRequired(workspaceIdentifier);
        Long workspaceId = workspaceReferenceResolver.resolveInternalId(workspace);
        String type = validator.normalizeRequired(schemaTypeCode);
        String canonicalType = schemaTypeQueryService
                .requireActiveAllowed(type, SchemaScopeTypeCode.workspace())
                .getCode();
        String code = validator.normalizeRequired(schemaCode);

        Schema schema = schemaRepository.findByTypeScopeAndCode(
                        canonicalType,
                        SchemaScopeTypeCode.workspace().value(),
                        workspaceId,
                        code
                )
                .orElseThrow(validator::schemaNotFound);

        return resolution(canonicalType, schema, false);
    }

    private SchemaResolution resolution(
            String requestedType,
            Schema schema,
            boolean fallback
    ) {
        String schemaTypeCode = schema.getSchemaType().value();
        validator.requireActive(schema, schemaTypeQueryService.requireActive(schemaTypeCode).isActive());

        SchemaVersion version = versionRepository
                .findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(
                        schema.getId(),
                        SchemaVersionStatusTypeCode.published()
                )
                .orElseThrow(validator::versionNotFound);

        return new SchemaResolution(
                requestedType,
                schemaTypeCode,
                schema.getIdentifier(),
                version.getIdentifier(),
                version.getSchemaVersion(),
                version.getDefinition(),
                fallback
        );
    }

}
