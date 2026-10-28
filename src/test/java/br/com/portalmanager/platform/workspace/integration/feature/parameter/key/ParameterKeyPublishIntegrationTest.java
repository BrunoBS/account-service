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
import java.util.List;
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;

@WithRedis
class ParameterKeyPublishIntegrationTest extends BaseIntegrationTest {
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

    private void await(java.util.function.BooleanSupplier condition) {
        br.com.portalmanager.platform.library.testing.lifecycle.PlatformAwait.until(
                condition, java.time.Duration.ofSeconds(10), java.time.Duration.ofMillis(25));
    }

    @Test
    @DisplayName("PUBLISH - aceita publicação em lote e publica a chave")
    void publicaChave() {
        String identifier = create("publish-key", "CONFIGURATION", "{\"published\":true}");

        client.publish(List.of(UUID.fromString(identifier)), List.of(UUID.fromString(parent))).expectAcceptedWithEmptyBody();
        await(() -> "PUBLISHED".equals(client.findByIdentifier(identifier).expectOk().status()));
        client.findByIdentifier(identifier).expectOk().expect("status", "PUBLISHED");
    }

    @Test
    @DisplayName("VALIDATION - rejeita publicação sem identificadores")
    void rejeitaLoteVazio() {
        client.publish(List.of(), List.of(UUID.fromString(parent))).expectBadRequest();
    }
}
