package br.com.itau.portalmanager.workspace.input.web.catalog.publisherscope;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.publisherscope.PublisherScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.publisherscope.PublisherScopeTypeService;
import br.com.itau.portalmanager.workspace.input.web.catalog.support.OwnerCatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/publisher-scope-type")
public class PublisherScopeTypeController extends OwnerCatalogController<PublisherScopeType> {

    public PublisherScopeTypeController(PublisherScopeTypeService service) {
        super(service);
    }
}
