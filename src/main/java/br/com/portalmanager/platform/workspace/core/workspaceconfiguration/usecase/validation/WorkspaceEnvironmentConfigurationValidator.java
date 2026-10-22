package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain.PublisherType;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.PublisherInput;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.ResolvedPublisher;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.PublisherReferencePort;
import br.com.portalmanager.platform.workspace.foundation.integration.PublisherReferencePort.PublisherReference;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaSettingsValidationAdapter;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class WorkspaceEnvironmentConfigurationValidator {
    private static final String SCHEMA_TYPE = "PLATFORM";
    private static final String SETTINGS_SCHEMA_CODE =
            "workspace-environment-configuration-settings";
    private final ObjectMapper objectMapper;
    private final PublisherReferencePort publishers;
    private final SchemaSettingsValidationAdapter schemas;

    public WorkspaceEnvironmentConfigurationValidator(
            PublisherReferencePort publishers,
            SchemaSettingsValidationAdapter schemas,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.publishers = publishers;
        this.schemas = schemas;
    }

    public void validateCreateVersion(Long version) {
        if (version != null) invalid("version");
    }

    public void validateUpdateVersion(Long requested, Long current) {
        if (requested == null || requested < 0) invalid("version");
        if (!Objects.equals(requested, current)) throw new ResourceVersionConflictException();
    }

    public void validateSettings(JsonNode settings) {
        ValidationResult result = new ValidationResult();
        schemas.validate(SCHEMA_TYPE, SETTINGS_SCHEMA_CODE, "settings", settings, result);
        reject(result);
    }

    public List<ResolvedPublisher> validatePublishers(
            List<PublisherInput> input, Set<Long> existingIds) {
        if (input == null || input.isEmpty()) invalid("publishers");
        Set<String> identifiers = new HashSet<>();
        Set<Integer> orders = new HashSet<>();
        ValidationResult result = new ValidationResult();
        for (int index = 0; index < input.size(); index++) {
            PublisherInput publisher = input.get(index);
            if (publisher == null
                    || publisher.publisherIdentifier() == null
                    || publisher.sortOrder() == null
                    || publisher.sortOrder() < 1) invalid("publishers[" + index + "]");
            if (!identifiers.add(publisher.publisherIdentifier())
                    || !orders.add(publisher.sortOrder())) invalid("publishers[" + index + "]");
        }
        reject(result);
        Map<String, PublisherReference> references =
                publishers.resolveForAssociation(identifiers).stream()
                        .collect(
                                Collectors.toMap(
                                        PublisherReference::identifier, reference -> reference));
        List<ResolvedPublisher> resolved = new ArrayList<>();
        for (int index = 0; index < input.size(); index++) {
            PublisherInput publisher = input.get(index);
            PublisherReference reference = references.get(publisher.publisherIdentifier());
            if (reference == null)
                throw new NotFoundException(PlatformMessageKeys.RESOURCE_NOT_FOUND);
            PublisherType type = PublisherType.fromCode(reference.code());
            boolean workspacePublisher =
                    (type == PublisherType.WEB_SOCKET || type == PublisherType.KAFKA)
                            && ResourceScopeTypeCode.workspace().equals(reference.scope());
            if (!workspacePublisher) invalid("publishers");
            if (!LifecycleTypeCode.active().equals(reference.lifecycle())
                    && !existingIds.contains(reference.id())) invalid("publishers");
            schemas.validateRequired(
                    SCHEMA_TYPE,
                    type.schemaCode(),
                    "publishers[" + index + "]",
                    objectMapper.valueToTree(publisher),
                    result);
            reject(result);
            resolved.add(
                    new ResolvedPublisher(
                            reference.id(),
                            reference.identifier(),
                            publisher.sortOrder(),
                            publisher.parameters().deepCopy()));
        }
        return resolved.stream()
                .sorted(Comparator.comparing(ResolvedPublisher::sortOrder))
                .toList();
    }

    public void validatePagination(Pageable pageable) {
        Set<String> allowed = Set.of("identifier", "version", "createdAt", "updatedAt", "id");
        if (pageable == null
                || pageable.isUnpaged()
                || pageable.getOffset() > Integer.MAX_VALUE
                || pageable.getSort().stream()
                        .anyMatch(order -> !allowed.contains(order.getProperty()))) invalid("page");
    }

    private void invalid(String field) {
        ValidationResult result = new ValidationResult();
        result.addError(field, PlatformMessageKeys.VALIDATION_FAILED);
        throw new ValidationException(result);
    }

    private void reject(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }
}
