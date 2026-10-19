package br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.messagetranslator.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.UpdateMessageTranslatorInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.messagetranslator.MessageTranslatorNormalizer;

@ValidateResourceSchemaRequest(
        type = "MESSAGE",
        code = "message-translator",
        normalizer = MessageTranslatorNormalizer.class)
public record UpdateMessageTranslatorRequest(
        Long version, String locale, String title, String detail, String suggestion) {
    public UpdateMessageTranslatorInput toInput() {
        return new UpdateMessageTranslatorInput(version, locale, title, detail, suggestion);
    }
}
