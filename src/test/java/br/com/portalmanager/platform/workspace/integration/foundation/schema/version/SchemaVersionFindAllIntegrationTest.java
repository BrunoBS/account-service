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

@DisplayName("Integração HTTP - schema version find all")
class SchemaVersionFindAllIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();
    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("QUERY - lista versões de plataforma em ordem decrescente")
    void listaVersoesDePlataformaMaisRecentesPrimeiro() {
        var platform = schemas.create(factory.valid()).expectCreated().extractSchema();
        var first = schemas.findVersions(platform.identifier()).expectOk().extractVersions().getFirst();
        schemas.publish(platform.identifier(), first.identifier()).expectOk();
        schemas.createVersion(platform.identifier(), factory.validVersion("v2")).expectCreated();
        schemas.createVersion(platform.identifier(), factory.validVersion("updated draft"))
                .expectCreated().expect("version", 2);
        schemas.findVersions(platform.identifier()).expectOk()
                .expect("[0].version", 2).expect("[1].version", 1);

    }

    @Test
    @DisplayName("QUERY - retorna não encontrado quando o schema não existe")
    void rejeitaSchemaAusente() { schemas.findVersions("missing-schema").expectNotFound(); }

    @Test
    @DisplayName("AUTH - exige autorização para listar versões")
    void exigeAutorizacao() {
        authorization.forbidden();
        schemas.findVersions("missing-schema").expectForbidden();
    }
}
