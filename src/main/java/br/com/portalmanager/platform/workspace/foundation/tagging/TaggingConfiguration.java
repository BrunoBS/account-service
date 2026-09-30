package br.com.portalmanager.platform.workspace.foundation.tagging;

import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationTag;
import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationTagRepository;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceTag;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceTagRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaggingConfiguration {

    @Bean
    TagManager<ApplicationTag, Application, Long, String> applicationTagManager(
            ApplicationTagRepository repository) {
        return new TagManager<>(repository);
    }

    @Bean
    TagManager<WorkspaceTag, Workspace, Long, String> workspaceTagManager(
            WorkspaceTagRepository repository) {
        return new TagManager<>(repository);
    }
}
