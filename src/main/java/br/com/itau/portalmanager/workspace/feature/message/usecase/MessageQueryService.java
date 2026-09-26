package br.com.itau.portalmanager.workspace.feature.message.usecase;

import br.com.itau.portalmanager.workspace.feature.message.domain.Message;
import br.com.itau.portalmanager.workspace.feature.message.repository.MessageRepository;
import br.com.itau.portalmanager.workspace.feature.message.usecase.model.MessageOutput;
import br.com.itau.portalmanager.workspace.feature.message.usecase.support.MessageFinder;
import br.com.itau.portalmanager.workspace.feature.message.usecase.support.MessageNormalizer;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.service.ServiceQueryService;
import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageQueryService {

    private final MessageRepository repository;
    private final MessageFinder finder;
    private final MessageNormalizer normalizer;
    private final ServiceQueryService serviceQueryService;

    public MessageQueryService(
            MessageRepository repository,
            MessageFinder finder,
            MessageNormalizer normalizer,
            ServiceQueryService serviceQueryService
    ) {
        this.repository = repository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.serviceQueryService = serviceQueryService;
    }

    @Transactional(readOnly = true)
    public MessageOutput findByIdentifier(String identifier) {
        return output(finder.findMessage(identifier));
    }

    @Transactional(readOnly = true)
    public List<MessageOutput> findAll(
            String serviceIdentifier,
            Boolean active,
            String code,
            String messageKey
    ) {
        String normalizedServiceIdentifier =
                normalizer.normalizeServiceIdentifierFilter(serviceIdentifier);
        Long serviceId = normalizedServiceIdentifier == null
                ? null
                : serviceQueryService.findInternalIdByIdentifier(normalizedServiceIdentifier);

        return repository.findFiltered(
                        serviceId,
                        lifecycle(active),
                        normalizer.normalizeCodeFilter(code),
                        normalizer.normalizeMessageKeyFilter(messageKey)
                ).stream()
                .map(this::output)
                .toList();
    }

    private MessageOutput output(Message message) {
        return MessageOutput.from(
                message,
                serviceQueryService.findIdentifierByInternalId(message.getServiceId())
        );
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
