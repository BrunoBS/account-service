package br.com.portalmanager.platform.workspace.integration.feature.parameter.key;

import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterClient;
import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterRequestFactory;
import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.UUID;

class ParameterKeyFindByIdentifierIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("QUERY - encontra chave pelo identificador")
    void encontraChave() {
        String identifier = create("lookup", "CONFIGURATION", "1");
        client.findByIdentifier(identifier).expectOk().expect("key", "lookup");
    }

    @Test
    @DisplayName("QUERY - retorna não encontrado para identificador inexistente")
    void naoEncontraChaveInexistente() {
        client.findByIdentifier(UUID.randomUUID().toString()).expectNotFound();
    }
}
