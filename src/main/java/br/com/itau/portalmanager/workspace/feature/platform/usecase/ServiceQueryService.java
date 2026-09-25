package br.com.itau.portalmanager.workspace.feature.platform.usecase;

import br.com.itau.portalmanager.workspace.feature.platform.repository.ServiceRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceQueryService {

    private final ServiceRepository repository;

    public ServiceQueryService(ServiceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean existsActive(String code) {
        return code != null
                && repository.findByCode(code)
                .filter(service -> LifecycleTypeCode.active().equals(service.getLifecycle()))
                .isPresent();
    }
}
