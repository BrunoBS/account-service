package br.com.portalmanager.platform.workspace.config;

import br.com.portalmanager.platform.workspace.feature.parameter.config.ParameterProperties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("workspace")
public record WorkspaceProperties(@DefaultValue ParameterProperties parameter) {}
