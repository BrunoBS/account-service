package br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.operations.contract;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationScopeReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.ResourceSharingMessageKeys;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.contract.Contract;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.integration.ResourceSharingReferenceAdapter;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.repository.contract.ContractRepository;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.validation.ResourceSharingValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ContractAccessService {

    private final ResourceSharingReferenceAdapter referenceResolver;
    private final ContractRepository contractRepository;
    private final ApplicationQueryService applicationQueryService;
    private final ResourceSharingValidator resourceSharingValidator;

    public ContractAccessService(
            ResourceSharingReferenceAdapter referenceResolver,
            ContractRepository contractRepository,
            ApplicationQueryService applicationQueryService,
            ResourceSharingValidator resourceSharingValidator) {
        this.referenceResolver = referenceResolver;
        this.contractRepository = contractRepository;
        this.applicationQueryService = applicationQueryService;
        this.resourceSharingValidator = resourceSharingValidator;
    }

    public ApplicationScopeReferenceOutput requiredSharedOwnerApplication(
            String workspaceIdentifier, String applicationIdentifier) {
        ApplicationScopeReferenceOutput ownerApplication =
                applicationQueryService.findActiveScopeForUpdate(
                        workspaceIdentifier, applicationIdentifier);
        resourceSharingValidator.validateContractOwnerApplication(
                ownerApplication.applicationScope());
        return ownerApplication;
    }

    public Contract requiredActiveContract(
            String workspaceIdentifier, String applicationIdentifier, String contractIdentifier) {
        ApplicationScopeReferenceOutput ownerApplication =
                requiredSharedOwnerApplication(workspaceIdentifier, applicationIdentifier);
        Contract contract = requiredContract(ownerApplication, contractIdentifier);
        requireActiveContract(contract);
        return contract;
    }

    public void requireActiveContract(Contract contract) {
        if (!contract.isActive()) throw new NotFoundException(ResourceSharingMessageKeys.NOT_FOUND);
    }

    public Contract requiredContract(
            ApplicationScopeReferenceOutput ownerApplication, String contractIdentifier) {
        return contractRepository
                .findOwnedContractForUpdate(
                        contractIdentifier,
                        ownerApplication.workspaceId(),
                        ownerApplication.applicationId())
                .orElseThrow(() -> new NotFoundException(ResourceSharingMessageKeys.NOT_FOUND));
    }

    public void requireAvailableFeature(Contract contract) {
        if (!referenceResolver.isFeatureAvailable(contract))
            throw new NotFoundException(ResourceSharingMessageKeys.NOT_FOUND);
    }

    public void requireFeatureForDetails(Contract contract) {
        if (!referenceResolver.isFeatureAvailable(contract)) {
            throw new ValidationException(ResourceSharingMessageKeys.FEATURE_UNAVAILABLE);
        }
    }

    public void requireActiveContractOwner(Contract contract) {
        ApplicationScopeReferenceOutput ownerApplication =
                applicationQueryService.findActiveScopeForShared(
                        contract.getOwnerWorkspaceId(), contract.getOwnerApplicationId());
        resourceSharingValidator.validateContractOwnerApplication(
                ownerApplication.applicationScope());
    }

    public Contract findOwnedContract(
            String workspaceIdentifier, String applicationIdentifier, String contractIdentifier) {
        return findOwnedContract(
                applicationQueryService.findActiveScope(workspaceIdentifier, applicationIdentifier),
                contractIdentifier);
    }

    public Contract findOwnedContract(
            ApplicationScopeReferenceOutput ownerApplication, String contractIdentifier) {
        return contractRepository
                .findByIdentifierAndOwnerWorkspaceIdAndOwnerApplicationId(
                        contractIdentifier,
                        ownerApplication.workspaceId(),
                        ownerApplication.applicationId())
                .orElseThrow(() -> new NotFoundException(ResourceSharingMessageKeys.NOT_FOUND));
    }

    public Contract requiredAvailableContract(String contractIdentifier) {
        Contract contract =
                contractRepository
                        .findByIdentifierAndLifecycleValue(
                                contractIdentifier, LifecycleTypeCode.active().value())
                        .orElseThrow(
                                () -> new NotFoundException(ResourceSharingMessageKeys.NOT_FOUND));
        requireAvailableContract(contract);
        return contract;
    }

    public void requireAvailableContract(Contract contract) {
        Optional<String> currentLifecycle =
                contractRepository.findLifecycleForShare(contract.getId());
        if (currentLifecycle.filter(LifecycleTypeCode.active().value()::equals).isEmpty()) {
            throw new NotFoundException(ResourceSharingMessageKeys.NOT_FOUND);
        }
        requireAvailableFeature(contract);
        requireActiveContractOwner(contract);
    }
}
