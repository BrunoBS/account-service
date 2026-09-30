package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationSystemTags;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationTag;
import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationTagRepository;
import br.com.portalmanager.platform.workspace.foundation.tagging.usecase.AbstractTagManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ApplicationTagManager extends AbstractTagManager<ApplicationTag, Application> {

    private final ApplicationTagRepository repository;

    public ApplicationTagManager(ApplicationTagRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void reconcile(Application application, String workspaceIdentifier, List<String> tags) {
        reconcileTags(
                application,
                application.getId(),
                tags,
                ApplicationSystemTags.resolve(application, workspaceIdentifier)
        );
    }

    @Transactional(readOnly = true)
    public List<String> findManual(Application application) {
        return findManualTags(application.getId());
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> findManualByIds(Collection<String> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return Map.of();
        }

        return repository.findByApplicationIdentifiersAndOrigin(identifiers, TagOriginType.MANUAL)
                .stream()
                .collect(Collectors.groupingBy(
                        tag -> tag.getApplication().getIdentifier(),
                        LinkedHashMap::new,
                        Collectors.mapping(ApplicationTag::getName, Collectors.toList())
                ));
    }

    @Transactional(readOnly = true)
    public List<String> findIdentifiersByTag(String tag) {
        String normalized = normalizeTag(tag);
        return normalized == null
                ? List.of()
                : repository.findApplicationIdentifiersByTag(normalized);
    }

    @Transactional
    public void deleteAll(Application application) {
        deleteByOwnerId(application.getId());
    }

    @Override
    protected List<ApplicationTag> findByOwnerId(Long ownerId) {
        return repository.findByApplicationId(ownerId);
    }

    @Override
    protected ApplicationTag newTag(Application owner, String name, TagOriginType originType) {
        return new ApplicationTag(owner, name, originType);
    }

    @Override
    protected void saveEntities(Collection<ApplicationTag> tags) {
        repository.saveAll(tags);
    }

    @Override
    protected void deleteEntities(Collection<ApplicationTag> tags) {
        repository.deleteAll(tags);
    }

    @Override
    protected void deleteByOwnerId(Long ownerId) {
        repository.deleteByApplicationId(ownerId);
    }
}
