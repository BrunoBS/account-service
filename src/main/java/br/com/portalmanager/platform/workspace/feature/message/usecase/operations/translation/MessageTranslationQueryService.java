package br.com.portalmanager.platform.workspace.feature.message.usecase.operations.translation;

import br.com.portalmanager.platform.workspace.feature.message.domain.Message;
import br.com.portalmanager.platform.workspace.feature.message.domain.MessageTranslation;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageTranslationRepository;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.MessageTranslationOutput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageFinder;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageNormalizer;
import br.com.portalmanager.platform.workspace.feature.message.usecase.validation.MessageValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageTranslationQueryService {

    private final MessageTranslationRepository translationRepository;
    private final MessageFinder finder;
    private final MessageNormalizer normalizer;
    private final MessageValidator validator;

    public MessageTranslationQueryService(
        MessageTranslationRepository translationRepository,
        MessageFinder finder,
        MessageNormalizer normalizer,
        MessageValidator validator
    ) {
        this.translationRepository = translationRepository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public Page<MessageTranslationOutput> findAll(
        String messageIdentifier,
        String locale,
        String lifecycle,
        Pageable pageable
    ) {
        validator.validateTranslationPagination(pageable);
        Sort requestedSort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("locale");
        Sort stableSort = requestedSort.getOrderFor("id") == null ? requestedSort.and(Sort.by("id")) : requestedSort;
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), stableSort);
        Message message = finder.findMessage(messageIdentifier);
        return translationRepository
            .findFiltered(
                message.getId(),
                normalizer.normalizeLocaleFilter(locale),
                LifecycleFilter.resolve(lifecycle).value(),
                sortedPageable
            )
            .map(MessageTranslationOutput::from);
    }

    @Transactional(readOnly = true)
    public MessageTranslationOutput findByIdentifier(String messageIdentifier, String translationIdentifier) {
        Message message = finder.findMessage(messageIdentifier);
        MessageTranslation translation = finder.findTranslation(message, translationIdentifier);
        return MessageTranslationOutput.from(translation);
    }
}
