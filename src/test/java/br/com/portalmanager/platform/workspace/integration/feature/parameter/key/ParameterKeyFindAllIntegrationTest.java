package br.com.portalmanager.platform.workspace.integration.feature.parameter.key;

import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterClient;
import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterRequestFactory;
import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ParameterKeyFindAllIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("QUERY - filtra chaves por nome")
    void filtraPorNome() {
        create("filter-one", "CONFIGURATION", "1");
        create("filter-two", "CONFIGURATION", "2");

        client.findAll(parent, null, null, "filter-one")
                .expectOk()
                .expectSize("content", 1)
                .expect("content[0].key", "filter-one");
    }
}
