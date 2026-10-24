package br.com.portalmanager.platform.workspace.entrypoint.catalog.application_scope;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.database.DatabaseSetupPhase;
import br.com.portalmanager.platform.library.testing.database.annotation.DatabaseScripts;
import br.com.portalmanager.platform.library.testing.database.annotation.WithDatabaseScripts;
import br.com.portalmanager.platform.workspace.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - tipo de escopo de aplicação")
@DatabaseScripts(@WithDatabaseScripts(setup = "classpath:sql/workspace-http-schema.sql",
        setupPhase = DatabaseSetupPhase.BEFORE_EACH))
class ApplicationScopeTypeIntegrationTest extends BaseIntegrationTest {
    private static final String CODE = "BACKEND";

    @Autowired private AuthorizationMock authorization;
    @Autowired private ApplicationScopeTypeClient applicationScopes;

    private final CatalogFactory factory = new CatalogFactory();

    @BeforeEach
    void prepareAuthorization() {
        authorization.reset();
        authorization.allow(session -> session.userName("owner-it").groups("PM5_OWNER"));
    }

    @Test
    @DisplayName("CREATE - cria, atualiza, exclui e restaura um tipo de escopo de aplicação")
    void criaAtualizaExcluiERestauraCatalogo() {
        applicationScopes.create(factory.valid(CODE))
                .expectCreated()
                .expect("code", CODE)
                .expect("label", "Catalog item for tests");

        applicationScopes.update(CODE, factory.withLabel(CODE, "Updated backend"))
                .expectOk()
                .expect("label", "Updated backend");

        applicationScopes.delete(CODE).expectNoContent();
        applicationScopes.findByCode(CODE).expectNotFound();
        applicationScopes.restore(CODE)
                .expectOk()
                .expect("code", CODE)
                .expect("label", "Updated backend");
    }
}
