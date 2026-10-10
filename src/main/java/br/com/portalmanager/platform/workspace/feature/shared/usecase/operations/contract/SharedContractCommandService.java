package br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.contract;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationScopeReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.feature.shared.domain.SharedMessageKeys;
import br.com.portalmanager.platform.workspace.feature.shared.domain.contract.SharedContract;
import br.com.portalmanager.platform.workspace.feature.shared.integration.SharedReferenceResolver;
import br.com.portalmanager.platform.workspace.feature.shared.repository.SharedContractRepository;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract.SharedContractInput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract.SharedContractOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.validation.SharedValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SharedContractCommandService {

    private final SharedReferenceResolver referenceResolver;
    private final SharedContractRepository contractRepository;
    private final ApplicationQueryService applicationQueryService;
    private final SharedValidator sharedValidator;
    private final SharedContractAccessService contractAccessService;

    public SharedContractCommandService(
        SharedReferenceResolver referenceResolver,
        SharedContractRepository contractRepository,
        ApplicationQueryService applicationQueryService,
        SharedValidator sharedValidator,
        SharedContractAccessService contractAccessService
    ) {
        this.referenceResolver = referenceResolver;
        this.contractRepository = contractRepository;
        this.applicationQueryService = applicationQueryService;
        this.sharedValidator = sharedValidator;
        this.contractAccessService = contractAccessService;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "SHARED_CONTRACT_CREATED", resourceType = "SHARED_CONTRACT")
    public SharedContractOutput createContract(
        String workspaceIdentifier,
        String applicationIdentifier,
        SharedContractInput input
    ) {
        ApplicationScopeReferenceOutput ownerApplication = contractAccessService.requiredSharedOwnerApplication(
            workspaceIdentifier,
            applicationIdentifier
        );
        sharedValidator.validateContract(input);
        Long featureId = referenceResolver.shareableFeatureId(input.featureIdentifier());
        if (
            contractRepository
                .findContractIdByOwnerAndFeatureForUpdate(
                    ownerApplication.workspaceId(),
                    ownerApplication.applicationId(),
                    featureId
                )
                .isPresent()
        ) throw new ValidationException(SharedMessageKeys.DUPLICATE);
        SharedContract contract = contractRepository.save(
            new SharedContract(
                ownerApplication.workspaceId(),
                ownerApplication.applicationId(),
                featureId,
                normalizeDescription(input.description()),
                LocalDateTime.now()
            )
        );
        return referenceResolver.contractOutput(contract, workspaceIdentifier, applicationIdentifier);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "SHARED_CONTRACT_UPDATED", resourceType = "SHARED_CONTRACT")
    public SharedContractOutput updateContract(
        String workspaceIdentifier,
        String applicationIdentifier,
        String contractIdentifier,
        SharedContractInput input
    ) {
        ApplicationScopeReferenceOutput ownerApplication = contractAccessService.requiredSharedOwnerApplication(
            workspaceIdentifier,
            applicationIdentifier
        );
        SharedContract contract = contractAccessService.findOwnedContract(ownerApplication, contractIdentifier);
        contractAccessService.requireActiveContract(contract);
        sharedValidator.validateContract(input);
        if (!referenceResolver.featureIdentifier(contract.getFeatureId()).equals(input.featureIdentifier())) {
            throw new ValidationException(SharedMessageKeys.FEATURE_IMMUTABLE);
        }

        referenceResolver.shareableFeatureId(input.featureIdentifier());
        // Keep write lock order consistent: application, feature, then contract.
        contract = contractAccessService.requiredContract(ownerApplication, contractIdentifier);
        contract.update(normalizeDescription(input.description()), LocalDateTime.now());
        return referenceResolver.contractOutput(contract, workspaceIdentifier, applicationIdentifier);
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "SHARED_CONTRACT_INACTIVATED", resourceType = "SHARED_CONTRACT")
    public SharedContractOutput inactivateContract(
        String workspaceIdentifier,
        String applicationIdentifier,
        String contractIdentifier
    ) {
        SharedContract contract = contractAccessService.requiredActiveContract(
            workspaceIdentifier,
            applicationIdentifier,
            contractIdentifier
        );
        contract.inactivate(LocalDateTime.now());
        return referenceResolver.contractOutput(contract, workspaceIdentifier, applicationIdentifier);
    }

    @Transactional
    @Auditable(action = AuditAction.ACTIVATE, event = "SHARED_CONTRACT_ACTIVATED", resourceType = "SHARED_CONTRACT")
    public SharedContractOutput activateContract(
        String workspaceIdentifier,
        String applicationIdentifier,
        String contractIdentifier
    ) {
        ApplicationScopeReferenceOutput ownerApplication = contractAccessService.requiredSharedOwnerApplication(
            workspaceIdentifier,
            applicationIdentifier
        );
        SharedContract contract = contractAccessService.requiredContract(ownerApplication, contractIdentifier);
        if (!LifecycleTypeCode.inactive().equals(contract.getLifecycle())) throw new ValidationException(
            SharedMessageKeys.LIFECYCLE_INVALID
        );
        contractAccessService.requireAvailableFeature(contract);
        contract.activate(LocalDateTime.now());
        return referenceResolver.contractOutput(contract, workspaceIdentifier, applicationIdentifier);
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE, event = "SHARED_CONTRACT_DELETED", resourceType = "SHARED_CONTRACT")
    public SharedContractOutput deleteContract(
        String workspaceIdentifier,
        String applicationIdentifier,
        String contractIdentifier
    ) {
        ApplicationScopeReferenceOutput ownerApplication = applicationQueryService.findActiveScopeForUpdate(
            workspaceIdentifier,
            applicationIdentifier
        );
        SharedContract contract = contractAccessService.requiredContract(ownerApplication, contractIdentifier);
        if (!LifecycleTypeCode.inactive().equals(contract.getLifecycle())) throw new ValidationException(
            SharedMessageKeys.DELETE_REQUIRES_INACTIVE
        );
        SharedContractOutput deletedContract = referenceResolver.contractOutput(
            contract,
            workspaceIdentifier,
            applicationIdentifier
        );
        contractRepository.delete(contract); // FK cascades participationRepository and their mappings.
        return deletedContract;
    }

    private String normalizeDescription(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
