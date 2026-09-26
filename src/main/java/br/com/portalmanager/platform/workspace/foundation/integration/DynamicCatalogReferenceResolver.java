package br.com.portalmanager.platform.workspace.foundation.integration;

/**
 * Public boundary for resolving references to dynamic catalogs.
 *
 * <p>Dynamic catalogs expose this integration by default so consuming features
 * do not depend directly on catalog use cases or repositories.</p>
 */
public interface DynamicCatalogReferenceResolver {

    boolean existsActive(String catalog, String code);
}
