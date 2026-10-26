package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support;

import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.message.request.CreateMessageRequest;
import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.message.request.UpdateMessageRequest;
import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;

public final class MessageFactory extends AbstractTestDataFactory<CreateMessageRequest, MessageBuilder> {
    @Override protected MessageBuilder builder() { return MessageBuilder.builder(); }

    public CreateMessageRequest valid(String microserviceIdentifier, String messageKey, String code) {
        return create(b -> b.withMicroserviceIdentifier(microserviceIdentifier).withMessageKey(messageKey).withCode(code));
    }

    public CreateMessageRequest withoutCode(String microserviceIdentifier) {
        return create(b -> b.withMicroserviceIdentifier(microserviceIdentifier).withCode(null));
    }

    public UpdateMessageRequest update(Long version, String microserviceIdentifier, String messageKey, String code) {
        return builder().withVersion(version).withMicroserviceIdentifier(microserviceIdentifier)
                .withMessageKey(messageKey).withCode(code).buildUpdate();
    }
}
