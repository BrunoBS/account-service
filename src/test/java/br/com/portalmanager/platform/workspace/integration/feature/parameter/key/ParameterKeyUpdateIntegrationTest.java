package br.com.portalmanager.platform.workspace.integration.feature.parameter.key;

import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterClient;
import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterRequestFactory;
import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ParameterKeyUpdateIntegrationTest extends BaseIntegrationTest {
    @Autowired private DynamicParameterClient client;
    @Autowired private DynamicParameterScenario scenario;
    private final DynamicParameterRequestFactory factory = new DynamicParameterRequestFactory();
    private String parent;

    @BeforeEach
    void prepare() {
        parent = scenario.setup();
    }

    private String create(String key, String type, String value) {
        return client.create(factory.valid(parent, key, type, value)).expectCreated().identifier();
    }

    @Test
    @DisplayName("UPDATE - atualiza os dados usando a versão corrente")
    void atualizaDados() {
        String identifier = create("update-key", "CONFIGURATION", "{\"before\":true}");

        client.update(identifier, factory.update(0, "updated description", "CONFIGURATION", "{\"after\":true}"))
                .expectOk()
                .expect("description", "updated description");
    }

    @Test
    @DisplayName("VALIDATION - rejeita atualização com versão desatualizada")
    void rejeitaVersaoDesatualizada() {
        String identifier = create("stale-key", "CONFIGURATION", "1");

        client.update(identifier, factory.update(1, "stale", "CONFIGURATION", "2"))
                .expectConflict();
    }
}
