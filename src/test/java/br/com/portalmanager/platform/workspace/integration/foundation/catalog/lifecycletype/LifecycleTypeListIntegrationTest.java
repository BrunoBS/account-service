package br.com.portalmanager.platform.workspace.integration.foundation.catalog.lifecycletype;

import br.com.portalmanager.platform.workspace.integration.foundation.catalog.lifecycletype.support.LifecycleTypeClient;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogTestDataFactory;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - lifecycle list")
class LifecycleTypeListIntegrationTest extends BaseIntegrationTest {
    private static final String CODE = "INACTIVE";

    @Autowired private AuthorizationMock authorization;
    @Autowired private LifecycleTypeClient catalogs;
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
        catalogs.findAll().expectOk().expectSize("", 2);
        catalogs.findAll(java.util.Map.of("active", "true")).expectOk().expectSize("", 2);
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
