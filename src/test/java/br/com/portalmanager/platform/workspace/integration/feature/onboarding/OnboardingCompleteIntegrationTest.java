package br.com.portalmanager.platform.workspace.integration.feature.onboarding;

import br.com.portalmanager.platform.workspace.integration.feature.onboarding.support.OnboardingClient;
import br.com.portalmanager.platform.workspace.integration.feature.onboarding.support.OnboardingScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OnboardingCompleteIntegrationTest extends BaseIntegrationTest {
    @Autowired private OnboardingClient onboarding;
    @Autowired private OnboardingScenario scenario;
    @Test @DisplayName("VALIDATION - impede concluir onboarding antes de completar as fases")
    void impedeConclusaoAntesDeCompletarAsFases() {
        String workspace = scenario.setup();
        onboarding.complete(workspace).expectBadRequest();
        onboarding.findProgress(workspace).expectOk().expect("completed", false);
    }

    @Test @DisplayName("UPDATE - conclui onboarding quando todas as fases foram completadas")
    void concluiOnboardingPronto() {
        String workspace = scenario.readyToComplete();

        onboarding.complete(workspace).expectNoContent();
        onboarding.findProgress(workspace).expectOk().expect("completed", true);
    }
}
