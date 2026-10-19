package br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.participant;

public record ParticipantStatusInput(
        String action, String publicationModeCode, EnvironmentMappingInput environmentMappings) {
    public ParticipantConfigurationInput approval() {
        return new ParticipantConfigurationInput(publicationModeCode, environmentMappings);
    }
}
