package br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.parameterkey;

import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.MicroserviceReference;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.microservice.MicroserviceQueryService;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DynamicParameterMicroserviceAdapter {
    private final MicroserviceQueryService microservices;

    public DynamicParameterMicroserviceAdapter(MicroserviceQueryService microservices) {
        this.microservices = microservices;
    }

    public Reference findByIdentifier(String identifier) {
        return toReference(microservices.findReferenceForDynamicParameter(identifier));
    }

    public Map<Long, Reference> findByInternalIds(Collection<Long> ids) {
        return microservices.findReferencesByInternalIds(ids).values().stream()
                .collect(Collectors.toMap(MicroserviceReference::id, this::toReference));
    }

    private Reference toReference(MicroserviceReference reference) {
        return new Reference(reference.id(), reference.identifier(), reference.code());
    }

    public record Reference(Long internalId, String identifier, String code) {}
}
