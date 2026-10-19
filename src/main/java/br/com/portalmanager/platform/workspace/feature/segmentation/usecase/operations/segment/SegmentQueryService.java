package br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.segment;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.SegmentationMessageKeys;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.segment.SegmentRepository;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment.SegmentOutput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.validation.SegmentQueryValidation;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.mapping.segment.SegmentMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SegmentQueryService {
    private final SegmentRepository repository;
    private final SegmentMapper mapper;

    public SegmentQueryService(SegmentRepository repository, SegmentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public SegmentOutput findByIdentifier(String identifier) {
        return mapper.toOutput(repository.findByIdentifier(identifier)
                .orElseThrow(() -> new NotFoundException(SegmentationMessageKeys.NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public Page<SegmentOutput> findAll(
            String code, String name, String lifecycle, Pageable pageable) {
        SegmentQueryValidation.validatePage(pageable);
        String normalizedCode = SegmentQueryValidation.normalizeCode(code);
        String normalizedName = SegmentQueryValidation.normalizeName(name);
        return repository.findPage(
                normalizedCode,
                normalizedName,
                SegmentQueryValidation.resolveLifecycle(lifecycle),
                pageable).map(mapper::toOutput);
    }
}
