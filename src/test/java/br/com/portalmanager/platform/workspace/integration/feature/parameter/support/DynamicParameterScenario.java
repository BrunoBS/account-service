package br.com.portalmanager.platform.workspace.integration.feature.parameter.support;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.integration.support.SchemaDefaultScenario;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceFactory;
import org.springframework.stereotype.Component;

/** Explicitly prepares the owner identity and parent microservice needed by HTTP parameter tests. */
@Component
public final class DynamicParameterScenario implements TestScenario<String> {
    private final SchemaDefaultScenario schemas;
    private final AuthorizationMock authorization;
    private final br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient microserviceClient;

    public DynamicParameterScenario(SchemaDefaultScenario schemas, AuthorizationMock authorization,
                             br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient microserviceClient) {
        this.schemas = schemas;
        this.authorization = authorization;
        this.microserviceClient = microserviceClient;
    }

    @Override
    public String setup() {
        schemas.setup();
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER").userName("parameter-owner"));
        return microserviceClient.create(new MicroserviceFactory().valid("account-service", "Account"))
                .expectCreated().extractBody().identifier();
    }
}
