package br.com.itau.portalmanager.workspace.feature.message.usecase;

import br.com.itau.portalmanager.workspace.feature.message.domain.Message;
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
    public List<MessageOutput> findAll(String service) {
        String normalizedService = normalizer.normalizeServiceFilter(service);
        List<Message> messages = normalizedService == null
                ? messageRepository.findAllByOrderByServiceCodeAscMessageKeyAsc()
                : messageRepository.findByServiceCodeOrderByMessageKeyAsc(normalizedService);

        return messages.stream().map(MessageOutput::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MessageTranslationOutput> findTranslations(String messageIdentifier) {
        Message message = finder.findMessage(messageIdentifier);
        return translationRepository.findByMessageIdOrderByLocaleAsc(message.getId())
                .stream()
                .map(MessageTranslationOutput::from)
                .toList();
    }
}
