package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Feature Group Create")
class FeatureGroupCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private FeatureGroupScenario scenario;
    @Autowired private FeatureGroupClient groups;
    private final FeatureGroupFactory factory = new FeatureGroupFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("CREATE - cria e consulta grupo ativo")
    void criaGrupo() {
        var created = groups.create(factory.valid()).expectCreated().expect("lifecycle", "ACTIVE").extractBody();
        groups.findByIdentifier(created.identifier()).expectOk().expect("code", created.code());
    }

    @Test @DisplayName("VALIDATION - rejeita campo obrigatório, código inválido e versão no cadastro")
    void rejeitaDadosInvalidos() {
        groups.create(factory.withoutName()).expectBadRequest();
        groups.create(factory.withoutCode()).expectBadRequest();
        groups.create(factory.withInvalidCode()).expectBadRequest();
        groups.create(factory.withVersion()).expectBadRequest().expectContainsItem("details.field", "version");
    }

    @Test @DisplayName("VALIDATION - rejeita código e nome duplicados")
    void rejeitaDuplicidade() {
        var existing = factory.valid();
        groups.create(existing).expectCreated();
        groups.create(factory.withCode(existing.code())).expectBadRequest().expectContainsItem("details.field", "code");
        groups.create(factory.withName(existing.name())).expectBadRequest().expectContainsItem("details.field", "name");
    }

    @Test @DisplayName("AUTH - exige autorização de owner para criar grupo")
    void exigeAutorizacaoDeOwner() {
        authorization.forbidden();
        groups.create(factory.valid()).expectForbidden();
        authorization.verifyCalledWithPolicy("OWNER");
    }
}
