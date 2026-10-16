package br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.validation.participant;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.ResourceSharingMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;

import org.springframework.stereotype.Component;

@Component
public class ResourceSharingTransitionPolicy {

    public void requireTransition(ShareStatusTypeCode current, ShareStatusTypeEnum next) {
        ShareStatusTypeEnum currentStatus;
        try {
            currentStatus = ShareStatusTypeEnum.valueOf(current.value());
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(ResourceSharingMessageKeys.STATE_INVALID);
        }
        if (!currentStatus.canTransitionTo(next))
            throw new ValidationException(ResourceSharingMessageKeys.STATE_INVALID);
    }
}
