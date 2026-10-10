package br.com.portalmanager.platform.workspace.feature.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.workspace.feature.shared.domain.contract.SharedContract;
import br.com.portalmanager.platform.workspace.feature.shared.domain.participation.SharedEnvironmentMapping;
import br.com.portalmanager.platform.workspace.feature.shared.domain.participation.SharedParticipation;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;

class SharedParticipationTest {

    @Test
    void requestingAgainReusesTheLinkButClearsApprovalConfiguration() {
        LocalDateTime now = LocalDateTime.now();
        SharedContract contract = new SharedContract(1L, 2L, 3L, null, now);
        SharedParticipation participation = new SharedParticipation(contract, 3L, 4L, now);
        participation.approve(PublicationModeTypeCode.of(PublicationModeTypeEnum.MANUAL), now);
        LinkedHashSet<SharedEnvironmentMapping> mappings = new LinkedHashSet<>();
        mappings.add(new SharedEnvironmentMapping(5L, 6L, now));
        participation.replaceMappings(mappings, now);

        String identifier = participation.getIdentifier();
        participation.revoke(now);
        participation.requestAgain(now);

        assertThat(participation.getIdentifier()).isEqualTo(identifier);
        assertThat(participation.getStatus().value()).isEqualTo(ShareStatusTypeEnum.PENDING.name());
        assertThat(participation.getPublicationMode()).isNull();
        assertThat(participation.getMappings()).isEmpty();
    }
}
