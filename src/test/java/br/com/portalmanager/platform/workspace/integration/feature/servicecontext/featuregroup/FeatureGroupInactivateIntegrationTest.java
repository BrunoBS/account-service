package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Feature Group Inactivate")
class FeatureGroupInactivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private FeatureGroupScenario scenario;
    @Autowired private FeatureGroupClient groups;
    private final FeatureGroupFactory factory = new FeatureGroupFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("INACTIVATE - inativa grupo e o lista como inativo")
    void inativaGrupo() {
        var created = groups.create(factory.valid()).expectCreated().extractBody();
        groups.inactivate(created.identifier()).expectOk().expect("lifecycle", "INACTIVE");
        groups.findByIdentifier(created.identifier()).expectNotFound();
        groups.findAll().lifecycle("INACTIVE").execute().expectOk().expectContainsItem("content.identifier", created.identifier());
    }

    @Test @DisplayName("VALIDATION - rejeita inativação repetida")
    void rejeitaInativacaoRepetida() {
        var created = groups.create(factory.valid()).expectCreated().extractBody();
        groups.inactivate(created.identifier()).expectOk();
        groups.inactivate(created.identifier()).expectNotFound();
    }
}
