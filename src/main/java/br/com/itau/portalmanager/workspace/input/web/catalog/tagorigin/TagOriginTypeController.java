package br.com.itau.portalmanager.workspace.input.web.catalog.tagorigin;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.tagorigin.TagOriginType;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.tagorigin.TagOriginTypeService;
import br.com.itau.portalmanager.workspace.input.web.catalog.support.OwnerCatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tag-origin-type")
public class TagOriginTypeController extends OwnerCatalogController<TagOriginType> {

    public TagOriginTypeController(TagOriginTypeService service) {
        super(service);
    }
}
