package br.com.portalmanager.account.configuration;

import br.com.portalmanager.core.messaging.repository.ApiMessageRepository;
import br.com.portalmanager.core.messaging.repository.NoOpApiMessageRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MessagingConfiguration {

    @Bean
    ApiMessageRepository accountApiMessageRepository() {
        return new NoOpApiMessageRepository();
    }
}
