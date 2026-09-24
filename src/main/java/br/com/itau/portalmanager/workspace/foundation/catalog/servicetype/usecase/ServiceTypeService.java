package br.com.itau.portalmanager.workspace.foundation.catalog.servicetype.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.servicetype.repository.ServiceTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceTypeService {

    private final ServiceTypeRepository repository;

    public ServiceTypeService(ServiceTypeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean existsActive(String code) {
        return code != null
                && repository.findById(code)
                .filter(service -> service.isActive())
                .isPresent();
    }
}
