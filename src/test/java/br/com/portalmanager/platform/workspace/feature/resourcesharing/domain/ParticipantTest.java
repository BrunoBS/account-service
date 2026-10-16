package br.com.portalmanager.platform.workspace.feature.resourcesharing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.contract.Contract;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.participant.EnvironmentMapping;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.participant.Participant;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;

class ParticipantTest {

    @Test
    void requestingAgainReusesTheLinkButClearsApprovalConfiguration() {
        LocalDateTime now = LocalDateTime.now();
        Contract contract = new Contract(1L, 2L, 3L, null, now);
        Participant participant = new Participant(contract, 3L, 4L, now);
        participant.approve(PublicationModeTypeCode.of(PublicationModeTypeEnum.MANUAL), now);
        LinkedHashSet<EnvironmentMapping> mappings = new LinkedHashSet<>();
        mappings.add(new EnvironmentMapping(5L, 6L, now));
        participant.replaceMappings(mappings, now);

        String identifier = participant.getIdentifier();
        participant.revoke(now);
        participant.requestAgain(now);

        assertThat(participant.getIdentifier()).isEqualTo(identifier);
        assertThat(participant.getStatus().value()).isEqualTo(ShareStatusTypeEnum.PENDING.name());
        assertThat(participant.getPublicationMode()).isNull();
        assertThat(participant.getMappings()).isEmpty();
    }
}
