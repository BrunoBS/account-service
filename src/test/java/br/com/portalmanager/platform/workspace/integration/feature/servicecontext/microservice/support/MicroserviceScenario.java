package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support;

import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.support.ServiceContextScenario;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class MicroserviceScenario implements TestScenario<MicroserviceScenario> {
    private final ServiceContextScenario common;
    public MicroserviceScenario(ServiceContextScenario common) { this.common = Objects.requireNonNull(common); }
    @Override public MicroserviceScenario setup() { common.setup(); return this; }
}
