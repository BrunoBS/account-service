package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support.FeatureGroupScenario;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroupassociation.support.FeatureGroupAssociationClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceFactory;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Feature Group Purge")
class FeatureGroupPurgeIntegrationTest extends BaseIntegrationTest {
    @Autowired private FeatureGroupScenario scenario;
    @Autowired private FeatureGroupClient groups;
    @Autowired private MicroserviceClient microservices;
    @Autowired private FeatureClient features;
    @Autowired private FeatureGroupAssociationClient associations;
    private final FeatureGroupFactory groupFactory = new FeatureGroupFactory();
    private final MicroserviceFactory microserviceFactory = new MicroserviceFactory();
    private final FeatureFactory featureFactory = new FeatureFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("DELETE - remove grupo sem features associadas")
    void removeGrupoVazio() {
        var created = groups.create(groupFactory.valid()).expectCreated().extractBody();
        groups.purge(created.identifier()).expectNoContent();
        groups.findByIdentifier(created.identifier()).expectNotFound();
    }

    @Test @DisplayName("VALIDATION - impede remover grupo ainda associado a feature")
    void impedeRemoverGrupoComFeature() {
        var microservice = microservices.create(microserviceFactory.valid()).expectCreated().extractBody();
        var feature = features.create(featureFactory.valid(microservice.identifier())).expectCreated().extractBody();
        var group = groups.create(groupFactory.valid()).expectCreated().extractBody();
        associations.associate(group.identifier(), feature.identifier()).expectOk();
        groups.purge(group.identifier()).expectBadRequest();
    }
}
