package br.com.portalmanager.platform.workspace.feature.message.usecase.operations;

import br.com.portalmanager.platform.workspace.feature.message.domain.MessageDetails;
import br.com.portalmanager.platform.workspace.feature.message.domain.MessageTranslationDetails;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageTranslationInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.UpdateMessageInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.UpdateMessageTranslationInput;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageDetails toDetails(CreateMessageInput input) {
        return new MessageDetails(input.messageKey(), input.code(), input.httpStatus(), input.observation());
    }

    public MessageDetails toDetails(UpdateMessageInput input) {
        return new MessageDetails(input.messageKey(), input.code(), input.httpStatus(), input.observation());
    }

    public MessageTranslationDetails toDetails(CreateMessageTranslationInput input) {
        return new MessageTranslationDetails(input.locale(), input.title(), input.detail(), input.suggestion());
    }

    public MessageTranslationDetails toDetails(UpdateMessageTranslationInput input) {
        return new MessageTranslationDetails(input.locale(), input.title(), input.detail(), input.suggestion());
    }
}
