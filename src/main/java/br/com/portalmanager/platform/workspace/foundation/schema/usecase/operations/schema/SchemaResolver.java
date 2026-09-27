package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaDefaults;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaResolutionPort;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaVersionRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schematype.SchemaTypeQueryService;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaResolutionValidator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class SchemaResolver implements SchemaResolutionPort {

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

    @Override
    @Transactional(readOnly = true)
    public String resolvePlatform(String schemaTypeCode) {
        String requestedType = normalizeSchemaTypeCode(schemaTypeCode);

        var specificType = schemaTypeQueryService.findActiveAllowed(
                requestedType,
                SchemaScopeTypeCode.platform()
        );

        if (specificType.isPresent()) {
            String canonicalType = specificType.get().getCode();
            var specific = schemaRepository.findByTypeAndScope(
                    canonicalType,
                    SchemaScopeTypeCode.platform().value(),
                    null
            );

            if (specific.isPresent()) {
                Schema schema = specific.get();
                if (schema.isActive()) {
                    var published = versionRepository
                            .findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(
                                    schema.getId(), SchemaVersionStatusTypeCode.published());
                    if (published.isPresent()) {
                        return published.get().getDefinition();
                    }
                }
            }
        }

        return SchemaDefaults.DEFAULT_JSON_SCHEMA;
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveWorkspace(
            String workspaceIdentifier,
            String schemaTypeCode,
            String schemaCode
    ) {
        String workspace = validator.normalizeRequired(workspaceIdentifier);
        Long workspaceId = workspaceReferenceResolver.resolveInternalId(workspace);
        String type = normalizeSchemaTypeCode(schemaTypeCode);
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

        return definition(schema);
    }

    private String normalizeSchemaTypeCode(String schemaTypeCode) {
        return validator.normalizeRequired(schemaTypeCode).toUpperCase(Locale.ROOT);
    }

    private String definition(Schema schema) {
        String schemaTypeCode = schema.getSchemaType().value();
        schemaTypeQueryService.requireActiveAllowed(schemaTypeCode, schema.getScope());
        validator.requireActive(schema);

        return versionRepository
                .findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(
                        schema.getId(),
                        SchemaVersionStatusTypeCode.published()
                )
                .orElseThrow(validator::versionNotFound)
                .getDefinition();
    }
}
