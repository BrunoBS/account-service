package br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.contract;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationScopeReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.feature.shared.domain.SharedMessageKeys;
import br.com.portalmanager.platform.workspace.feature.shared.domain.contract.SharedContract;
import br.com.portalmanager.platform.workspace.feature.shared.integration.SharedReferenceResolver;
import br.com.portalmanager.platform.workspace.feature.shared.repository.SharedContractRepository;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract.SharedContractOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract.SharedContractPageOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.pagination.SharedPageInput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.validation.SharedValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SharedContractQueryService {

    private final SharedValidator sharedValidator;
    private final SharedReferenceResolver referenceResolver;
    private final SharedContractRepository contractRepository;
    private final ApplicationQueryService applicationQueryService;
    private final SharedContractAccessService contractAccessService;

    public SharedContractQueryService(
        SharedValidator sharedValidator,
        SharedReferenceResolver referenceResolver,
        SharedContractRepository contractRepository,
        ApplicationQueryService applicationQueryService,
        SharedContractAccessService contractAccessService
    ) {
        this.sharedValidator = sharedValidator;
        this.referenceResolver = referenceResolver;
        this.contractRepository = contractRepository;
        this.applicationQueryService = applicationQueryService;
        this.contractAccessService = contractAccessService;
    }

    @Transactional(readOnly = true)
    public SharedContractOutput findContract(
        String workspaceIdentifier,
        String applicationIdentifier,
        String identifier
    ) {
        return referenceResolver.contractOutput(
            contractAccessService.findOwnedContract(workspaceIdentifier, applicationIdentifier, identifier)
        );
    }

    @Transactional(readOnly = true)
    public SharedContractPageOutput listContracts(
        String workspaceIdentifier,
        String applicationIdentifier,
        String page,
        String size
    ) {
        SharedPageInput pagination = sharedValidator.parsePage(page, size);
        ApplicationScopeReferenceOutput applicationReference = applicationQueryService.findActiveScope(
            workspaceIdentifier,
            applicationIdentifier
        );
        Page<SharedContract> contractsPage = contractRepository.findByOwnerWorkspaceIdAndOwnerApplicationId(
            applicationReference.workspaceId(),
            applicationReference.applicationId(),
            PageRequest.of(pagination.page(), pagination.size(), Sort.by("id"))
        );
        return contractPageOutput(contractsPage);
    }

    @Transactional(readOnly = true)
    public SharedContractPageOutput listAvailableContracts(
        String participantWorkspaceIdentifier,
        String participantApplicationIdentifier,
        String page,
        String size,
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier
    ) {
        SharedPageInput pagination = sharedValidator.parsePage(page, size);
        sharedValidator.validateContractOwnerFilters(ownerWorkspaceIdentifier, ownerApplicationIdentifier);
        ApplicationScopeReferenceOutput participantApplication = applicationQueryService.findActiveScope(
            participantWorkspaceIdentifier,
            participantApplicationIdentifier
        );
        Page<SharedContract> contractsPage = contractRepository.findAvailableContracts(
            LifecycleTypeCode.active().value(),
            participantApplication.applicationId(),
            ownerWorkspaceIdentifier,
            ownerApplicationIdentifier,
            PageRequest.of(pagination.page(), pagination.size())
        );
        return contractPageOutput(contractsPage);
    }

    private SharedContractPageOutput contractPageOutput(Page<SharedContract> contractsPage) {
        return new SharedContractPageOutput(
            referenceResolver.contractOutputs(contractsPage.getContent()),
            contractsPage.getNumber(),
            contractsPage.getSize(),
            contractsPage.getTotalElements(),
            contractsPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public SharedContractOutput findAvailableContract(
        String participantWorkspaceIdentifier,
        String participantApplicationIdentifier,
        String contractIdentifier
    ) {
        ApplicationScopeReferenceOutput participantApplicationReference = applicationQueryService.findActiveScope(
            participantWorkspaceIdentifier,
            participantApplicationIdentifier
        );
        SharedContract contract = contractRepository
            .findByIdentifierAndLifecycleValue(contractIdentifier, LifecycleTypeCode.active().value())
            .filter(
                availableContract ->
                    !availableContract.getOwnerWorkspaceId().equals(participantApplicationReference.workspaceId()) ||
                    !availableContract.getOwnerApplicationId().equals(participantApplicationReference.applicationId())
            )
            .filter(this::hasActiveOwnerApplication)
            .filter(referenceResolver::isFeatureAvailable)
            .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
        return referenceResolver.contractOutput(contract);
    }

    private boolean hasActiveOwnerApplication(SharedContract contract) {
        return applicationQueryService
            .findActiveReferencesForShared(List.of(contract.getOwnerApplicationId()))
            .stream()
            .anyMatch(owner -> owner.workspaceId().equals(contract.getOwnerWorkspaceId()));
    }
}
