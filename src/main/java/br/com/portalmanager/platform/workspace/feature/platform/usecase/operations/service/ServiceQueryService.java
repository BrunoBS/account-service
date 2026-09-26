package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.service;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.platform.repository.ServiceRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.ServiceOutput;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
public class ServiceQueryService {
    private final ServiceRepository repository;

    public ServiceQueryService(ServiceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean existsActive(String code) {
        return code != null && repository.findByCode(code)
                .filter(service -> LifecycleTypeCode.active().equals(service.getLifecycle()))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public boolean existsActiveByIdentifier(String identifier) {
        return identifier != null && repository.findByIdentifier(identifier)
                .filter(service -> LifecycleTypeCode.active().equals(service.getLifecycle()))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public Long findInternalIdByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier)
                .map(br.com.portalmanager.platform.workspace.feature.platform.domain.Service::getId)
                .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.SERVICE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Long findActiveInternalIdByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier)
                .filter(service -> LifecycleTypeCode.active().equals(service.getLifecycle()))
                .map(br.com.portalmanager.platform.workspace.feature.platform.domain.Service::getId)
                .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.ACTIVE_SERVICE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public String findIdentifierByInternalId(Long id) {
        return repository.findById(id)
                .map(br.com.portalmanager.platform.workspace.feature.platform.domain.Service::getIdentifier)
                .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.SERVICE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ServiceOutput findByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier)
                .map(ServiceOutput::from)
                .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.SERVICE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<ServiceOutput> findAll() {
        return repository.findAll().stream().map(ServiceOutput::from).toList();
    }
}
