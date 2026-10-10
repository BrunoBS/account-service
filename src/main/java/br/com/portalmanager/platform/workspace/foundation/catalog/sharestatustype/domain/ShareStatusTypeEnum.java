package br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain;

import br.com.portalmanager.platform.library.catalog.model.CatalogEnum;
import java.util.List;

public enum ShareStatusTypeEnum implements CatalogEnum<ShareStatusTypeEnum> {
    PENDING {
        @Override public List<ShareStatusTypeEnum> nextStatus() { return List.of(APPROVED, REJECTED); }
        @Override public ShareActor allowedActor() { return ShareActor.DESTINATION; }
    },
    APPROVED {
        @Override public List<ShareStatusTypeEnum> nextStatus() { return List.of(REVOKED); }
        @Override public ShareActor allowedActor() { return ShareActor.DESTINATION; }
    },
    REJECTED {
        @Override public List<ShareStatusTypeEnum> nextStatus() { return List.of(PENDING); }
        @Override public ShareActor allowedActor() { return ShareActor.ORIGIN; }
    },
    REVOKED {
        @Override public List<ShareStatusTypeEnum> nextStatus() { return List.of(PENDING); }
        @Override public ShareActor allowedActor() { return ShareActor.ORIGIN; }
    };

    public abstract List<ShareStatusTypeEnum> nextStatus();
    public ShareActor allowedActor() { return null; }
    public boolean canTransitionTo(ShareStatusTypeEnum nextStatus) {
        return nextStatus != null && nextStatus().contains(nextStatus);
    }
    public boolean isWaiting() { return this == PENDING; }
    public enum ShareActor { ORIGIN, DESTINATION, BOTH }
}
