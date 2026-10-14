package br.com.portalmanager.platform.workspace.core.publisher.usecase.operations;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.core.publisher.domain.Publisher;
import br.com.portalmanager.platform.workspace.core.publisher.repository.PublisherRepository;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.mapping.PublisherMapper;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.CreatePublisherInput;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.PublisherOutput;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.UpdatePublisherInput;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.validation.PublisherValidator;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublisherCommandService {

    private final PublisherRepository repository;
    private final PublisherFinder finder;
    private final PublisherValidator validator;
    private final PublisherMapper mapper;

    public PublisherCommandService(
        PublisherRepository repository,
        PublisherFinder finder,
        PublisherValidator validator,
        PublisherMapper mapper
    ) {
        this.repository = repository;
        this.finder = finder;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "PUBLISHER_CREATED", resourceType = "PUBLISHER")
    public PublisherOutput create(CreatePublisherInput input) {
        validator.validateForCreate(input);
        Publisher publisher = new Publisher(input.code(), mapper.toDetails(input), LocalDateTime.now());
        return PublisherOutput.from(repository.saveAndFlush(publisher));
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "PUBLISHER_UPDATED", resourceType = "PUBLISHER")
    public PublisherOutput update(String identifier, UpdatePublisherInput input) {
        Publisher publisher = finder.findActive(identifier);
        validator.validateForUpdate(publisher, input);
        publisher.update(mapper.toDetails(input, publisher.isDeprecated()), LocalDateTime.now());
        return PublisherOutput.from(repository.saveAndFlush(publisher));
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "PUBLISHER_DEACTIVATED", resourceType = "PUBLISHER")
    public PublisherOutput inactivate(String identifier) {
        Publisher publisher = finder.findActive(identifier);
        publisher.inactivate(LocalDateTime.now());
        return PublisherOutput.from(repository.saveAndFlush(publisher));
    }

    @Transactional
    @Auditable(action = AuditAction.RESTORE, event = "PUBLISHER_RESTORED", resourceType = "PUBLISHER")
    public PublisherOutput restore(String identifier) {
        Publisher publisher = finder.findInactive(identifier);
        publisher.restore(LocalDateTime.now());
        return PublisherOutput.from(repository.saveAndFlush(publisher));
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE, event = "PUBLISHER_DELETED", resourceType = "PUBLISHER")
    public PublisherOutput delete(String identifier) {
        Publisher publisher = finder.findInactiveForDeletion(identifier);
        publisher.quarantine(LocalDateTime.now());
        return PublisherOutput.from(repository.saveAndFlush(publisher));
    }
}
