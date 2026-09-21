package br.com.itau.portalmanager.workspace.foundation.messaging;

import br.com.portalmanager.platform.messaging.repository.ApiMessageRepository;
import br.com.portalmanager.platform.messaging.repository.NoOpApiMessageRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MessagingConfiguration {

    @Bean
    ApiMessageRepository workspaceApiMessageRepository() {
        return new NoOpApiMessageRepository();
    }
}
