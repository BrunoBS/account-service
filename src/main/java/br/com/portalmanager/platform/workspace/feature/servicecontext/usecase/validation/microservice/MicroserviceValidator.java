package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.microservice;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.ServiceContextMessageKeys;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.microservice.MicroserviceRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.CreateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.UpdateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.port.MicroserviceUsagePort;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.port.SchemaValidationPort;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.ServiceContextValidation;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class MicroserviceValidator {

    private static final Set<String> SORTABLE_PROPERTIES =
            Set.of("id", "identifier", "code", "name", "createdAt", "updatedAt");

    private final MicroserviceRepository repository;
    private final SchemaValidationPort settingsValidator;
    private final List<MicroserviceUsagePort> usagePorts;

    public MicroserviceValidator(
            MicroserviceRepository repository,
            SchemaValidationPort settingsValidator,
            List<MicroserviceUsagePort> usagePorts) {
        this.repository = repository;
        this.settingsValidator = settingsValidator;
        this.usagePorts = usagePorts;
    }

    public void validateForPurge(Microservice microservice) {
        microservice.requireInactiveForDeletion();
        if (usagePorts.stream().anyMatch(port -> port.isInUse(microservice.getId())))
            throw new ValidationException(ServiceContextMessageKeys.MICROSERVICE_IN_USE);
    }

    public void validateForCreate(CreateMicroserviceInput input) {
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

    public void validateForUpdate(Microservice microservice, UpdateMicroserviceInput input) {
        ServiceContextValidation.requireInput(input);
        if (input.version() == null)
            ServiceContextValidation.reject("version", ServiceContextMessageKeys.VERSION_REQUIRED);
        if (!Objects.equals(microservice.getVersion(), input.version()))
            throw new ResourceVersionConflictException();
        ValidationResult result = new ValidationResult();
        if (!microservice.getCode().equals(input.code()))
            result.addError("code", ServiceContextMessageKeys.CODE_IMMUTABLE);
        if (repository.existsByNameAndIdNot(input.name(), microservice.getId())) {
            result.addError("name", ServiceContextMessageKeys.NAME_DUPLICATE);
        }
        reject(result);
        validateSettings(microservice.getCode(), input.settings());
    }

    public void validateQuery(Pageable pageable) {
        if (pageable.isUnpaged()
                || pageable.getOffset() > Integer.MAX_VALUE
                || pageable.getSort().stream()
                        .anyMatch(order -> !SORTABLE_PROPERTIES.contains(order.getProperty()))) {
            throw new ValidationException(ServiceContextMessageKeys.MICROSERVICE_PAGE_INVALID);
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
