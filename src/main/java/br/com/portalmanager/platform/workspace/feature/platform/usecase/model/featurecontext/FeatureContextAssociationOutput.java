package br.com.portalmanager.platform.workspace.feature.platform.usecase.model.featurecontext;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.FeatureOutput;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** Feature snapshot plus the target context, captured by the association audit. */
public record FeatureContextAssociationOutput(@JsonUnwrapped FeatureOutput feature, String contextIdentifier) {}
