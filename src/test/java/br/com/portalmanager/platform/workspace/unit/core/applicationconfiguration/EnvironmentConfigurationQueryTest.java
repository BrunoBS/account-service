package br.com.portalmanager.platform.workspace.unit.core.applicationconfiguration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.domain.ApplicationEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.repository.ApplicationEnvironmentConfigurationRepository;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.mapping.ApplicationEnvironmentConfigurationMapper;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.operations.ApplicationEnvironmentConfigurationQueryService;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.operations.ApplicationEnvironmentConfigurationScope;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.validation.ApplicationEnvironmentConfigurationValidator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

class EnvironmentConfigurationQueryTest {
    private final ApplicationEnvironmentConfigurationRepository repository = mock(ApplicationEnvironmentConfigurationRepository.class);
    private final ApplicationEnvironmentConfigurationScope scopes = mock(ApplicationEnvironmentConfigurationScope.class);
    private final ApplicationEnvironmentConfigurationMapper mapper = mock(ApplicationEnvironmentConfigurationMapper.class);
    private final ApplicationEnvironmentConfigurationValidator validator = mock(ApplicationEnvironmentConfigurationValidator.class);
    private final ApplicationEnvironmentConfigurationQueryService queries = new ApplicationEnvironmentConfigurationQueryService(repository, scopes, mapper, validator);

    @Test
    void findsConfigurationInsideApplication() {
        var configuration = configuration();
        when(scopes.resolve("workspace", "app", false)).thenReturn(new ApplicationEnvironmentConfigurationScope.Scope(1L, 2L));
        when(repository.findByIdentifierAndApplicationId("config", 2L)).thenReturn(Optional.of(configuration));
        queries.find("workspace", "app", "config");
        verify(mapper).toOutput("workspace", "app", configuration);
    }

    @Test
    void missingConfigurationIsNotFound() {
        when(scopes.resolve("workspace", "app", false)).thenReturn(new ApplicationEnvironmentConfigurationScope.Scope(1L, 2L));
        when(repository.findByIdentifierAndApplicationId("missing", 2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> queries.find("workspace", "app", "missing")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void listsPageAndAddsStableIdSort() {
        var configuration = configuration();
        when(scopes.resolve("workspace", "app", false)).thenReturn(new ApplicationEnvironmentConfigurationScope.Scope(1L, 2L));
        when(repository.findByApplicationIdAndLifecycle(eq(2L), any(), any())).thenReturn(new PageImpl<>(List.of(configuration)));
        queries.findAll("workspace", "app", "ACTIVE", PageRequest.of(0, 4, Sort.by("createdAt").descending()));
        ArgumentCaptor<org.springframework.data.domain.Pageable> pageable = ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(repository).findByApplicationIdAndLifecycle(eq(2L), any(), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getValue().getSort().getOrderFor("id")).isNotNull();
        verify(mapper).toOutput("workspace", "app", configuration);
    }

    @Test
    void retainsExplicitIdSort() {
        when(scopes.resolve("workspace", "app", false)).thenReturn(new ApplicationEnvironmentConfigurationScope.Scope(1L, 2L));
        when(repository.findByApplicationIdAndLifecycle(eq(2L), any(), any())).thenReturn(new PageImpl<>(List.of()));
        queries.findAll("workspace", "app", null, PageRequest.of(1, 3, Sort.by("id").descending()));
        ArgumentCaptor<org.springframework.data.domain.Pageable> pageable = ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(repository).findByApplicationIdAndLifecycle(eq(2L), any(), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getValue().getSort().stream()).hasSize(1);
    }

    private ApplicationEnvironmentConfiguration configuration() {
        var configuration = new ApplicationEnvironmentConfiguration(2L, 3L, JsonMapper.builder().build().createObjectNode(), LocalDateTime.now());
        ReflectionTestUtils.setField(configuration, "id", 4L);
        return configuration;
    }
}
