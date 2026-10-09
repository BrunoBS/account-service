package br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature;

import br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.response.FeatureContextResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.request.CreateFeatureRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.request.UpdateFeatureRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.response.FeatureResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import br.com.portalmanager.platform.workspace.feature.platform.facade.FeatureFacade;

@RestController
@RequestMapping("/api/v1/platform/features")
public class FeatureController {

    private final FeatureFacade facade;
    public FeatureController(FeatureFacade facade) { this.facade = facade; }

    @PostMapping
    public ResponseEntity<FeatureResponse> create(@RequestBody CreateFeatureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facade.create(request));
    }

    @GetMapping("/{identifier}")
    public FeatureResponse findByIdentifier(@PathVariable String identifier) {
        return facade.findByIdentifier(identifier);
    }

    @GetMapping
    public List<FeatureResponse> findAll(@RequestParam(required = false) String contextCode) {
        return facade.findAll(contextCode);
    }

    @GetMapping("/{identifier}/contexts")
    public List<FeatureContextResponse> findContexts(@PathVariable String identifier) {
        return facade.findContexts(identifier);
    }

    @PutMapping("/{identifier}")
    public FeatureResponse update(
            @PathVariable String identifier,
            @RequestBody UpdateFeatureRequest request
    ) {
        return facade.update(identifier, request);
    }

    @PatchMapping("/{identifier}/activate")
    public FeatureResponse activate(@PathVariable String identifier) {
        return facade.activate(identifier);
    }

    @PatchMapping("/{identifier}/inactivate")
    public FeatureResponse inactivate(@PathVariable String identifier) {
        return facade.inactivate(identifier);
    }

    @PostMapping("/{identifier}/contexts/{contextIdentifier}")
    public FeatureResponse associateContext(
            @PathVariable String identifier,
            @PathVariable String contextIdentifier
    ) {
        return facade.associateContext(identifier, contextIdentifier);
    }

    @DeleteMapping("/{identifier}/contexts/{contextIdentifier}")
    public FeatureResponse removeContext(
            @PathVariable String identifier,
            @PathVariable String contextIdentifier
    ) {
        return facade.removeContext(identifier, contextIdentifier);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        facade.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
