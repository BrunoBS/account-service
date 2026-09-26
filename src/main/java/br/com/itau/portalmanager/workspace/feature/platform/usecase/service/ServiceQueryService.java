package br.com.itau.portalmanager.workspace.feature.platform.usecase.service;

import br.com.itau.portalmanager.workspace.feature.platform.repository.ServiceRepository;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.ServiceOutput;
import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
                .map(br.com.itau.portalmanager.workspace.feature.platform.domain.Service::getId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
    }

    @Transactional(readOnly = true)
    public Long findActiveInternalIdByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier)
                .filter(service -> LifecycleTypeCode.active().equals(service.getLifecycle()))
                .map(br.com.itau.portalmanager.workspace.feature.platform.domain.Service::getId)
                .orElseThrow(() -> new IllegalArgumentException("Active service not found"));
    }

    @Transactional(readOnly = true)
    public String findIdentifierByInternalId(Long id) {
        return repository.findById(id)
                .map(br.com.itau.portalmanager.workspace.feature.platform.domain.Service::getIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
    }

    @Transactional(readOnly = true)
    public ServiceOutput findByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier)
                .map(ServiceOutput::from)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
    }

    @Transactional(readOnly = true)
    public List<ServiceOutput> findAll() {
        return repository.findAll().stream().map(ServiceOutput::from).toList();
    }
}
