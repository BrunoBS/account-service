package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup;

import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.FeatureOutput;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** Feature snapshot plus the target featureGroupEntity, captured by the association audit. */
public record FeatureGroupAssociationOutput(
        @JsonUnwrapped FeatureOutput feature, String featureGroupIdentifier) {}
