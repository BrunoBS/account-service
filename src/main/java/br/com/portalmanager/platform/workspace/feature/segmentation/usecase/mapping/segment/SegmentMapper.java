package br.com.portalmanager.platform.workspace.feature.segmentation.usecase.mapping.segment;

import br.com.portalmanager.platform.workspace.feature.segmentation.domain.segment.Segment;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment.CreateSegmentInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment.SegmentOutput;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class SegmentMapper {

    public Segment toDomain(CreateSegmentInput input, LocalDateTime now) {
        return new Segment(input.code(), input.name(), now);
    }

    public SegmentOutput toOutput(Segment segment) {
        return new SegmentOutput(
                segment.getVersion(), segment.getIdentifier(), segment.getCode(), segment.getName(),
                segment.getLifecycle().value(), segment.getCreatedAt(), segment.getUpdatedAt());
    }
}
