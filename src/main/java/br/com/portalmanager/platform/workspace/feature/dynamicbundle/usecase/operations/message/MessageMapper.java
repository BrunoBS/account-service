package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message;

import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.MessageDetails;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.messagetranslator.MessageTranslatorDetails;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.CreateMessageInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.UpdateMessageInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.CreateMessageTranslatorInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.UpdateMessageTranslatorInput;

import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageDetails toDetails(CreateMessageInput input) {
        return new MessageDetails(
                input.messageKey(), input.code(), input.httpStatus(), input.observation());
    }

    public MessageDetails toDetails(UpdateMessageInput input) {
        return new MessageDetails(
                input.messageKey(), input.code(), input.httpStatus(), input.observation());
    }

    public MessageTranslatorDetails toDetails(CreateMessageTranslatorInput input) {
        return new MessageTranslatorDetails(
                input.locale(), input.title(), input.detail(), input.suggestion());
    }

    public MessageTranslatorDetails toDetails(UpdateMessageTranslatorInput input) {
        return new MessageTranslatorDetails(
                input.locale(), input.title(), input.detail(), input.suggestion());
    }
}
