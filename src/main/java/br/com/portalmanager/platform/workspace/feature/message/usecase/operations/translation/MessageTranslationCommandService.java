package br.com.portalmanager.platform.workspace.feature.message.usecase.operations.translation;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.feature.message.domain.Message;
import br.com.portalmanager.platform.workspace.feature.message.domain.MessageTranslation;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageTranslationRepository;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageTranslationRepository.DeletionSnapshot;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageTranslationInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.MessageTranslationOutput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.UpdateMessageTranslationInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageFinder;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageMapper;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageNormalizer;
import br.com.portalmanager.platform.workspace.feature.message.usecase.validation.MessageValidator;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageTranslationCommandService {

    private final MessageTranslationRepository translationRepository;
    private final MessageFinder finder;
    private final MessageNormalizer normalizer;
    private final MessageMapper mapper;
    private final MessageValidator validator;

    public MessageTranslationCommandService(
        MessageTranslationRepository translationRepository,
        MessageFinder finder,
        MessageNormalizer normalizer,
        MessageMapper mapper,
        MessageValidator validator
    ) {
        this.translationRepository = translationRepository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "MESSAGE_TRANSLATION_CREATED", resourceType = "MESSAGE_TRANSLATION")
    public MessageTranslationOutput create(String messageIdentifier, CreateMessageTranslationInput rawInput) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        CreateMessageTranslationInput input = normalizer.normalize(rawInput);

        validator.validateTranslationForCreate(message, input);

        MessageTranslation translation = new MessageTranslation(message, mapper.toDetails(input), LocalDateTime.now());
        return MessageTranslationOutput.from(translationRepository.saveAndFlush(translation));
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "MESSAGE_TRANSLATION_UPDATED", resourceType = "MESSAGE_TRANSLATION")
    public MessageTranslationOutput update(
        String messageIdentifier,
        String translationIdentifier,
        UpdateMessageTranslationInput rawInput
    ) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        MessageTranslation translation = finder.findTranslationForUpdate(message, translationIdentifier);
        UpdateMessageTranslationInput input = normalizer.normalize(rawInput);

        validator.validateTranslationForUpdate(translation, input);

        translation.update(mapper.toDetails(input), LocalDateTime.now());
        translationRepository.flush();
        return MessageTranslationOutput.from(translation);
    }

    @Transactional
    @Auditable(
        action = AuditAction.ACTIVATE,
        event = "MESSAGE_TRANSLATION_ACTIVATED",
        resourceType = "MESSAGE_TRANSLATION"
    )
    public MessageTranslationOutput activate(String messageIdentifier, String translationIdentifier) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        MessageTranslation translation = finder.findTranslationForUpdate(message, translationIdentifier);
        translation.activate(LocalDateTime.now());
        translationRepository.flush();
        return MessageTranslationOutput.from(translation);
    }

    @Transactional
    @Auditable(
        action = AuditAction.DEACTIVATE,
        event = "MESSAGE_TRANSLATION_DEACTIVATED",
        resourceType = "MESSAGE_TRANSLATION"
    )
    public MessageTranslationOutput inactivate(String messageIdentifier, String translationIdentifier) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        MessageTranslation translation = finder.findTranslationForUpdate(message, translationIdentifier);
        translation.inactivate(LocalDateTime.now());
        translationRepository.flush();
        return MessageTranslationOutput.from(translation);
    }

    @Transactional
    @Auditable(action = AuditAction.PURGE, event = "MESSAGE_TRANSLATION_PURGED", resourceType = "MESSAGE_TRANSLATION")
    public MessageTranslationOutput delete(String messageIdentifier, String translationIdentifier) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        MessageTranslation translation = finder.findTranslationForUpdate(message, translationIdentifier);
        translation.requireInactiveForDeletion();
        MessageTranslationOutput removedTranslation = MessageTranslationOutput.from(translation);
        translationRepository.delete(translation);
        translationRepository.flush();
        return removedTranslation;
    }

    /** Receives current, nonempty snapshots under the parent/translation locks held by the caller. */
    @Transactional
    @Auditable(action = AuditAction.PURGE, event = "MESSAGE_TRANSLATION_PURGED", resourceType = "MESSAGE_TRANSLATION")
    public List<MessageTranslationOutput> deleteAllForMessage(Message message, List<DeletionSnapshot> snapshots) {
        message.requireInactiveForDeletion();
        List<MessageTranslationOutput> removedTranslations = snapshots.stream()
            .map(snapshot -> new MessageTranslationOutput(
                snapshot.getIdentifier(),
                message.getIdentifier(),
                snapshot.getVersion(),
                snapshot.getLocale(),
                snapshot.getTitle(),
                snapshot.getDetail(),
                snapshot.getSuggestion(),
                snapshot.getLifecycle()
            ))
            .toList();
        translationRepository.deleteAllByMessageId(message.getId());
        translationRepository.flush();
        return removedTranslations;
    }

    /** Receives normalized inputs and a message just persisted by the enclosing creation transaction. */
    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "MESSAGE_TRANSLATION_CREATED", resourceType = "MESSAGE_TRANSLATION")
    public List<MessageTranslationOutput> createAllForNewMessage(
        Message message,
        List<CreateMessageTranslationInput> inputs,
        LocalDateTime now
    ) {
        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }
        message.requireActiveForTranslation();
        validator.validateInlineTranslations(inputs);
        List<MessageTranslation> messageTranslations = inputs.stream()
            .map(input -> new MessageTranslation(message, mapper.toDetails(input), now))
            .toList();
        return translationRepository.saveAllAndFlush(messageTranslations).stream()
            .map(MessageTranslationOutput::from)
            .toList();
    }
}
