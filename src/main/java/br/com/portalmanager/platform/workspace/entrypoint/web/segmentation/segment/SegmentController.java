package br.com.portalmanager.platform.workspace.entrypoint.web.segmentation.segment;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.web.segmentation.segment.request.CreateSegmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.segmentation.segment.request.UpdateSegmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.segmentation.segment.response.SegmentResponse;
import br.com.portalmanager.platform.workspace.feature.segmentation.facade.segment.SegmentFacade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/segmentation/segments")
public class SegmentController {
    private final SegmentFacade facade;

    public SegmentController(SegmentFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<SegmentResponse> create(
            AuthorizationContext authorizationContext, @RequestBody CreateSegmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SegmentResponse.from(facade.create(authorizationContext, request.toInput())));
    }

    @GetMapping("/{segmentIdentifier}")
    public SegmentResponse findByIdentifier(
            AuthorizationContext authorizationContext, @PathVariable String segmentIdentifier) {
        return SegmentResponse.from(
                facade.findByIdentifier(authorizationContext, segmentIdentifier));
    }

    @GetMapping
    public Page<SegmentResponse> findAll(
            AuthorizationContext authorizationContext,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String lifecycle,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return facade.findAll(authorizationContext, code, name, lifecycle, pageable)
                .map(SegmentResponse::from);
    }

    @PutMapping("/{segmentIdentifier}")
    public SegmentResponse update(
            AuthorizationContext authorizationContext,
            @PathVariable String segmentIdentifier,
            @RequestBody UpdateSegmentRequest request) {
        return SegmentResponse.from(
                facade.update(authorizationContext, segmentIdentifier, request.toInput()));
    }

    @PatchMapping("/{segmentIdentifier}/activate")
    public SegmentResponse activate(
            AuthorizationContext authorizationContext, @PathVariable String segmentIdentifier) {
        return SegmentResponse.from(facade.activate(authorizationContext, segmentIdentifier));
    }

    @PatchMapping("/{segmentIdentifier}/inactivate")
    public SegmentResponse inactivate(
            AuthorizationContext authorizationContext, @PathVariable String segmentIdentifier) {
        return SegmentResponse.from(facade.inactivate(authorizationContext, segmentIdentifier));
    }
}
