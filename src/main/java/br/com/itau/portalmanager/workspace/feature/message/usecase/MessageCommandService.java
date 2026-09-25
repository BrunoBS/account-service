package br.com.itau.portalmanager.workspace.feature.message.usecase;

import br.com.itau.portalmanager.workspace.feature.message.domain.Message;
import br.com.itau.portalmanager.workspace.feature.message.domain.MessageMessageKeys;
import br.com.itau.portalmanager.workspace.feature.message.domain.MessageTranslation;
import br.com.itau.portalmanager.workspace.feature.message.repository.MessageRepository;
import br.com.itau.portalmanager.workspace.feature.message.repository.MessageTranslationRepository;
import br.com.itau.portalmanager.workspace.feature.message.usecase.model.CreateMessageInput;
import br.com.itau.portalmanager.workspace.feature.message.usecase.model.CreateMessageTranslationInput;
import br.com.itau.portalmanager.workspace.feature.message.usecase.model.MessageOutput;
import br.com.itau.portalmanager.workspace.feature.message.usecase.model.UpdateMessageInput;
import br.com.itau.portalmanager.workspace.feature.message.usecase.support.MessageFinder;
import br.com.itau.portalmanager.workspace.feature.message.usecase.support.MessageNormalizer;
import br.com.itau.portalmanager.workspace.feature.message.usecase.validation.MessageValidator;
import br.com.itau.portalmanager.workspace.feature.platform.repository.ServiceRepository;
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
    private final ServiceRepository serviceRepository;

    public MessageCommandService(
            MessageRepository messageRepository,
            MessageTranslationRepository translationRepository,
            MessageFinder finder,
            MessageNormalizer normalizer,
            MessageValidator validator,
            ServiceRepository serviceRepository
    ) {
        this.messageRepository = messageRepository;
        this.translationRepository = translationRepository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.validator = validator;
        this.serviceRepository = serviceRepository;
    }

    @Transactional
    public MessageOutput create(CreateMessageInput rawInput) {
        CreateMessageInput input = normalizer.normalize(rawInput);

        boolean keyDuplicate = input != null
                && input.service() != null
                && input.messageKey() != null
                && messageRepository.existsByService_CodeAndMessageKey(
                        input.service(),
                        input.messageKey()
                );
        boolean codeDuplicate = input != null
                && input.service() != null
                && input.code() != null
                && messageRepository.existsByService_CodeAndCode(input.service(), input.code());

        validator.validateForCreate(input, keyDuplicate, codeDuplicate);
        validateTranslationsForCreate(input);

        var service = serviceRepository.findByCode(input.service())
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        LocalDateTime now = LocalDateTime.now();
        Message message = new Message(
                service,
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
                && messageRepository.existsByService_CodeAndMessageKeyAndIdNot(
                        input.service(),
                        input.messageKey(),
                        message.getId()
                );
        boolean codeDuplicate = input != null
                && input.service() != null
                && input.code() != null
                && messageRepository.existsByService_CodeAndCodeAndIdNot(
                        input.service(),
                        input.code(),
                        message.getId()
                );

        validator.validateForUpdate(input, keyDuplicate, codeDuplicate);
        validateVersion(message.getVersion(), input.version());

        var service = serviceRepository.findByCode(input.service())
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        message.update(
                service,
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
        message.quarantine(LocalDateTime.now());
        messageRepository.saveAndFlush(message);
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
