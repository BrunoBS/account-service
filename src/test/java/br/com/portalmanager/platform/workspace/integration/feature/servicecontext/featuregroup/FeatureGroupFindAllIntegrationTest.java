package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupScenario;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroupassociation.support.FeatureGroupAssociationClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceFactory;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Feature Group FindAll")
class FeatureGroupFindAllIntegrationTest extends BaseIntegrationTest {
    @Autowired private FeatureGroupScenario scenario;
    @Autowired private MicroserviceClient microservices;
    @Autowired private FeatureClient features;
    @Autowired private FeatureGroupClient groups;
    @Autowired private FeatureGroupAssociationClient associations;
    private final MicroserviceFactory microserviceFactory = new MicroserviceFactory();
    private final FeatureFactory featureFactory = new FeatureFactory();
    private final FeatureGroupFactory groupFactory = new FeatureGroupFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("QUERY - filtra grupos por lifecycle e feature associada")
    void filtraPorLifecycleEFeature() {
        var microservice = microservices.create(microserviceFactory.valid()).expectCreated().extractBody();
        var feature = features.create(featureFactory.valid(microservice.identifier())).expectCreated().extractBody();
        var selected = groups.create(groupFactory.valid()).expectCreated().extractBody();
        var other = groups.create(groupFactory.valid()).expectCreated().extractBody();
        associations.associate(selected.identifier(), feature.identifier()).expectOk();
        groups.inactivate(other.identifier()).expectOk();

        groups.findAll().execute().expectOk().expectContainsItem("content.identifier", selected.identifier()).expectNotContainsItem("content.identifier", other.identifier());
        groups.findAll().lifecycle("INACTIVE").execute().expectOk().expectContainsItem("content.identifier", other.identifier());
        groups.findAll().feature(feature.identifier()).execute().expectOk().expectSize("content", 1).expect("content[0].identifier", selected.identifier());
    }

    @Test @DisplayName("VALIDATION - rejeita filtro de feature inexistente e ordenação inválida")
    void rejeitaFiltrosInvalidos() {
        groups.findAll().feature("00000000-0000-0000-0000-000000000000").execute().expectNotFound();
        groups.findAll().sort("description").execute().expectBadRequest();
    }
}
