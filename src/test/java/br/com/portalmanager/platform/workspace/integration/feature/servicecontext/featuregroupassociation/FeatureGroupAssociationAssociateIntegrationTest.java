package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroupassociation;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroupassociation.support.FeatureGroupAssociationClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroupassociation.support.FeatureGroupAssociationScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Feature Group Association Associate")
class FeatureGroupAssociationAssociateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private FeatureGroupAssociationScenario scenario;
    @Autowired private FeatureGroupAssociationClient associations;
    @Autowired private FeatureGroupClient groups;
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("ASSOCIATE - associa feature ao grupo e permite repetir a operação")
    void associaFeatureIdempotentemente() {
        associations.associate(scenario.groupIdentifier(), scenario.featureIdentifier())
                .expectOk().expect("identifier", scenario.featureIdentifier());
        associations.associate(scenario.groupIdentifier(), scenario.featureIdentifier())
                .expectOk().expect("identifier", scenario.featureIdentifier());
        groups.findAll().feature(scenario.featureIdentifier()).execute().expectOk()
                .expectSize("content", 1).expect("content[0].identifier", scenario.groupIdentifier());
    }

    @Test @DisplayName("VALIDATION - rejeita grupo ou feature inexistente e grupo inativo")
    void rejeitaAssociacaoInvalida() {
        associations.associate("00000000-0000-0000-0000-000000000000", scenario.featureIdentifier()).expectNotFound();
        associations.associate(scenario.groupIdentifier(), "00000000-0000-0000-0000-000000000000").expectNotFound();
        groups.inactivate(scenario.groupIdentifier()).expectOk();
        associations.associate(scenario.groupIdentifier(), scenario.featureIdentifier()).expectBadRequest();
    }

    @Test @DisplayName("AUTH - exige autorização de owner para associar uma feature")
    void exigeAutorizacaoDeOwner() {
        authorization.forbidden();
        associations.associate(scenario.groupIdentifier(), scenario.featureIdentifier()).expectForbidden();
        authorization.verifyCalledWithPolicy("OWNER");
    }
}
