package br.com.portalmanager.platform.workspace.feature.message.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.message.domain.Message;
import br.com.portalmanager.platform.workspace.feature.message.domain.MessageMessageKeys;
import br.com.portalmanager.platform.workspace.feature.message.domain.MessageTranslation;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageRepository;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageTranslationRepository;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageTranslationRepository.DeletionSnapshot;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MessageFinder {

    private final MessageRepository messageRepository;
    private final MessageTranslationRepository translationRepository;
    private final EntityManager entityManager;

    public MessageFinder(
        MessageRepository messageRepository,
        MessageTranslationRepository translationRepository,
        EntityManager entityManager
    ) {
        this.messageRepository = messageRepository;
        this.translationRepository = translationRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public Message findMessage(String messageIdentifier) {
        return messageRepository
            .findByIdentifier(messageIdentifier)
            .orElseThrow(() -> new NotFoundException(MessageMessageKeys.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public MessageTranslation findTranslation(Message message, String translationIdentifier) {
        return translationRepository
            .findByIdentifierAndMessageId(translationIdentifier, message.getId())
            .orElseThrow(() -> new NotFoundException(MessageMessageKeys.TRANSLATION_NOT_FOUND));
    }

    public Message findMessageForUpdate(String messageIdentifier) {
        Long messageId = messageRepository.findIdByIdentifier(messageIdentifier)
            .orElseThrow(() -> new NotFoundException(MessageMessageKeys.NOT_FOUND));
        // Detach before the locking read so Hibernate reloads current state even after an earlier read.
        entityManager.detach(entityManager.getReference(Message.class, messageId));
        return messageRepository.findByIdentifierForUpdate(messageIdentifier)
            .orElseThrow(() -> new NotFoundException(MessageMessageKeys.NOT_FOUND));
    }

    public MessageTranslation findTranslationForUpdate(Message message, String translationIdentifier) {
        Long translationId = translationRepository.findIdByIdentifierAndMessageId(translationIdentifier, message.getId())
            .orElseThrow(() -> new NotFoundException(MessageMessageKeys.TRANSLATION_NOT_FOUND));
        entityManager.detach(entityManager.getReference(MessageTranslation.class, translationId));
        return translationRepository.findByIdentifierForUpdate(translationIdentifier, message.getId())
            .orElseThrow(() -> new NotFoundException(MessageMessageKeys.TRANSLATION_NOT_FOUND));
    }

    /** The parent write lock is acquired first; scalar snapshots avoid stale managed translations. */
    public List<DeletionSnapshot> findTranslationSnapshotsForDeletion(Message message) {
        return translationRepository.findDeletionSnapshotsByMessageId(message.getId());
    }
}
