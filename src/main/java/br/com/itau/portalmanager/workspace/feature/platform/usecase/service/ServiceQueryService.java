package br.com.itau.portalmanager.workspace.feature.platform.usecase.service;

import br.com.itau.portalmanager.workspace.feature.platform.domain.Service;
import br.com.itau.portalmanager.workspace.feature.platform.repository.ServiceRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@org.springframework.stereotype.Service
public class ServiceQueryService {
    private final ServiceRepository repository;
    public ServiceQueryService(ServiceRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public boolean existsActive(String code) {
        return code != null && repository.findByCode(code)
                .filter(service -> LifecycleTypeCode.active().equals(service.getLifecycle())).isPresent();
    }
    @Transactional(readOnly = true)
    public Service findByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier).orElseThrow(() -> new IllegalArgumentException("Service not found"));
    }
    @Transactional(readOnly = true)
    public List<Service> findAll() { return repository.findAll(); }
}
