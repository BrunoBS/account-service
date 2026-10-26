package br.com.portalmanager.platform.workspace.integration.feature.onboarding;

import br.com.portalmanager.platform.workspace.integration.feature.onboarding.support.OnboardingClient;
import br.com.portalmanager.platform.workspace.integration.feature.onboarding.support.OnboardingScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OnboardingFindProgressIntegrationTest extends BaseIntegrationTest {
    @Autowired private OnboardingClient onboarding;
    @Autowired private OnboardingScenario scenario;
    private String workspace;

    @BeforeEach void prepare() { workspace = scenario.setup(); }

    @Test @DisplayName("QUERY - apresenta as fases de onboarding do workspace")
    void apresentaProgressoDoWorkspace() {
        onboarding.findProgress(workspace).expectOk()
                .expect("workspaceIdentifier", workspace)
                .expect("completed", false)
                .expectSize("phases", 4);
    }

    @Test @DisplayName("QUERY - retorna não encontrado para workspace inexistente")
    void naoEncontraWorkspaceInexistente() {
        onboarding.findProgress("missing-workspace").expectNotFound();
    }
}
