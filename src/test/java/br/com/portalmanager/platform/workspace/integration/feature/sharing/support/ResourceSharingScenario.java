package br.com.portalmanager.platform.workspace.integration.feature.sharing.support;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationClient;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationFactory;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceClient;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceFactory;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceScenario;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingPhaseFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.publicationmodetype.support.PublicationModeTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.sharestatustype.support.ShareStatusTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype.support.ApplicationScopeTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype.support.CatalogFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogScenario;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaScenario;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Creates sharing prerequisites through the public HTTP clients used by their owning modules. */
@Component
public final class ResourceSharingScenario implements TestScenario<ResourceSharingScenario.Data> {
    private final AuthorizationMock authorization;
    private final WorkspaceClient workspaces;
    private final WorkspaceTypeClient workspaceTypes;
    private final OnboardingTypeClient onboardingTypes;
    private final SchemaScenario schemas;
    private final ApplicationScopeTypeClient applicationScopes;
    private final PublicationModeTypeClient publicationModes;
    private final ShareStatusTypeClient shareStatuses;
    private final ApplicationClient applications;
    private final MicroserviceClient microservices;
    private final FeatureClient features;
    private final ContractClient contracts;

    public ResourceSharingScenario(AuthorizationMock authorization, WorkspaceClient workspaces,
            WorkspaceTypeClient workspaceTypes, OnboardingTypeClient onboardingTypes,
            ApplicationScopeTypeClient applicationScopes, PublicationModeTypeClient publicationModes,
            ShareStatusTypeClient shareStatuses, SchemaScenario schemas, ApplicationClient applications,
            MicroserviceClient microservices, FeatureClient features, ContractClient contracts) {
        this.authorization=Objects.requireNonNull(authorization); this.workspaces=Objects.requireNonNull(workspaces);
        this.workspaceTypes=Objects.requireNonNull(workspaceTypes); this.onboardingTypes=Objects.requireNonNull(onboardingTypes);
        this.applicationScopes=Objects.requireNonNull(applicationScopes); this.schemas=Objects.requireNonNull(schemas);
        this.publicationModes=Objects.requireNonNull(publicationModes);
        this.shareStatuses=Objects.requireNonNull(shareStatuses);
        this.applications=Objects.requireNonNull(applications); this.microservices=Objects.requireNonNull(microservices);
        this.features=Objects.requireNonNull(features); this.contracts=Objects.requireNonNull(contracts);
    }

    @Override public Data setup() {
        new WorkspaceScenario(authorization, workspaceTypes, new WorkspaceTypeFactory(),
                onboardingTypes, new OnboardingPhaseFactory(), schemas).setup();
        CatalogScenario.builder()
                .withApplicationScopeTypes(applicationScopes,
                        java.util.List.of(new CatalogFactory().valid("BACKEND"), new CatalogFactory().valid("SHARED")))
                .withPublicationModes(publicationModes, java.util.List.of(
                        new CatalogFactory().valid("AUTOMATIC"), new CatalogFactory().valid("MANUAL")))
                .withShareStatuses(shareStatuses, java.util.List.of(
                        new CatalogFactory().valid("PENDING"), new CatalogFactory().valid("APPROVED"),
                        new CatalogFactory().valid("REJECTED"), new CatalogFactory().valid("REVOKED")))
                .setup();
        SchemaFactory schemaFactory = new SchemaFactory();
        schemas.bindResource("SERVICE_CONTEXT", "microservice", schemaFactory.resource(
                "microservice-resource", "Microservice Resource", "Microservice test schema",
                "Integration test v1", "microservice.json"));
        schemas.bindResource("SERVICE_CONTEXT", "feature", schemaFactory.resource(
                "feature-resource", "Feature Resource", "Feature test schema",
                "Integration test v1", "feature.json"));
        schemas.bindResource("APPLICATION", "application", schemaFactory.applicationResourceSchema());
        WorkspaceFactory workspaceFactory = new WorkspaceFactory();
        String ownerWorkspace = workspaces.create(workspaceFactory.manager("Sharing owner workspace")).expectCreated().extractBody().identifier();
        String participantWorkspace = workspaces.create(workspaceFactory.manager("Sharing participant workspace")).expectCreated().extractBody().identifier();
        ApplicationFactory applicationFactory = new ApplicationFactory();
        String ownerApplication = applications.create(ownerWorkspace, applicationFactory.withApplicationScope("SHARED")).expectCreated().extractBody().identifier();
        String participantApplication = applications.create(participantWorkspace, applicationFactory.withApplicationScope("BACKEND")).expectCreated().extractBody().identifier();
        String microservice = microservices.create(new MicroserviceFactory().valid("sharing-service", "Sharing service")).expectCreated().extractBody().identifier();
        String feature = features.create(new FeatureFactory().shareable(microservice)).expectCreated().extractBody().identifier();
        return new Data(ownerWorkspace, ownerApplication, participantWorkspace, participantApplication, feature);
    }

    public ContractResponse createContract(Data data) {
        return contracts.create(data.ownerWorkspace(), data.ownerApplication(), new ContractFactory().valid(data.feature())).expectCreated().extractBody();
    }

    public String requestParticipation(Data data, String contractIdentifier, ParticipantClient participants) {
        return participants.request(data.participantWorkspace(), data.participantApplication(), contractIdentifier)
                .expectCreated().extractBody().identifier();
    }

    public record Data(String ownerWorkspace, String ownerApplication, String participantWorkspace,
            String participantApplication, String feature) {}
}
