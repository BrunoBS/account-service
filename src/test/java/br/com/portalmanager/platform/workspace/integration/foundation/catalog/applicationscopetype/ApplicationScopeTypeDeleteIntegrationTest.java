package br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype;

import br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype.support.ApplicationScopeTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype.support.CatalogFactory;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogTestDataFactory;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - application scope delete")
class ApplicationScopeTypeDeleteIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("DELETE - exclui catálogo e deixa de encontrá-lo")
    void excluiCatalogo() {
        catalogs.create(factory.valid(CODE)).expectCreated();
        catalogs.delete(CODE).expectNoContent();
        catalogs.findByCode(CODE).expectNotFound();
    }

    @Test
    @DisplayName("DELETE - retorna não encontrado para catálogo inexistente")
    void excluiCatalogoInexistente() {
        catalogs.delete("UNKNOWN_CATALOG").expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige autorização para excluir catálogo")
    void exigeAutorizacaoParaExcluir() {
        authorization.forbidden();
        catalogs.delete(CODE).expectForbidden();
    }
}
