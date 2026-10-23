package br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.feature.parameter.domain.*;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.mapping.ParameterKeyMapper;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterKeyMetadata;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.MicroserviceReferencePort;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.MicroserviceReferencePort.Reference;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.validation.ParameterKeyValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ParameterKeyPublicationCommandService {
    private final ParameterKeyFinder finder;
    private final ParameterKeyRepository repository;
    private final ParameterKeyValidator validator;
    private final ParameterKeyMapper mapper;
    private final MicroserviceReferencePort microservices;

    public ParameterKeyPublicationCommandService(
            ParameterKeyFinder finder,
            ParameterKeyRepository repository,
            ParameterKeyValidator validator,
            ParameterKeyMapper mapper,
            MicroserviceReferencePort microservices) {
        this.finder = finder;
        this.repository = repository;
        this.validator = validator;
        this.mapper = mapper;
        this.microservices = microservices;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Auditable(
            action = AuditAction.UPDATE,
            event = "PARAMETER_KEY_PUBLISHED",
            resourceType = "PARAMETER_KEY")
    public ParameterKeyMetadata publishIdentifier(String identifier) {
        ParameterKey resource = finder.findForUpdate(identifier);
        validator.validatePublication(resource);
        Reference reference =
                microservices
                        .findByInternalIds(List.of(resource.getMicroserviceId()))
                        .get(resource.getMicroserviceId());
        resource.publish(LocalDateTime.now());
        return mapper.toMetadata(repository.saveAndFlush(resource), reference);
    }
}
