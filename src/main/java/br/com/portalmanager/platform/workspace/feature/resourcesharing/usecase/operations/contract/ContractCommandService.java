package br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.operations.contract;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationScopeReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.ResourceSharingMessageKeys;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.contract.Contract;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.integration.ResourceSharingReferenceAdapter;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.repository.contract.ContractRepository;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.contract.ContractInput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.contract.ContractOutput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.validation.ResourceSharingValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ContractCommandService {

    private final ResourceSharingReferenceAdapter referenceResolver;
    private final ContractRepository contractRepository;
    private final ApplicationQueryService applicationQueryService;
    private final ResourceSharingValidator resourceSharingValidator;
    private final ContractAccessService contractAccessService;

    public ContractCommandService(
            ResourceSharingReferenceAdapter referenceResolver,
            ContractRepository contractRepository,
            ApplicationQueryService applicationQueryService,
            ResourceSharingValidator resourceSharingValidator,
            ContractAccessService contractAccessService) {
        this.referenceResolver = referenceResolver;
        this.contractRepository = contractRepository;
        this.applicationQueryService = applicationQueryService;
        this.resourceSharingValidator = resourceSharingValidator;
        this.contractAccessService = contractAccessService;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "CONTRACT_CREATED", resourceType = "CONTRACT")
    public ContractOutput createContract(
            String workspaceIdentifier, String applicationIdentifier, ContractInput input) {
        ApplicationScopeReferenceOutput ownerApplication =
                contractAccessService.requiredSharedOwnerApplication(
                        workspaceIdentifier, applicationIdentifier);
        resourceSharingValidator.validateContract(input);
        Long featureId = referenceResolver.shareableFeatureId(input.featureIdentifier());
        Optional<Long> contractIdByOwnerAndFeatureForUpdate =
                contractRepository.findContractIdByOwnerAndFeatureForUpdate(
                        ownerApplication.workspaceId(),
                        ownerApplication.applicationId(),
                        featureId);

        if (contractIdByOwnerAndFeatureForUpdate.isPresent()) {
            throw new ValidationException(ResourceSharingMessageKeys.DUPLICATE);
        }
        Contract contract =
                contractRepository.saveAndFlush(
                        new Contract(
                                ownerApplication.workspaceId(),
                                ownerApplication.applicationId(),
                                featureId,
                                normalizeDescription(input.description()),
                                LocalDateTime.now()));
        return referenceResolver.contractOutput(
                contract, workspaceIdentifier, applicationIdentifier);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "CONTRACT_UPDATED", resourceType = "CONTRACT")
    public ContractOutput updateContract(
            String workspaceIdentifier,
            String applicationIdentifier,
            String contractIdentifier,
            ContractInput input) {
        ApplicationScopeReferenceOutput ownerApplication =
                contractAccessService.requiredSharedOwnerApplication(
                        workspaceIdentifier, applicationIdentifier);
        Contract contract =
                contractAccessService.findOwnedContract(ownerApplication, contractIdentifier);
        contractAccessService.requireActiveContract(contract);
        resourceSharingValidator.validateContract(input);
        if (!referenceResolver
                .featureIdentifier(contract.getFeatureId())
                .equals(input.featureIdentifier())) {
            throw new ValidationException(ResourceSharingMessageKeys.FEATURE_IMMUTABLE);
        }

        referenceResolver.shareableFeatureId(input.featureIdentifier());
        contract = contractAccessService.requiredContract(ownerApplication, contractIdentifier);
        contract.update(normalizeDescription(input.description()), LocalDateTime.now());
        Contract savedContract = contractRepository.saveAndFlush(contract);
        return referenceResolver.contractOutput(
                savedContract, workspaceIdentifier, applicationIdentifier);
    }

    @Transactional
    @Auditable(
            action = AuditAction.DEACTIVATE,
            event = "CONTRACT_INACTIVATED",
            resourceType = "CONTRACT")
    public ContractOutput inactivateContract(
            String workspaceIdentifier, String applicationIdentifier, String contractIdentifier) {
        Contract contract =
                contractAccessService.requiredActiveContract(
                        workspaceIdentifier, applicationIdentifier, contractIdentifier);
        contract.inactivate(LocalDateTime.now());
        Contract savedContract = contractRepository.saveAndFlush(contract);
        return referenceResolver.contractOutput(
                savedContract, workspaceIdentifier, applicationIdentifier);
    }

    @Transactional
    @Auditable(
            action = AuditAction.ACTIVATE,
            event = "CONTRACT_ACTIVATED",
            resourceType = "CONTRACT")
    public ContractOutput activateContract(
            String workspaceIdentifier, String applicationIdentifier, String contractIdentifier) {
        ApplicationScopeReferenceOutput ownerApplication =
                contractAccessService.requiredSharedOwnerApplication(
                        workspaceIdentifier, applicationIdentifier);
        Contract contract =
                contractAccessService.requiredContract(ownerApplication, contractIdentifier);
        if (!LifecycleTypeCode.inactive().equals(contract.getLifecycle()))
            throw new ValidationException(ResourceSharingMessageKeys.LIFECYCLE_INVALID);
        contractAccessService.requireAvailableFeature(contract);
        contract.activate(LocalDateTime.now());
        Contract savedContract = contractRepository.saveAndFlush(contract);
        return referenceResolver.contractOutput(
                savedContract, workspaceIdentifier, applicationIdentifier);
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE, event = "CONTRACT_DELETED", resourceType = "CONTRACT")
    public ContractOutput deleteContract(
            String workspaceIdentifier, String applicationIdentifier, String contractIdentifier) {
        ApplicationScopeReferenceOutput ownerApplication =
                applicationQueryService.findActiveScopeForUpdate(
                        workspaceIdentifier, applicationIdentifier);
        Contract contract =
                contractAccessService.requiredContract(ownerApplication, contractIdentifier);
        if (!LifecycleTypeCode.inactive().equals(contract.getLifecycle()))
            throw new ValidationException(ResourceSharingMessageKeys.DELETE_REQUIRES_INACTIVE);
        ContractOutput deletedContract =
                referenceResolver.contractOutput(
                        contract, workspaceIdentifier, applicationIdentifier);
        contractRepository.delete(
                contract); // FK cascades participantRepository and their mappings.
        return deletedContract;
    }

    private String normalizeDescription(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
