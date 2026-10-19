package br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.participant;

public record ParticipantConfigurationInput(
        String publicationModeCode, EnvironmentMappingInput environmentMappings) {}
