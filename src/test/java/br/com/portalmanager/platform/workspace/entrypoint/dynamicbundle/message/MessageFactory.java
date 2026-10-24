package br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.message;

import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.message.request.CreateMessageRequest;

public final class MessageFactory {
    public CreateMessageRequest valid(String microserviceIdentifier, String messageKey, String code) {
        return new CreateMessageRequest(null, microserviceIdentifier, messageKey, code, 400, null, null);
    }
}
