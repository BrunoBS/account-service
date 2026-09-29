package br.com.portalmanager.platform.workspace.core.publisher.usecase.operations;

import br.com.portalmanager.platform.workspace.core.publisher.domain.Publisher;
import br.com.portalmanager.platform.workspace.core.publisher.repository.PublisherRepository;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.CreatePublisherInput;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.PublisherOutput;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.UpdatePublisherInput;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.validation.PublisherValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeTypeCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PublisherCommandService {
    private final PublisherRepository repository;
    private final PublisherFinder finder;
    private final PublisherNormalizer normalizer;
    private final PublisherValidator validator;
    public PublisherCommandService(PublisherRepository repository, PublisherFinder finder,
                                   PublisherNormalizer normalizer, PublisherValidator validator) {
        this.repository = repository; this.finder = finder; this.normalizer = normalizer; this.validator = validator;
    }
    @Transactional
    public PublisherOutput create(CreatePublisherInput raw) {
        CreatePublisherInput input = normalizer.normalize(raw);
        ResourceScopeTypeCode scope = validator.validateCreate(input, input != null && input.code() != null
                && repository.existsByCode(input.code()));
        validator.validateSettings(input.code(), input.settings());
        Publisher publisher = new Publisher(input.code(), input.name(), input.description(), scope,
                Boolean.TRUE.equals(input.deprecated()), input.settings(), LocalDateTime.now());
        return PublisherOutput.from(repository.saveAndFlush(publisher));
    }
    @Transactional
    public PublisherOutput update(String identifier, UpdatePublisherInput raw) {
        Publisher publisher = finder.findActive(identifier);
        UpdatePublisherInput input = normalizer.normalize(raw);
        ResourceScopeTypeCode scope = validator.validateUpdate(input);
        validator.requireVersion(publisher.getVersion(), input.version());
        validator.validateSettings(publisher.getCode(), input.settings());
        publisher.update(input.name(), input.description(), scope,
                input.deprecated() == null ? publisher.isDeprecated() : input.deprecated(), input.settings(), LocalDateTime.now());
        return PublisherOutput.from(repository.saveAndFlush(publisher));
    }
    @Transactional public void inactivate(String identifier) { Publisher p = finder.findActive(identifier); p.inactivate(LocalDateTime.now()); repository.saveAndFlush(p); }
    @Transactional public PublisherOutput restore(String identifier) { Publisher p = finder.findInactive(identifier); p.restore(LocalDateTime.now()); return PublisherOutput.from(repository.saveAndFlush(p)); }
    @Transactional public void delete(String identifier) { Publisher p = finder.findInactiveForDeletion(identifier); p.quarantine(LocalDateTime.now()); repository.saveAndFlush(p); }
}
