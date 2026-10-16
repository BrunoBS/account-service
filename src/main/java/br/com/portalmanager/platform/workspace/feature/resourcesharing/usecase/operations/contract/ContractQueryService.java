package br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.operations.contract;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationScopeReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.ResourceSharingMessageKeys;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.contract.Contract;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.integration.ResourceSharingReferenceAdapter;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.repository.contract.ContractRepository;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.contract.ContractOutput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.contract.ContractPageOutput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.pagination.ResourceSharingPageInput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.validation.ResourceSharingValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleFilter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContractQueryService {

    private final ResourceSharingValidator resourceSharingValidator;
    private final ResourceSharingReferenceAdapter referenceResolver;
    private final ContractRepository contractRepository;
    private final ApplicationQueryService applicationQueryService;
    private final ContractAccessService contractAccessService;

    public ContractQueryService(
            ResourceSharingValidator resourceSharingValidator,
            ResourceSharingReferenceAdapter referenceResolver,
            ContractRepository contractRepository,
            ApplicationQueryService applicationQueryService,
            ContractAccessService contractAccessService) {
        this.resourceSharingValidator = resourceSharingValidator;
        this.referenceResolver = referenceResolver;
        this.contractRepository = contractRepository;
        this.applicationQueryService = applicationQueryService;
        this.contractAccessService = contractAccessService;
    }

    @Transactional(readOnly = true)
    public ContractOutput findContract(
            String workspaceIdentifier, String applicationIdentifier, String identifier) {
        Contract contract =
                contractAccessService.findOwnedContract(
                        workspaceIdentifier, applicationIdentifier, identifier);
        contractAccessService.requireFeatureForDetails(contract);
        return referenceResolver.contractOutput(contract);
    }

    @Transactional(readOnly = true)
    public ContractPageOutput listContracts(
            String workspaceIdentifier,
            String applicationIdentifier,
            String lifecycle,
            String page,
            String size) {
        ResourceSharingPageInput pagination = resourceSharingValidator.parsePage(page, size);
        ApplicationScopeReferenceOutput applicationReference =
                applicationQueryService.findActiveScope(workspaceIdentifier, applicationIdentifier);
        Page<Contract> contractsPage =
                contractRepository.findByOwnerWorkspaceIdAndOwnerApplicationIdAndLifecycleValue(
                        applicationReference.workspaceId(),
                        applicationReference.applicationId(),
                        LifecycleFilter.resolve(lifecycle).value(),
                        PageRequest.of(pagination.page(), pagination.size(), Sort.by("id")));
        return contractPageOutput(contractsPage);
    }

    @Transactional(readOnly = true)
    public ContractPageOutput listAvailableContracts(
            String participantWorkspaceIdentifier,
            String participantApplicationIdentifier,
            String page,
            String size,
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier) {
        ResourceSharingPageInput pagination = resourceSharingValidator.parsePage(page, size);
        resourceSharingValidator.validateContractOwnerFilters(
                ownerWorkspaceIdentifier, ownerApplicationIdentifier);
        ApplicationScopeReferenceOutput participantApplication =
                applicationQueryService.findActiveScope(
                        participantWorkspaceIdentifier, participantApplicationIdentifier);
        Page<Contract> contractsPage =
                contractRepository.findAvailableContracts(
                        LifecycleTypeCode.active().value(),
                        participantApplication.applicationId(),
                        ownerWorkspaceIdentifier,
                        ownerApplicationIdentifier,
                        PageRequest.of(pagination.page(), pagination.size()));
        return contractPageOutput(contractsPage);
    }

    private ContractPageOutput contractPageOutput(Page<Contract> contractsPage) {
        return new ContractPageOutput(
                referenceResolver.contractOutputs(contractsPage.getContent()),
                contractsPage.getNumber(),
                contractsPage.getSize(),
                contractsPage.getTotalElements(),
                contractsPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ContractOutput findAvailableContract(
            String participantWorkspaceIdentifier,
            String participantApplicationIdentifier,
            String contractIdentifier) {
        ApplicationScopeReferenceOutput participantApplicationReference =
                applicationQueryService.findActiveScope(
                        participantWorkspaceIdentifier, participantApplicationIdentifier);
        Contract contract =
                contractRepository
                        .findByIdentifierAndLifecycleValue(
                                contractIdentifier, LifecycleTypeCode.active().value())
                        .filter(
                                availableContract ->
                                        !availableContract
                                                        .getOwnerWorkspaceId()
                                                        .equals(
                                                                participantApplicationReference
                                                                        .workspaceId())
                                                || !availableContract
                                                        .getOwnerApplicationId()
                                                        .equals(
                                                                participantApplicationReference
                                                                        .applicationId()))
                        .filter(this::hasActiveOwnerApplication)
                        .orElseThrow(
                                () -> new NotFoundException(ResourceSharingMessageKeys.NOT_FOUND));
        contractAccessService.requireFeatureForDetails(contract);
        return referenceResolver.contractOutput(contract);
    }

    private boolean hasActiveOwnerApplication(Contract contract) {
        return applicationQueryService
                .findActiveReferencesForResourceSharing(List.of(contract.getOwnerApplicationId()))
                .stream()
                .anyMatch(owner -> owner.workspaceId().equals(contract.getOwnerWorkspaceId()));
    }
}
