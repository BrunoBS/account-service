package br.com.portalmanager.platform.workspace.feature.shared.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentQueryService;
import br.com.portalmanager.platform.workspace.feature.shared.domain.SharedContract;
import br.com.portalmanager.platform.workspace.feature.shared.repository.SharedContractRepository;
import br.com.portalmanager.platform.workspace.feature.shared.repository.SharedParticipationRepository;
import br.com.portalmanager.platform.workspace.foundation.integration.SharedEligibilityPort;
import br.com.portalmanager.platform.workspace.foundation.integration.SharedEligibilityResult;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;

/** Public Shared use case for a future Publisher integration; it does not publish data. */
@Service
public class SharedEligibilityQueryService implements SharedEligibilityPort {
    private final SharedContractRepository contracts;
    private final SharedParticipationRepository participations;
    private final ApplicationQueryService applications;
    private final EnvironmentQueryService environments;

    public SharedEligibilityQueryService(SharedContractRepository contracts, SharedParticipationRepository participations,
            ApplicationQueryService applications, EnvironmentQueryService environments) {
        this.contracts = contracts; this.participations = participations;
        this.applications = applications; this.environments = environments;
    }

    @Override
    @Transactional(readOnly = true)
    public SharedEligibilityResult resolve(String ownerWorkspace, String ownerApplication, String contractIdentifier,
            String participantWorkspace, String participantApplication, String sourceEnvironmentIdentifier) {
        try {
            applications.findActiveForShared(ownerWorkspace, ownerApplication);
            applications.findActiveForShared(participantWorkspace, participantApplication);
        } catch (NotFoundException ex) {
            return ineligible();
        }
        SharedContract contract = contracts.findByIdentifierAndOwnerWorkspaceIdentifierAndOwnerApplicationIdentifier(
                contractIdentifier, ownerWorkspace, ownerApplication).filter(SharedContract::isActive).orElse(null);
        if (contract == null) return ineligible();
        var participation = participations.findByContractIdAndParticipantApplicationIdentifier(contract.getId(), participantApplication)
                .filter(p -> p.getParticipantWorkspaceIdentifier().equals(participantWorkspace))
                .filter(p -> ShareStatusTypeEnum.APPROVED.name().equals(p.getStatus().value()))
                .orElse(null);
        if (participation == null || sourceEnvironmentIdentifier == null || sourceEnvironmentIdentifier.isBlank()) return ineligible();
        EnvironmentOutput source = activeEnvironment(participantWorkspace, sourceEnvironmentIdentifier);
        if (source == null) return ineligible();
        List<String> destinations = participation.getMappings().stream()
                .filter(mapping -> mapping.getSourceEnvironmentIdentifier().equals(sourceEnvironmentIdentifier))
                .map(mapping -> activeEnvironment(ownerWorkspace, mapping.getDestinationEnvironmentIdentifier()))
                .filter(Objects::nonNull)
                .filter(destination -> Objects.equals(base(source), base(destination)))
                .map(EnvironmentOutput::identifier).distinct().toList();
        String mode = participation.getPublicationMode() == null ? null : participation.getPublicationMode().value();
        if (destinations.isEmpty() || mode == null) return ineligible();
        return new SharedEligibilityResult(true, mode, destinations);
    }

    private EnvironmentOutput activeEnvironment(String workspace, String identifier) {
        try { return environments.findActiveForShared(workspace, identifier); }
        catch (NotFoundException ex) {
            try { return environments.findDefault(identifier); }
            catch (NotFoundException ignored) { return null; }
        }
    }
    private String base(EnvironmentOutput e) {
        return e.workspaceIdentifier() == null || e.parentIdentifier() == null ? e.identifier() : e.parentIdentifier();
    }
    private SharedEligibilityResult ineligible() { return new SharedEligibilityResult(false, null, List.of()); }
}
