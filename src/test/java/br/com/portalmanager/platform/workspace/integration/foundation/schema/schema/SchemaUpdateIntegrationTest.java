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

@DisplayName("Integração HTTP - schema update")
class SchemaUpdateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();

    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("UPDATE - atualiza metadados de schemas de plataforma e workspace")
    void atualizaMetadadosEmAmbosEscopos() {
        var platform = schemas.create(factory.valid()).expectCreated().extractSchema();
        schemas.update(platform.identifier(), factory.update(platform.version()))
                .expectOk().expect("name", "Updated schema name").expect("code", platform.code());
        schemas.findByIdentifier(platform.identifier()).expectOk().expect("description", "Updated schema description");

        String workspace = scenario.createWorkspace().identifier();
        var scoped = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();
        schemas.update(workspace, scoped.identifier(), factory.update(scoped.version()))
                .expectOk().expect("workspaceIdentifier", workspace).expect("name", "Updated schema name")
                .expect("code", scoped.code());
    }

    @Test
    @DisplayName("VALIDATION - rejeita versão desatualizada e schema inexistente")
    void rejeitaVersaoDesatualizadaEInexistente() {
        var created = schemas.create(factory.valid()).expectCreated().extractSchema();
        schemas.update(created.identifier(), factory.update(created.version())).expectOk();
        schemas.update(created.identifier(), factory.withStaleVersion(created.version())).expectConflict();
        schemas.update("missing-schema", factory.update(0L)).expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige autorização de owner para atualizar schema")
    void exigeAutorizacaoParaAtualizar() {
        var created = schemas.create(factory.valid()).expectCreated().extractSchema();
        authorization.forbidden();
        schemas.update(created.identifier(), factory.update(created.version())).expectForbidden();
    }
}
