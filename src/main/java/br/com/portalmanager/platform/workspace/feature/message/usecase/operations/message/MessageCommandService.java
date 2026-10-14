package br.com.portalmanager.platform.workspace.feature.message.usecase.operations.message;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.feature.message.domain.Message;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageRepository;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageTranslationRepository.DeletionSnapshot;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.MessageOutput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.UpdateMessageInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageFinder;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageMapper;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageNormalizer;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.translation.MessageTranslationCommandService;
import br.com.portalmanager.platform.workspace.feature.message.usecase.validation.MessageValidator;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceQueryService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageCommandService {

    private final MessageRepository messageRepository;
    private final MessageTranslationCommandService translationCommandService;
    private final MessageFinder finder;
    private final MessageNormalizer normalizer;
    private final MessageMapper mapper;
    private final MessageValidator validator;
    private final MicroserviceQueryService microserviceQueryService;

    public MessageCommandService(
        MessageRepository messageRepository,
        MessageTranslationCommandService translationCommandService,
        MessageFinder finder,
        MessageNormalizer normalizer,
        MessageMapper mapper,
        MessageValidator validator,
        MicroserviceQueryService microserviceQueryService
    ) {
        this.messageRepository = messageRepository;
        this.translationCommandService = translationCommandService;
        this.finder = finder;
        this.normalizer = normalizer;
        this.mapper = mapper;
        this.validator = validator;
        this.microserviceQueryService = microserviceQueryService;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "MESSAGE_CREATED", resourceType = "MESSAGE")
    public MessageOutput create(CreateMessageInput rawInput) {
        CreateMessageInput input = normalizer.normalize(rawInput);

        Long microserviceId = validator.validateForCreate(input);

        LocalDateTime now = LocalDateTime.now();
        Message message = new Message(microserviceId, mapper.toDetails(input), now);
        Message savedMessage = messageRepository.saveAndFlush(message);

        if (input.translations() != null && !input.translations().isEmpty()) {
            translationCommandService.createAllForNewMessage(savedMessage, input.translations(), now);
        }
        return MessageOutput.from(savedMessage, input.microserviceIdentifier());
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "MESSAGE_UPDATED", resourceType = "MESSAGE")
    public MessageOutput update(String messageIdentifier, UpdateMessageInput rawInput) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        UpdateMessageInput input = normalizer.normalize(rawInput);

        Long microserviceId = validator.validateForUpdate(message, input);

        message.update(microserviceId, mapper.toDetails(input), LocalDateTime.now());
        messageRepository.flush();
        return MessageOutput.from(message, input.microserviceIdentifier());
    }

    @Transactional
    @Auditable(action = AuditAction.ACTIVATE, event = "MESSAGE_ACTIVATED", resourceType = "MESSAGE")
    public MessageOutput activate(String messageIdentifier) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        message.activate(LocalDateTime.now());
        messageRepository.flush();
        return toOutput(message);
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "MESSAGE_DEACTIVATED", resourceType = "MESSAGE")
    public MessageOutput inactivate(String messageIdentifier) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        message.inactivate(LocalDateTime.now());
        messageRepository.flush();
        return toOutput(message);
    }

    @Transactional
    @Auditable(action = AuditAction.PURGE, event = "MESSAGE_PURGED", resourceType = "MESSAGE")
    public MessageOutput delete(String messageIdentifier) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        message.requireInactiveForDeletion();
        MessageOutput removedMessage = toOutput(message);
        List<DeletionSnapshot> translationSnapshots = finder.findTranslationSnapshotsForDeletion(message);
        if (!translationSnapshots.isEmpty()) {
            translationCommandService.deleteAllForMessage(message, translationSnapshots);
        }
        messageRepository.delete(message);
        messageRepository.flush();
        return removedMessage;
    }

    private MessageOutput toOutput(Message message) {
        return MessageOutput.from(
            message,
            microserviceQueryService.findIdentifierByInternalId(message.getMicroserviceId())
        );
    }
}
