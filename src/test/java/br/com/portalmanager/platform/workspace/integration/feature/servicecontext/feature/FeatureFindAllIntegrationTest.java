package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureScenario;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroupassociation.support.FeatureGroupAssociationClient;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Feature FindAll")
class FeatureFindAllIntegrationTest extends BaseIntegrationTest {
    @Autowired private FeatureScenario scenario;
    @Autowired private FeatureClient features;
    @Autowired private FeatureGroupClient groups;
    @Autowired private FeatureGroupAssociationClient associations;
    private final FeatureFactory factory = new FeatureFactory();
    private final FeatureGroupFactory groupFactory = new FeatureGroupFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("QUERY - filtra features por lifecycle e grupo e pagina resultados")
    void filtraPorLifecycleGrupoEPagina() {
        var first = features.create(factory.valid(scenario.microserviceIdentifier())).expectCreated().extractBody();
        var second = features.create(factory.valid(scenario.microserviceIdentifier())).expectCreated().extractBody();
        var group = groups.create(groupFactory.valid()).expectCreated().extractBody();
        associations.associate(group.identifier(), first.identifier()).expectOk();
        features.inactivate(second.identifier()).expectOk();

        features.findAll().execute().expectOk().expectContainsItem("content.identifier", first.identifier()).expectNotContainsItem("content.identifier", second.identifier());
        features.findAll().lifecycle("INACTIVE").execute().expectOk().expectContainsItem("content.identifier", second.identifier());
        features.findAll().featureGroup(group.identifier()).execute().expectOk().expectSize("content", 1).expect("content[0].identifier", first.identifier());
        features.findAll().page(0).size(1).sort("code,asc").execute().expectOk().expectSize("content", 1);
    }

    @Test @DisplayName("VALIDATION - rejeita grupo inexistente, lifecycle e ordenação inválidos")
    void rejeitaFiltrosInvalidos() {
        features.findAll().featureGroup("00000000-0000-0000-0000-000000000000").execute().expectNotFound();
        features.findAll().lifecycle("UNKNOWN").execute().expectBadRequest();
        features.findAll().sort("settings").execute().expectBadRequest();
    }
}
