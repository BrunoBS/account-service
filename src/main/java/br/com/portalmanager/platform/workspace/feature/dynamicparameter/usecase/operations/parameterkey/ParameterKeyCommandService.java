package br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.parameterkey;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.domain.parameterkey.ParameterKey;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.parameterkey.DynamicParameterMicroserviceAdapter;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.parameterkey.DynamicParameterMicroserviceAdapter.Reference;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.repository.parameterkey.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.CreateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.ParameterKeyMetadata;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.UpdateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.validation.parameterkey.ParameterKeyValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.domain.ParameterKeyTypeCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ParameterKeyCommandService {
    private final ParameterKeyRepository repository;
    private final ParameterKeyFinder finder;
    private final ParameterKeyValidator validator;
    private final DynamicParameterMicroserviceAdapter microservices;
    private final ParameterKeyLifecycleOperations lifecycle;
    private final ParameterKeyAfterCommitSynchronization synchronization;

    public ParameterKeyCommandService(
            ParameterKeyRepository repository,
            ParameterKeyFinder finder,
            ParameterKeyValidator validator,
            DynamicParameterMicroserviceAdapter microservices,
            ParameterKeyLifecycleOperations lifecycle,
            ParameterKeyAfterCommitSynchronization synchronization) {
        this.repository = repository;
        this.finder = finder;
        this.validator = validator;
        this.microservices = microservices;
        this.lifecycle = lifecycle;
        this.synchronization = synchronization;
    }

    @Transactional
    @Auditable(
            action = AuditAction.CREATE,
            event = "PARAMETER_KEY_CREATED",
            resourceType = "PARAMETER_KEY")
    public ParameterKeyMetadata create(CreateParameterKeyInput input) {
        validator.validateCreateInput(input);
        Reference reference = microservices.findByIdentifier(input.microserviceIdentifier());
        validator.validateUnique(reference.internalId(), input.key());
        ParameterKey resource =
                new ParameterKey(
                        reference.internalId(),
                        input.key(),
                        input.description(),
                        ParameterKeyTypeCode.of(input.type()),
                        input.value(),
                        LocalDateTime.now());
        return ParameterKeyMetadata.from(repository.saveAndFlush(resource), reference);
    }

    @Transactional
    @Auditable(
            action = AuditAction.UPDATE,
            event = "PARAMETER_KEY_UPDATED",
            resourceType = "PARAMETER_KEY")
    public ParameterKeyMetadata update(String identifier, UpdateParameterKeyInput input) {
        ParameterKey resource = finder.findForUpdate(identifier);
        validator.validateUpdate(resource, input);
        resource.update(
                input.description(),
                ParameterKeyTypeCode.of(input.type()),
                input.value(),
                LocalDateTime.now());
        return ParameterKeyMetadata.from(repository.saveAndFlush(resource), reference(resource));
    }

    @Transactional
    public ParameterKeyMetadata activate(String identifier) {
        ParameterKey resource = finder.findForUpdate(identifier);
        Reference reference = reference(resource);
        return resource.isActive()
                ? ParameterKeyMetadata.from(resource, reference)
                : lifecycle.activate(resource, reference);
    }

    @Transactional
    public ParameterKeyMetadata inactivate(String identifier) {
        ParameterKey resource = finder.findForUpdate(identifier);
        Reference reference = reference(resource);
        ParameterKeyMetadata metadata =
                !resource.isActive()
                        ? ParameterKeyMetadata.from(resource, reference)
                        : lifecycle.inactivate(resource, reference);
        synchronization.schedule(metadata);
        return metadata;
    }

    @Transactional
    @Auditable(
            action = AuditAction.PURGE,
            event = "PARAMETER_KEY_DELETED",
            resourceType = "PARAMETER_KEY")
    public ParameterKeyMetadata delete(String identifier) {
        ParameterKey resource = finder.findForUpdate(identifier);
        resource.requireDeletable();
        ParameterKeyMetadata snapshot = ParameterKeyMetadata.from(resource, reference(resource));
        repository.delete(resource);
        repository.flush();
        synchronization.schedule(snapshot);
        return snapshot;
    }

    private Reference reference(ParameterKey resource) {
        return microservices
                .findByInternalIds(List.of(resource.getMicroserviceId()))
                .get(resource.getMicroserviceId());
    }
}
