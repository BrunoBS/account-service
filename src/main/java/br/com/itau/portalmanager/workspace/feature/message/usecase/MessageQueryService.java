package br.com.itau.portalmanager.workspace.feature.message.usecase;

import br.com.itau.portalmanager.workspace.feature.message.domain.Message;
import br.com.itau.portalmanager.workspace.feature.message.domain.MessageTranslation;
import br.com.itau.portalmanager.workspace.feature.message.repository.MessageRepository;
import br.com.itau.portalmanager.workspace.feature.message.repository.MessageTranslationRepository;
import br.com.itau.portalmanager.workspace.feature.message.usecase.model.MessageOutput;
import br.com.itau.portalmanager.workspace.feature.message.usecase.model.MessageTranslationOutput;
import br.com.itau.portalmanager.workspace.feature.message.usecase.support.MessageFinder;
import br.com.itau.portalmanager.workspace.feature.message.usecase.support.MessageNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageQueryService {

    private final MessageRepository messageRepository;
    private final MessageTranslationRepository translationRepository;
    private final MessageFinder finder;
    private final MessageNormalizer normalizer;

    public MessageQueryService(
            MessageRepository messageRepository,
            MessageTranslationRepository translationRepository,
            MessageFinder finder,
            MessageNormalizer normalizer
    ) {
        this.messageRepository = messageRepository;
        this.translationRepository = translationRepository;
        this.finder = finder;
        this.normalizer = normalizer;
    }

    @Transactional(readOnly = true)
    public MessageOutput findByIdentifier(String identifier) {
        return MessageOutput.from(finder.findMessage(identifier));
    }

    @Transactional(readOnly = true)
    public List<MessageOutput> findAll(
            String service,
            Boolean active,
            String code,
            String messageKey
    ) {
        String lifecycle = lifecycle(active);
        String normalizedService = normalizer.normalizeServiceFilter(service);
        String normalizedCode = normalizer.normalizeCodeFilter(code);
        String normalizedMessageKey = normalizer.normalizeMessageKeyFilter(messageKey);

        return messageRepository.findFiltered(
                        normalizedService,
                        lifecycle,
                        normalizedCode,
                        normalizedMessageKey
                ).stream()
                .map(MessageOutput::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageTranslationOutput> findTranslations(
            String messageIdentifier,
            String locale,
            Boolean active
    ) {
        Message message = finder.findMessage(messageIdentifier);
        String normalizedLocale = normalizer.normalizeLocaleFilter(locale);
        String lifecycle = lifecycle(active);

        return translationRepository.findFiltered(
                        message.getId(),
                        normalizedLocale,
                        lifecycle
                ).stream()
                .map(MessageTranslationOutput::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MessageTranslationOutput findTranslation(
            String messageIdentifier,
            String translationIdentifier
    ) {
        Message message = finder.findMessage(messageIdentifier);
        MessageTranslation translation = finder.findTranslation(
                message,
                translationIdentifier
        );
        return MessageTranslationOutput.from(translation);
    }

    private String lifecycle(Boolean active) {
        if (active == null) {
            return null;
        }
        return active ? Message.ACTIVE : Message.INACTIVE;
    }
}
