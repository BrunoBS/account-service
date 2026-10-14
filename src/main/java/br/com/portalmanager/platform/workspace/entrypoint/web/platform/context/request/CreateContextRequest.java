package br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.CreateContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context.ContextNormalizer;

@ValidateResourceSchemaRequest(type = "PLATFORM", code = "context", normalizer = ContextNormalizer.class)
public record CreateContextRequest(Long version, String code, String name, String description) {
    public CreateContextInput toInput() {
        return new CreateContextInput(version, code, name, description);
    }
}
