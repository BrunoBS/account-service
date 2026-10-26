package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Feature Group Find")
class FeatureGroupFindByIdentifierIntegrationTest extends BaseIntegrationTest {
    @Autowired private FeatureGroupScenario scenario;
    @Autowired private FeatureGroupClient groups;
    private final FeatureGroupFactory factory = new FeatureGroupFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("QUERY - encontra grupo ativo pelo identificador")
    void encontraGrupo() {
        var created = groups.create(factory.valid()).expectCreated().extractBody();
        groups.findByIdentifier(created.identifier()).expectOk().expect("identifier", created.identifier());
    }

    @Test @DisplayName("QUERY - não encontra grupo inexistente ou inativo")
    void naoEncontraGrupoIndisponivel() {
        groups.findByIdentifier("00000000-0000-0000-0000-000000000000").expectNotFound();
        var created = groups.create(factory.valid()).expectCreated().extractBody();
        groups.inactivate(created.identifier()).expectOk();
        groups.findByIdentifier(created.identifier()).expectNotFound();
    }
}
