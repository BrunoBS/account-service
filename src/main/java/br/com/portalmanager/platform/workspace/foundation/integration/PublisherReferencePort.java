package br.com.portalmanager.platform.workspace.foundation.integration;

import java.util.Collection;
import java.util.List;

public interface PublisherReferencePort {
    List<PublisherReference> resolveForAssociation(Collection<String> identifiers);

    List<PublisherReference> findReferences(Collection<Long> ids);

    record PublisherReference(
            Long id,
            String identifier,
            String code,
            String scope,
            String lifecycle,
            boolean deprecated) {}
}
