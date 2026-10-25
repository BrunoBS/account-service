package br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype;

import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeClient;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.database.DatabaseSetupPhase;
import br.com.portalmanager.platform.library.testing.database.annotation.DatabaseScripts;
import br.com.portalmanager.platform.library.testing.database.annotation.WithDatabaseScripts;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogTestDataFactory;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - workspace update")
@DatabaseScripts(@WithDatabaseScripts(setup = "classpath:sql/catalog-active-lifecycle.sql",
        setupPhase = DatabaseSetupPhase.BEFORE_EACH))
class WorkspaceTypeUpdateIntegrationTest extends BaseIntegrationTest {
    private static final String CODE = "ADMIN";

    @Autowired private AuthorizationMock authorization;
    @Autowired private WorkspaceTypeClient catalogs;
    private final CatalogTestDataFactory factory = new CatalogTestDataFactory();

    @BeforeEach
    void prepareAuthorization() {
        authorization.reset();
        authorization.allow(session -> session.userName("owner-it").groups("PM5_OWNER"));
    }

    @Test
    @DisplayName("UPDATE - atualiza catálogo e persiste a alteração")
    void atualizaCatalogo() {
        catalogs.create(factory.valid(CODE)).expectCreated();
        String label = "Updated catalog label";
        catalogs.update(CODE, factory.withLabel(CODE, label)).expectOk().expect("label", label);
        catalogs.findByCode(CODE).expectOk().expect("label", label);
    }

    @Test
    @DisplayName("VALIDATION - rejeita catálogo inexistente e label ausente")
    void rejeitaAtualizacaoInvalida() {
        catalogs.update(CODE, factory.withLabel(CODE, "Updated label")).expectNotFound();
        catalogs.create(factory.valid(CODE)).expectCreated();
        catalogs.update(CODE, factory.withoutLabel(CODE)).expectBadRequest();
    }

    @Test
    @DisplayName("AUTH - exige autorização para atualizar catálogo")
    void exigeAutorizacaoParaAtualizar() {
        authorization.forbidden();
        catalogs.update(CODE, factory.valid(CODE)).expectForbidden();
    }
}
