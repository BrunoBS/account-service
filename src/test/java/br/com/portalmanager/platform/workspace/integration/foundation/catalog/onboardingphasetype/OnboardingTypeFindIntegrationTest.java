package br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype;

import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingTypeClient;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogTestDataFactory;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - onboarding find")
class OnboardingTypeFindIntegrationTest extends BaseIntegrationTest {
    private static final String CODE = "WORKSPACE_REGISTRATION";

    @Autowired private AuthorizationMock authorization;
    @Autowired private OnboardingTypeClient catalogs;
    private final CatalogTestDataFactory factory = new CatalogTestDataFactory();

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
