package br.com.portalmanager.platform.workspace.entrypoint.web.shared.response;

import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentResponse;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.SharedParticipationOutput;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

public record SharedParticipationResponse(
    String identifier,
    String contractIdentifier,
    ContractSummary contract,
    String participantWorkspaceIdentifier,
    String participantApplicationIdentifier,
    String status,
    String publicationMode,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<EnvironmentMapping> mappings,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<EnvironmentResponse> sourceEnvironments
) {
    public record ContractSummary(
        String identifier,
        String name,
        String lifecycle,
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String featureIdentifier
    ) {}

    public record EnvironmentMapping(String sourceEnvironmentIdentifier, String destinationEnvironmentIdentifier) {}

    public static SharedParticipationResponse from(SharedParticipationOutput output) {
        return new SharedParticipationResponse(
            output.identifier(),
            output.contractIdentifier(),
            new ContractSummary(
                output.contract().identifier(),
                output.contract().name(),
                output.contract().lifecycle(),
                output.contract().ownerWorkspaceIdentifier(),
                output.contract().ownerApplicationIdentifier(),
                output.contract().featureIdentifier()
            ),
            output.participantWorkspaceIdentifier(),
            output.participantApplicationIdentifier(),
            output.status(),
            output.publicationMode(),
            output.createdAt(),
            output.updatedAt(),
            output
                .mappings()
                .stream()
                .map(mapping ->
                    new EnvironmentMapping(
                        mapping.sourceEnvironmentIdentifier(),
                        mapping.destinationEnvironmentIdentifier()
                    )
                )
                .toList(),
            output.sourceEnvironments() == null
                ? null
                : output.sourceEnvironments().stream().map(EnvironmentResponse::from).toList()
        );
    }
}
