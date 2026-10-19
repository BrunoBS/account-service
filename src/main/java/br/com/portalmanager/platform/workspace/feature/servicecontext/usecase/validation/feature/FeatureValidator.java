package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.feature;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.ServiceContextMessageKeys;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.feature.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.UpdateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.ServiceContextValidation;
import br.com.portalmanager.platform.workspace.foundation.integration.FeatureUsagePort;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaSettingsValidationAdapter;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
public class FeatureValidator {
    private static final Set<String> SORTABLE_PROPERTIES =
            Set.of("id", "identifier", "code", "name", "createdAt", "updatedAt");
    private final FeatureRepository repository;
    private final SchemaSettingsValidationAdapter settingsValidator;
    private final FeatureUsagePort featureUsage;

    public FeatureValidator(
            FeatureRepository repository,
            SchemaSettingsValidationAdapter settingsValidator,
            FeatureUsagePort featureUsage) {
        this.repository = repository;
        this.settingsValidator = settingsValidator;
        this.featureUsage = featureUsage;
    }

    public void validateForCreate(CreateFeatureInput input) {
        ServiceContextValidation.requireInput(input);
        if (input.version() != null)
            ServiceContextValidation.reject(
                    "version", ServiceContextMessageKeys.VERSION_NOT_ALLOWED_ON_CREATE);
        ValidationResult result = new ValidationResult();
        if (repository.existsByCode(input.code()))
            result.addError("code", ServiceContextMessageKeys.CODE_DUPLICATE);
        if (repository.existsByName(input.name()))
            result.addError("name", ServiceContextMessageKeys.NAME_DUPLICATE);
        reject(result);
        validateSettings(input.code(), input.settings());
    }

    public void validateForUpdate(Feature feature, UpdateFeatureInput input) {
        ServiceContextValidation.requireInput(input);
        if (input.version() == null)
            ServiceContextValidation.reject("version", ServiceContextMessageKeys.VERSION_REQUIRED);
        if (!Objects.equals(feature.getVersion(), input.version()))
            throw new ResourceVersionConflictException();
        ValidationResult result = new ValidationResult();
        if (!feature.getCode().equals(input.code()))
            result.addError("code", ServiceContextMessageKeys.CODE_IMMUTABLE);
        if (repository.existsByNameAndIdNot(input.name(), feature.getId()))
            result.addError("name", ServiceContextMessageKeys.NAME_DUPLICATE);
        reject(result);
        if (feature.isShareable()
                && !input.shareable()
                && featureUsage.isInUse(feature.getId())) {
            throw new ValidationException(
                    ServiceContextMessageKeys.FEATURE_HAS_RESOURCE_SHARING_CONTRACTS);
        }
        validateSettings(feature.getCode(), input.settings());
    }

    public void validateQuery(Pageable pageable) {
        if (pageable.isUnpaged()
                || pageable.getOffset() > Integer.MAX_VALUE
                || pageable.getSort().stream()
                        .anyMatch(order -> !SORTABLE_PROPERTIES.contains(order.getProperty()))) {
            throw new ValidationException(ServiceContextMessageKeys.FEATURE_PAGE_INVALID);
        }
    }

    private void validateSettings(String featureCode, String settings) {
        ValidationResult result = new ValidationResult();
        settingsValidator.validate("FEATURE", featureCode, "settings", settings, result);
        reject(result);
    }

    private void reject(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }
}
