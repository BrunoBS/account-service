package br.com.portalmanager.platform.workspace.integration.foundation.schema.schema;

import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaClient;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaIntegrationScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - schema find all")
class SchemaFindAllIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();

    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("QUERY - filtra schemas de plataforma e workspace por lifecycle")
    void filtraPorLifecycleNosEscopos() {
        var platform = schemas.create(factory.valid()).expectCreated().extractSchema();
        schemas.inactivate(platform.identifier()).expectOk();
        assertTrue(schemas.findAll("INACTIVE").expectOk().extractSchemas().stream()
                .anyMatch(schema -> platform.identifier().equals(schema.identifier())));

        String workspace = scenario.createWorkspace().identifier();
        var scoped = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();
        schemas.inactivate(workspace, scoped.identifier()).expectOk();
        assertTrue(schemas.findAll(workspace, "INACTIVE").expectOk().extractSchemas().stream()
                .anyMatch(schema -> scoped.identifier().equals(schema.identifier())));
    }

    @Test
    @DisplayName("QUERY - retorna lista vazia para lifecycle sem resultados")
    void retornaListaVaziaParaLifecycleSemResultados() {
        schemas.findAll("QUARANTINED").expectOk().expectSize("", 0);
    }

    @Test
    @DisplayName("AUTH - exige autorização para listar schemas")
    void exigeAutorizacaoParaListar() {
        authorization.forbidden();
        schemas.findAll().expectForbidden();
    }
}
