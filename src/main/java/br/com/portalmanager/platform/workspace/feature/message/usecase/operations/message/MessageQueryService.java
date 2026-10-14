package br.com.portalmanager.platform.workspace.feature.message.usecase.operations.message;

import br.com.portalmanager.platform.workspace.feature.message.domain.Message;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageRepository;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.MessageOutput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageFinder;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageNormalizer;
import br.com.portalmanager.platform.workspace.feature.message.usecase.validation.MessageValidator;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceQueryService;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleFilter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageQueryService {

    private final MessageRepository messageRepository;
    private final MessageFinder finder;
    private final MessageNormalizer normalizer;
    private final MessageValidator validator;
    private final MicroserviceQueryService microserviceQueryService;

    public MessageQueryService(
        MessageRepository messageRepository,
        MessageFinder finder,
        MessageNormalizer normalizer,
        MicroserviceQueryService microserviceQueryService,
        MessageValidator validator
    ) {
        this.messageRepository = messageRepository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.validator = validator;
        this.microserviceQueryService = microserviceQueryService;
    }

    @Transactional(readOnly = true)
    public MessageOutput findByIdentifier(String messageIdentifier) {
        return toOutput(finder.findMessage(messageIdentifier));
    }

    @Transactional(readOnly = true)
    public Page<MessageOutput> findAll(
        String microserviceIdentifier,
        String lifecycle,
        String code,
        String messageKey,
        Pageable pageable
    ) {
        validator.validateMessagePagination(pageable);
        Sort requestedSort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("messageKey");
        Sort stableSort = requestedSort.getOrderFor("id") == null ? requestedSort.and(Sort.by("id")) : requestedSort;
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), stableSort);
        String normalizedMicroserviceIdentifier = normalizer.normalizeMicroserviceIdentifierFilter(
            microserviceIdentifier
        );
        Long microserviceId =
            normalizedMicroserviceIdentifier == null
                ? null
                : microserviceQueryService.findInternalIdByIdentifier(normalizedMicroserviceIdentifier);

        Page<Message> messagePage = messageRepository.findFiltered(
            microserviceId,
            LifecycleFilter.resolve(lifecycle).value(),
            normalizer.normalizeCodeFilter(code),
            normalizer.normalizeMessageKeyFilter(messageKey),
            sortedPageable
        );
        Set<Long> microserviceIds = messagePage.stream().map(Message::getMicroserviceId).collect(Collectors.toSet());
        Map<Long, String> microserviceIdentifiers = microserviceQueryService.findIdentifiersByInternalIds(microserviceIds);
        return messagePage.map(message -> MessageOutput.from(
            message,
            microserviceIdentifiers.get(message.getMicroserviceId())
        ));
    }

    private MessageOutput toOutput(Message message) {
        return MessageOutput.from(
            message,
            microserviceQueryService.findIdentifierByInternalId(message.getMicroserviceId())
        );
    }
}
