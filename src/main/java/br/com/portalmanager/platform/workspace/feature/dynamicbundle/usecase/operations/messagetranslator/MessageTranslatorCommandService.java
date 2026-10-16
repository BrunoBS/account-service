package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.messagetranslator;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.Message;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.messagetranslator.MessageTranslator;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.messagetranslator.MessageTranslatorRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.messagetranslator.MessageTranslatorRepository.DeletionSnapshot;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.CreateMessageTranslatorInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.MessageTranslatorOutput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.UpdateMessageTranslatorInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message.MessageFinder;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message.MessageMapper;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message.MessageNormalizer;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.validation.messagetranslator.MessageTranslatorValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageTranslatorCommandService {

    private final MessageTranslatorRepository translatorRepository;
    private final MessageFinder finder;
    private final MessageNormalizer normalizer;
    private final MessageMapper mapper;
    private final MessageTranslatorValidator validator;

    public MessageTranslatorCommandService(
            MessageTranslatorRepository translatorRepository,
            MessageFinder finder,
            MessageNormalizer normalizer,
            MessageMapper mapper,
            MessageTranslatorValidator validator) {
        this.translatorRepository = translatorRepository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Transactional
    @Auditable(
            action = AuditAction.CREATE,
            event = "MESSAGE_TRANSLATOR_CREATED",
            resourceType = "MESSAGE_TRANSLATOR")
    public MessageTranslatorOutput create(
            String messageIdentifier, CreateMessageTranslatorInput rawInput) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        CreateMessageTranslatorInput input = normalizer.normalize(rawInput);

        validator.validateForCreate(message, input);

        MessageTranslator translator =
                new MessageTranslator(message, mapper.toDetails(input), LocalDateTime.now());
        return MessageTranslatorOutput.from(translatorRepository.saveAndFlush(translator));
    }

    @Transactional
    @Auditable(
            action = AuditAction.UPDATE,
            event = "MESSAGE_TRANSLATOR_UPDATED",
            resourceType = "MESSAGE_TRANSLATOR")
    public MessageTranslatorOutput update(
            String messageIdentifier,
            String translatorIdentifier,
            UpdateMessageTranslatorInput rawInput) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        MessageTranslator translator =
                finder.findTranslatorForUpdate(message, translatorIdentifier);
        UpdateMessageTranslatorInput input = normalizer.normalize(rawInput);

        validator.validateForUpdate(translator, input);

        translator.update(mapper.toDetails(input), LocalDateTime.now());
        return MessageTranslatorOutput.from(translatorRepository.saveAndFlush(translator));
    }

    @Transactional
    @Auditable(
            action = AuditAction.ACTIVATE,
            event = "MESSAGE_TRANSLATOR_ACTIVATED",
            resourceType = "MESSAGE_TRANSLATOR")
    public MessageTranslatorOutput activate(String messageIdentifier, String translatorIdentifier) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        MessageTranslator translator =
                finder.findTranslatorForUpdate(message, translatorIdentifier);
        translator.activate(LocalDateTime.now());
        return MessageTranslatorOutput.from(translatorRepository.saveAndFlush(translator));
    }

    @Transactional
    @Auditable(
            action = AuditAction.DEACTIVATE,
            event = "MESSAGE_TRANSLATOR_DEACTIVATED",
            resourceType = "MESSAGE_TRANSLATOR")
    public MessageTranslatorOutput inactivate(
            String messageIdentifier, String translatorIdentifier) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        MessageTranslator translator =
                finder.findTranslatorForUpdate(message, translatorIdentifier);
        translator.inactivate(LocalDateTime.now());
        return MessageTranslatorOutput.from(translatorRepository.saveAndFlush(translator));
    }

    @Transactional
    @Auditable(
            action = AuditAction.PURGE,
            event = "MESSAGE_TRANSLATOR_PURGED",
            resourceType = "MESSAGE_TRANSLATOR")
    public MessageTranslatorOutput delete(String messageIdentifier, String translatorIdentifier) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        MessageTranslator translator =
                finder.findTranslatorForUpdate(message, translatorIdentifier);
        translator.requireInactiveForDeletion();
        MessageTranslatorOutput removedTranslator = MessageTranslatorOutput.from(translator);
        translatorRepository.delete(translator);
        translatorRepository.flush();
        return removedTranslator;
    }

    /**
     * Receives current, nonempty snapshots under the parent/translator locks held by the caller.
     */
    @Transactional
    @Auditable(
            action = AuditAction.PURGE,
            event = "MESSAGE_TRANSLATOR_PURGED",
            resourceType = "MESSAGE_TRANSLATOR")
    public List<MessageTranslatorOutput> deleteAllForMessage(
            Message message, List<DeletionSnapshot> snapshots) {
        message.requireInactiveForDeletion();
        List<MessageTranslatorOutput> removedTranslators =
                snapshots.stream()
                        .map(
                                snapshot ->
                                        new MessageTranslatorOutput(
                                                snapshot.getIdentifier(),
                                                message.getIdentifier(),
                                                snapshot.getVersion(),
                                                snapshot.getLocale(),
                                                snapshot.getTitle(),
                                                snapshot.getDetail(),
                                                snapshot.getSuggestion(),
                                                snapshot.getLifecycle()))
                        .toList();
        translatorRepository.deleteAllByMessageId(message.getId());
        translatorRepository.flush();
        return removedTranslators;
    }

    /**
     * Receives normalized inputs and a message just persisted by the enclosing creation
     * transaction.
     */
    @Transactional
    @Auditable(
            action = AuditAction.CREATE,
            event = "MESSAGE_TRANSLATOR_CREATED",
            resourceType = "MESSAGE_TRANSLATOR")
    public List<MessageTranslatorOutput> createAllForNewMessage(
            Message message, List<CreateMessageTranslatorInput> inputs, LocalDateTime now) {
        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }
        message.requireActiveForTranslator();
        validator.validateInline(inputs);
        List<MessageTranslator> messageTranslators =
                inputs.stream()
                        .map(input -> new MessageTranslator(message, mapper.toDetails(input), now))
                        .toList();
        return translatorRepository.saveAllAndFlush(messageTranslators).stream()
                .map(MessageTranslatorOutput::from)
                .toList();
    }
}
