package br.com.itau.portalmanager.workspace.foundation.catalog.domain.sharestatus;

import br.com.portalmanager.platform.catalog.model.CatalogEnum;

public enum ShareStatusTypeEnum implements CatalogEnum<ShareStatusTypeEnum> {
    WAITING_DESTINATION_APPROVAL {
        @Override public java.util.List<ShareStatusTypeEnum> nextStatus() { return java.util.List.of(APPROVED, REJECTED); }
        @Override public ShareActor allowedActor() { return ShareActor.DESTINATION; }
    },
    WAITING_SOURCE_APPROVAL {
        @Override public java.util.List<ShareStatusTypeEnum> nextStatus() { return java.util.List.of(APPROVED, REJECTED); }
        @Override public ShareActor allowedActor() { return ShareActor.ORIGIN; }
    },
    APPROVED {
        @Override public java.util.List<ShareStatusTypeEnum> nextStatus() { return java.util.List.of(CANCELLED); }
        @Override public ShareActor allowedActor() { return ShareActor.BOTH; }
    },
    REJECTED {
        @Override public java.util.List<ShareStatusTypeEnum> nextStatus() { return java.util.List.of(); }
    },
    CANCELLED {
        @Override public java.util.List<ShareStatusTypeEnum> nextStatus() { return java.util.List.of(); }
    },
    NOT_REQUESTED {
        @Override public java.util.List<ShareStatusTypeEnum> nextStatus() { return java.util.List.of(); }
    };

    public abstract java.util.List<ShareStatusTypeEnum> nextStatus();

    public ShareActor allowedActor() { return null; }

    public boolean canTransitionTo(ShareStatusTypeEnum nextStatus) {
        return nextStatus != null && nextStatus().contains(nextStatus);
    }

    public boolean isWaiting() {
        return this == WAITING_DESTINATION_APPROVAL || this == WAITING_SOURCE_APPROVAL;
    }

    public enum ShareActor {
        ORIGIN,
        DESTINATION,
        BOTH
    }
}
