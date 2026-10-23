package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.port;

@FunctionalInterface
public interface MicroserviceUsagePort {
    boolean isInUse(Long microserviceId);
}
