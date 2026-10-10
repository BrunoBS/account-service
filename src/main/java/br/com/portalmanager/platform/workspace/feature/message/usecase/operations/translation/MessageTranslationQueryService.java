package br.com.portalmanager.platform.workspace.feature.message.usecase.operations.translation;

import br.com.portalmanager.platform.workspace.feature.message.domain.Message;
import br.com.portalmanager.platform.workspace.feature.message.domain.MessageTranslation;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageTranslationRepository;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.MessageTranslationOutput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageFinder;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageNormalizer;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageTranslationQueryService {

    private final MessageTranslationRepository repository;
    private final MessageFinder finder;
    private final MessageNormalizer normalizer;

    public MessageTranslationQueryService(
        MessageTranslationRepository repository,
        MessageFinder finder,
        MessageNormalizer normalizer
    ) {
        this.repository = repository;
        this.finder = finder;
        this.normalizer = normalizer;
    }

    @Transactional(readOnly = true)
    public List<MessageTranslationOutput> findAll(String messageIdentifier, String locale, Boolean active) {
        Message message = finder.findMessage(messageIdentifier);
        return repository
            .findFiltered(message.getId(), normalizer.normalizeLocaleFilter(locale), lifecycle(active))
            .stream()
            .map(MessageTranslationOutput::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public MessageTranslationOutput findByIdentifier(String messageIdentifier, String translationIdentifier) {
        Message message = finder.findMessage(messageIdentifier);
        MessageTranslation translation = finder.findTranslation(message, translationIdentifier);
        return MessageTranslationOutput.from(translation);
    }

    private String lifecycle(Boolean active) {
        if (active == null) {
            return null;
        }
        return active ? LifecycleTypeCode.active().value() : LifecycleTypeCode.inactive().value();
    }
}
