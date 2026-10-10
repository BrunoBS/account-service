package br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.contract;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationScopeReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.feature.shared.domain.SharedMessageKeys;
import br.com.portalmanager.platform.workspace.feature.shared.domain.contract.SharedContract;
import br.com.portalmanager.platform.workspace.feature.shared.integration.SharedReferenceResolver;
import br.com.portalmanager.platform.workspace.feature.shared.repository.SharedContractRepository;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.validation.SharedValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SharedContractAccessService {

    private final SharedReferenceResolver referenceResolver;
    private final SharedContractRepository contractRepository;
    private final ApplicationQueryService applicationQueryService;
    private final SharedValidator sharedValidator;

    public SharedContractAccessService(
        SharedReferenceResolver referenceResolver,
        SharedContractRepository contractRepository,
        ApplicationQueryService applicationQueryService,
        SharedValidator sharedValidator
    ) {
        this.referenceResolver = referenceResolver;
        this.contractRepository = contractRepository;
        this.applicationQueryService = applicationQueryService;
        this.sharedValidator = sharedValidator;
    }

    public ApplicationScopeReferenceOutput requiredSharedOwnerApplication(
        String workspaceIdentifier,
        String applicationIdentifier
    ) {
        ApplicationScopeReferenceOutput ownerApplication = applicationQueryService.findActiveScopeForUpdate(
            workspaceIdentifier,
            applicationIdentifier
        );
        sharedValidator.validateContractOwnerApplication(ownerApplication.applicationScope());
        return ownerApplication;
    }

    public SharedContract requiredActiveContract(
        String workspaceIdentifier,
        String applicationIdentifier,
        String contractIdentifier
    ) {
        ApplicationScopeReferenceOutput ownerApplication = requiredSharedOwnerApplication(
            workspaceIdentifier,
            applicationIdentifier
        );
        SharedContract contract = requiredContract(ownerApplication, contractIdentifier);
        requireActiveContract(contract);
        return contract;
    }

    public void requireActiveContract(SharedContract contract) {
        if (!contract.isActive()) throw new NotFoundException(SharedMessageKeys.NOT_FOUND);
    }

    public SharedContract requiredContract(
        ApplicationScopeReferenceOutput ownerApplication,
        String contractIdentifier
    ) {
        return contractRepository
            .findOwnedContractForUpdate(
                contractIdentifier,
                ownerApplication.workspaceId(),
                ownerApplication.applicationId()
            )
            .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
    }

    public void requireAvailableFeature(SharedContract contract) {
        if (!referenceResolver.isFeatureAvailable(contract)) throw new NotFoundException(SharedMessageKeys.NOT_FOUND);
    }

    public void requireActiveContractOwner(SharedContract contract) {
        ApplicationScopeReferenceOutput ownerApplication = applicationQueryService.findActiveScopeForShared(
            contract.getOwnerWorkspaceId(),
            contract.getOwnerApplicationId()
        );
        sharedValidator.validateContractOwnerApplication(ownerApplication.applicationScope());
    }

    public SharedContract findOwnedContract(
        String workspaceIdentifier,
        String applicationIdentifier,
        String contractIdentifier
    ) {
        return findOwnedContract(
            applicationQueryService.findActiveScope(workspaceIdentifier, applicationIdentifier),
            contractIdentifier
        );
    }

    public SharedContract findOwnedContract(
        ApplicationScopeReferenceOutput ownerApplication,
        String contractIdentifier
    ) {
        return contractRepository
            .findByIdentifierAndOwnerWorkspaceIdAndOwnerApplicationId(
                contractIdentifier,
                ownerApplication.workspaceId(),
                ownerApplication.applicationId()
            )
            .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
    }

    public SharedContract requiredAvailableContract(String contractIdentifier) {
        SharedContract contract = contractRepository
            .findByIdentifierAndLifecycleValue(contractIdentifier, LifecycleTypeCode.active().value())
            .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
        requireAvailableContract(contract);
        return contract;
    }

    public void requireAvailableContract(SharedContract contract) {
        Optional<String> currentLifecycle = contractRepository.findLifecycleForShare(contract.getId());
        if (currentLifecycle.filter(LifecycleTypeCode.active().value()::equals).isEmpty()) {
            throw new NotFoundException(SharedMessageKeys.NOT_FOUND);
        }
        requireAvailableFeature(contract);
        requireActiveContractOwner(contract);
    }
}
