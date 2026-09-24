package br.com.itau.portalmanager.workspace.feature.message.usecase.support;

import br.com.itau.portalmanager.workspace.feature.message.domain.Message;
import br.com.itau.portalmanager.workspace.feature.message.domain.MessageMessageKeys;
import br.com.itau.portalmanager.workspace.feature.message.domain.MessageTranslation;
import br.com.itau.portalmanager.workspace.feature.message.repository.MessageRepository;
import br.com.itau.portalmanager.workspace.feature.message.repository.MessageTranslationRepository;
import br.com.portalmanager.platform.messaging.exception.NotFoundException;
import org.springframework.stereotype.Component;

@Component
public class MessageFinder {

    private final MessageRepository messageRepository;
    private final MessageTranslationRepository translationRepository;

    public MessageFinder(
            MessageRepository messageRepository,
            MessageTranslationRepository translationRepository
    ) {
        this.messageRepository = messageRepository;
        this.translationRepository = translationRepository;
    }

    public Message findMessage(String identifier) {
        return messageRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new NotFoundException(MessageMessageKeys.NOT_FOUND));
    }

    public MessageTranslation findTranslation(Message message, String translationIdentifier) {
        return translationRepository
                .findByIdentifierAndMessageId(translationIdentifier, message.getId())
                .orElseThrow(() -> new NotFoundException(MessageMessageKeys.TRANSLATION_NOT_FOUND));
    }
}
