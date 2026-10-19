package br.com.portalmanager.platform.workspace.entrypoint.catalog;

import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.domain.TagOriginType;
import br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.facade.TagOriginTypeFacade;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tag-origin-type")
public class TagOriginTypeController extends CatalogController<TagOriginType> {

    public TagOriginTypeController(TagOriginTypeFacade facade) {
        super(facade);
    }
}
