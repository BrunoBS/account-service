package br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.participation;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationScopeReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentQueryService;
import br.com.portalmanager.platform.workspace.feature.shared.domain.SharedMessageKeys;
import br.com.portalmanager.platform.workspace.feature.shared.domain.contract.SharedContract;
import br.com.portalmanager.platform.workspace.feature.shared.domain.participation.SharedParticipation;
import br.com.portalmanager.platform.workspace.feature.shared.integration.SharedReferenceResolver;
import br.com.portalmanager.platform.workspace.feature.shared.repository.SharedParticipationRepository;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.pagination.SharedPageInput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.SharedParticipationOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.SharedParticipationPageOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.contract.SharedContractAccessService;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.validation.SharedValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SharedParticipationQueryService {

    private final SharedValidator sharedValidator;
    private final SharedReferenceResolver referenceResolver;
    private final SharedParticipationRepository participationRepository;
    private final ApplicationQueryService applicationQueryService;
    private final EnvironmentQueryService environmentQueryService;
    private final SharedContractAccessService contractAccessService;

    public SharedParticipationQueryService(
        SharedValidator sharedValidator,
        SharedReferenceResolver referenceResolver,
        SharedParticipationRepository participationRepository,
        ApplicationQueryService applicationQueryService,
        EnvironmentQueryService environmentQueryService,
        SharedContractAccessService contractAccessService
    ) {
        this.sharedValidator = sharedValidator;
        this.referenceResolver = referenceResolver;
        this.participationRepository = participationRepository;
        this.applicationQueryService = applicationQueryService;
        this.environmentQueryService = environmentQueryService;
        this.contractAccessService = contractAccessService;
    }

    @Transactional(readOnly = true)
    public SharedParticipationPageOutput listOwnerParticipations(
        String workspaceIdentifier,
        String applicationIdentifier,
        String contractIdentifier,
        String participantName,
        String participantApplicationIdentifier,
        String status,
        String page,
        String size
    ) {
        SharedPageInput pagination = sharedValidator.parsePage(page, size);
        sharedValidator.validateOwnerParticipationFilters(status, participantApplicationIdentifier);
        SharedContract contract = contractAccessService.findOwnedContract(
            workspaceIdentifier,
            applicationIdentifier,
            contractIdentifier
        );
        String normalizedParticipantName =
            participantName == null || participantName.isBlank()
                ? null
                : participantName.trim().toLowerCase(Locale.ROOT);
        Page<Long> participationIds = participationRepository.findOwnerParticipationIds(
            contract.getId(),
            status,
            participantApplicationIdentifier,
            normalizedParticipantName,
            PageRequest.of(pagination.page(), pagination.size())
        );
        return participationPageOutput(participationIds);
    }

    @Transactional(readOnly = true)
    public SharedParticipationOutput findOwnerParticipation(
        String workspaceIdentifier,
        String applicationIdentifier,
        String contractIdentifier,
        String participationIdentifier
    ) {
        return referenceResolver.participationOutput(
            findOwnedParticipation(
                workspaceIdentifier,
                applicationIdentifier,
                contractIdentifier,
                participationIdentifier
            )
        );
    }

    @Transactional(readOnly = true)
    public SharedParticipationOutput findOwnerParticipationDetails(
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String contractIdentifier,
        String participationIdentifier
    ) {
        SharedParticipation participation = findOwnedParticipation(
            ownerWorkspaceIdentifier,
            ownerApplicationIdentifier,
            contractIdentifier,
            participationIdentifier
        );
        SharedParticipationOutput output = referenceResolver.participationOutput(participation);
        if (
            !LifecycleTypeCode.active().value().equals(participation.getContract().getLifecycle().value()) ||
            applicationQueryService
                .findActiveReferencesForShared(List.of(participation.getParticipantApplicationId()))
                .isEmpty()
        ) {
            return output.withSourceEnvironments(List.of());
        }
        return output.withSourceEnvironments(
            environmentQueryService.listActiveForShared(output.participantWorkspaceIdentifier())
        );
    }

    private SharedParticipation findOwnedParticipation(
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String contractIdentifier,
        String participationIdentifier
    ) {
        SharedContract contract = contractAccessService.findOwnedContract(
            ownerWorkspaceIdentifier,
            ownerApplicationIdentifier,
            contractIdentifier
        );
        return participationRepository
            .findByContractIdAndIdentifier(contract.getId(), participationIdentifier)
            .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public SharedParticipationPageOutput listParticipantParticipations(
        String workspaceIdentifier,
        String applicationIdentifier,
        String status,
        String contractIdentifier,
        String page,
        String size
    ) {
        SharedPageInput pagination = sharedValidator.parsePage(page, size);
        sharedValidator.validateParticipationFilters(status, contractIdentifier);
        ApplicationScopeReferenceOutput applicationReference = applicationQueryService.findActiveScope(
            workspaceIdentifier,
            applicationIdentifier
        );
        Page<Long> participationIds = participationRepository.findParticipantParticipationIds(
            applicationReference.workspaceId(),
            applicationReference.applicationId(),
            status,
            contractIdentifier,
            PageRequest.of(pagination.page(), pagination.size())
        );
        return participationPageOutput(participationIds);
    }

    private SharedParticipationPageOutput participationPageOutput(Page<Long> participationIds) {
        if (participationIds.isEmpty()) {
            return new SharedParticipationPageOutput(
                List.of(),
                participationIds.getNumber(),
                participationIds.getSize(),
                participationIds.getTotalElements(),
                participationIds.getTotalPages()
            );
        }
        Map<Long, SharedParticipation> participationsById = participationRepository
            .findByIdIn(participationIds.getContent())
            .stream()
            .collect(Collectors.toMap(SharedParticipation::getId, participation -> participation));
        List<SharedParticipation> orderedParticipations = participationIds
            .getContent()
            .stream()
            .map(participationsById::get)
            .toList();
        return new SharedParticipationPageOutput(
            referenceResolver.participationOutputs(orderedParticipations),
            participationIds.getNumber(),
            participationIds.getSize(),
            participationIds.getTotalElements(),
            participationIds.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public SharedParticipationOutput findParticipantParticipation(
        String workspaceIdentifier,
        String applicationIdentifier,
        String participationIdentifier
    ) {
        ApplicationScopeReferenceOutput applicationReference = applicationQueryService.findActiveScope(
            workspaceIdentifier,
            applicationIdentifier
        );
        SharedParticipation participation = participationRepository
            .findByIdentifierAndParticipantWorkspaceIdAndParticipantApplicationId(
                participationIdentifier,
                applicationReference.workspaceId(),
                applicationReference.applicationId()
            )
            .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
        return referenceResolver.participationOutput(participation);
    }
}
