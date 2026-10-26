package br.com.portalmanager.platform.workspace.integration.feature.parameter.key;

import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterClient;
import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterRequestFactory;
import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ParameterKeyInactivateIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("UPDATE - inativa uma chave ativa")
    void inativaChave() {
        String identifier = create("inactivate-key", "CONFIGURATION", "1");

        client.inactivate(identifier).expectOk().expect("active", false);
    }
}
