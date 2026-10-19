package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.mapping.message;

import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.MessageDetails;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.Message;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.Message;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.CreateMessageInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.MessageOutput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.UpdateMessageInput;

import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageDetails toDetails(CreateMessageInput input) {
        return new MessageDetails(
                input.messageKey(), input.code(), input.httpStatus(), input.observation());
    }

    public Message toDomain(Long microserviceId, CreateMessageInput input, java.time.LocalDateTime now) {
        return new Message(microserviceId, toDetails(input), now);
    }

    public MessageDetails toDetails(UpdateMessageInput input) {
        return new MessageDetails(
                input.messageKey(), input.code(), input.httpStatus(), input.observation());
    }

    public MessageOutput toOutput(Message message, String microserviceIdentifier) {
        return new MessageOutput(
                message.getIdentifier(), message.getVersion(), microserviceIdentifier,
                message.getMessageKey(), message.getCode(), message.getHttpStatus(),
                message.getLifecycle().value(), message.getObservation());
    }

}
