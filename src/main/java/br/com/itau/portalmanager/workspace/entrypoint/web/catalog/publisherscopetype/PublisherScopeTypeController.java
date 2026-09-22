package br.com.itau.portalmanager.workspace.entrypoint.web.catalog.publisherscopetype;

import br.com.itau.portalmanager.workspace.foundation.catalog.publisherscopetype.domain.PublisherScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.publisherscopetype.usecase.PublisherScopeTypeService;
import br.com.portalmanager.platform.catalog.web.CatalogController;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/publisher-scope-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class PublisherScopeTypeController extends CatalogController<PublisherScopeType> {

    public PublisherScopeTypeController(PublisherScopeTypeService service) {
        super(service);
    }
}
