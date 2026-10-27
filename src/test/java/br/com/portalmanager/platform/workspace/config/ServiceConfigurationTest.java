package br.com.portalmanager.platform.workspace.config;

import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationAutoConfiguration;
import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.config.AuthorizationMode;
import br.com.portalmanager.platform.library.authorization.aop.MockAuthorizationFacadeAspect;
import br.com.portalmanager.platform.library.authorization.aop.AuthorizationFacadeAspect;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThat;

/** Loads the actual service YAML with the Golden authorization autoconfiguration. */
class ServiceConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(PlatformAuthorizationAutoConfiguration.class))
            .withBean(RestClient.Builder.class, RestClient::builder);

    @Test void localExplicitlyAllowsMockWithoutPrivilegedIdentity() {
        runner.withPropertyValues("spring.profiles.active=local").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(MockAuthorizationFacadeAspect.class);
            assertThat(context.getBean(PlatformAuthorizationProperties.class).getMock().getGroups()).containsExactly("GUEST");
        });
    }

    @Test void productionRejectsMock() {
        runner.withPropertyValues("spring.profiles.active=prod", "platform.authorization.mode=MOCK").run(context -> assertThat(context).hasFailed());
    }

    @Test void productionWithoutAuthorizationUrlFailsStartup() {
        runner.withPropertyValues("spring.profiles.active=prod", "platform.authorization.service-url=").run(context -> assertThat(context).hasFailed());
    }

    @Test void noProfileDoesNotActivateLocalOrMock() {
        runner.withPropertyValues("platform.authorization.service-url=").run(context -> assertThat(context).hasFailed());
    }

    @Test void invalidModeFailsStartup() {
        runner.withPropertyValues("spring.profiles.active=prod", "platform.authorization.mode=INVALID").run(context -> assertThat(context).hasFailed());
    }

    @Test void blankModeFailsStartupEvenWhenServiceUrlIsConfigured() {
        runner.withPropertyValues("spring.profiles.active=prod", "platform.authorization.mode=", "platform.authorization.service-url=http://authorization.internal").run(context -> assertThat(context).hasFailed());
    }

    @Test void productionUsesRealAuthorizationAndOperationalLogging() {
        runner.withPropertyValues("spring.profiles.active=prod", "platform.authorization.service-url=http://authorization.internal").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(AuthorizationFacadeAspect.class).doesNotHaveBean(MockAuthorizationFacadeAspect.class);
            assertThat(context.getBean(PlatformAuthorizationProperties.class).getMode()).isEqualTo(AuthorizationMode.REAL);
            assertThat(context.getEnvironment().getProperty("platform.observability.logging.format")).isEqualTo("JSON");
            assertThat(context.getEnvironment().getProperty("platform.observability.logging.levels.br.com.portalmanager.platform")).isEqualTo("INFO");
            assertThat(context.getEnvironment().getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
            assertThat(context.getEnvironment().getProperty("management.endpoint.health.show-details")).isEqualTo("never");
        });
    }
}
