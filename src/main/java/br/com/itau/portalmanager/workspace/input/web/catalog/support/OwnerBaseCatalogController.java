package br.com.itau.portalmanager.workspace.input.web.catalog.support;

import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.catalog.dto.BaseCatalogDTO;
import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import br.com.portalmanager.platform.catalog.web.BaseCatalogController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

public abstract class OwnerBaseCatalogController<
        D extends BaseCatalogDTO<D>,
        E extends BaseCatalogEntity>
        extends BaseCatalogController<D, E> {

    @Override
    @GetMapping
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public List<D> findAll(@RequestParam Map<String, String> filters) {
        return super.findAll(filters);
    }

    @Override
    @GetMapping("/{id}")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public D findById(@PathVariable Long id) {
        return super.findById(id);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public D create(@RequestBody D dto) {
        return super.create(dto);
    }

    @Override
    @PutMapping("/{id}")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public D update(@PathVariable Long id, @RequestBody D dto) {
        return super.update(id, dto);
    }

    @Override
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public void delete(@PathVariable Long id) {
        super.delete(id);
    }

    @Override
    @PostMapping("/{id}/restore")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public D restore(@PathVariable Long id) {
        return super.restore(id);
    }
}
