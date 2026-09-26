package br.com.portalmanager.platform.workspace.feature.message.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.message.domain.Message;
import br.com.portalmanager.platform.workspace.feature.message.domain.MessageMessageKeys;
import br.com.portalmanager.platform.workspace.feature.message.domain.MessageTranslation;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageTranslationInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.UpdateMessageInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.UpdateMessageTranslationInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceQueryService;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class MessageValidator {

    private static final Pattern MESSAGE_KEY_PATTERN =
            Pattern.compile("^[a-z0-9][a-z0-9._-]{0,254}$");
    private static final Pattern CODE_PATTERN =
            Pattern.compile("^[A-Z][A-Z0-9-]{2,49}$");
    private static final Pattern LOCALE_PATTERN =
            Pattern.compile("^[a-z]{2,3}(?:-[A-Z]{2}|-[0-9]{3})?(?:-[A-Za-z0-9]{4,8})*$");
    private final MicroserviceQueryService microserviceQueryService;
    public MessageValidator(MicroserviceQueryService microserviceQueryService) {
        this.microserviceQueryService = microserviceQueryService;
    }

    public void validateVersion(Long currentVersion, Long requestedVersion) {
        if (!Objects.equals(currentVersion, requestedVersion)) {
            throw new ResourceVersionConflictException();
        }
    }

    public void validateDeletion(Message message) {
        if (!message.isInactive()) throw new ValidationException(MessageMessageKeys.DELETE_INVALID);
    }

    public void validateDeletion(MessageTranslation translation) {
        if (!translation.isInactive()) {
            throw new ValidationException(MessageMessageKeys.TRANSLATION_DELETE_INVALID);
        }
    }

    public void validateForCreate(
            CreateMessageInput input,
            boolean keyDuplicate,
            boolean codeDuplicate
    ) {
        ValidationResult result = new ValidationResult();
        validateMessage(
                input == null ? null : input.microserviceIdentifier(),
                input == null ? null : input.messageKey(),
                input == null ? null : input.code(),
                input == null ? null : input.httpStatus(),
                input == null ? null : input.observation(),
                result
        );
        validateDuplicates(keyDuplicate, codeDuplicate, result);
        rejectIfInvalid(result);
    }

    public void validateForUpdate(
            UpdateMessageInput input,
            boolean keyDuplicate,
            boolean codeDuplicate
    ) {
        ValidationResult result = new ValidationResult();
        if (input == null || input.version() == null || input.version() < 0) {
            result.addError("version", MessageMessageKeys.VERSION_REQUIRED);
        }
        validateMessage(
                input == null ? null : input.microserviceIdentifier(),
                input == null ? null : input.messageKey(),
                input == null ? null : input.code(),
                input == null ? null : input.httpStatus(),
                input == null ? null : input.observation(),
                result
        );
        validateDuplicates(keyDuplicate, codeDuplicate, result);
        rejectIfInvalid(result);
    }

    public void validateTranslationForCreate(
            CreateMessageTranslationInput input,
            boolean localeDuplicate
    ) {
        ValidationResult result = new ValidationResult();
        validateTranslation(
                input == null ? null : input.locale(),
                input == null ? null : input.title(),
                input == null ? null : input.detail(),
                input == null ? null : input.suggestion(),
                result
        );
        if (localeDuplicate) {
            result.addError("locale", MessageMessageKeys.LOCALE_DUPLICATE);
        }
        rejectIfInvalid(result);
    }

    public void validateTranslationForUpdate(
            UpdateMessageTranslationInput input,
            boolean localeDuplicate
    ) {
        ValidationResult result = new ValidationResult();
        if (input == null || input.version() == null || input.version() < 0) {
            result.addError("version", MessageMessageKeys.VERSION_REQUIRED);
        }
        validateTranslation(
                input == null ? null : input.locale(),
                input == null ? null : input.title(),
                input == null ? null : input.detail(),
                input == null ? null : input.suggestion(),
                result
        );
        if (localeDuplicate) {
            result.addError("locale", MessageMessageKeys.LOCALE_DUPLICATE);
        }
        rejectIfInvalid(result);
    }

    private void validateMessage(
            String microserviceIdentifier,
            String messageKey,
            String code,
            Integer httpStatus,
            String observation,
            ValidationResult result
    ) {
        if (microserviceIdentifier == null || microserviceIdentifier.isBlank()) {
            result.addError("microserviceIdentifier", MessageMessageKeys.MICROSERVICE_REQUIRED);
        } else if (!microserviceQueryService.existsActiveByIdentifier(microserviceIdentifier)) {
            result.addError("microserviceIdentifier", MessageMessageKeys.MICROSERVICE_INVALID);
        }

        if (messageKey == null || messageKey.isBlank()) {
            result.addError("messageKey", MessageMessageKeys.KEY_REQUIRED);
        } else if (!MESSAGE_KEY_PATTERN.matcher(messageKey).matches()) {
            result.addError("messageKey", MessageMessageKeys.KEY_INVALID);
        }

        if (code == null || code.isBlank()) {
            result.addError("code", MessageMessageKeys.CODE_REQUIRED);
        } else if (!CODE_PATTERN.matcher(code).matches()) {
            result.addError("code", MessageMessageKeys.CODE_INVALID);
        }

        if (httpStatus == null || httpStatus < 100 || httpStatus > 599) {
            result.addError("httpStatus", MessageMessageKeys.HTTP_STATUS_INVALID);
        }

        if (observation != null && observation.length() > 500) {
            result.addError("observation", MessageMessageKeys.OBSERVATION_SIZE);
        }
    }

    private void validateTranslation(
            String locale,
            String title,
            String detail,
            String suggestion,
            ValidationResult result
    ) {
        if (locale == null || !LOCALE_PATTERN.matcher(locale).matches()) {
            result.addError("locale", MessageMessageKeys.LOCALE_INVALID);
        }
        if (title == null || title.isBlank() || title.length() > 150) {
            result.addError("title", MessageMessageKeys.TITLE_REQUIRED);
        }
        if (detail == null || detail.isBlank() || detail.length() > 1000) {
            result.addError("detail", MessageMessageKeys.DETAIL_REQUIRED);
        }
        if (suggestion == null || suggestion.isBlank() || suggestion.length() > 1000) {
            result.addError("suggestion", MessageMessageKeys.SUGGESTION_REQUIRED);
        }
    }

    private void validateDuplicates(
            boolean keyDuplicate,
            boolean codeDuplicate,
            ValidationResult result
    ) {
        if (keyDuplicate) {
            result.addError("messageKey", MessageMessageKeys.KEY_DUPLICATE);
        }
        if (codeDuplicate) {
            result.addError("code", MessageMessageKeys.CODE_DUPLICATE);
        }
    }

    private void rejectIfInvalid(ValidationResult result) {
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
    }
}
