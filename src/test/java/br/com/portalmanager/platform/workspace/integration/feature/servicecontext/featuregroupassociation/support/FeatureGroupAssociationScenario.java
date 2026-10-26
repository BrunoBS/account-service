package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroupassociation.support;

import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.support.ServiceContextScenario;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class FeatureGroupAssociationScenario implements TestScenario<FeatureGroupAssociationScenario> {
    private final ServiceContextScenario common;
    private final MicroserviceClient microservices;
    private final FeatureClient features;
    private final FeatureGroupClient groups;
    private final MicroserviceFactory microserviceFactory = new MicroserviceFactory();
    private final FeatureFactory featureFactory = new FeatureFactory();
    private final FeatureGroupFactory groupFactory = new FeatureGroupFactory();
    private String featureIdentifier;
    private String groupIdentifier;
    public FeatureGroupAssociationScenario(ServiceContextScenario common, MicroserviceClient microservices,
            FeatureClient features, FeatureGroupClient groups) {
        this.common = Objects.requireNonNull(common); this.microservices = Objects.requireNonNull(microservices);
        this.features = Objects.requireNonNull(features); this.groups = Objects.requireNonNull(groups);
    }
    @Override public FeatureGroupAssociationScenario setup() {
        common.setup();
        String microservice = microservices.create(microserviceFactory.valid()).expectCreated().extractBody().identifier();
        featureIdentifier = features.create(featureFactory.valid(microservice)).expectCreated().extractBody().identifier();
        groupIdentifier = groups.create(groupFactory.valid()).expectCreated().extractBody().identifier();
        return this;
    }
    public String featureIdentifier() { return featureIdentifier; }
    public String groupIdentifier() { return groupIdentifier; }
}
