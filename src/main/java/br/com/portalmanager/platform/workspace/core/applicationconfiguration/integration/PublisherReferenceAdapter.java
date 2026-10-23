package br.com.portalmanager.platform.workspace.core.applicationconfiguration.integration;

import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port.PublisherReferencePort;
import br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.usecase.PublisherTypeService;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component("applicationconfigurationPublisherReferenceAdapter")
public class PublisherReferenceAdapter implements PublisherReferencePort {
    private final PublisherTypeService publishers;

    public PublisherReferenceAdapter(PublisherTypeService publishers) {
        this.publishers = publishers;
    }

    @Override
    public List<PublisherReference> resolveForAssociation(Collection<String> codes) {
        return publishers.resolveForAssociation(codes).stream()
                .map(
                        reference ->
                                new PublisherReference(
                                        reference.code(), reference.active(), reference.settings()))
                .toList();
    }
}
