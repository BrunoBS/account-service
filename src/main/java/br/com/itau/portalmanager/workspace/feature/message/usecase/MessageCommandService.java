package br.com.itau.portalmanager.workspace.feature.message.usecase;

import br.com.itau.portalmanager.workspace.feature.message.domain.Message;
import br.com.itau.portalmanager.workspace.feature.message.domain.MessageMessageKeys;
import br.com.itau.portalmanager.workspace.feature.message.domain.MessageTranslation;
import br.com.itau.portalmanager.workspace.feature.message.repository.MessageRepository;
import br.com.itau.portalmanager.workspace.feature.message.repository.MessageTranslationRepository;
import br.com.itau.portalmanager.workspace.feature.message.usecase.model.CreateMessageInput;
import br.com.itau.portalmanager.workspace.feature.message.usecase.model.CreateMessageTranslationInput;
import br.com.itau.portalmanager.workspace.feature.message.usecase.model.MessageOutput;
import br.com.itau.portalmanager.workspace.feature.message.usecase.model.MessageTranslationOutput;
import br.com.itau.portalmanager.workspace.feature.message.usecase.model.UpdateMessageInput;
import br.com.itau.portalmanager.workspace.feature.message.usecase.model.UpdateMessageTranslationInput;
import br.com.itau.portalmanager.workspace.feature.message.usecase.support.MessageFinder;
import br.com.itau.portalmanager.workspace.feature.message.usecase.support.MessageNormalizer;
import br.com.itau.portalmanager.workspace.feature.message.usecase.validation.MessageValidator;
import br.com.portalmanager.platform.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.messaging.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Service
public class MessageCommandService {

    private final MessageRepository messageRepository;
    private final MessageTranslationRepository translationRepository;
    private final MessageFinder finder;
    private final MessageNormalizer normalizer;
    private final MessageValidator validator;

    public MessageCommandService(
            MessageRepository messageRepository,
            MessageTranslationRepository translationRepository,
            MessageFinder finder,
            MessageNormalizer normalizer,
            MessageValidator validator
    ) {
        this.messageRepository = messageRepository;
        this.translationRepository = translationRepository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.validator = validator;
    }

    @Transactional
    public MessageOutput create(CreateMessageInput rawInput) {
        CreateMessageInput input = normalizer.normalize(rawInput);

        boolean keyDuplicate = input != null
                && input.service() != null
                && input.messageKey() != null
                && messageRepository.existsByServiceCodeAndMessageKey(
                        input.service(),
                        input.messageKey()
                );
        boolean codeDuplicate = input != null
                && input.service() != null
                && input.code() != null
                && messageRepository.existsByServiceCodeAndCode(input.service(), input.code());

        validator.validateForCreate(input, keyDuplicate, codeDuplicate);
        validateTranslationsForCreate(input);

        LocalDateTime now = LocalDateTime.now();
        Message message = new Message(
                input.service(),
                input.messageKey(),
                input.code(),
                input.httpStatus(),
                input.observation(),
                now
        );
        Message saved = messageRepository.saveAndFlush(message);

        if (input.translations() != null && !input.translations().isEmpty()) {
            input.translations().forEach(translation -> translationRepository.save(
                    new MessageTranslation(
                            saved,
                            translation.locale(),
                            translation.title(),
                            translation.detail(),
                            translation.suggestion(),
                            now
                    )
            ));
            translationRepository.flush();
        }

        return MessageOutput.from(saved);
    }

    @Transactional
    public MessageOutput update(String identifier, UpdateMessageInput rawInput) {
        Message message = finder.findMessage(identifier);
        UpdateMessageInput input = normalizer.normalize(rawInput);

        boolean keyDuplicate = input != null
                && input.service() != null
                && input.messageKey() != null
                && messageRepository.existsByServiceCodeAndMessageKeyAndIdNot(
                        input.service(),
                        input.messageKey(),
                        message.getId()
                );
        boolean codeDuplicate = input != null
                && input.service() != null
                && input.code() != null
                && messageRepository.existsByServiceCodeAndCodeAndIdNot(
                        input.service(),
                        input.code(),
                        message.getId()
                );

        validator.validateForUpdate(input, keyDuplicate, codeDuplicate);
        validateVersion(message.getVersion(), input.version());

        message.update(
                input.service(),
                input.messageKey(),
                input.code(),
                input.httpStatus(),
                input.observation(),
                LocalDateTime.now()
        );
        return MessageOutput.from(messageRepository.saveAndFlush(message));
    }

    @Transactional
    public MessageOutput activate(String identifier) {
        Message message = finder.findMessage(identifier);
        message.activate(LocalDateTime.now());
        return MessageOutput.from(messageRepository.saveAndFlush(message));
    }

    @Transactional
    public MessageOutput inactivate(String identifier) {
        Message message = finder.findMessage(identifier);
        message.inactivate(LocalDateTime.now());
        return MessageOutput.from(messageRepository.saveAndFlush(message));
    }

    @Transactional
    public void delete(String identifier) {
        Message message = finder.findMessage(identifier);
        if (!message.isInactive()) {
            throw new ValidationException(MessageMessageKeys.DELETE_INVALID);
        }
        messageRepository.delete(message);
        messageRepository.flush();
    }

    @Transactional
    public MessageTranslationOutput createTranslation(
            String messageIdentifier,
            CreateMessageTranslationInput rawInput
    ) {
        Message message = finder.findMessage(messageIdentifier);
        CreateMessageTranslationInput input = normalizer.normalize(rawInput);

        boolean localeDuplicate = input != null
                && input.locale() != null
                && translationRepository.existsByMessageIdAndLocale(
                        message.getId(),
                        input.locale()
                );

        validator.validateTranslationForCreate(input, localeDuplicate);

        MessageTranslation translation = new MessageTranslation(
                message,
                input.locale(),
                input.title(),
                input.detail(),
                input.suggestion(),
                LocalDateTime.now()
        );
        return MessageTranslationOutput.from(translationRepository.saveAndFlush(translation));
    }

    @Transactional
    public MessageTranslationOutput updateTranslation(
            String messageIdentifier,
            String translationIdentifier,
            UpdateMessageTranslationInput rawInput
    ) {
        Message message = finder.findMessage(messageIdentifier);
        MessageTranslation translation = finder.findTranslation(message, translationIdentifier);
        UpdateMessageTranslationInput input = normalizer.normalize(rawInput);

        boolean localeDuplicate = input != null
                && input.locale() != null
                && translationRepository.existsByMessageIdAndLocaleAndIdNot(
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
        return MessageTranslationOutput.from(
                translationRepository.saveAndFlush(translation)
        );
    }

    @Transactional
    public MessageTranslationOutput activateTranslation(
            String messageIdentifier,
            String translationIdentifier
    ) {
        Message message = finder.findMessage(messageIdentifier);
        MessageTranslation translation = finder.findTranslation(message, translationIdentifier);
        translation.activate(LocalDateTime.now());
        return MessageTranslationOutput.from(
                translationRepository.saveAndFlush(translation)
        );
    }

    @Transactional
    public MessageTranslationOutput inactivateTranslation(
            String messageIdentifier,
            String translationIdentifier
    ) {
        Message message = finder.findMessage(messageIdentifier);
        MessageTranslation translation = finder.findTranslation(message, translationIdentifier);
        translation.inactivate(LocalDateTime.now());
        return MessageTranslationOutput.from(
                translationRepository.saveAndFlush(translation)
        );
    }

    @Transactional
    public void deleteTranslation(
            String messageIdentifier,
            String translationIdentifier
    ) {
        Message message = finder.findMessage(messageIdentifier);
        MessageTranslation translation = finder.findTranslation(message, translationIdentifier);
        if (!translation.isInactive()) {
            throw new ValidationException(MessageMessageKeys.TRANSLATION_DELETE_INVALID);
        }
        translationRepository.delete(translation);
        translationRepository.flush();
    }

    private void validateTranslationsForCreate(CreateMessageInput input) {
        if (input == null || input.translations() == null || input.translations().isEmpty()) {
            return;
        }

        Set<String> locales = new HashSet<>();
        for (CreateMessageTranslationInput translation : input.translations()) {
            boolean duplicateLocale = translation != null
                    && translation.locale() != null
                    && !locales.add(translation.locale());
            validator.validateTranslationForCreate(translation, duplicateLocale);
        }
    }

    private void validateVersion(Long currentVersion, Long inputVersion) {
        if (!Objects.equals(currentVersion, inputVersion)) {
            throw new ResourceVersionConflictException();
        }
    }
}
