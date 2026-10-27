package br.com.portalmanager.platform.workspace.integration.foundation.schema.schema;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaClient;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaIntegrationScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - schema find")
class SchemaFindIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();

    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("QUERY - busca schema de plataforma pelo identificador")
    void buscaSchemaDePlataforma() {
        var platform = schemas.create(factory.valid()).expectCreated().extractSchema();
        schemas.findByIdentifier(platform.identifier()).expectOk()
                .expect("identifier", platform.identifier()).expect("scope", "PLATFORM");

    }

    @Test
    @DisplayName("QUERY - retorna não encontrado para identificador desconhecido")
    void retornaNaoEncontradoParaSchemaAusente() {
        schemas.findByIdentifier("missing-schema").expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige autorização para consultar schema")
    void exigeAutorizacaoParaConsultar() {
        authorization.forbidden();
        schemas.findAll().expectForbidden();
    }
}
