package br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype;

import br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype.support.ApplicationScopeTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype.support.CatalogFactory;
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

@DisplayName("Integração HTTP - application scope find")
@DatabaseScripts(@WithDatabaseScripts(setup = "classpath:sql/catalog-active-lifecycle.sql",
        setupPhase = DatabaseSetupPhase.BEFORE_EACH))
class ApplicationScopeTypeFindIntegrationTest extends BaseIntegrationTest {
    private static final String CODE = "BACKEND";

    @Autowired private AuthorizationMock authorization;
    @Autowired private ApplicationScopeTypeClient catalogs;
    private final CatalogFactory factory = new CatalogFactory();

    @BeforeEach
    void prepareAuthorization() {
        authorization.reset();
        authorization.allow(session -> session.userName("owner-it").groups("PM5_OWNER"));
    }

    @Test
    @DisplayName("QUERY - consulta catálogo existente por código")
    void consultaCatalogoExistente() {
        catalogs.create(factory.valid(CODE)).expectCreated();
        catalogs.findByCode(CODE).expectOk().expect("code", CODE);
    }

    @Test
    @DisplayName("QUERY - retorna não encontrado para código inexistente")
    void consultaCatalogoInexistente() {
        catalogs.findByCode("UNKNOWN_CATALOG").expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige autorização para consultar catálogo")
    void exigeAutorizacaoParaConsultar() {
        authorization.forbidden();
        catalogs.findByCode(CODE).expectForbidden();
    }
}
