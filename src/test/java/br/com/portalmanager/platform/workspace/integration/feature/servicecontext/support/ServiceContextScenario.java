package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.support;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.integration.support.SchemaDefaultScenario;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Prepares the shared HTTP schema and owner authorization used by Service Context scenarios. */
@Component
public final class ServiceContextScenario implements TestScenario<ServiceContextScenario> {
    private final AuthorizationMock authorization;
    private final SchemaDefaultScenario schemas;

    public ServiceContextScenario(AuthorizationMock authorization, SchemaDefaultScenario schemas) {
        this.authorization = Objects.requireNonNull(authorization);
        this.schemas = Objects.requireNonNull(schemas);
    }

    @Override
    public ServiceContextScenario setup() {
        authorization.reset();
        authorization.allow(session -> session.userName("owner-it").groups("PM5_OWNER"));
        schemas.setup();
        return this;
    }
}
