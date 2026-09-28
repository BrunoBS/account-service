package br.com.portalmanager.platform.workspace.core.publisher.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.publisher.domain.PublisherMessageKeys;
import br.com.portalmanager.platform.workspace.core.publisher.domain.PublisherSchemaType;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.CreatePublisherInput;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.UpdatePublisherInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.usecase.ResourceScopeTypeService;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schematype.SchemaTypeQueryService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class PublisherValidator {
    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{0,39}$");
    private final SchemaTypeQueryService schemaTypes;
    private final ResourceScopeTypeService scopeService;
    public PublisherValidator(SchemaTypeQueryService schemaTypes, ResourceScopeTypeService scopeService) {
        this.schemaTypes = schemaTypes;
        this.scopeService = scopeService;
    }

    public ResourceScopeTypeCode validateCreate(CreatePublisherInput input, boolean duplicate) {
        ValidationResult result = new ValidationResult();
        if (input == null) { result.addError("request", PublisherMessageKeys.CODE_INVALID); reject(result); return null; }
        if (input.code() == null || !CODE.matcher(input.code()).matches())
            result.addError("code", PublisherMessageKeys.CODE_INVALID);
        if (duplicate) result.addError("code", PublisherMessageKeys.CODE_DUPLICATE);
        common(input.name(), input.description(), input.scope(), result);
        reject(result);
        if (schemaTypes.findActiveAllowed(PublisherSchemaType.forCode(input.code()), SchemaScopeTypeCode.platform()).isEmpty()) {
            result.addError("code", PublisherMessageKeys.SCHEMA_TYPE_REQUIRED);
            reject(result);
        }
        return ResourceScopeTypeCode.of(input.scope());
    }

    public ResourceScopeTypeCode validateUpdate(UpdatePublisherInput input) {
        ValidationResult result = new ValidationResult();
        if (input == null) { result.addError("request", PublisherMessageKeys.NAME_INVALID); reject(result); return null; }
        if (input.version() == null || input.version() < 0)
            result.addError("version", PublisherMessageKeys.VERSION_REQUIRED);
        common(input.name(), input.description(), input.scope(), result);
        reject(result);
        return ResourceScopeTypeCode.of(input.scope());
    }

    public void requireVersion(Long current, Long requested) {
        if (!Objects.equals(current, requested)) throw new ResourceVersionConflictException();
    }
    private void common(String name, String description, String scope, ValidationResult result) {
        if (name == null || name.length() < 3 || name.length() > 50)
            result.addError("name", PublisherMessageKeys.NAME_INVALID);
        if (description == null || description.isBlank() || description.length() > 500)
            result.addError("description", PublisherMessageKeys.DESCRIPTION_INVALID);
        if (scope == null || Arrays.stream(ResourceScopeTypeEnum.values()).noneMatch(v -> v.name().equals(scope))
                || !scopeService.existsActive(scope))
            result.addError("scope", PublisherMessageKeys.SCOPE_INVALID);
    }
    private void reject(ValidationResult result) { if (result.hasErrors()) throw new ValidationException(result); }
}
