package br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.validation.participant.ResourceSharingTransitionPolicy;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;

import org.junit.jupiter.api.Test;

class ResourceSharingTransitionPolicyTest {

    private final ResourceSharingTransitionPolicy policy = new ResourceSharingTransitionPolicy();

    @Test
    void allowsOnlyDocumentedParticipantTransitions() {
        assertThatCode(
                        () ->
                                policy.requireTransition(
                                        code(ShareStatusTypeEnum.PENDING),
                                        ShareStatusTypeEnum.APPROVED))
                .doesNotThrowAnyException();
        assertThatCode(
                        () ->
                                policy.requireTransition(
                                        code(ShareStatusTypeEnum.PENDING),
                                        ShareStatusTypeEnum.REJECTED))
                .doesNotThrowAnyException();
        assertThatCode(
                        () ->
                                policy.requireTransition(
                                        code(ShareStatusTypeEnum.APPROVED),
                                        ShareStatusTypeEnum.REVOKED))
                .doesNotThrowAnyException();
        assertThatCode(
                        () ->
                                policy.requireTransition(
                                        code(ShareStatusTypeEnum.REJECTED),
                                        ShareStatusTypeEnum.PENDING))
                .doesNotThrowAnyException();
        assertThatCode(
                        () ->
                                policy.requireTransition(
                                        code(ShareStatusTypeEnum.REVOKED),
                                        ShareStatusTypeEnum.PENDING))
                .doesNotThrowAnyException();
    }

    @Test
    void refusesTransitionsOutsideTheWorkflow() {
        assertThatThrownBy(
                        () ->
                                policy.requireTransition(
                                        code(ShareStatusTypeEnum.PENDING),
                                        ShareStatusTypeEnum.REVOKED))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(
                        () ->
                                policy.requireTransition(
                                        code(ShareStatusTypeEnum.APPROVED),
                                        ShareStatusTypeEnum.PENDING))
                .isInstanceOf(ValidationException.class);
    }

    private ShareStatusTypeCode code(ShareStatusTypeEnum status) {
        return ShareStatusTypeCode.of(status);
    }
}
