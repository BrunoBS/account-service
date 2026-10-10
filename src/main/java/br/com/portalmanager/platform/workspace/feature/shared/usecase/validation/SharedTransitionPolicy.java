package br.com.portalmanager.platform.workspace.feature.shared.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.SharedMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;
import org.springframework.stereotype.Component;

@Component
public class SharedTransitionPolicy {
    public void requireTransition(ShareStatusTypeCode current, ShareStatusTypeEnum next) {
        ShareStatusTypeEnum currentStatus;
        try { currentStatus = ShareStatusTypeEnum.valueOf(current.value()); }
        catch (IllegalArgumentException ex) { throw new ValidationException(SharedMessageKeys.STATE_INVALID); }
        if (!currentStatus.canTransitionTo(next)) throw new ValidationException(SharedMessageKeys.STATE_INVALID);
    }
}
