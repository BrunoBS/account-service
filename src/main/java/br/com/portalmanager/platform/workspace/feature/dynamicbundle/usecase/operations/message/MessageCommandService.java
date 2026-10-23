package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.Message;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.message.MessageRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.messagetranslator.MessageTranslatorRepository.DeletionSnapshot;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.mapping.message.MessageMapper;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.CreateMessageInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.MessageOutput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.UpdateMessageInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.messagetranslator.MessageTranslatorCommandService;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.port.MicroserviceReferencePort;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.validation.message.MessageValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageCommandService {

    private final MessageRepository messageRepository;
    private final MessageTranslatorCommandService translatorCommandService;
    private final MessageFinder finder;
    private final MessageNormalizer normalizer;
    private final MessageMapper mapper;
    private final MessageValidator validator;
    private final MicroserviceReferencePort microservices;

    public MessageCommandService(
            MessageRepository messageRepository,
            MessageTranslatorCommandService translatorCommandService,
            MessageFinder finder,
            MessageNormalizer normalizer,
            MessageMapper mapper,
            MessageValidator validator,
            MicroserviceReferencePort microservices) {
        this.messageRepository = messageRepository;
        this.translatorCommandService = translatorCommandService;
        this.finder = finder;
        this.normalizer = normalizer;
        this.mapper = mapper;
        this.validator = validator;
        this.microservices = microservices;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "MESSAGE_CREATED", resourceType = "MESSAGE")
    public MessageOutput create(CreateMessageInput rawInput) {
        CreateMessageInput input = normalizer.normalize(rawInput);

        Long microserviceId = validator.validateForCreate(input);

        LocalDateTime now = LocalDateTime.now();
        Message message = mapper.toDomain(microserviceId, input, now);
        Message savedMessage = messageRepository.saveAndFlush(message);

        if (input.translators() != null && !input.translators().isEmpty()) {
            translatorCommandService.createAllForNewMessage(savedMessage, input.translators(), now);
        }
        return mapper.toOutput(savedMessage, input.microserviceIdentifier());
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "MESSAGE_UPDATED", resourceType = "MESSAGE")
    public MessageOutput update(String messageIdentifier, UpdateMessageInput rawInput) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        UpdateMessageInput input = normalizer.normalize(rawInput);

        Long microserviceId = validator.validateForUpdate(message, input);

        message.update(microserviceId, mapper.toDetails(input), LocalDateTime.now());
        return mapper.toOutput(
                messageRepository.saveAndFlush(message), input.microserviceIdentifier());
    }

    @Transactional
    @Auditable(action = AuditAction.ACTIVATE, event = "MESSAGE_ACTIVATED", resourceType = "MESSAGE")
    public MessageOutput activate(String messageIdentifier) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        message.activate(LocalDateTime.now());
        return toOutput(messageRepository.saveAndFlush(message));
    }

    @Transactional
    @Auditable(
            action = AuditAction.DEACTIVATE,
            event = "MESSAGE_DEACTIVATED",
            resourceType = "MESSAGE")
    public MessageOutput inactivate(String messageIdentifier) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        message.inactivate(LocalDateTime.now());
        return toOutput(messageRepository.saveAndFlush(message));
    }

    @Transactional
    @Auditable(action = AuditAction.PURGE, event = "MESSAGE_PURGED", resourceType = "MESSAGE")
    public MessageOutput delete(String messageIdentifier) {
        Message message = finder.findMessageForUpdate(messageIdentifier);
        message.requireInactiveForDeletion();
        MessageOutput removedMessage = toOutput(message);
        List<DeletionSnapshot> translatorSnapshots =
                finder.findTranslatorSnapshotsForDeletion(message);
        if (!translatorSnapshots.isEmpty()) {
            translatorCommandService.deleteAllForMessage(message, translatorSnapshots);
        }
        messageRepository.delete(message);
        messageRepository.flush();
        return removedMessage;
    }

    private MessageOutput toOutput(Message message) {
        return mapper.toOutput(
                message,
                microservices
                        .findByInternalIds(List.of(message.getMicroserviceId()))
                        .get(message.getMicroserviceId())
                        .identifier());
    }
}
