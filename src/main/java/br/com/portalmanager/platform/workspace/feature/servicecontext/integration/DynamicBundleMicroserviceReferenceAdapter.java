package br.com.portalmanager.platform.workspace.feature.servicecontext.integration;

import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.port.MicroserviceReferencePort;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.MicroserviceReference;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.microservice.MicroserviceQueryService;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DynamicBundleMicroserviceReferenceAdapter implements MicroserviceReferencePort {
    private final MicroserviceQueryService microservices;

    public DynamicBundleMicroserviceReferenceAdapter(MicroserviceQueryService microservices) {
        this.microservices = microservices;
    }

    @Override
    public Reference findByIdentifier(String identifier) {
        return toReference(microservices.findReferenceByIdentifier(identifier));
    }

    @Override
    public Optional<Reference> findActiveByIdentifier(String identifier) {
        return microservices
                .findActiveReferenceIdByIdentifier(identifier)
                .map(id -> findByInternalIds(List.of(id)).get(id));
    }

    @Override
    public Map<Long, Reference> findByInternalIds(Collection<Long> ids) {
        return microservices.findReferencesByInternalIds(ids).values().stream()
                .map(this::toReference)
                .collect(Collectors.toMap(Reference::internalId, reference -> reference));
    }

    private Reference toReference(MicroserviceReference reference) {
        return new Reference(reference.id(), reference.identifier(), reference.code());
    }
}
