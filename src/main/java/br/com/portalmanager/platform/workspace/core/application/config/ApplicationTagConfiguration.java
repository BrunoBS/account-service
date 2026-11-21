package br.com.portalmanager.platform.workspace.core.application.config;

import br.com.portalmanager.platform.library.tagging.api.contract.TagManager;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationTag;
import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationTagRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ApplicationTagConfiguration {

    @Bean
    TagManager<ApplicationTag, Application> applicationTagManager(ApplicationTagRepository repository) {
        return new TagManager<>(repository, ApplicationTag::new);
    }
}
