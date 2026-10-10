package br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.facade;

import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeType;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.usecase.PublicationModeTypeService;
import org.springframework.stereotype.Component;

@Component
public class PublicationModeTypeFacade extends AbstractCatalogFacade<PublicationModeType> {
    public PublicationModeTypeFacade(PublicationModeTypeService service) { super(service); }
}
