package br.com.portalmanager.platform.workspace.feature.dynamicbundle.integration;

import br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.message.MessageRepository;
import br.com.portalmanager.platform.workspace.foundation.integration.MicroserviceUsagePort;

import org.springframework.stereotype.Component;

@Component
public class MessageUsageAdapter implements MicroserviceUsagePort {
    private final MessageRepository repository;

    public MessageUsageAdapter(MessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isInUse(Long microserviceId) {
        return repository.existsByMicroserviceId(microserviceId);
    }
}
