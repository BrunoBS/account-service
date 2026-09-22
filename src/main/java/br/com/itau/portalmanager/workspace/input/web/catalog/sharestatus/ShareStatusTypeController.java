package br.com.itau.portalmanager.workspace.input.web.catalog.sharestatus;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.sharestatus.ShareStatusType;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.sharestatus.ShareStatusTypeService;
import br.com.itau.portalmanager.workspace.input.web.catalog.support.OwnerCatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/share-status-type")
public class ShareStatusTypeController extends OwnerCatalogController<ShareStatusType> {

    public ShareStatusTypeController(ShareStatusTypeService service) {
        super(service);
    }
}
