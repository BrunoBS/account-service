package br.com.portalmanager.platform.workspace.integration.foundation.schema.version;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.entrypoint.schema.response.SchemaResponse;
import br.com.portalmanager.platform.workspace.entrypoint.schema.response.SchemaVersionResponse;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaClient;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaIntegrationScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - workspace schema version delete")
class WorkspaceSchemaVersionDeleteIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();

    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("DELETE - remove versão draft do schema no workspace")
    void removeDraftDoWorkspace() {
        String workspace = scenario.createWorkspace().identifier();
        SchemaResponse schema = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();
        SchemaVersionResponse draft = schemas.findVersions(workspace, schema.identifier()).expectOk().extractVersions().getFirst();

        schemas.deleteDraft(workspace, schema.identifier(), draft.identifier()).expectNoContent();
        schemas.findVersions(workspace, schema.identifier()).expectOk().expectSize("", 0);
    }

    @Test
    @DisplayName("VALIDATION - não remove versão publicada nem versão ausente")
    void preservaVersaoPublicada() {
        String workspace = scenario.createWorkspace().identifier();
        SchemaResponse schema = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();
        SchemaVersionResponse draft = schemas.findVersions(workspace, schema.identifier()).expectOk().extractVersions().getFirst();
        schemas.publish(workspace, schema.identifier(), draft.identifier()).expectOk();

        schemas.deleteDraft(workspace, schema.identifier(), draft.identifier()).expectConflict();
        schemas.deleteDraft(workspace, schema.identifier(), "missing-version").expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige autorização para excluir versão draft no workspace")
    void exigeAutorizacao() {
        String workspace = scenario.createWorkspace().identifier();
        authorization.forbidden();

        schemas.deleteDraft(workspace, "missing-schema", "missing-version").expectForbidden();
    }
}
