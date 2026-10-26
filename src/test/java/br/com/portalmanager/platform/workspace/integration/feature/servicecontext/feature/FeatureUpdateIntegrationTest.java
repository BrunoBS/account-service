package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Feature Update")
class FeatureUpdateIntegrationTest extends BaseIntegrationTest {
    @Autowired private FeatureScenario scenario;
    @Autowired private FeatureClient features;
    private final FeatureFactory factory = new FeatureFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("UPDATE - atualiza feature e persiste a nova versão")
    void atualizaFeature() {
        var created = features.create(factory.valid(scenario.microserviceIdentifier())).expectCreated().extractBody();
        var updated = features.update(created.identifier(), factory.update(created.version(), created.code(), "Updated feature", scenario.microserviceIdentifier(), false))
                .expectOk().expect("name", "Updated feature").extractBody();
        org.junit.jupiter.api.Assertions.assertEquals(created.version() + 1, updated.version());
        features.findByIdentifier(created.identifier()).expectOk().expect("name", "Updated feature");
    }

    @Test @DisplayName("VALIDATION - rejeita versão ausente ou antiga, código imutável e nome duplicado")
    void rejeitaUpdateInvalido() {
        var first = features.create(factory.valid(scenario.microserviceIdentifier())).expectCreated().extractBody();
        var second = features.create(factory.valid(scenario.microserviceIdentifier())).expectCreated().extractBody();
        features.update(first.identifier(), factory.updateWithoutVersion(first.code(), "No version", scenario.microserviceIdentifier())).expectBadRequest();
        var update = factory.update(first.version(), first.code(), "Current feature", scenario.microserviceIdentifier(), false);
        features.update(first.identifier(), update).expectOk();
        features.update(first.identifier(), update).expectConflict();
        var current = features.findByIdentifier(first.identifier()).expectOk().extractBody();
        features.update(first.identifier(), factory.updateWithChangedCode(current.version(), scenario.microserviceIdentifier())).expectBadRequest();
        features.update(second.identifier(), factory.update(second.version(), second.code(), "Current feature", scenario.microserviceIdentifier(), false)).expectBadRequest().expectContainsItem("details.field", "name");
    }
}
