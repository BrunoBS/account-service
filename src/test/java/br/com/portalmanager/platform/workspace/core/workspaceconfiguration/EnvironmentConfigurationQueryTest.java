package br.com.portalmanager.platform.workspace.core.workspaceconfiguration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain.WorkspaceEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.repository.WorkspaceEnvironmentConfigurationRepository;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.mapping.WorkspaceEnvironmentConfigurationMapper;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.operations.WorkspaceEnvironmentConfigurationQueryService;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.operations.WorkspaceEnvironmentConfigurationScope;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.validation.WorkspaceEnvironmentConfigurationValidator;
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
    private final WorkspaceEnvironmentConfigurationRepository repository = mock(WorkspaceEnvironmentConfigurationRepository.class);
    private final WorkspaceEnvironmentConfigurationScope scopes = mock(WorkspaceEnvironmentConfigurationScope.class);
    private final WorkspaceEnvironmentConfigurationMapper mapper = mock(WorkspaceEnvironmentConfigurationMapper.class);
    private final WorkspaceEnvironmentConfigurationValidator validator = mock(WorkspaceEnvironmentConfigurationValidator.class);
    private final WorkspaceEnvironmentConfigurationQueryService queries = new WorkspaceEnvironmentConfigurationQueryService(repository, scopes, mapper, validator);

    @Test
    void findsConfigurationWithinWorkspace() {
        var configuration = configuration();
        when(scopes.resolve("workspace", false)).thenReturn(new WorkspaceEnvironmentConfigurationScope.Scope(1L, 1L));
        when(repository.findByIdentifierAndWorkspaceId("config", 1L)).thenReturn(Optional.of(configuration));
        queries.find("workspace", "config");
        verify(mapper).toOutput("workspace", configuration);
    }

    @Test
    void missingConfigurationIsNotFound() {
        when(scopes.resolve("workspace", false)).thenReturn(new WorkspaceEnvironmentConfigurationScope.Scope(1L, 1L));
        when(repository.findByIdentifierAndWorkspaceId("missing", 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> queries.find("workspace", "missing")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void listsPageAndAddsIdAsStableSecondarySort() {
        var configuration = configuration();
        when(scopes.resolve("workspace", false)).thenReturn(new WorkspaceEnvironmentConfigurationScope.Scope(1L, 1L));
        when(repository.findByWorkspaceIdAndLifecycle(eq(1L), any(), any()))
                .thenReturn(new PageImpl<>(List.of(configuration)));
        queries.findAll("workspace", "ACTIVE", PageRequest.of(0, 5, Sort.by("createdAt").descending()));
        ArgumentCaptor<org.springframework.data.domain.Pageable> pageable = ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(repository).findByWorkspaceIdAndLifecycle(eq(1L), any(), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getValue().getSort().getOrderFor("id")).isNotNull();
        verify(mapper).toOutput("workspace", configuration);
    }

    @Test
    void retainsExplicitIdSortWithoutAppendingDuplicate() {
        when(scopes.resolve("workspace", false)).thenReturn(new WorkspaceEnvironmentConfigurationScope.Scope(1L, 1L));
        when(repository.findByWorkspaceIdAndLifecycle(eq(1L), any(), any())).thenReturn(new PageImpl<>(List.of()));
        queries.findAll("workspace", null, PageRequest.of(1, 3, Sort.by("id").descending()));
        ArgumentCaptor<org.springframework.data.domain.Pageable> pageable = ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(repository).findByWorkspaceIdAndLifecycle(eq(1L), any(), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getValue().getSort().stream()).hasSize(1);
    }

    private WorkspaceEnvironmentConfiguration configuration() {
        var configuration = new WorkspaceEnvironmentConfiguration(1L, 2L, JsonMapper.builder().build().createObjectNode(), LocalDateTime.now());
        ReflectionTestUtils.setField(configuration, "id", 3L);
        return configuration;
    }
}
