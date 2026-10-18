package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.validation.message;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.MessageMessageKeys;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.Message;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.message.MessageRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.CreateMessageInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.UpdateMessageInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.microservice.MicroserviceQueryService;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class MessageValidator {

    private final MessageRepository messageRepository;
    private final MicroserviceQueryService microserviceQueryService;

    public MessageValidator(
            MessageRepository messageRepository,
            MicroserviceQueryService microserviceQueryService) {
        this.messageRepository = messageRepository;
        this.microserviceQueryService = microserviceQueryService;
    }

    public Long validateForCreate(CreateMessageInput input) {
        requireInput(input);
        rejectVersionOnCreate(input.version());
        Long microserviceId = resolveActiveMicroserviceId(input.microserviceIdentifier());
        validateUniqueMessage(microserviceId, input.messageKey(), input.code(), null);
        return microserviceId;
    }

    public Long validateForUpdate(Message message, UpdateMessageInput input) {
        requireInput(input);
        message.requireActiveForUpdate();
        requireVersionForUpdate(input.version());
        validateVersion(message.getVersion(), input.version());
        Long microserviceId = resolveActiveMicroserviceId(input.microserviceIdentifier());
        validateUniqueMessage(microserviceId, input.messageKey(), input.code(), message.getId());
        return microserviceId;
    }

    public void validateVersion(Long currentVersion, Long requestedVersion) {
        if (!Objects.equals(currentVersion, requestedVersion)) {
            throw new br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException();
        }
    }

    public void validatePagination(Pageable pageable) {
        if (pageable.isUnpaged()
                || pageable.getPageSize() > 100
                || pageable.getOffset() > Integer.MAX_VALUE
                || pageable.getSort().stream()
                        .anyMatch(order -> !MESSAGE_SORT_PROPERTIES.contains(order.getProperty()))) {
            throw new ValidationException(MessageMessageKeys.PAGE_INVALID);
        }
    }

    private static final Set<String> MESSAGE_SORT_PROPERTIES =
            Set.of("id", "identifier", "messageKey", "code", "httpStatus", "createdAt", "updatedAt");

    private Long resolveActiveMicroserviceId(String microserviceIdentifier) {
        return microserviceQueryService
                .findActiveInternalIdForMessage(microserviceIdentifier)
                .orElseThrow(
                        () -> fieldError("microserviceIdentifier", MessageMessageKeys.MICROSERVICE_INVALID));
    }

    private void validateUniqueMessage(
            Long microserviceId, String messageKey, String code, Long excludedMessageId) {
        boolean duplicateKey =
                excludedMessageId == null
                        ? messageRepository.existsByMicroserviceIdAndMessageKey(microserviceId, messageKey)
                        : messageRepository.existsByMicroserviceIdAndMessageKeyAndIdNot(
                                microserviceId, messageKey, excludedMessageId);
        boolean duplicateCode =
                excludedMessageId == null
                        ? messageRepository.existsByMicroserviceIdAndCode(microserviceId, code)
                        : messageRepository.existsByMicroserviceIdAndCodeAndIdNot(
                                microserviceId, code, excludedMessageId);
        ValidationResult result = new ValidationResult();
        if (duplicateKey) result.addError("messageKey", MessageMessageKeys.KEY_DUPLICATE);
        if (duplicateCode) result.addError("code", MessageMessageKeys.CODE_DUPLICATE);
        if (result.hasErrors()) throw new ValidationException(result);
    }

    private void requireInput(Object input) {
        if (input == null) rejectField("request", MessageMessageKeys.INPUT_REQUIRED);
    }

    private void rejectVersionOnCreate(Long version) {
        if (version != null) rejectField("version", MessageMessageKeys.VERSION_NOT_ALLOWED_ON_CREATE);
    }

    private void requireVersionForUpdate(Long version) {
        if (version == null) rejectField("version", MessageMessageKeys.VERSION_REQUIRED);
    }

    private void rejectField(String field, String messageKey) {
        throw fieldError(field, messageKey);
    }

    private ValidationException fieldError(String field, String messageKey) {
        ValidationResult result = new ValidationResult();
        result.addError(field, messageKey);
        return new ValidationException(result);
    }
}
