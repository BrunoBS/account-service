package br.com.portalmanager.platform.workspace.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WorkspaceProperties.class)
public class WorkspaceConfiguration {}
