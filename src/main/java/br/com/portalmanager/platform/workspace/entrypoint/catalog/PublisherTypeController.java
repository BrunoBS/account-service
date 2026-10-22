package br.com.portalmanager.platform.workspace.entrypoint.catalog;

import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.domain.PublisherType;
import br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.facade.PublisherTypeFacade;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/publishers")
public class PublisherTypeController extends CatalogController<PublisherType> {
    public PublisherTypeController(PublisherTypeFacade facade) {
        super(facade);
    }
}
