package br.com.portalmanager.platform.workspace.foundation.integration;

import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeTypeCode;

import java.util.Collection;
import java.util.List;

public interface PublisherReferencePort {
    List<PublisherReference> resolveForAssociation(Collection<String> identifiers);

    List<PublisherReference> findReferences(Collection<Long> ids);

    record PublisherReference(
            Long id,
            String identifier,
            String code,
            ResourceScopeTypeCode scope,
            LifecycleTypeCode lifecycle,
            boolean deprecated) {}
}
