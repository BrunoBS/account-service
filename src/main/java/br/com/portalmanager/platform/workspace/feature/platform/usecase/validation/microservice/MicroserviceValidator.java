package br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.microservice;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.platform.repository.microservice.MicroserviceRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.microservice.CreateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.microservice.UpdateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.PlatformValidation;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaSettingsValidationAdapter;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class MicroserviceValidator {

    private static final Set<String> SORTABLE_PROPERTIES = Set.of(
        "id", "identifier", "code", "name", "createdAt", "updatedAt"
    );

    private final MicroserviceRepository repository;
    private final SchemaSettingsValidationAdapter settingsValidator;

    public MicroserviceValidator(
        MicroserviceRepository repository,
        SchemaSettingsValidationAdapter settingsValidator
    ) {
        this.repository = repository;
        this.settingsValidator = settingsValidator;
    }

    public void validateForCreate(CreateMicroserviceInput input) {
        PlatformValidation.requireInput(input);
        if (input.version() != null) PlatformValidation.reject("version", PlatformMessageKeys.VERSION_NOT_ALLOWED_ON_CREATE);
        ValidationResult result = new ValidationResult();
        if (repository.existsByCode(input.code())) result.addError("code", PlatformMessageKeys.CODE_DUPLICATE);
        if (repository.existsByName(input.name())) result.addError("name", PlatformMessageKeys.NAME_DUPLICATE);
        reject(result);
        validateSettings(input.code(), input.settings());
    }

    public void validateForUpdate(Microservice microservice, UpdateMicroserviceInput input) {
        PlatformValidation.requireInput(input);
        if (input.version() == null) PlatformValidation.reject("version", PlatformMessageKeys.VERSION_REQUIRED);
        if (!Objects.equals(microservice.getVersion(), input.version())) throw new ResourceVersionConflictException();
        ValidationResult result = new ValidationResult();
        if (!microservice.getCode().equals(input.code())) result.addError("code", PlatformMessageKeys.CODE_IMMUTABLE);
        if (repository.existsByNameAndIdNot(input.name(), microservice.getId())) {
            result.addError("name", PlatformMessageKeys.NAME_DUPLICATE);
        }
        reject(result);
        validateSettings(microservice.getCode(), input.settings());
    }

    public void validateQuery(Pageable pageable) {
        if (pageable.isUnpaged() || pageable.getOffset() > Integer.MAX_VALUE ||
            pageable.getSort().stream().anyMatch(order -> !SORTABLE_PROPERTIES.contains(order.getProperty()))) {
            throw new ValidationException(PlatformMessageKeys.MICROSERVICE_PAGE_INVALID);
        }
    }

    private void validateSettings(String code, String settings) {
        ValidationResult result = new ValidationResult();
        settingsValidator.validate("MICROSERVICE", code, "settings", settings, result);
        reject(result);
    }

    private void reject(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }
}
