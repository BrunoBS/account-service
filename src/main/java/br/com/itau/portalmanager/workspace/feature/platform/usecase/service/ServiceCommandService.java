package br.com.itau.portalmanager.workspace.feature.platform.usecase.service;

import br.com.itau.portalmanager.workspace.feature.platform.domain.Service;
import br.com.itau.portalmanager.workspace.feature.platform.repository.ServiceRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;

@org.springframework.stereotype.Service
public class ServiceCommandService {
    private final ServiceRepository repository;
    private final Clock clock;

    public ServiceCommandService(ServiceRepository repository, Clock clock) {
        this.repository = repository; this.clock = clock;
    }

    @Transactional
    public Service create(String code, String name, String description) {
        if (repository.existsByCode(code)) throw new IllegalArgumentException("Service code already exists");
        return repository.save(new Service(code, name, description, now()));
    }

    @Transactional
    public Service update(String identifier, String name, String description) {
        Service service = required(identifier); service.update(name, description, now()); return service;
    }

    @Transactional
    public Service activate(String identifier) {
        Service service = required(identifier); service.activate(now()); return service;
    }

    @Transactional
    public Service inactivate(String identifier) {
        Service service = required(identifier); service.inactivate(now()); return service;
    }

    @Transactional
    public Service delete(String identifier) {
        Service service = required(identifier);
        if (!service.getFeatures().isEmpty()) throw new IllegalStateException("Service with features cannot be quarantined");
        service.quarantine(now()); return service;
    }

    private Service required(String identifier) {
        return repository.findByIdentifier(identifier).orElseThrow(() -> new IllegalArgumentException("Service not found"));
    }
    private LocalDateTime now() { return LocalDateTime.now(clock); }
}
