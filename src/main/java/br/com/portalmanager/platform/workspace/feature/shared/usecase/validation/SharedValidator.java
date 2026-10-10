package br.com.portalmanager.platform.workspace.feature.shared.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.SharedContractInput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.SharedMessageKeys;
import org.springframework.stereotype.Component;

@Component
public class SharedValidator {

    public void validateContract(SharedContractInput input) {
        if (
            input == null ||
            input.name() == null ||
            input.name().isBlank() ||
            input.name().trim().length() > 120 ||
            (input.description() != null && input.description().length() > 500)
        ) throw new ValidationException(SharedMessageKeys.REQUEST_INVALID);
    }

    public void validateMappingNames(String source, String destination, boolean duplicateDestination) {
        if (
            source == null || source.isBlank() || destination == null || destination.isBlank() || duplicateDestination
        ) throw new ValidationException(SharedMessageKeys.MAPPING_INVALID);
    }

    public void validateSourceIdentifier(String source) {
        if (source == null || source.isBlank()) throw new ValidationException(SharedMessageKeys.MAPPING_INVALID);
    }
}
