package br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.feature;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.platform.repository.feature.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.UpdateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.PlatformValidation;
import br.com.portalmanager.platform.workspace.foundation.integration.FeatureSharingReferencePort;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaSettingsValidationAdapter;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class FeatureValidator {
    private static final Set<String> SORTABLE_PROPERTIES = Set.of("id", "identifier", "code", "name", "createdAt", "updatedAt");
    private final FeatureRepository repository;
    private final SchemaSettingsValidationAdapter settingsValidator;
    private final FeatureSharingReferencePort sharingReferences;

    public FeatureValidator(FeatureRepository repository, SchemaSettingsValidationAdapter settingsValidator,
        FeatureSharingReferencePort sharingReferences) {
        this.repository = repository;
        this.settingsValidator = settingsValidator;
        this.sharingReferences = sharingReferences;
    }

    public void validateForCreate(CreateFeatureInput input) {
        PlatformValidation.requireInput(input);
        if (input.version() != null) PlatformValidation.reject("version", PlatformMessageKeys.VERSION_NOT_ALLOWED_ON_CREATE);
        ValidationResult result = new ValidationResult();
        if (repository.existsByCode(input.code())) result.addError("code", PlatformMessageKeys.CODE_DUPLICATE);
        if (repository.existsByName(input.name())) result.addError("name", PlatformMessageKeys.NAME_DUPLICATE);
        reject(result);
        validateSettings(input.code(), input.settings());
    }

    public void validateForUpdate(Feature feature, UpdateFeatureInput input) {
        PlatformValidation.requireInput(input);
        if (input.version() == null) PlatformValidation.reject("version", PlatformMessageKeys.VERSION_REQUIRED);
        if (!Objects.equals(feature.getVersion(), input.version())) throw new ResourceVersionConflictException();
        ValidationResult result = new ValidationResult();
        if (!feature.getCode().equals(input.code())) result.addError("code", PlatformMessageKeys.CODE_IMMUTABLE);
        if (repository.existsByNameAndIdNot(input.name(), feature.getId())) result.addError("name", PlatformMessageKeys.NAME_DUPLICATE);
        reject(result);
        if (feature.isShareable() && !input.shareable() && sharingReferences.hasSharedContracts(feature.getId())) {
            throw new ValidationException(PlatformMessageKeys.FEATURE_HAS_SHARED_CONTRACTS);
        }
        validateSettings(feature.getCode(), input.settings());
    }

    public void validateQuery(Pageable pageable) {
        if (pageable.isUnpaged() || pageable.getOffset() > Integer.MAX_VALUE ||
            pageable.getSort().stream().anyMatch(order -> !SORTABLE_PROPERTIES.contains(order.getProperty()))) {
            throw new ValidationException(PlatformMessageKeys.FEATURE_PAGE_INVALID);
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
