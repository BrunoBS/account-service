package br.com.portalmanager.platform.workspace.integration.core.environment.support;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogScenario;
import br.com.portalmanager.platform.workspace.integration.core.environment.compatibility.support.EnvironmentTypeCompatibilityClient;
import br.com.portalmanager.platform.workspace.integration.core.environment.compatibility.support.EnvironmentTypeCompatibilityFactory;
import br.com.portalmanager.platform.workspace.integration.core.environment.type.support.EnvironmentTypeBuilder;
import br.com.portalmanager.platform.workspace.integration.core.environment.type.support.EnvironmentTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.authorizationtype.support.AuthorizationTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingPhaseFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaScenario;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceClient;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceFactory;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceScenario;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Prepares the HTTP contracts, catalog dependencies, authorization and workspace for environment tests. */
@Component
public final class EnvironmentScenario implements TestScenario<EnvironmentScenario> {
    private final AuthorizationMock authorization;
    private final SchemaScenario schemas;
    private final EnvironmentTypeClient types;
    private final EnvironmentTypeCompatibilityClient compatibilities;
    private final EnvironmentTypeCompatibilityFactory compatibilityFactory = new EnvironmentTypeCompatibilityFactory();
    private final AuthorizationTypeClient authorizationTypes;
    private final WorkspaceTypeClient workspaceTypes;
    private final OnboardingTypeClient onboardingTypes;
    private final WorkspaceClient workspaces;
    private final SchemaFactory schemaFactory = new SchemaFactory();
    private final WorkspaceTypeFactory workspaceTypeFactory = new WorkspaceTypeFactory();
    private final OnboardingPhaseFactory onboardingFactory = new OnboardingPhaseFactory();
    private final WorkspaceFactory workspaceFactory = new WorkspaceFactory();
    private String workspaceIdentifier;

    public EnvironmentScenario(AuthorizationMock authorization, SchemaScenario schemas,
            EnvironmentTypeClient types, EnvironmentTypeCompatibilityClient compatibilities,
            AuthorizationTypeClient authorizationTypes, WorkspaceTypeClient workspaceTypes,
            OnboardingTypeClient onboardingTypes, WorkspaceClient workspaces) {
        this.authorization=Objects.requireNonNull(authorization); this.schemas=Objects.requireNonNull(schemas);
        this.types=Objects.requireNonNull(types); this.compatibilities=Objects.requireNonNull(compatibilities);
        this.authorizationTypes=Objects.requireNonNull(authorizationTypes); this.workspaceTypes=Objects.requireNonNull(workspaceTypes);
        this.onboardingTypes=Objects.requireNonNull(onboardingTypes); this.workspaces=Objects.requireNonNull(workspaces);
    }

    @Override public EnvironmentScenario setup() {
        authorization.reset();
        authorization.allow(session -> session.userName("owner-it").groups("PM5_OWNER"));
        new WorkspaceScenario(authorization, workspaceTypes, workspaceTypeFactory, onboardingTypes, onboardingFactory, schemas).setup();
        schemas.bindResource("ENVIRONMENT", "environment", schemaFactory.resource("environment-resource", "Environment", "Environment payload", "Environment v1", "environment.json"));
        schemas.bindResource("ENVIRONMENT", "environment-type", schemaFactory.resource("environment-type-resource", "Environment Type", "Environment type payload", "Environment Type v1", "environment-type.json"));
        schemas.bindResource("ENVIRONMENT", "environment-type-compatibility", schemaFactory.resource("environment-type-compatibility-resource", "Environment Type Compatibility", "Compatibility payload", "Compatibility v1", "environment-type-compatibility.json"));
        ensureAuthorizationTypes();
        ensureEnvironmentTypes();
        types.list("ACTIVE").expectOk().expectSize("",4);
        ensureCompatibilities();
        workspaceIdentifier=workspaces.create(workspaceFactory.manager("Environment test workspace"))
                .expectCreated().extractBody().identifier();
        return this;
    }

    public String workspaceIdentifier(){return workspaceIdentifier;}

    public void prepareResourceSharingEnvironments() {
        schemas.bindResourceIfMissing("ENVIRONMENT", "environment", schemaFactory.resource(
                "environment-resource", "Environment", "Environment payload", "Environment v1", "environment.json"));
        schemas.bindResourceIfMissing("ENVIRONMENT", "environment-type", schemaFactory.resource(
                "environment-type-resource", "Environment Type", "Environment type payload", "Environment Type v1", "environment-type.json"));
        schemas.bindResourceIfMissing("ENVIRONMENT", "environment-type-compatibility", schemaFactory.resource(
                "environment-type-compatibility-resource", "Environment Type Compatibility", "Compatibility payload", "Compatibility v1", "environment-type-compatibility.json"));
        ensureAuthorizationTypes();
        ensureEnvironmentTypes();
        ensureCompatibilities();
    }

    private void ensureAuthorizationTypes() {
        List.of("DEV","TST","ADM").forEach(code -> {
            var existing=authorizationTypes.findByCode(code);
            if(existing.statusCode()==404) authorizationTypes.create(new CatalogDTO(code,code,"Environment authorization",1,JsonMapper.builder().build().createObjectNode())).expectCreated();
            else existing.expect2xx();
        });
    }

    private void ensureEnvironmentTypes() {
        List.of(EnvironmentTypeBuilder.builder().withCode("DEFAULT").withName("Default").withDescription("Global environment").withRootAllowed(true).withAllowsChildren(true).withWorkspaceRequired(false).withDisplayOrder(1).build(),
                EnvironmentTypeBuilder.builder().withCode("CUSTOM").withName("Custom").withDescription("Workspace environment").withRootAllowed(true).withAllowsChildren(true).withWorkspaceRequired(true).withDisplayOrder(2).build(),
                EnvironmentTypeBuilder.builder().withCode("SHARD").withName("Shard").withDescription("Workspace shard").withRootAllowed(false).withAllowsChildren(true).withWorkspaceRequired(true).withDisplayOrder(3).build(),
                EnvironmentTypeBuilder.builder().withCode("CELL").withName("Cell").withDescription("Terminal environment").withRootAllowed(false).withAllowsChildren(false).withWorkspaceRequired(true).withDisplayOrder(4).build())
                .forEach(request -> {
                    var existing=types.find(request.code());
                    if(existing.statusCode()==404) types.create(request).expectCreated(); else existing.expect2xx();
                });
    }

    private void ensureCompatibilities() {
        List.of(compatibilityFactory.validDefaultToShard(),
                compatibilityFactory.validCustomToShard(),
                compatibilityFactory.validShardToCell())
                .forEach(request -> compatibilities.create(request));
    }
}
