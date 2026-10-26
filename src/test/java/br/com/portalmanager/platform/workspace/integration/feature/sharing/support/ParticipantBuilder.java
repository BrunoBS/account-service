package br.com.portalmanager.platform.workspace.integration.feature.sharing.support;

import br.com.portalmanager.platform.workspace.entrypoint.sharing.participant.request.EnvironmentMappingRequest;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.participant.request.ParticipantConfigurationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.participant.request.ParticipantStatusRequest;
import java.util.List;

public final class ParticipantBuilder {
    private String publicationMode = "MANUAL";
    private EnvironmentMappingRequest mappings;
    private ParticipantBuilder() {}
    public static ParticipantBuilder builder() { return new ParticipantBuilder(); }
    public ParticipantBuilder publicationMode(String value) { publicationMode = value; return this; }
    public ParticipantBuilder mappings(String source, String destination) {
        mappings = new EnvironmentMappingRequest(List.of(new EnvironmentMappingRequest.Mapping(source, List.of(destination))));
        return this;
    }
    public ParticipantConfigurationRequest buildConfiguration() { return new ParticipantConfigurationRequest(publicationMode, mappings); }
    public ParticipantStatusRequest buildApproval() { return new ParticipantStatusRequest("APPROVE", publicationMode, mappings); }
    public ParticipantStatusRequest buildRejection() { return new ParticipantStatusRequest("REJECT", null, null); }
}
