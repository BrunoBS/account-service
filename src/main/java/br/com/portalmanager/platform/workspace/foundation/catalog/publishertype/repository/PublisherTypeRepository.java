package br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.repository;

import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.domain.PublisherType;

import org.springframework.stereotype.Repository;

@Repository
public interface PublisherTypeRepository extends CatalogRepository<PublisherType> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @org.springframework.data.jpa.repository.Query(
            "select p from PublisherType p where p.code in :codes order by p.code")
    java.util.List<PublisherType> findForAssociation(
            @org.springframework.data.repository.query.Param("codes")
                    java.util.Collection<String> codes);
}
