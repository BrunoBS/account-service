package br.com.portalmanager.platform.workspace.integration.feature.onboarding.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import br.com.portalmanager.platform.workspace.entrypoint.onboarding.response.OnboardingProgressResponse;
import io.restassured.response.ValidatableResponse;

public final class OnboardingClientResponse extends BaseResponse<OnboardingClientResponse> {
    public OnboardingClientResponse(ValidatableResponse response) { super(response); }

    public OnboardingProgressResponse extractBody() {
        return extract(OnboardingProgressResponse.class);
    }
}
