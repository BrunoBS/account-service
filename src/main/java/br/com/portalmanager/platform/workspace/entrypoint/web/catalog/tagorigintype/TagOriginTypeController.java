package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.tagorigintype;

import br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.domain.TagOriginType;
import br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.usecase.TagOriginTypeService;
import br.com.portalmanager.platform.catalog.web.CatalogController;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tag-origin-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class TagOriginTypeController extends CatalogController<TagOriginType> {

    public TagOriginTypeController(TagOriginTypeService service) {
        super(service);
    }
}
