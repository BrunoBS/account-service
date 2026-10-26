package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceFactory;
import br.com.portalmanager.platform.workspace.integration.support.SchemaDefaultScenario;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Prepares schema and microservice prerequisites for Dynamic Bundle HTTP scenarios. */
@Component
public final class MessageScenario implements TestScenario<String> {
    private final AuthorizationMock authorization;
    private final SchemaDefaultScenario schemas;
    private final MicroserviceClient microservices;

    public MessageScenario(AuthorizationMock authorization, SchemaDefaultScenario schemas,
                           MicroserviceClient microservices) {
        this.authorization = Objects.requireNonNull(authorization);
        this.schemas = Objects.requireNonNull(schemas);
        this.microservices = Objects.requireNonNull(microservices);
    }

    @Override public String setup() {
        authorization.reset();
        authorization.allow(session -> session.userName("message-owner").groups("PM5_OWNER"));
        schemas.setup();
        return microservices.create(new MicroserviceFactory().valid("message-service", "Message service"))
                .expectCreated().extractBody().identifier();
    }
}
