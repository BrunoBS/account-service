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

@DisplayName("Integração HTTP - schema create")
class SchemaCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("CREATE - cria schema de plataforma com primeira versão draft")
    void criaSchemaDePlataformaComVersaoInicial() {
        var created = schemas.create(factory.valid()).expectCreated().extractSchema();

        schemas.findByIdentifier(created.identifier()).expectOk()
                .expect("scope", "PLATFORM").expect("lifecycle", "ACTIVE");
        schemas.findVersions(created.identifier()).expectOk()
                .expect("[0].version", 1).expect("[0].status", "DRAFT");
    }

    @Test
    @DisplayName("CREATE - cria schema no escopo de um workspace")
    void criaSchemaDeWorkspace() {
        String workspace = scenario.createWorkspace().identifier();

        var created = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();

        schemas.findByIdentifier(workspace, created.identifier()).expectOk()
                .expect("scope", "WORKSPACE").expect("workspaceIdentifier", workspace);
    }

    @Test
    @DisplayName("VALIDATION - rejeita dados obrigatórios, definição inválida e código duplicado")
    void rejeitaSchemaInvalidoOuDuplicado() {
        schemas.create(factory.withoutCode()).expectBadRequest().expectContainsItem("details.field", "code");
        schemas.create(factory.withoutName()).expectBadRequest().expectContainsItem("details.field", "name");
        schemas.create(factory.withInvalidCode()).expectBadRequest().expectContainsItem("details.field", "code");
        schemas.create(factory.withInvalidDefinition()).expectBadRequest();
        schemas.create("missing-workspace", factory.valid()).expectNotFound();

        var valid = factory.valid();
        schemas.create(valid).expectCreated();
        schemas.create(factory.withDuplicateCode(valid.code())).expectConflict();
    }

    @Test
    @DisplayName("AUTH - exige autorização de owner para criar schema de plataforma")
    void exigeAutorizacaoDeOwner() {
        authorization.forbidden();

        schemas.create(factory.valid()).expectForbidden();
    }
}
