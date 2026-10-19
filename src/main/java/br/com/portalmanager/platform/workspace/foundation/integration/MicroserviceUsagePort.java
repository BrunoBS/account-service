package br.com.portalmanager.platform.workspace.foundation.integration;

@FunctionalInterface
public interface MicroserviceUsagePort {
    boolean isInUse(Long microserviceId);
}
