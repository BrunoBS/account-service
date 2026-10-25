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

@DisplayName("Integração HTTP - schema version delete draft")
class SchemaVersionDeleteIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();
    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("DELETE - remove drafts de schemas de plataforma e workspace")
    void removeDrafts() {
        var platform = schemas.create(factory.valid()).expectCreated().extractSchema();
        var platformDraft = schemas.findVersions(platform.identifier()).expectOk().extractVersions().getFirst();
        schemas.deleteDraft(platform.identifier(), platformDraft.identifier()).expectNoContent();
        schemas.findVersions(platform.identifier()).expectOk().expectSize("", 0);

        String workspace = scenario.createWorkspace().identifier();
        var scoped = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();
        var scopedDraft = schemas.findVersions(workspace, scoped.identifier()).expectOk().extractVersions().getFirst();
        schemas.deleteDraft(workspace, scoped.identifier(), scopedDraft.identifier()).expectNoContent();
        schemas.findVersions(workspace, scoped.identifier()).expectOk().expectSize("", 0);
    }

    @Test
    @DisplayName("VALIDATION - não remove versão publicada nem versão inexistente")
    void preservaVersaoPublicada() {
        var schema = schemas.create(factory.valid()).expectCreated().extractSchema();
        var draft = schemas.findVersions(schema.identifier()).expectOk().extractVersions().getFirst();
        schemas.publish(schema.identifier(), draft.identifier()).expectOk();
        schemas.deleteDraft(schema.identifier(), draft.identifier()).expectConflict();
        schemas.deleteDraft(schema.identifier(), "missing-version").expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige autorização para excluir draft")
    void exigeAutorizacao() {
        authorization.forbidden();
        schemas.deleteDraft("missing-schema", "missing-version").expectForbidden();
    }
}
