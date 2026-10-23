package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port;

import tools.jackson.databind.JsonNode;

import java.util.Collection;
import java.util.List;

public interface PublisherReferencePort {
    List<PublisherReference> resolveForAssociation(Collection<String> codes);

    record PublisherReference(String code, boolean active, JsonNode settings) {}
}
