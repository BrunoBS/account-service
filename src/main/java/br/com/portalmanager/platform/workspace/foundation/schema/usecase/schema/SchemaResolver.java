package br.com.portalmanager.platform.workspace.foundation.schema.usecase.schema;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.schematype.usecase.SchemaTypeService;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaDefaults;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaVersion;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaVersionRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaResolution;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.WorkspaceReferenceResolver;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SchemaResolver {

    private static final String NOT_FOUND = "workspace-service.schema.resolution.not-found";
    private static final String PUBLISHED_NOT_FOUND = "workspace-service.schema.published.not-found";
    private static final String INACTIVE = "workspace-service.schema.inactive";

    private final SchemaRepository schemaRepository;
    private final SchemaVersionRepository versionRepository;
    private final SchemaTypeService schemaTypeService;
    private final WorkspaceReferenceResolver workspaceReferenceResolver;

    public SchemaResolver(
            SchemaRepository schemaRepository,
            SchemaVersionRepository versionRepository,
            SchemaTypeService schemaTypeService,
            WorkspaceReferenceResolver workspaceReferenceResolver
    ) {
        this.schemaRepository = schemaRepository;
        this.versionRepository = versionRepository;
        this.schemaTypeService = schemaTypeService;
        this.workspaceReferenceResolver = workspaceReferenceResolver;
    }

    @Transactional(readOnly = true)
    public SchemaResolution resolvePlatform(String schemaTypeCode) {
        String requestedType = normalizeRequired(schemaTypeCode);

        var specific = schemaRepository.findByTypeAndScope(
                requestedType,
                SchemaScopeTypeCode.platform().value(),
                null
        );

        if (specific.isPresent()) {
            return resolution(requestedType, specific.get(), false);
        }

        Schema fallback = schemaRepository.findByTypeAndScope(
                        SchemaDefaults.DEFAULT_SCHEMA_TYPE_CODE,
                        SchemaScopeTypeCode.platform().value(),
                        null
                )
                .orElseThrow(() -> validation("schemaType", NOT_FOUND));

        return resolution(requestedType, fallback, true);
    }

    @Transactional(readOnly = true)
    public SchemaResolution resolveWorkspace(
            String workspaceIdentifier,
            String schemaTypeCode,
            String schemaCode
    ) {
        String workspace = normalizeRequired(workspaceIdentifier);
        Long workspaceId = workspaceReferenceResolver.resolveInternalId(workspace);
        String type = normalizeRequired(schemaTypeCode);
        String code = normalizeRequired(schemaCode);

        Schema schema = schemaRepository.findByTypeScopeAndCode(
                        type,
                        SchemaScopeTypeCode.workspace().value(),
                        workspaceId,
                        code
                )
                .orElseThrow(() -> validation("schema", NOT_FOUND));

        return resolution(type, schema, false);
    }

    private SchemaResolution resolution(
            String requestedType,
            Schema schema,
            boolean fallback
    ) {
        if (!schema.isActive() || !schemaTypeService.existsActive(schema.getSchemaTypeCode())) {
            throw validation("schema", INACTIVE);
        }

        SchemaVersion version = versionRepository
                .findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(
                        schema.getId(),
                        SchemaVersionStatusTypeCode.published()
                )
                .orElseThrow(() -> validation("schemaVersion", PUBLISHED_NOT_FOUND));

        return new SchemaResolution(
                requestedType,
                schema.getSchemaTypeCode(),
                schema.getIdentifier(),
                version.getIdentifier(),
                version.getSchemaVersion(),
                version.getDefinition(),
                fallback
        );
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw validation("schema", NOT_FOUND);
        }
        return value.trim();
    }

    private ValidationException validation(String field, String messageKey) {
        ValidationResult result = new ValidationResult();
        result.addError(field, messageKey);
        return new ValidationException(result);
    }
}
