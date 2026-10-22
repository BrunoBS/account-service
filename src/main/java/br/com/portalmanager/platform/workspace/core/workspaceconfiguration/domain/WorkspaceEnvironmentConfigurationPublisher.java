package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import tools.jackson.databind.JsonNode;

import java.util.Objects;

@Entity
@Table(
        name = "workspace_environment_configuration_publishers",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_workspace_env_config_publisher",
                    columnNames = {"configuration_id", "publisher_id"}),
            @UniqueConstraint(
                    name = "uk_workspace_env_config_order",
                    columnNames = {"configuration_id", "sort_order"})
        })
public class WorkspaceEnvironmentConfigurationPublisher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "configuration_id", nullable = false, updatable = false)
    private WorkspaceEnvironmentConfiguration configuration;

    @Column(name = "publisher_id", nullable = false, updatable = false)
    private Long publisherId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private JsonNode parameters;

    protected WorkspaceEnvironmentConfigurationPublisher() {}

    public WorkspaceEnvironmentConfigurationPublisher(
            WorkspaceEnvironmentConfiguration configuration,
            Long publisherId,
            Integer sortOrder,
            JsonNode parameters) {
        this.configuration = Objects.requireNonNull(configuration);
        this.publisherId = Objects.requireNonNull(publisherId);
        this.sortOrder = Objects.requireNonNull(sortOrder);
        if (sortOrder < 1) throw new IllegalArgumentException("sortOrder must be positive");
        this.parameters = Objects.requireNonNull(parameters).deepCopy();
    }

    public Long getId() {
        return id;
    }

    public WorkspaceEnvironmentConfiguration getConfiguration() {
        return configuration;
    }

    public Long getPublisherId() {
        return publisherId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public JsonNode getParameters() {
        return parameters.deepCopy();
    }
}
