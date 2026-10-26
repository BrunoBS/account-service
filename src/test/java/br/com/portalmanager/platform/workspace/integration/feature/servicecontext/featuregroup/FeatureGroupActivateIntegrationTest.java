package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Feature Group Activate")
class FeatureGroupActivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private FeatureGroupScenario scenario;
    @Autowired private FeatureGroupClient groups;
    private final FeatureGroupFactory factory = new FeatureGroupFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("ACTIVATE - reativa grupo inativo")
    void reativaGrupo() {
        var created = groups.create(factory.valid()).expectCreated().extractBody();
        groups.inactivate(created.identifier()).expectOk();
        groups.activate(created.identifier()).expectOk().expect("lifecycle", "ACTIVE");
    }

    @Test @DisplayName("VALIDATION - rejeita ativação de grupo ativo ou inexistente")
    void rejeitaAtivacaoInvalida() {
        var created = groups.create(factory.valid()).expectCreated().extractBody();
        groups.activate(created.identifier()).expectBadRequest();
        groups.activate("00000000-0000-0000-0000-000000000000").expectNotFound();
    }
}
