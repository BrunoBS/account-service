package br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.parameterkey;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.parameterkey.DynamicParameterMicroserviceAdapter;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.repository.parameterkey.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.ParameterKeyMetadata;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.validation.parameterkey.ParameterKeyValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ParameterKeyPublicationOperations {
    private final ParameterKeyFinder finder;
    private final ParameterKeyValidator validator;
    private final ParameterKeyRepository repository;
    private final DynamicParameterMicroserviceAdapter microservices;

    public ParameterKeyPublicationOperations(
            ParameterKeyFinder finder,
            ParameterKeyValidator validator,
            ParameterKeyRepository repository,
            DynamicParameterMicroserviceAdapter microservices) {
        this.finder = finder;
        this.validator = validator;
        this.repository = repository;
        this.microservices = microservices;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Auditable(
            action = AuditAction.UPDATE,
            event = "PARAMETER_KEY_PUBLISHED",
            resourceType = "PARAMETER_KEY")
    public ParameterKeyMetadata publish(String identifier) {
        var resource = finder.findForUpdate(identifier);
        validator.validatePublication(resource);
        var reference =
                microservices
                        .findByInternalIds(List.of(resource.getMicroserviceId()))
                        .get(resource.getMicroserviceId());
        resource.publish(LocalDateTime.now());
        var metadata = ParameterKeyMetadata.from(repository.saveAndFlush(resource), reference);
        return metadata;
    }
}
