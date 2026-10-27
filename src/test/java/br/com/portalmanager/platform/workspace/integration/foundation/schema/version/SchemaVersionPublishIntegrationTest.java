package br.com.portalmanager.platform.workspace.integration.foundation.schema.version;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaClient;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaIntegrationScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - schema version publish")
class SchemaVersionPublishIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();
    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("PUBLISH - publica versão draft e mantém publicação monotônica em plataforma")
    void publicaVersoesDePlataformaMonotonicamente() {
        var platform = schemas.create(factory.valid()).expectCreated().extractSchema();
        var first = schemas.findVersions(platform.identifier()).expectOk().extractVersions().getFirst();
        schemas.publish(platform.identifier(), first.identifier()).expectOk().expect("status", "PUBLISHED");
        schemas.publish(platform.identifier(), first.identifier()).expectOk().expect("status", "PUBLISHED");
        schemas.createVersion(platform.identifier(), factory.validVersion("v2")).expectCreated();
        var second = schemas.findVersions(platform.identifier()).expectOk().extractVersions().getFirst();
        schemas.publish(platform.identifier(), second.identifier()).expectOk()
                .expect("version", 2).expect("status", "PUBLISHED");

    }

    @Test
    @DisplayName("VALIDATION - retorna não encontrado para schema ou versão ausente")
    void rejeitaSchemaOuVersaoAusente() {
        schemas.publish("missing-schema", "missing-version").expectNotFound();
        var schema = schemas.create(factory.valid()).expectCreated().extractSchema();
        schemas.publish(schema.identifier(), "missing-version").expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige autorização para publicar versão")
    void exigeAutorizacao() {
        authorization.forbidden();
        schemas.publish("missing-schema", "missing-version").expectForbidden();
    }
}
