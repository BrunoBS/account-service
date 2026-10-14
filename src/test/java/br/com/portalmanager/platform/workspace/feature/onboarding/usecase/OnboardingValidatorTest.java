package br.com.portalmanager.platform.workspace.feature.onboarding.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.workspace.feature.onboarding.domain.OnboardingMessageKeys;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.validation.OnboardingValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.usecase.OnboardingPhaseTypeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OnboardingValidatorTest {

    private final OnboardingValidator validator = new OnboardingValidator(mock(OnboardingPhaseTypeService.class));

    @BeforeEach
    @AfterEach
    void clearSession() {
        UserContext.clear();
    }

    @Test
    void rejectsAnAbsentIdentityWithThePlatformMessageKey() {
        assertMissingIdentity();
        UserSession session = new UserSession();
        session.setUserName("   ");
        UserContext.set(session);
        assertMissingIdentity();
    }

    @Test
    void preservesAuthenticatedIdentityAtTheCharacterLimit() {
        UserSession session = new UserSession();
        session.setUserName("1234567890");
        UserContext.set(session);
        assertThat(validator.requireAuthenticatedRequester()).isEqualTo("1234567890");
        session.setUserName("12345678901");
        assertThatThrownBy(validator::requireAuthenticatedRequester)
            .isInstanceOfSatisfying(ApiException.class, failure ->
                assertThat(failure.getMessageKey()).isEqualTo(OnboardingMessageKeys.REQUESTER_INVALID));
    }

    private void assertMissingIdentity() {
        assertThatThrownBy(validator::requireAuthenticatedRequester)
            .isInstanceOfSatisfying(ApiException.class, failure ->
                assertThat(failure.getMessageKey()).isEqualTo(OnboardingMessageKeys.AUTHORIZATION_CONTEXT_UNAVAILABLE));
    }
}
