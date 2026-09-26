package br.com.portalmanager.platform.workspace.feature.message.usecase;

import br.com.portalmanager.platform.workspace.feature.message.domain.Message;
import br.com.portalmanager.platform.workspace.feature.message.domain.MessageMessageKeys;
import br.com.portalmanager.platform.workspace.feature.message.domain.MessageTranslation;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageTranslationRepository;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageTranslationInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.MessageTranslationOutput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.UpdateMessageTranslationInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.support.MessageFinder;
import br.com.portalmanager.platform.workspace.feature.message.usecase.support.MessageNormalizer;
import br.com.portalmanager.platform.workspace.feature.message.usecase.validation.MessageValidator;
import br.com.portalmanager.platform.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.messaging.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class MessageTranslationCommandService {

    private final MessageTranslationRepository repository;
    private final MessageFinder finder;
    private final MessageNormalizer normalizer;
    private final MessageValidator validator;

    public MessageTranslationCommandService(
            MessageTranslationRepository repository,
            MessageFinder finder,
            MessageNormalizer normalizer,
            MessageValidator validator
    ) {
        this.repository = repository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.validator = validator;
    }

    @Transactional
    public MessageTranslationOutput create(
            String messageIdentifier,
            CreateMessageTranslationInput rawInput
    ) {
        Message message = finder.findMessage(messageIdentifier);
        CreateMessageTranslationInput input = normalizer.normalize(rawInput);

        boolean localeDuplicate = input != null
                && input.locale() != null
                && repository.existsByMessageIdAndLocale(message.getId(), input.locale());

        validator.validateTranslationForCreate(input, localeDuplicate);

        MessageTranslation translation = new MessageTranslation(
                message,
                input.locale(),
                input.title(),
                input.detail(),
                input.suggestion(),
                LocalDateTime.now()
        );
        return MessageTranslationOutput.from(repository.saveAndFlush(translation));
    }

    @Transactional
    public MessageTranslationOutput update(
            String messageIdentifier,
            String translationIdentifier,
            UpdateMessageTranslationInput rawInput
    ) {
        Message message = finder.findMessage(messageIdentifier);
        MessageTranslation translation = finder.findTranslation(message, translationIdentifier);
        UpdateMessageTranslationInput input = normalizer.normalize(rawInput);

        boolean localeDuplicate = input != null
                && input.locale() != null
                && repository.existsByMessageIdAndLocaleAndIdNot(
                        message.getId(),
                        input.locale(),
                        translation.getId()
                );

        validator.validateTranslationForUpdate(input, localeDuplicate);
        validateVersion(translation.getVersion(), input.version());

        translation.update(
                input.locale(),
                input.title(),
                input.detail(),
                input.suggestion(),
                LocalDateTime.now()
        );
        return MessageTranslationOutput.from(repository.saveAndFlush(translation));
    }

    @Transactional
    public MessageTranslationOutput activate(
            String messageIdentifier,
            String translationIdentifier
    ) {
        Message message = finder.findMessage(messageIdentifier);
        MessageTranslation translation = finder.findTranslation(message, translationIdentifier);
        translation.activate(LocalDateTime.now());
        return MessageTranslationOutput.from(repository.saveAndFlush(translation));
    }

    @Transactional
    public MessageTranslationOutput inactivate(
            String messageIdentifier,
            String translationIdentifier
    ) {
        Message message = finder.findMessage(messageIdentifier);
        MessageTranslation translation = finder.findTranslation(message, translationIdentifier);
        translation.inactivate(LocalDateTime.now());
        return MessageTranslationOutput.from(repository.saveAndFlush(translation));
    }

    @Transactional
    public void delete(String messageIdentifier, String translationIdentifier) {
        Message message = finder.findMessage(messageIdentifier);
        MessageTranslation translation = finder.findTranslation(message, translationIdentifier);
        if (!translation.isInactive()) {
            throw new ValidationException(MessageMessageKeys.TRANSLATION_DELETE_INVALID);
        }
        translation.quarantine(LocalDateTime.now());
        repository.saveAndFlush(translation);
    }

    private void validateVersion(Long currentVersion, Long inputVersion) {
        if (!Objects.equals(currentVersion, inputVersion)) {
            throw new ResourceVersionConflictException();
        }
    }
}
