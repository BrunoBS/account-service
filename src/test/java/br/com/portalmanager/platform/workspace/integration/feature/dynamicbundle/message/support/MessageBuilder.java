package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.message.request.CreateMessageRequest;
import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.message.request.UpdateMessageRequest;

public final class MessageBuilder implements TestDataBuilder<CreateMessageRequest> {
    private Long version;
    private String microserviceIdentifier = "00000000-0000-0000-0000-000000000001";
    private String messageKey = "workspace.created";
    private String code = "WORKSPACE-CREATED";
    private Integer httpStatus = 400;
    private String observation = "Workspace creation failed";

    public static MessageBuilder builder() { return new MessageBuilder(); }
    public MessageBuilder withVersion(Long value) { version = value; return this; }
    public MessageBuilder withMicroserviceIdentifier(String value) { microserviceIdentifier = value; return this; }
    public MessageBuilder withMessageKey(String value) { messageKey = value; return this; }
    public MessageBuilder withCode(String value) { code = value; return this; }
    public MessageBuilder withHttpStatus(Integer value) { httpStatus = value; return this; }
    public MessageBuilder withObservation(String value) { observation = value; return this; }

    @Override
    public CreateMessageRequest build() {
        return new CreateMessageRequest(version, microserviceIdentifier, messageKey, code, httpStatus, observation, null);
    }

    public UpdateMessageRequest buildUpdate() {
        return new UpdateMessageRequest(version, microserviceIdentifier, messageKey, code, httpStatus, observation);
    }
}
