package br.com.portalmanager.platform.workspace.entrypoint.web.dynamicbundle.messagetranslator.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.CreateMessageTranslatorInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message.MessageNormalizer;

@ValidateResourceSchemaRequest(
        type = "MESSAGE",
        code = "message-translator",
        normalizer = MessageNormalizer.class)
public record CreateMessageTranslatorRequest(
        Long version, String locale, String title, String detail, String suggestion) {
    public CreateMessageTranslatorInput toInput() {
        return new CreateMessageTranslatorInput(version, locale, title, detail, suggestion);
    }
}
