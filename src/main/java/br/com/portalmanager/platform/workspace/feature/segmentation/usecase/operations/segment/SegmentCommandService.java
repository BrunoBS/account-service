package br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.segment;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.segment.Segment;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.segment.SegmentRepository;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment.CreateSegmentInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment.SegmentOutput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment.UpdateSegmentInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.validation.segment.SegmentValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SegmentCommandService {
    private final SegmentRepository repository;
    private final SegmentFinder finder;
    private final SegmentValidator validator;

    public SegmentCommandService(
            SegmentRepository repository, SegmentFinder finder, SegmentValidator validator) {
        this.repository = repository;
        this.finder = finder;
        this.validator = validator;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "SEGMENT_CREATED", resourceType = "SEGMENT")
    public SegmentOutput create(CreateSegmentInput input) {
        validator.validateForCreate(input);
        Segment resource =
                repository.saveAndFlush(
                        new Segment(input.code(), input.name(), LocalDateTime.now()));
        return SegmentOutput.from(resource);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "SEGMENT_UPDATED", resourceType = "SEGMENT")
    public SegmentOutput update(String identifier, UpdateSegmentInput input) {
        Segment resource = finder.findForUpdate(identifier);
        validator.validateForUpdate(resource, input);
        resource.update(input.name(), LocalDateTime.now());
        return SegmentOutput.from(repository.saveAndFlush(resource));
    }

    @Transactional
    @Auditable(action = AuditAction.ACTIVATE, event = "SEGMENT_ACTIVATED", resourceType = "SEGMENT")
    public SegmentOutput activate(String identifier) {
        Segment resource = finder.findForUpdate(identifier);
        resource.activate(LocalDateTime.now());
        return SegmentOutput.from(repository.saveAndFlush(resource));
    }

    @Transactional
    @Auditable(
            action = AuditAction.DEACTIVATE,
            event = "SEGMENT_DEACTIVATED",
            resourceType = "SEGMENT")
    public SegmentOutput inactivate(String identifier) {
        Segment resource = finder.findForUpdate(identifier);
        resource.inactivate(LocalDateTime.now());
        return SegmentOutput.from(repository.saveAndFlush(resource));
    }
}
