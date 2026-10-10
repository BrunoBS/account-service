package br.com.portalmanager.platform.workspace.feature.shared.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.shared.domain.SharedMessageKeys;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract.SharedContractInput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.mapping.EnvironmentMappingInput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.pagination.SharedPageInput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.ParticipationAction;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.ParticipationConfigurationInput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.ParticipationStatusInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;
import java.util.HashSet;
import org.springframework.stereotype.Component;

@Component
public class SharedValidator {

    public void validateContractOwnerApplication(String applicationScope) {
        if (!ApplicationScopeTypeEnum.SHARED.name().equals(applicationScope)) {
            throw new ValidationException(SharedMessageKeys.OWNER_APPLICATION_TYPE_INVALID);
        }
    }

    public ParticipationAction participationAction(ParticipationStatusInput input) {
        if (input == null || input.action() == null) throw new ValidationException(SharedMessageKeys.REQUEST_INVALID);
        try {
            return ParticipationAction.valueOf(input.action());
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(SharedMessageKeys.REQUEST_INVALID);
        }
    }

    public SharedPageInput parsePage(String page, String size) {
        try {
            SharedPageInput pagination = new SharedPageInput(Integer.parseInt(page), Integer.parseInt(size));
            validatePage(pagination.page(), pagination.size());
            return pagination;
        } catch (NumberFormatException exception) {
            throw new ValidationException(SharedMessageKeys.REQUEST_INVALID);
        }
    }

    public void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new ValidationException(SharedMessageKeys.REQUEST_INVALID);
    }

    public void validateParticipationFilters(String status, String contractIdentifier) {
        if (status != null) {
            try {
                ShareStatusTypeEnum.valueOf(status);
            } catch (IllegalArgumentException exception) {
                throw new ValidationException(SharedMessageKeys.REQUEST_INVALID);
            }
        }
        if (contractIdentifier != null && contractIdentifier.isBlank()) throw new ValidationException(
            SharedMessageKeys.REQUEST_INVALID
        );
    }

    public void validateOwnerParticipationFilters(String status, String participantApplicationIdentifier) {
        validateParticipationFilters(status, null);
        if (participantApplicationIdentifier != null && participantApplicationIdentifier.isBlank()) {
            throw new ValidationException(SharedMessageKeys.REQUEST_INVALID);
        }
    }

    public void validateContractOwnerFilters(String ownerWorkspaceIdentifier, String ownerApplicationIdentifier) {
        if (
            (ownerWorkspaceIdentifier != null && ownerWorkspaceIdentifier.isBlank()) ||
            (ownerApplicationIdentifier != null && ownerApplicationIdentifier.isBlank())
        ) {
            throw new ValidationException(SharedMessageKeys.SCOPE_INVALID);
        }
    }

    public void validateConfiguration(ParticipationConfigurationInput input) {
        if (input == null || input.publicationModeCode() == null) throw new ValidationException(
            SharedMessageKeys.PUBLICATION_MODE_INVALID
        );
        try {
            PublicationModeTypeEnum.valueOf(input.publicationModeCode());
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(SharedMessageKeys.PUBLICATION_MODE_INVALID);
        }
        if (
            input.environmentMappings() == null ||
            input.environmentMappings().mappings() == null ||
            input.environmentMappings().mappings().isEmpty()
        ) throw new ValidationException(SharedMessageKeys.MAPPING_INVALID);
        validateEnvironmentMappings(input.environmentMappings());
    }

    public void validateEnvironmentMappings(EnvironmentMappingInput input) {
        HashSet<String> destinationIdentifiers = new HashSet<String>();
        for (EnvironmentMappingInput.Mapping mapping : input.mappings()) {
            if (
                mapping == null ||
                mapping.destinationEnvironmentIdentifiers() == null ||
                mapping.destinationEnvironmentIdentifiers().isEmpty()
            ) {
                throw new ValidationException(SharedMessageKeys.MAPPING_INVALID);
            }
            validateSourceIdentifier(mapping.sourceEnvironmentIdentifier());
            for (String destinationIdentifier : mapping.destinationEnvironmentIdentifiers()) {
                validateMappingNames(
                    mapping.sourceEnvironmentIdentifier(),
                    destinationIdentifier,
                    !destinationIdentifiers.add(destinationIdentifier)
                );
            }
        }
    }

    public void validateContract(SharedContractInput input) {
        if (input != null && (input.featureIdentifier() == null || input.featureIdentifier().isBlank())) {
            throw new ValidationException(SharedMessageKeys.FEATURE_REQUIRED);
        }
        if (
            input == null || (input.description() != null && input.description().length() > 500)
        ) throw new ValidationException(SharedMessageKeys.REQUEST_INVALID);
    }

    public void validateMappingNames(
        String sourceEnvironmentIdentifier,
        String destinationEnvironmentIdentifier,
        boolean duplicateDestination
    ) {
        if (
            sourceEnvironmentIdentifier == null ||
            sourceEnvironmentIdentifier.isBlank() ||
            destinationEnvironmentIdentifier == null ||
            destinationEnvironmentIdentifier.isBlank() ||
            duplicateDestination
        ) throw new ValidationException(SharedMessageKeys.MAPPING_INVALID);
    }

    public void validateSourceIdentifier(String sourceEnvironmentIdentifier) {
        if (sourceEnvironmentIdentifier == null || sourceEnvironmentIdentifier.isBlank()) throw new ValidationException(
            SharedMessageKeys.MAPPING_INVALID
        );
    }
}
