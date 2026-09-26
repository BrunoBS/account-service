package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.platform.repository.MicroserviceRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.MicroserviceOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service
public class MicroserviceQueryService {
    private final MicroserviceRepository repository;

    public MicroserviceQueryService(MicroserviceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean existsActive(String code) {
        return code != null && repository.findByCode(code)
                .filter(microservice -> LifecycleTypeCode.active().equals(microservice.getLifecycle()))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public boolean existsActiveByIdentifier(String identifier) {
        return identifier != null && repository.findByIdentifier(identifier)
                .filter(microservice -> LifecycleTypeCode.active().equals(microservice.getLifecycle()))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public Long findInternalIdByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier)
                .map(br.com.portalmanager.platform.workspace.feature.platform.domain.Microservice::getId)
                .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.MICROSERVICE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Long findActiveInternalIdByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier)
                .filter(microservice -> LifecycleTypeCode.active().equals(microservice.getLifecycle()))
                .map(br.com.portalmanager.platform.workspace.feature.platform.domain.Microservice::getId)
                .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.ACTIVE_MICROSERVICE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public String findIdentifierByInternalId(Long id) {
        return repository.findById(id)
                .map(br.com.portalmanager.platform.workspace.feature.platform.domain.Microservice::getIdentifier)
                .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.MICROSERVICE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public MicroserviceOutput findByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier)
                .map(MicroserviceOutput::from)
                .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.MICROSERVICE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<MicroserviceOutput> findAll() {
        return repository.findAll().stream().map(MicroserviceOutput::from).toList();
    }
}
