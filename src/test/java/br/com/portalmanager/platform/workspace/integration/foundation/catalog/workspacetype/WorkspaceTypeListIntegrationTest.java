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

@DisplayName("Integração HTTP - workspace list")
@DatabaseScripts(@WithDatabaseScripts(setup = "classpath:sql/catalog-active-lifecycle.sql",
        setupPhase = DatabaseSetupPhase.BEFORE_EACH))
class WorkspaceTypeListIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("QUERY - lista catálogos ativos e filtra por status")
    void listaCatalogosAtivos() {
        catalogs.create(factory.valid(CODE)).expectCreated();
        catalogs.findAll().expectOk().expectSize("", 1);
        catalogs.findAll(java.util.Map.of("active", "true")).expectOk().expectSize("", 1);
        catalogs.findAll(java.util.Map.of("active", "false")).expectOk().expectSize("", 0);
    }

    @Test
    @DisplayName("VALIDATION - rejeita filtro de lista inválido")
    void rejeitaFiltroInvalido() {
        catalogs.findAll(java.util.Map.of("active", "not-a-boolean")).expectBadRequest();
    }

    @Test
    @DisplayName("AUTH - exige autorização para listar catálogos")
    void exigeAutorizacaoParaListar() {
        authorization.forbidden();
        catalogs.findAll().expectForbidden();
    }
}
