package br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.facade;

import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.domain.PublisherType;
import br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.usecase.PublisherTypeService;

import org.springframework.stereotype.Component;

@Component
public class PublisherTypeFacade extends AbstractCatalogFacade<PublisherType> {

    public PublisherTypeFacade(PublisherTypeService service) {
        super(service);
    }
}
