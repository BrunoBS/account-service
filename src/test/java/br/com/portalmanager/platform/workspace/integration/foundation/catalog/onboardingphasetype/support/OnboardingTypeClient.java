package br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support;

import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogClient;
import org.springframework.stereotype.Component;

@Component
public final class OnboardingTypeClient extends CatalogClient {
    public OnboardingTypeClient(PlatformRequestSpecificationFactory requests) {
        super(requests, "/api/v1/onboarding-type");
    }
}
