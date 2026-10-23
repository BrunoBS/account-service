package br.com.portalmanager.platform.workspace.feature.sharing.usecase.port;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.*;

public interface SharingReferencePort {
    List<ApplicationReferenceOutput> findActiveReferencesForResourceSharing(Collection<Long> ids);

    ApplicationScopeReferenceOutput findActiveScope(String workspaceIdentifier, String identifier);

    ApplicationScopeReferenceOutput findActiveScopeForUpdate(
            String workspaceIdentifier, String identifier);

    ApplicationScopeReferenceOutput findActiveScopeForShared(Long workspaceId, Long applicationId);

    String findIdentifierByInternalId(Long workspaceId, Long applicationId);

    Map<Long, ApplicationReferenceOutput> findReferencesForResourceSharing(
            Collection<Long> applicationIds);

    List<EnvironmentOutput> listActiveForShared(Long workspaceId, String workspaceIdentifier);

    Map<String, EnvironmentReferenceOutput> findAvailableReferencesForResourceSharing(
            Long workspaceId, String workspaceIdentifier, Collection<String> identifiers);

    Map<Long, String> findIdentifiersByInternalIds(Collection<Long> environmentIds);

    Long findShareableInternalIdByIdentifier(String identifier);

    String findIdentifierByInternalId(Long id);

    FeatureReferenceOutput findReferenceByInternalId(Long id);

    Map<Long, FeatureReferenceOutput> findReferencesByInternalIds(Collection<Long> ids);

    Map<Long, String> findAvailableIdentifiersForResourceSharing(Collection<Long> ids);

    String findReferenceIdentifierForResourceSharing(Long id);

    record ApplicationReferenceOutput(
            Long id,
            Long workspaceId,
            String identifier,
            String workspaceIdentifier,
            String name) {}

    record ApplicationScopeReferenceOutput(
            Long workspaceId, Long applicationId, String applicationScope) {}

    record EnvironmentOutput(
            Long version,
            String identifier,
            String workspaceIdentifier,
            String name,
            String description,
            String authorizationType,
            String environmentTypeCode,
            String parentIdentifier,
            Integer sortOrder,
            String authorizerGroup,
            JsonNode settings,
            String lifecycle,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {}

    record EnvironmentReferenceOutput(Long id, EnvironmentOutput environment) {}

    record FeatureReferenceOutput(String identifier, String name) {}
}
