package br.com.portalmanager.platform.workspace.feature.parameter.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import br.com.portalmanager.platform.workspace.feature.parameter.domain.ParameterKey;
import br.com.portalmanager.platform.workspace.feature.parameter.domain.ParameterKeyMessageKeys;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.CreateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.UpdateParameterKeyInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.usecase.ParameterKeyTypeService;

import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Component
public class ParameterKeyValidator {
    private final ParameterKeyRepository repository;
    private final ParameterKeyTypeService types;
    private final SchemaValidator schemas;
    private final ObjectMapper mapper;

    public ParameterKeyValidator(
            ParameterKeyRepository repository,
            ParameterKeyTypeService types,
            SchemaValidator schemas,
            ObjectMapper mapper) {
        this.repository = repository;
        this.types = types;
        this.schemas = schemas;
        this.mapper = mapper;
    }

    public void validateCreateInput(CreateParameterKeyInput input) {
        if (input == null) throw new ValidationException(ParameterKeyMessageKeys.REQUEST_REQUIRED);
        if (input.version() != null)
            throw new ValidationException(
                    new ValidationResult("version", ParameterKeyMessageKeys.VERSION_NOT_ALLOWED));
        if (input.microserviceIdentifier() == null)
            throw new ValidationException(ParameterKeyMessageKeys.REFERENCE_REQUIRED);
        validateTypeAvailability(input.type());
    }

    public void validateUnique(Long microserviceId, String key) {
        if (repository.existsByMicroserviceIdAndKey(microserviceId, key))
            throw new ConflictException(ParameterKeyMessageKeys.KEY_DUPLICATE);
    }

    public void validatePublicationIdentifiers(List<String> identifiers) {
        if (identifiers == null
                || identifiers.isEmpty()
                || identifiers.stream().anyMatch(Objects::isNull))
            throw new ValidationException(
                    new ValidationResult(
                            "identifiers", ParameterKeyMessageKeys.PUBLICATION_REQUIRED));
        if (new HashSet<>(identifiers).size() != identifiers.size())
            throw new ValidationException(
                    new ValidationResult(
                            "identifiers", ParameterKeyMessageKeys.PUBLICATION_DUPLICATE));
    }

    public void validatePublication(ParameterKey resource) {
        resource.requirePublishable();
        validateTypeAvailability(resource.getType().value());
        var request =
                mapper.createObjectNode()
                        .put("version", resource.getVersion())
                        .put("type", resource.getType().value());
        request.set("value", resource.getValue());
        schemas.validate("PLATFORM", "dynamic-parameter", request);
    }

    public void validateUpdate(ParameterKey resource, UpdateParameterKeyInput input) {
        if (input == null) throw new ValidationException(ParameterKeyMessageKeys.REQUEST_REQUIRED);
        if (input.version() == null)
            throw new ValidationException(
                    new ValidationResult("version", ParameterKeyMessageKeys.VERSION_REQUIRED));
        if (!Objects.equals(resource.getVersion(), input.version()))
            throw new ResourceVersionConflictException();
        validateTypeAvailability(input.type());
    }

    private void validateTypeAvailability(String type) {
        if (!types.existsActive(type))
            throw new ValidationException(ParameterKeyMessageKeys.TYPE_UNAVAILABLE);
    }
}
