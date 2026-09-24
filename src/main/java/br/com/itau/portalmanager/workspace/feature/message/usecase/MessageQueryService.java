package br.com.itau.portalmanager.workspace.feature.message.usecase;

import br.com.itau.portalmanager.workspace.feature.message.repository.MessageRepository;
import br.com.itau.portalmanager.workspace.feature.message.usecase.model.MessageOutput;
import br.com.itau.portalmanager.workspace.feature.message.usecase.support.MessageFinder;
import br.com.itau.portalmanager.workspace.feature.message.usecase.support.MessageNormalizer;
import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageQueryService {

    private final MessageRepository repository;
    private final MessageFinder finder;
    private final MessageNormalizer normalizer;

    public MessageQueryService(
            MessageRepository repository,
            MessageFinder finder,
            MessageNormalizer normalizer
    ) {
        this.repository = repository;
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
        return repository.findFiltered(
                        normalizer.normalizeServiceFilter(service),
                        lifecycle(active),
                        normalizer.normalizeCodeFilter(code),
                        normalizer.normalizeMessageKeyFilter(messageKey)
                ).stream()
                .map(MessageOutput::from)
                .toList();
    }

    private String lifecycle(Boolean active) {
        if (active == null) {
            return null;
        }
        return active
                ? LifecycleTypeCode.active().value()
                : LifecycleTypeCode.inactive().value();
    }
}
