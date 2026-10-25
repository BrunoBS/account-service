package br.com.portalmanager.platform.workspace.integration.feature.parameter.support;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.integration.support.SchemaDefaultScenario;

/** Explicitly prepares the owner identity and parent microservice needed by HTTP parameter tests. */
final class DynamicParameterScenario implements TestScenario<String> {
    private final SchemaDefaultScenario schemas;
    private final AuthorizationMock authorization;
    private final MicroserviceClient microserviceClient;

    DynamicParameterScenario(SchemaDefaultScenario schemas, AuthorizationMock authorization,
                             MicroserviceClient microserviceClient) {
        this.schemas = schemas;
        this.authorization = authorization;
        this.microserviceClient = microserviceClient;
    }

    @Override
    public String setup() {
        schemas.setup();
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER").userName("parameter-owner"));
        return microserviceClient.createParent().expectCreated().identifier();
    }
}
