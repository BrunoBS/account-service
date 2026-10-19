package br.com.portalmanager.platform.workspace.entrypoint.servicecontext.microservice;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.microservice.request.CreateMicroserviceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.microservice.request.UpdateMicroserviceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.microservice.response.MicroserviceResponse;
import br.com.portalmanager.platform.workspace.feature.servicecontext.facade.microservice.MicroserviceFacade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/service-contexts/microservices")
public class MicroserviceController {

    private final MicroserviceFacade facade;

    public MicroserviceController(MicroserviceFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<MicroserviceResponse> create(
            AuthorizationContext authorizationContext,
            @RequestBody CreateMicroserviceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        MicroserviceResponse.from(
                                facade.create(authorizationContext, request.toInput())));
    }

    @GetMapping("/{microserviceIdentifier}")
    public MicroserviceResponse findByIdentifier(
            AuthorizationContext authorizationContext,
            @PathVariable String microserviceIdentifier) {
        return MicroserviceResponse.from(
                facade.findByIdentifier(authorizationContext, microserviceIdentifier));
    }

    @GetMapping
    public Page<MicroserviceResponse> findAll(
            AuthorizationContext authorizationContext,
            @RequestParam(required = false) String lifecycle,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return facade.findAll(authorizationContext, lifecycle, pageable)
                .map(MicroserviceResponse::from);
    }

    @PutMapping("/{microserviceIdentifier}")
    public MicroserviceResponse update(
            AuthorizationContext authorizationContext,
            @PathVariable String microserviceIdentifier,
            @RequestBody UpdateMicroserviceRequest request) {
        return MicroserviceResponse.from(
                facade.update(authorizationContext, microserviceIdentifier, request.toInput()));
    }

    @PatchMapping("/{microserviceIdentifier}/activate")
    public MicroserviceResponse activate(
            AuthorizationContext authorizationContext,
            @PathVariable String microserviceIdentifier) {
        return MicroserviceResponse.from(
                facade.activate(authorizationContext, microserviceIdentifier));
    }

    @PatchMapping("/{microserviceIdentifier}/inactivate")
    public MicroserviceResponse inactivate(
            AuthorizationContext authorizationContext,
            @PathVariable String microserviceIdentifier) {
        return MicroserviceResponse.from(
                facade.inactivate(authorizationContext, microserviceIdentifier));
    }

    @DeleteMapping("/{microserviceIdentifier}")
    public ResponseEntity<Void> purge(
            AuthorizationContext authorizationContext,
            @PathVariable String microserviceIdentifier) {
        facade.purge(authorizationContext, microserviceIdentifier);
        return ResponseEntity.noContent().build();
    }
}
