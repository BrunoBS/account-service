package br.com.portalmanager.platform.workspace.feature.message.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.message.domain.Message;
import br.com.portalmanager.platform.workspace.feature.message.domain.MessageMessageKeys;
import br.com.portalmanager.platform.workspace.feature.message.domain.MessageTranslation;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageRepository;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageTranslationRepository;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageTranslationInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.UpdateMessageInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.UpdateMessageTranslationInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceQueryService;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class MessageValidator {

    private final MessageRepository messageRepository;
    private final MessageTranslationRepository translationRepository;
    private final MicroserviceQueryService microserviceQueryService;

    public MessageValidator(
        MessageRepository messageRepository,
        MessageTranslationRepository translationRepository,
        MicroserviceQueryService microserviceQueryService
    ) {
        this.messageRepository = messageRepository;
        this.translationRepository = translationRepository;
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

    public void validateTranslationForCreate(Message message, CreateMessageTranslationInput input) {
        requireInput(input);
        message.requireActiveForTranslation();
        rejectVersionOnCreate(input.version());
        validateUniqueLocale(message.getId(), input.locale(), null);
    }

    public void validateTranslationForUpdate(MessageTranslation translation, UpdateMessageTranslationInput input) {
        requireInput(input);
        translation.requireActiveForUpdate();
        requireVersionForUpdate(input.version());
        validateVersion(translation.getVersion(), input.version());
        validateUniqueLocale(translation.getMessage().getId(), input.locale(), translation.getId());
    }

    public void validateInlineTranslations(List<CreateMessageTranslationInput> inputs) {
        if (inputs == null) {
            return;
        }
        Set<String> uniqueLocales = new HashSet<>();
        for (CreateMessageTranslationInput input : inputs) {
            requireInput(input);
            rejectVersionOnCreate(input.version());
            if (!uniqueLocales.add(input.locale())) {
                rejectField("locale", MessageMessageKeys.LOCALE_DUPLICATE);
            }
        }
    }

    public void validateVersion(Long currentVersion, Long requestedVersion) {
        if (!Objects.equals(currentVersion, requestedVersion)) {
            throw new ResourceVersionConflictException();
        }
    }

    public void validateMessagePagination(Pageable pageable) {
        validatePagination(pageable, Set.of("id", "identifier", "messageKey", "code", "httpStatus", "createdAt", "updatedAt"));
    }

    public void validateTranslationPagination(Pageable pageable) {
        validatePagination(pageable, Set.of("id", "identifier", "locale", "title", "createdAt", "updatedAt"));
    }

    private void validatePagination(Pageable pageable, Set<String> sortableProperties) {
        if (pageable.isUnpaged() || pageable.getPageSize() > 100 || pageable.getOffset() > Integer.MAX_VALUE ||
            pageable.getSort().stream().anyMatch(order -> !sortableProperties.contains(order.getProperty()))) {
            throw new ValidationException(MessageMessageKeys.PAGE_INVALID);
        }
    }

    private Long resolveActiveMicroserviceId(String microserviceIdentifier) {
        return microserviceQueryService.findActiveInternalIdForMessage(microserviceIdentifier)
            .orElseThrow(() -> fieldError("microserviceIdentifier", MessageMessageKeys.MICROSERVICE_INVALID));
    }

    private void validateUniqueMessage(Long microserviceId, String messageKey, String code, Long excludedMessageId) {
        boolean duplicateKey = excludedMessageId == null
            ? messageRepository.existsByMicroserviceIdAndMessageKey(microserviceId, messageKey)
            : messageRepository.existsByMicroserviceIdAndMessageKeyAndIdNot(microserviceId, messageKey, excludedMessageId);
        boolean duplicateCode = excludedMessageId == null
            ? messageRepository.existsByMicroserviceIdAndCode(microserviceId, code)
            : messageRepository.existsByMicroserviceIdAndCodeAndIdNot(microserviceId, code, excludedMessageId);
        ValidationResult result = new ValidationResult();
        if (duplicateKey) result.addError("messageKey", MessageMessageKeys.KEY_DUPLICATE);
        if (duplicateCode) result.addError("code", MessageMessageKeys.CODE_DUPLICATE);
        if (result.hasErrors()) throw new ValidationException(result);
    }

    private void validateUniqueLocale(Long messageId, String locale, Long excludedTranslationId) {
        boolean duplicateLocale = excludedTranslationId == null
            ? translationRepository.existsByMessageIdAndLocale(messageId, locale)
            : translationRepository.existsByMessageIdAndLocaleAndIdNot(messageId, locale, excludedTranslationId);
        if (duplicateLocale) {
            rejectField("locale", MessageMessageKeys.LOCALE_DUPLICATE);
        }
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
