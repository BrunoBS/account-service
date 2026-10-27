package br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype;

import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingTypeClient;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogTestDataFactory;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - onboarding restore")
class OnboardingTypeRestoreIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("RESTORE - restaura catálogo excluído e confirma persistência")
    void restauraCatalogoExcluido() {
        catalogs.create(factory.valid(CODE)).expectCreated();
        catalogs.delete(CODE).expectNoContent();
        catalogs.restore(CODE).expectOk().expect("code", CODE);
        catalogs.findByCode(CODE).expectOk().expect("code", CODE);
    }

    @Test
    @DisplayName("VALIDATION - não restaura catálogo ativo ou inexistente")
    void rejeitaRestauracaoInvalida() {
        catalogs.create(factory.valid(CODE)).expectCreated();
        catalogs.restore(CODE).expectBadRequest();
        catalogs.restore("UNKNOWN_CATALOG").expectBadRequest();
    }

    @Test
    @DisplayName("AUTH - exige autorização para restaurar catálogo")
    void exigeAutorizacaoParaRestaurar() {
        authorization.forbidden();
        catalogs.restore(CODE).expectForbidden();
    }
}
