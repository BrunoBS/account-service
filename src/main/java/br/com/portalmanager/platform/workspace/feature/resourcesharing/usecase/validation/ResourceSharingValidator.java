package br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.ResourceSharingMessageKeys;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.contract.ContractInput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.pagination.ResourceSharingPageInput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant.EnvironmentMappingInput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant.ParticipantAction;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant.ParticipantConfigurationInput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant.ParticipantStatusInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;

import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class ResourceSharingValidator {

    public void validateContractOwnerApplication(String applicationScope) {
        if (!ApplicationScopeTypeEnum.SHARED.name().equals(applicationScope)) {
            throw new ValidationException(
                    ResourceSharingMessageKeys.OWNER_APPLICATION_TYPE_INVALID);
        }
    }

    public ParticipantAction participantAction(ParticipantStatusInput input) {
        if (input == null || input.action() == null)
            throw new ValidationException(ResourceSharingMessageKeys.REQUEST_INVALID);
        try {
            return ParticipantAction.valueOf(input.action());
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(ResourceSharingMessageKeys.REQUEST_INVALID);
        }
    }

    public ResourceSharingPageInput parsePage(String page, String size) {
        try {
            ResourceSharingPageInput pagination =
                    new ResourceSharingPageInput(Integer.parseInt(page), Integer.parseInt(size));
            validatePage(pagination.page(), pagination.size());
            return pagination;
        } catch (NumberFormatException exception) {
            throw new ValidationException(ResourceSharingMessageKeys.REQUEST_INVALID);
        }
    }

    public void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new ValidationException(ResourceSharingMessageKeys.REQUEST_INVALID);
    }

    public void validateParticipantFilters(String status, String contractIdentifier) {
        if (status != null) {
            try {
                ShareStatusTypeEnum.valueOf(status);
            } catch (IllegalArgumentException exception) {
                throw new ValidationException(ResourceSharingMessageKeys.REQUEST_INVALID);
            }
        }
        if (contractIdentifier != null && contractIdentifier.isBlank())
            throw new ValidationException(ResourceSharingMessageKeys.REQUEST_INVALID);
    }

    public void validateOwnerParticipantFilters(
            String status, String participantApplicationIdentifier) {
        validateParticipantFilters(status, null);
        if (participantApplicationIdentifier != null
                && participantApplicationIdentifier.isBlank()) {
            throw new ValidationException(ResourceSharingMessageKeys.REQUEST_INVALID);
        }
    }

    public void validateContractOwnerFilters(
            String ownerWorkspaceIdentifier, String ownerApplicationIdentifier) {
        if ((ownerWorkspaceIdentifier != null && ownerWorkspaceIdentifier.isBlank())
                || (ownerApplicationIdentifier != null && ownerApplicationIdentifier.isBlank())) {
            throw new ValidationException(ResourceSharingMessageKeys.SCOPE_INVALID);
        }
    }

    public void validateConfiguration(ParticipantConfigurationInput input) {
        if (input == null || input.publicationModeCode() == null)
            throw new ValidationException(ResourceSharingMessageKeys.PUBLICATION_MODE_INVALID);
        try {
            PublicationModeTypeEnum.valueOf(input.publicationModeCode());
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(ResourceSharingMessageKeys.PUBLICATION_MODE_INVALID);
        }
        if (input.environmentMappings() == null
                || input.environmentMappings().mappings() == null
                || input.environmentMappings().mappings().isEmpty())
            throw new ValidationException(ResourceSharingMessageKeys.MAPPING_INVALID);
        validateEnvironmentMappings(input.environmentMappings());
    }

    public void validateEnvironmentMappings(EnvironmentMappingInput input) {
        HashSet<String> destinationIdentifiers = new HashSet<String>();
        for (EnvironmentMappingInput.Mapping mapping : input.mappings()) {
            if (mapping == null
                    || mapping.destinationEnvironmentIdentifiers() == null
                    || mapping.destinationEnvironmentIdentifiers().isEmpty()) {
                throw new ValidationException(ResourceSharingMessageKeys.MAPPING_INVALID);
            }
            validateSourceIdentifier(mapping.sourceEnvironmentIdentifier());
            for (String destinationIdentifier : mapping.destinationEnvironmentIdentifiers()) {
                validateMappingNames(
                        mapping.sourceEnvironmentIdentifier(),
                        destinationIdentifier,
                        !destinationIdentifiers.add(destinationIdentifier));
            }
        }
    }

    public void validateContract(ContractInput input) {
        if (input != null
                && (input.featureIdentifier() == null || input.featureIdentifier().isBlank())) {
            throw new ValidationException(ResourceSharingMessageKeys.FEATURE_REQUIRED);
        }
        if (input == null || (input.description() != null && input.description().length() > 500))
            throw new ValidationException(ResourceSharingMessageKeys.REQUEST_INVALID);
    }

    public void validateMappingNames(
            String sourceEnvironmentIdentifier,
            String destinationEnvironmentIdentifier,
            boolean duplicateDestination) {
        if (sourceEnvironmentIdentifier == null
                || sourceEnvironmentIdentifier.isBlank()
                || destinationEnvironmentIdentifier == null
                || destinationEnvironmentIdentifier.isBlank()
                || duplicateDestination)
            throw new ValidationException(ResourceSharingMessageKeys.MAPPING_INVALID);
    }

    public void validateSourceIdentifier(String sourceEnvironmentIdentifier) {
        if (sourceEnvironmentIdentifier == null || sourceEnvironmentIdentifier.isBlank())
            throw new ValidationException(ResourceSharingMessageKeys.MAPPING_INVALID);
    }
}
