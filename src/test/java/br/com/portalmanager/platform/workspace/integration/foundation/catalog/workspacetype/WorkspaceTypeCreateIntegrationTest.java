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

@DisplayName("Integração HTTP - workspace create")
@DatabaseScripts(@WithDatabaseScripts(setup = "classpath:sql/catalog-active-lifecycle.sql",
        setupPhase = DatabaseSetupPhase.BEFORE_EACH))
class WorkspaceTypeCreateIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("CREATE - cria o catálogo e permite consultá-lo")
    void criaCatalogoEConsultaPersistencia() {
        catalogs.create(factory.valid(CODE)).expectCreated().expect("code", CODE);
        catalogs.findByCode(CODE).expectOk().expect("code", CODE);
    }

    @Test
    @DisplayName("VALIDATION - rejeita código fora do enum, label ausente e duplicidade")
    void rejeitaCatalogoInvalidoOuDuplicado() {
        catalogs.create(factory.withInvalidCode()).expectBadRequest();
        catalogs.create(factory.withoutLabel(CODE)).expectBadRequest();
        catalogs.create(factory.valid(CODE)).expectCreated();
        catalogs.create(factory.valid(CODE)).expectBadRequest();
    }

    @Test
    @DisplayName("AUTH - exige autorização para criar catálogo")
    void exigeAutorizacaoParaCriar() {
        authorization.forbidden();
        catalogs.create(factory.valid(CODE)).expectForbidden();
    }
}
