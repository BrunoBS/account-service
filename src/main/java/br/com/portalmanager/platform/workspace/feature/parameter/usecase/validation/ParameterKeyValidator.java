package br.com.portalmanager.platform.workspace.feature.parameter.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.library.schemavalidation.resolver.ResourceSchemaResolver;
import br.com.portalmanager.platform.library.schemavalidation.validation.JsonSchemaValidator;
import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;
import br.com.portalmanager.platform.workspace.feature.parameter.domain.ParameterKey;
import br.com.portalmanager.platform.workspace.feature.parameter.domain.ParameterKeyMessageKeys;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.CreateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishParameterKeysInput;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.projection.ParameterKeyReadProjection;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.UpdateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.ParameterKeyTypePort;

import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Component
public class ParameterKeyValidator {
    private final ParameterKeyRepository repository;
    private final ParameterKeyTypePort types;
    private final ResourceSchemaResolver schemaResolver;
    private final JsonSchemaValidator schemas;
    private final ObjectMapper mapper;

    public ParameterKeyValidator(
            ParameterKeyRepository repository,
            ParameterKeyTypePort types,
            ResourceSchemaResolver schemaResolver,
            JsonSchemaValidator schemas,
            ObjectMapper mapper) {
        this.repository = repository;
        this.types = types;
        this.schemaResolver = schemaResolver;
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

    public void validatePublicationInput(PublishParameterKeysInput input) {
        if (input == null) throw new ValidationException(ParameterKeyMessageKeys.REQUEST_REQUIRED);
        if (input.microserviceIdentifiers() == null || input.microserviceIdentifiers().isEmpty()
                || input.microserviceIdentifiers().stream().anyMatch(Objects::isNull)) {
            throw new ValidationException(new ValidationResult(
                    "microserviceIdentifiers", ParameterKeyMessageKeys.REFERENCE_REQUIRED));
        }
        if (input.all()) return;
        if (input.identifiers() == null || input.identifiers().isEmpty()
                || input.identifiers().stream().anyMatch(Objects::isNull)) {
            throw new ValidationException(new ValidationResult(
                    "identifiers", ParameterKeyMessageKeys.PUBLICATION_REQUIRED));
        }
        if (new HashSet<>(input.identifiers()).size() != input.identifiers().size()) {
            throw new ValidationException(new ValidationResult(
                    "identifiers", ParameterKeyMessageKeys.PUBLICATION_DUPLICATE));
        }
    }

    public void validatePublications(List<ParameterKeyReadProjection> drafts) {
        drafts.stream().map(ParameterKeyReadProjection::getType).distinct()
                .forEach(this::validateTypeAvailability);
        ResourceSchema schema = schemaResolver.resolve("PLATFORM", "dynamic-parameter");
        for (ParameterKeyReadProjection resource : drafts) {
            ObjectNode request = mapper.createObjectNode()
                    .put("version", resource.getVersion()).put("type", resource.getType());
            request.set("value", resource.getValue());
            schemas.validate(schema, request);
        }
    }

    public void validatePublication(ParameterKey resource) {
        resource.requirePublishable();
        validateTypeAvailability(resource.getType().value());
        ObjectNode request =
                mapper.createObjectNode()
                        .put("version", resource.getVersion())
                        .put("type", resource.getType().value());
        request.set("value", resource.getValue());
        schemas.validate(schemaResolver.resolve("PLATFORM", "dynamic-parameter"), request);
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
