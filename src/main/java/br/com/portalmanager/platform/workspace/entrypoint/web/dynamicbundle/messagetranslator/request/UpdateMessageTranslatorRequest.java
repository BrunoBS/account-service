package br.com.portalmanager.platform.workspace.entrypoint.web.dynamicbundle.messagetranslator.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.UpdateMessageTranslatorInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message.MessageNormalizer;

@ValidateResourceSchemaRequest(
        type = "MESSAGE",
        code = "message-translator",
        normalizer = MessageNormalizer.class)
public record UpdateMessageTranslatorRequest(
        Long version, String locale, String title, String detail, String suggestion) {
    public UpdateMessageTranslatorInput toInput() {
        return new UpdateMessageTranslatorInput(version, locale, title, detail, suggestion);
    }
}
