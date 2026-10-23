package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message;

import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.Message;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.message.MessageRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.mapping.message.MessageMapper;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.MessageOutput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.port.MicroserviceReferencePort;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.port.MicroserviceReferencePort.Reference;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.validation.message.MessageValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleFilter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessageQueryService {

    private final MessageRepository messageRepository;
    private final MessageFinder finder;
    private final MessageNormalizer normalizer;
    private final MessageMapper mapper;
    private final MessageValidator validator;
    private final MicroserviceReferencePort microservices;

    public MessageQueryService(
            MessageRepository messageRepository,
            MessageFinder finder,
            MessageNormalizer normalizer,
            MessageMapper mapper,
            MicroserviceReferencePort microservices,
            MessageValidator validator) {
        this.messageRepository = messageRepository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.mapper = mapper;
        this.validator = validator;
        this.microservices = microservices;
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
            Pageable pageable) {
        validator.validatePagination(pageable);
        Sort requestedSort =
                pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("messageKey");
        Sort stableSort =
                requestedSort.getOrderFor("id") == null
                        ? requestedSort.and(Sort.by("id"))
                        : requestedSort;
        Pageable sortedPageable =
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), stableSort);
        String normalizedMicroserviceIdentifier =
                normalizer.normalizeMicroserviceIdentifierFilter(microserviceIdentifier);
        Long microserviceId =
                normalizedMicroserviceIdentifier == null
                        ? null
                        : microservices
                                .findByIdentifier(normalizedMicroserviceIdentifier)
                                .internalId();

        Page<Message> messagePage =
                messageRepository.findFiltered(
                        microserviceId,
                        LifecycleFilter.resolve(lifecycle).value(),
                        normalizer.normalizeCodeFilter(code),
                        normalizer.normalizeMessageKeyFilter(messageKey),
                        sortedPageable);
        Set<Long> microserviceIds =
                messagePage.stream().map(Message::getMicroserviceId).collect(Collectors.toSet());
        Map<Long, Reference> microserviceReferences =
                microservices.findByInternalIds(microserviceIds);
        return messagePage.map(
                message ->
                        mapper.toOutput(
                                message,
                                microserviceReferences
                                        .get(message.getMicroserviceId())
                                        .identifier()));
    }

    private MessageOutput toOutput(Message message) {
        return mapper.toOutput(
                message,
                microservices
                        .findByInternalIds(Set.of(message.getMicroserviceId()))
                        .get(message.getMicroserviceId())
                        .identifier());
    }
}
