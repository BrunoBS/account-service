package br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.parameterkey;

import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.runtime.DynamicParameterRuntimeProperties;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.PublishParameterKeysInput;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.runtime.DynamicParameterRuntimeService;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.validation.parameterkey.ParameterKeyValidator;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ParameterKeyPublicationService {
    private final ParameterKeyPublicationOperations operations;
    private final ParameterKeyValidator validator;
    private final TaskExecutor executor;
    private final DynamicParameterRuntimeService runtime;
    private final DynamicParameterRuntimeProperties properties;

    public ParameterKeyPublicationService(
            ParameterKeyPublicationOperations operations,
            ParameterKeyValidator validator,
            @Qualifier("applicationTaskExecutor") TaskExecutor executor,
            DynamicParameterRuntimeService runtime,
            DynamicParameterRuntimeProperties properties) {
        this.operations = operations;
        this.validator = validator;
        this.executor = executor;
        this.runtime = runtime;
        this.properties = properties;
    }

    public void publish(PublishParameterKeysInput input) {
        validator.validatePublicationIdentifiers(input == null ? null : input.identifiers());
        List<String> identifiers = List.copyOf(input.identifiers());
        var session = UserContext.get().orElse(null);
        var context = MDC.getCopyOfContextMap();
        executor.execute(
                () -> {
                    var previousSession = UserContext.get().orElse(null);
                    var previousContext = MDC.getCopyOfContextMap();
                    try {
                        UserContext.set(session);
                        if (context == null) MDC.clear();
                        else MDC.setContextMap(context);
                        int batchSize = properties.runtime().batchSize();
                        for (int start = 0; start < identifiers.size(); start += batchSize) {
                            List<String> published = new ArrayList<>();
                            for (String identifier :
                                    identifiers.subList(
                                            start,
                                            Math.min(start + batchSize, identifiers.size()))) {
                                try {
                                    published.add(operations.publish(identifier).identifier());
                                } catch (RuntimeException exception) {
                                    runtime.logPublicationFailure(identifier, exception);
                                }
                            }
                            runtime.publish(published);
                        }
                    } finally {
                        UserContext.set(previousSession);
                        if (previousContext == null) MDC.clear();
                        else MDC.setContextMap(previousContext);
                    }
                });
    }
}
