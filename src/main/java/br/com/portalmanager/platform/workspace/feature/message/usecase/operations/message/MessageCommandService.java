package br.com.portalmanager.platform.workspace.feature.message.usecase.operations.message;

import br.com.portalmanager.platform.workspace.feature.message.domain.Message;
import br.com.portalmanager.platform.workspace.feature.message.domain.MessageTranslation;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageRepository;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageTranslationRepository;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageTranslationInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.MessageOutput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.UpdateMessageInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageFinder;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageNormalizer;
import br.com.portalmanager.platform.workspace.feature.message.usecase.validation.MessageValidator;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class MessageCommandService {

    private final MessageRepository messageRepository;
    private final MessageTranslationRepository translationRepository;
    private final MessageFinder finder;
    private final MessageNormalizer normalizer;
    private final MessageValidator validator;
    private final MicroserviceQueryService microserviceQueryService;

    public MessageCommandService(
            MessageRepository messageRepository,
            MessageTranslationRepository translationRepository,
            MessageFinder finder,
            MessageNormalizer normalizer,
            MessageValidator validator,
            MicroserviceQueryService microserviceQueryService
    ) {
        this.messageRepository = messageRepository;
        this.translationRepository = translationRepository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.validator = validator;
        this.microserviceQueryService = microserviceQueryService;
    }

    @Transactional
    public MessageOutput create(CreateMessageInput rawInput) {
        CreateMessageInput input = normalizer.normalize(rawInput);

        validator.validateForCreate(input, false, false);
        Long microserviceId = microserviceQueryService.findActiveInternalIdByIdentifier(input.microserviceIdentifier());

        boolean keyDuplicate = input.messageKey() != null
                && messageRepository.existsByMicroserviceIdAndMessageKey(microserviceId, input.messageKey());
        boolean codeDuplicate = input.code() != null
                && messageRepository.existsByMicroserviceIdAndCode(microserviceId, input.code());

        validator.validateForCreate(input, keyDuplicate, codeDuplicate);
        validateTranslationsForCreate(input);

        LocalDateTime now = LocalDateTime.now();
        Message message = new Message(
                microserviceId,
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

        return output(saved);
    }

    @Transactional
    public MessageOutput update(String identifier, UpdateMessageInput rawInput) {
        Message message = finder.findMessage(identifier);
        UpdateMessageInput input = normalizer.normalize(rawInput);

        validator.validateForUpdate(input, false, false);
        Long microserviceId = microserviceQueryService.findActiveInternalIdByIdentifier(input.microserviceIdentifier());

        boolean keyDuplicate = input.messageKey() != null
                && messageRepository.existsByMicroserviceIdAndMessageKeyAndIdNot(
                microserviceId,
                input.messageKey(),
                message.getId()
        );
        boolean codeDuplicate = input.code() != null
                && messageRepository.existsByMicroserviceIdAndCodeAndIdNot(
                microserviceId,
                input.code(),
                message.getId()
        );

        validator.validateForUpdate(input, keyDuplicate, codeDuplicate);
        validator.validateVersion(message.getVersion(), input.version());

        message.update(
                microserviceId,
                input.messageKey(),
                input.code(),
                input.httpStatus(),
                input.observation(),
                LocalDateTime.now()
        );
        return output(messageRepository.saveAndFlush(message));
    }

    @Transactional
    public MessageOutput activate(String identifier) {
        Message message = finder.findMessage(identifier);
        message.activate(LocalDateTime.now());
        return output(messageRepository.saveAndFlush(message));
    }

    @Transactional
    public MessageOutput inactivate(String identifier) {
        Message message = finder.findMessage(identifier);
        message.inactivate(LocalDateTime.now());
        return output(messageRepository.saveAndFlush(message));
    }

    @Transactional
    public void delete(String identifier) {
        Message message = finder.findMessage(identifier);
        validator.validateDeletion(message);
        message.quarantine(LocalDateTime.now());
        messageRepository.saveAndFlush(message);
    }

    private MessageOutput output(Message message) {
        return MessageOutput.from(
                message,
                microserviceQueryService.findIdentifierByInternalId(message.getMicroserviceId())
        );
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

}
