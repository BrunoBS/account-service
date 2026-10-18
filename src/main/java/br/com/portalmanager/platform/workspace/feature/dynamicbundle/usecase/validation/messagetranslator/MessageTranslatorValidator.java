package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.validation.messagetranslator;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.MessageMessageKeys;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.Message;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.messagetranslator.MessageTranslator;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.messagetranslator.MessageTranslatorRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.CreateMessageTranslatorInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.UpdateMessageTranslatorInput;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class MessageTranslatorValidator {

    private static final Set<String> SORT_PROPERTIES =
            Set.of("id", "identifier", "locale", "title", "createdAt", "updatedAt");

    private final MessageTranslatorRepository translatorRepository;

    public MessageTranslatorValidator(MessageTranslatorRepository translatorRepository) {
        this.translatorRepository = translatorRepository;
    }

    public void validateForCreate(Message message, CreateMessageTranslatorInput input) {
        requireInput(input);
        message.requireActiveForTranslator();
        rejectVersionOnCreate(input.version());
        validateUniqueLocale(message.getId(), input.locale(), null);
    }

    public void validateForUpdate(MessageTranslator translator, UpdateMessageTranslatorInput input) {
        requireInput(input);
        translator.requireActiveForUpdate();
        requireVersionForUpdate(input.version());
        validateVersion(translator.getVersion(), input.version());
        validateUniqueLocale(translator.getMessage().getId(), input.locale(), translator.getId());
    }

    public void validateInline(List<CreateMessageTranslatorInput> inputs) {
        if (inputs == null) return;
        Set<String> uniqueLocales = new HashSet<>();
        for (CreateMessageTranslatorInput input : inputs) {
            requireInput(input);
            rejectVersionOnCreate(input.version());
            if (!uniqueLocales.add(input.locale())) {
                rejectField("locale", MessageMessageKeys.LOCALE_DUPLICATE);
            }
        }
    }

    public void validatePagination(Pageable pageable) {
        if (pageable.isUnpaged()
                || pageable.getPageSize() > 100
                || pageable.getOffset() > Integer.MAX_VALUE
                || pageable.getSort().stream()
                        .anyMatch(order -> !SORT_PROPERTIES.contains(order.getProperty()))) {
            throw new ValidationException(MessageMessageKeys.PAGE_INVALID);
        }
    }

    private void validateVersion(Long currentVersion, Long requestedVersion) {
        if (!Objects.equals(currentVersion, requestedVersion)) throw new ResourceVersionConflictException();
    }

    private void validateUniqueLocale(Long messageId, String locale, Long excludedTranslatorId) {
        boolean duplicateLocale =
                excludedTranslatorId == null
                        ? translatorRepository.existsByMessageIdAndLocale(messageId, locale)
                        : translatorRepository.existsByMessageIdAndLocaleAndIdNot(
                                messageId, locale, excludedTranslatorId);
        if (duplicateLocale) rejectField("locale", MessageMessageKeys.LOCALE_DUPLICATE);
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
        ValidationResult result = new ValidationResult();
        result.addError(field, messageKey);
        throw new ValidationException(result);
    }
}
