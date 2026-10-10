package br.com.portalmanager.platform.workspace.feature.shared.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentQueryService;
import br.com.portalmanager.platform.workspace.feature.shared.domain.SharedContract;
import br.com.portalmanager.platform.workspace.feature.shared.domain.SharedParticipation;
import br.com.portalmanager.platform.workspace.feature.shared.repository.SharedContractRepository;
import br.com.portalmanager.platform.workspace.feature.shared.repository.SharedParticipationRepository;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.SharedContractOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.SharedMessageKeys;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.SharedParticipationOutput;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SharedQueryService {
    private final SharedContractRepository contracts;
    private final SharedParticipationRepository participations;
    private final ApplicationQueryService applications;
    private final EnvironmentQueryService environments;
    public SharedQueryService(SharedContractRepository contracts, SharedParticipationRepository participations,
                              ApplicationQueryService applications, EnvironmentQueryService environments) {
        this.contracts = contracts; this.participations = participations;
        this.applications = applications; this.environments = environments;
    }

    @Transactional(readOnly = true)
    public SharedContractOutput findContract(String workspace, String application, String identifier) {
        applications.findByIdentifier(workspace, application);
        return SharedContractOutput.from(contracts.findByIdentifierAndOwnerWorkspaceIdentifierAndOwnerApplicationIdentifier(
                identifier, workspace, application)
                .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public List<SharedContractOutput> listContracts(String workspace, String application) {
        applications.findByIdentifier(workspace, application);
        return contracts.findByOwnerWorkspaceIdentifierAndOwnerApplicationIdentifierOrderById(workspace, application)
                .stream().map(SharedContractOutput::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SharedContractOutput> listAvailableContracts(String participantWorkspace, String participantApplication) {
        applications.findByIdentifier(participantWorkspace, participantApplication);
        return contracts.findByLifecycleValueOrderById(LifecycleTypeCode.active().value()).stream()
                .filter(c -> !c.getOwnerWorkspaceIdentifier().equals(participantWorkspace)
                        || !c.getOwnerApplicationIdentifier().equals(participantApplication))
                .filter(this::hasActiveOwnerApplication)
                .map(SharedContractOutput::from).toList();
    }

    @Transactional(readOnly = true)
    public SharedContractOutput findAvailableContract(String participantWorkspace, String participantApplication,
            String contractIdentifier) {
        applications.findByIdentifier(participantWorkspace, participantApplication);
        SharedContract contract = contracts.findByIdentifierAndLifecycleValue(contractIdentifier, LifecycleTypeCode.active().value())
                .filter(c -> !c.getOwnerWorkspaceIdentifier().equals(participantWorkspace)
                        || !c.getOwnerApplicationIdentifier().equals(participantApplication))
                .filter(this::hasActiveOwnerApplication)
                .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
        return SharedContractOutput.from(contract);
    }

    @Transactional(readOnly = true)
    public List<SharedParticipationOutput> listOwnerParticipations(String workspace, String application, String contractIdentifier,
            String participantName, String participantApplication, String status) {
        SharedContract contract = contracts.findByIdentifierAndOwnerWorkspaceIdentifierAndOwnerApplicationIdentifier(
                contractIdentifier, workspace, application).filter(SharedContract::isActive)
                .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
        applications.findByIdentifier(workspace, application);
        List<SharedParticipation> found = participations.findByContractAndStatuses(contract.getId(),
                List.of(ShareStatusTypeEnum.PENDING.name(), ShareStatusTypeEnum.APPROVED.name()));
        if (status != null && !status.equals(ShareStatusTypeEnum.PENDING.name())
                && !status.equals(ShareStatusTypeEnum.APPROVED.name()))
            throw new ValidationException(SharedMessageKeys.STATE_INVALID);
        String nameQuery = participantName == null ? null : participantName.trim().toLowerCase(java.util.Locale.ROOT);
        return found.stream()
                .filter(p -> participantApplication == null || participantApplication.equals(p.getParticipantApplicationIdentifier()))
                .filter(p -> status == null || status.equals(p.getStatus().value()))
                .filter(p -> nameQuery == null || nameQuery.isBlank() || participantApplicationName(p).toLowerCase(java.util.Locale.ROOT).contains(nameQuery))
                .map(SharedParticipationOutput::from).toList();
    }

    @Transactional(readOnly = true)
    public SharedParticipationOutput findOwnerParticipation(String workspace, String application, String contractIdentifier,
            String participationIdentifier) {
        SharedContract contract = contracts.findByIdentifierAndOwnerWorkspaceIdentifierAndOwnerApplicationIdentifier(
                contractIdentifier, workspace, application).filter(SharedContract::isActive)
                .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
        applications.findByIdentifier(workspace, application);
        SharedParticipation participation = participations.findByContractIdAndIdentifier(contract.getId(), participationIdentifier)
                .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
        if (!ShareStatusTypeEnum.PENDING.name().equals(participation.getStatus().value())
                && !ShareStatusTypeEnum.APPROVED.name().equals(participation.getStatus().value()))
            throw new NotFoundException(SharedMessageKeys.NOT_FOUND);
        return SharedParticipationOutput.from(participation);
    }

    @Transactional(readOnly = true)
    public List<EnvironmentOutput> listSourceEnvironments(String ownerWorkspace, String ownerApplication,
            String contractIdentifier, String participationIdentifier) {
        SharedContract contract = contracts.findByIdentifierAndOwnerWorkspaceIdentifierAndOwnerApplicationIdentifier(
                contractIdentifier, ownerWorkspace, ownerApplication).filter(SharedContract::isActive)
                .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
        applications.findByIdentifier(ownerWorkspace, ownerApplication);
        SharedParticipation participation = participations.findByContractIdAndIdentifier(contract.getId(), participationIdentifier)
                .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
        if (!ShareStatusTypeEnum.PENDING.name().equals(participation.getStatus().value())
                && !ShareStatusTypeEnum.APPROVED.name().equals(participation.getStatus().value()))
            throw new NotFoundException(SharedMessageKeys.NOT_FOUND);
        applications.findActiveForShared(participation.getParticipantWorkspaceIdentifier(),
                participation.getParticipantApplicationIdentifier());
        return environments.listActiveForShared(participation.getParticipantWorkspaceIdentifier());
    }

    private String participantApplicationName(SharedParticipation participation) {
        try { return applications.findActiveForShared(participation.getParticipantWorkspaceIdentifier(),
                participation.getParticipantApplicationIdentifier()).name(); }
        catch (NotFoundException ex) {
            try { return applications.findInactiveForShared(participation.getParticipantWorkspaceIdentifier(),
                    participation.getParticipantApplicationIdentifier()).name(); }
            catch (NotFoundException ignored) { return ""; }
        }
    }

    private boolean hasActiveOwnerApplication(SharedContract contract) {
        try {
            applications.findActiveForShared(contract.getOwnerWorkspaceIdentifier(),
                    contract.getOwnerApplicationIdentifier());
            return true;
        } catch (NotFoundException ex) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<SharedParticipationOutput> listParticipantParticipations(String workspace, String application) {
        applications.findByIdentifier(workspace, application);
        return participations.findByParticipantWorkspaceIdentifierAndParticipantApplicationIdentifierOrderByCreatedAtDesc(
                        workspace, application).stream().filter(p -> p.getContract().isActive())
                .map(SharedParticipationOutput::from).toList();
    }

    @Transactional(readOnly = true)
    public SharedParticipationOutput findParticipantParticipation(String workspace, String application, String participationIdentifier) {
        applications.findByIdentifier(workspace, application);
        SharedParticipation p = participations.findByIdentifierAndParticipantWorkspaceIdentifierAndParticipantApplicationIdentifier(
                        participationIdentifier, workspace, application)
                .filter(x -> x.getContract().isActive())
                .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
        return SharedParticipationOutput.from(p);
    }
}
