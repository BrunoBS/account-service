package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.port;

import br.com.portalmanager.platform.library.messaging.api.validation.ValidationResult;

public interface SchemaValidationPort {
    void validate(String type, String code, String field, String payload, ValidationResult result);
}
