package br.com.portalmanager.platform.workspace.core.workspace.usecase.configuration;

import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceTag;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceTagRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class WorkspaceTagConfiguration {

    @Bean
    TagManager<WorkspaceTag, Workspace> workspaceTagManager(WorkspaceTagRepository repository) {
        return new TagManager<>(repository, WorkspaceTag::new);
    }
}
