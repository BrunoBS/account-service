package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.publicationmodetype;

import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeType;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.facade.PublicationModeTypeFacade;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/publication-mode-type")
public class PublicationModeTypeController extends CatalogController<PublicationModeType> {

    public PublicationModeTypeController(PublicationModeTypeFacade facade) {
        super(facade);
    }
}
