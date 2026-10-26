package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroupassociation;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroupassociation.support.FeatureGroupAssociationClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroupassociation.support.FeatureGroupAssociationScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Feature Group Association Remove")
class FeatureGroupAssociationRemoveIntegrationTest extends BaseIntegrationTest {
    @Autowired private FeatureGroupAssociationScenario scenario;
    @Autowired private FeatureGroupAssociationClient associations;
    @Autowired private FeatureGroupClient groups;
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("REMOVE - remove associação existente e confirma a consulta vazia")
    void removeAssociacao() {
        associations.associate(scenario.groupIdentifier(), scenario.featureIdentifier()).expectOk();
        associations.remove(scenario.groupIdentifier(), scenario.featureIdentifier()).expectNoContent();
        groups.findAll().feature(scenario.featureIdentifier()).execute().expectOk().expectSize("content", 0);
    }

    @Test @DisplayName("REMOVE - é idempotente para associação ausente e rejeita referências inexistentes")
    void removeAssociacaoAusente() {
        associations.remove(scenario.groupIdentifier(), scenario.featureIdentifier()).expectNoContent();
        associations.remove("00000000-0000-0000-0000-000000000000", scenario.featureIdentifier()).expectNotFound();
        associations.remove(scenario.groupIdentifier(), "00000000-0000-0000-0000-000000000000").expectNotFound();
    }
}
