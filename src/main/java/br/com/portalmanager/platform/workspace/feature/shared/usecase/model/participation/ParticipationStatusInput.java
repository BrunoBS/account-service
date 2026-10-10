package br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation;

import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.mapping.EnvironmentMappingInput;

public record ParticipationStatusInput(
    String action,
    String publicationModeCode,
    EnvironmentMappingInput environmentMappings
) {
    public ParticipationConfigurationInput approval() {
        return new ParticipationConfigurationInput(publicationModeCode, environmentMappings);
    }
}
