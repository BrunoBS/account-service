package br.com.portalmanager.platform.workspace.feature.message.usecase.operations.message;

import br.com.portalmanager.platform.workspace.feature.message.domain.Message;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageRepository;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.MessageOutput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageFinder;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageNormalizer;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceQueryService;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageQueryService {

    private final MessageRepository repository;
    private final MessageFinder finder;
    private final MessageNormalizer normalizer;
    private final MicroserviceQueryService microserviceQueryService;

    public MessageQueryService(
            MessageRepository repository,
            MessageFinder finder,
            MessageNormalizer normalizer,
            MicroserviceQueryService microserviceQueryService
    ) {
        this.repository = repository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.microserviceQueryService = microserviceQueryService;
    }

    @Transactional(readOnly = true)
    public MessageOutput findByIdentifier(String identifier) {
        return output(finder.findMessage(identifier));
    }

    @Transactional(readOnly = true)
    public List<MessageOutput> findAll(
            String microserviceIdentifier,
            Boolean active,
            String code,
            String messageKey
    ) {
        String normalizedMicroserviceIdentifier =
                normalizer.normalizeMicroserviceIdentifierFilter(microserviceIdentifier);
        Long microserviceId = normalizedMicroserviceIdentifier == null
                ? null
                : microserviceQueryService.findInternalIdByIdentifier(normalizedMicroserviceIdentifier);

        return repository.findFiltered(
                        microserviceId,
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
                microserviceQueryService.findIdentifierByInternalId(message.getMicroserviceId())
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
