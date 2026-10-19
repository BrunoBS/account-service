package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.messagetranslator;

import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.Message;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.messagetranslator.MessageTranslator;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.messagetranslator.MessageTranslatorRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.MessageTranslatorOutput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.mapping.messagetranslator.MessageTranslatorMapper;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message.MessageFinder;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.messagetranslator.MessageTranslatorNormalizer;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.validation.messagetranslator.MessageTranslatorValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleFilter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageTranslatorQueryService {

    private final MessageTranslatorRepository translatorRepository;
    private final MessageFinder finder;
    private final MessageTranslatorNormalizer normalizer;
    private final MessageTranslatorMapper mapper;
    private final MessageTranslatorValidator validator;

    public MessageTranslatorQueryService(
            MessageTranslatorRepository translatorRepository,
            MessageFinder finder,
            MessageTranslatorNormalizer normalizer,
            MessageTranslatorMapper mapper,
            MessageTranslatorValidator validator) {
        this.translatorRepository = translatorRepository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public Page<MessageTranslatorOutput> findAll(
            String messageIdentifier, String locale, String lifecycle, Pageable pageable) {
        validator.validatePagination(pageable);
        Sort requestedSort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("locale");
        Sort stableSort =
                requestedSort.getOrderFor("id") == null
                        ? requestedSort.and(Sort.by("id"))
                        : requestedSort;
        Pageable sortedPageable =
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), stableSort);
        Message message = finder.findMessage(messageIdentifier);
        return translatorRepository
                .findFiltered(
                        message.getId(),
                        normalizer.normalizeLocaleFilter(locale),
                        LifecycleFilter.resolve(lifecycle).value(),
                        sortedPageable)
                .map(mapper::toOutput);
    }

    @Transactional(readOnly = true)
    public MessageTranslatorOutput findByIdentifier(
            String messageIdentifier, String translatorIdentifier) {
        Message message = finder.findMessage(messageIdentifier);
        MessageTranslator translator = finder.findTranslator(message, translatorIdentifier);
        return mapper.toOutput(translator);
    }
}
