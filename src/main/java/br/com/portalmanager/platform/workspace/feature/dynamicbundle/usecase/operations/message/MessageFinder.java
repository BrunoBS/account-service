package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.MessageMessageKeys;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.Message;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.messagetranslator.MessageTranslator;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.message.MessageRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.messagetranslator.MessageTranslatorRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.messagetranslator.MessageTranslatorRepository.DeletionSnapshot;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class MessageFinder {

    private final MessageRepository messageRepository;
    private final MessageTranslatorRepository translatorRepository;
    private final EntityManager entityManager;

    public MessageFinder(
            MessageRepository messageRepository,
            MessageTranslatorRepository translatorRepository,
            EntityManager entityManager) {
        this.messageRepository = messageRepository;
        this.translatorRepository = translatorRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public Message findMessage(String messageIdentifier) {
        return messageRepository
                .findByIdentifier(messageIdentifier)
                .orElseThrow(() -> new NotFoundException(MessageMessageKeys.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public MessageTranslator findTranslator(Message message, String translatorIdentifier) {
        return translatorRepository
                .findByIdentifierAndMessageId(translatorIdentifier, message.getId())
                .orElseThrow(() -> new NotFoundException(MessageMessageKeys.TRANSLATOR_NOT_FOUND));
    }

    public Message findMessageForUpdate(String messageIdentifier) {
        Long messageId =
                messageRepository
                        .findIdByIdentifier(messageIdentifier)
                        .orElseThrow(() -> new NotFoundException(MessageMessageKeys.NOT_FOUND));
        // Detach before the locking read so Hibernate reloads current state even after an earlier
        // read.
        entityManager.detach(entityManager.getReference(Message.class, messageId));
        return messageRepository
                .findByIdentifierForUpdate(messageIdentifier)
                .orElseThrow(() -> new NotFoundException(MessageMessageKeys.NOT_FOUND));
    }

    public MessageTranslator findTranslatorForUpdate(Message message, String translatorIdentifier) {
        Long translatorId =
                translatorRepository
                        .findIdByIdentifierAndMessageId(translatorIdentifier, message.getId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                MessageMessageKeys.TRANSLATOR_NOT_FOUND));
        entityManager.detach(entityManager.getReference(MessageTranslator.class, translatorId));
        return translatorRepository
                .findByIdentifierForUpdate(translatorIdentifier, message.getId())
                .orElseThrow(() -> new NotFoundException(MessageMessageKeys.TRANSLATOR_NOT_FOUND));
    }

    /**
     * The parent write lock is acquired first; scalar snapshots avoid stale managed translators.
     */
    public List<DeletionSnapshot> findTranslatorSnapshotsForDeletion(Message message) {
        return translatorRepository.findDeletionSnapshotsByMessageId(message.getId());
    }
}
