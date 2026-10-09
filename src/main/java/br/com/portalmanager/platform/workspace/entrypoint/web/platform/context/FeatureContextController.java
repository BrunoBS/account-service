package br.com.portalmanager.platform.workspace.entrypoint.web.platform.context;

import br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.request.CreateFeatureContextRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.request.UpdateFeatureContextRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.response.FeatureContextResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import br.com.portalmanager.platform.workspace.feature.platform.facade.context.FeatureContextFacade;

@RestController
@RequestMapping("/api/v1/platform/contexts")
public class FeatureContextController {

    private final FeatureContextFacade facade;
    public FeatureContextController(FeatureContextFacade facade) { this.facade = facade; }
    @PostMapping
    public ResponseEntity<FeatureContextResponse> create(@RequestBody CreateFeatureContextRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(facade.create(request)); }
    @GetMapping("/{identifier}")
    public FeatureContextResponse findByIdentifier(@PathVariable String identifier) { return facade.findByIdentifier(identifier); }
    @GetMapping
    public List<FeatureContextResponse> findAll() { return facade.findAll(); }
    @PutMapping("/{identifier}")
    public FeatureContextResponse update(@PathVariable String identifier, @RequestBody UpdateFeatureContextRequest request) { return facade.update(identifier, request); }
    @PatchMapping("/{identifier}/activate")
    public FeatureContextResponse activate(@PathVariable String identifier) { return facade.activate(identifier); }
    @PatchMapping("/{identifier}/inactivate")
    public FeatureContextResponse inactivate(@PathVariable String identifier) { return facade.inactivate(identifier); }
    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(@PathVariable String identifier) { facade.delete(identifier); return ResponseEntity.noContent().build(); }
}
