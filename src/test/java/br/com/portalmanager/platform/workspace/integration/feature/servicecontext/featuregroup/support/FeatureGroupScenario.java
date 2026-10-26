package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support;

import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.support.ServiceContextScenario;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class FeatureGroupScenario implements TestScenario<FeatureGroupScenario> {
    private final ServiceContextScenario common;
    public FeatureGroupScenario(ServiceContextScenario common) { this.common = Objects.requireNonNull(common); }
    @Override public FeatureGroupScenario setup() { common.setup(); return this; }
}
