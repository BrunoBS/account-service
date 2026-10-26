package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Feature Group Update")
class FeatureGroupUpdateIntegrationTest extends BaseIntegrationTest {
    @Autowired private FeatureGroupScenario scenario;
    @Autowired private FeatureGroupClient groups;
    private final FeatureGroupFactory factory = new FeatureGroupFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("UPDATE - altera dados e incrementa versão")
    void atualizaGrupo() {
        var created = groups.create(factory.valid()).expectCreated().extractBody();
        var updated = groups.update(created.identifier(), factory.update(created.version(), created.code(), "Updated group"))
                .expectOk().expect("name", "Updated group").extractBody();
        org.junit.jupiter.api.Assertions.assertEquals(created.version() + 1, updated.version());
        groups.findByIdentifier(created.identifier()).expectOk().expect("name", "Updated group");
    }

    @Test @DisplayName("VALIDATION - rejeita versão ausente ou obsoleta, código imutável e nome duplicado")
    void rejeitaUpdateInvalido() {
        var first = groups.create(factory.valid()).expectCreated().extractBody();
        var second = groups.create(factory.valid()).expectCreated().extractBody();
        groups.update(first.identifier(), factory.updateWithoutVersion(first.code(), "Missing version")).expectBadRequest();
        var update = factory.update(first.version(), first.code(), "Updated unique group");
        groups.update(first.identifier(), update).expectOk();
        groups.update(first.identifier(), update).expectConflict();
        var current = groups.findByIdentifier(first.identifier()).expectOk().extractBody();
        groups.update(first.identifier(), factory.withChangedCode(current.version())).expectBadRequest();
        groups.update(second.identifier(), factory.update(second.version(), second.code(), "Updated unique group"))
                .expectBadRequest().expectContainsItem("details.field", "name");
    }
}
