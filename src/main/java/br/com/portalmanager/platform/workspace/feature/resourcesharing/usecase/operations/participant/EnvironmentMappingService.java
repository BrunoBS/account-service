package br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.operations.participant;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentReferenceOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentQueryService;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.ResourceSharingMessageKeys;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.participant.EnvironmentMapping;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant.EnvironmentMappingInput;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EnvironmentMappingService {

    private final EnvironmentQueryService environmentQueryService;

    public EnvironmentMappingService(EnvironmentQueryService environmentQueryService) {
        this.environmentQueryService = environmentQueryService;
    }

    /** The command validates the complete input before resolving any references. */
    public LinkedHashSet<EnvironmentMapping> build(
            String ownerWorkspaceIdentifier,
            String participantWorkspaceIdentifier,
            EnvironmentMappingInput input,
            LocalDateTime timestamp) {
        Map<String, EnvironmentReferenceOutput> sourceEnvironments =
                findSourceEnvironments(participantWorkspaceIdentifier, input.mappings());
        Map<String, EnvironmentReferenceOutput> destinationEnvironments =
                findDestinationEnvironments(ownerWorkspaceIdentifier, input.mappings());
        LinkedHashSet<EnvironmentMapping> environmentMappings = new LinkedHashSet<>();
        for (EnvironmentMappingInput.Mapping mapping : input.mappings()) {
            EnvironmentReferenceOutput sourceEnvironment =
                    sourceEnvironments.get(mapping.sourceEnvironmentIdentifier());
            for (String destinationIdentifier : mapping.destinationEnvironmentIdentifiers()) {
                EnvironmentReferenceOutput destinationEnvironment =
                        destinationEnvironments.get(destinationIdentifier);
                environmentMappings.add(
                        createMapping(sourceEnvironment, destinationEnvironment, timestamp));
            }
        }
        return environmentMappings;
    }

    private Map<String, EnvironmentReferenceOutput> findSourceEnvironments(
            String participantWorkspaceIdentifier, List<EnvironmentMappingInput.Mapping> mappings) {
        List<String> sourceEnvironmentIdentifiers =
                mappings.stream()
                        .map(EnvironmentMappingInput.Mapping::sourceEnvironmentIdentifier)
                        .distinct()
                        .toList();
        return environmentQueryService.findAvailableReferencesForResourceSharing(
                participantWorkspaceIdentifier, sourceEnvironmentIdentifiers);
    }

    private Map<String, EnvironmentReferenceOutput> findDestinationEnvironments(
            String ownerWorkspaceIdentifier, List<EnvironmentMappingInput.Mapping> mappings) {
        List<String> destinationEnvironmentIdentifiers =
                mappings.stream()
                        .flatMap(mapping -> mapping.destinationEnvironmentIdentifiers().stream())
                        .distinct()
                        .toList();
        return environmentQueryService.findAvailableReferencesForResourceSharing(
                ownerWorkspaceIdentifier, destinationEnvironmentIdentifiers);
    }

    private EnvironmentMapping createMapping(
            EnvironmentReferenceOutput sourceEnvironment,
            EnvironmentReferenceOutput destinationEnvironment,
            LocalDateTime timestamp) {
        String sourceBaseIdentifier = baseIdentifier(sourceEnvironment.environment());
        String destinationBaseIdentifier = baseIdentifier(destinationEnvironment.environment());
        if (!Objects.equals(sourceBaseIdentifier, destinationBaseIdentifier)) {
            throw new ValidationException(ResourceSharingMessageKeys.MAPPING_INVALID);
        }
        return new EnvironmentMapping(
                sourceEnvironment.id(), destinationEnvironment.id(), timestamp);
    }

    private String baseIdentifier(EnvironmentOutput environment) {
        return environment.workspaceIdentifier() == null || environment.parentIdentifier() == null
                ? environment.identifier()
                : environment.parentIdentifier();
    }
}
