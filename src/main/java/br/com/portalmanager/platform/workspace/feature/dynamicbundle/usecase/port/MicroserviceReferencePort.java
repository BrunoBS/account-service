package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.port;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface MicroserviceReferencePort {
    Reference findByIdentifier(String identifier);

    Optional<Reference> findActiveByIdentifier(String identifier);

    Map<Long, Reference> findByInternalIds(Collection<Long> ids);

    record Reference(Long internalId, String identifier, String code) {}
}
