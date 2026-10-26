package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support;

import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.support.ServiceContextScenario;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class FeatureScenario implements TestScenario<FeatureScenario> {
    private final ServiceContextScenario common;
    private final MicroserviceClient microservices;
    private final MicroserviceFactory factory = new MicroserviceFactory();
    private String microserviceIdentifier;
    public FeatureScenario(ServiceContextScenario common, MicroserviceClient microservices) {
        this.common = Objects.requireNonNull(common); this.microservices = Objects.requireNonNull(microservices);
    }
    @Override public FeatureScenario setup() {
        common.setup();
        microserviceIdentifier = microservices.create(factory.valid()).expectCreated().extractBody().identifier();
        return this;
    }
    public String microserviceIdentifier() { return microserviceIdentifier; }
    public String createMicroservice(String code) {
        return microservices.create(factory.withCode(code)).expectCreated().extractBody().identifier();
    }
}
