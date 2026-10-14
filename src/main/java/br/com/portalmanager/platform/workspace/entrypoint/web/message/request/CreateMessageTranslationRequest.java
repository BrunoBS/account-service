package br.com.portalmanager.platform.workspace.entrypoint.web.message.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageNormalizer;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageTranslationInput;

@ValidateResourceSchemaRequest(type = "MESSAGE", code = "message-translation", normalizer = MessageNormalizer.class)
public record CreateMessageTranslationRequest(Long version, String locale, String title, String detail, String suggestion) {
    public CreateMessageTranslationInput toInput() {
        return new CreateMessageTranslationInput(version, locale, title, detail, suggestion);
    }
}
