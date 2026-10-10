package br.com.portalmanager.platform.workspace.feature.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;

class SharedParticipationTest {

    @Test
    void resubmissionReusesTheLinkButClearsApprovalConfiguration() {
        LocalDateTime now = LocalDateTime.now();
        SharedContract contract = new SharedContract("owner-w", "owner-a", "Sharing", null, now);
        SharedParticipation participation = new SharedParticipation(contract, "source-w", "source-a", now);
        participation.approve(PublicationModeTypeCode.of(PublicationModeTypeEnum.MANUAL), now);
        LinkedHashSet<SharedEnvironmentMapping> mappings = new LinkedHashSet<>();
        mappings.add(new SharedEnvironmentMapping("source-env", "destination-env", now));
        participation.replaceMappings(mappings, now);

        String identifier = participation.getIdentifier();
        participation.revoke(now);
        participation.resubmit(now);

        assertThat(participation.getIdentifier()).isEqualTo(identifier);
        assertThat(participation.getStatus().value()).isEqualTo(ShareStatusTypeEnum.PENDING.name());
        assertThat(participation.getPublicationMode()).isNull();
        assertThat(participation.getMappings()).isEmpty();
    }
}
