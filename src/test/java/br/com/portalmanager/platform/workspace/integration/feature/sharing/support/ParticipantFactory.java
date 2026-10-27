package br.com.portalmanager.platform.workspace.integration.feature.sharing.support;

import br.com.portalmanager.platform.workspace.entrypoint.sharing.participant.request.ParticipantConfigurationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.participant.request.ParticipantStatusRequest;

public final class ParticipantFactory {
    public ParticipantConfigurationRequest validConfiguration(String source, String destination) {
        return ParticipantBuilder.builder().mappings(source, destination).buildConfiguration();
    }
    public ParticipantConfigurationRequest withoutMappings() {
        return ParticipantBuilder.builder().buildConfiguration();
    }
    public ParticipantConfigurationRequest withInvalidPublicationMode(String source, String destination) {
        return ParticipantBuilder.builder().publicationMode("INVALID").mappings(source, destination).buildConfiguration();
    }
    public ParticipantStatusRequest reject() { return ParticipantBuilder.builder().buildRejection(); }
    public ParticipantStatusRequest approve(String sourceEnvironment, String destinationEnvironment) {
        return ParticipantBuilder.builder().mappings(sourceEnvironment, destinationEnvironment).buildApproval();
    }
    public ParticipantStatusRequest revoke() {
        return new ParticipantStatusRequest("REVOKE", null, null);
    }
    public ParticipantStatusRequest withInvalidAction() {
        return new ParticipantStatusRequest("UNKNOWN", null, null);
    }
}
