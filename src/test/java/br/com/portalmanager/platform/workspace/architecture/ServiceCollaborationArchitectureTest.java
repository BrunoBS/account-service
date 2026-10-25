package br.com.portalmanager.platform.workspace.architecture;

import static org.assertj.core.api.Assertions.*;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.lang.reflect.*;
import java.util.*;

class ServiceCollaborationArchitectureTest {
    @Test
    void injectedCollaborationsAreAcyclicIncludingConsumerOwnedPorts() {
        Set<Class<?>> beans = new HashSet<>();
        new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages(GoldenArchitectureRules.ROOT)
                        .stream()
                        .map(type -> type.reflect())
                        .filter(type -> AnnotatedElementUtils.hasAnnotation(type, Component.class))
                        .forEach(beans::add);
        assertThat(beans).isNotEmpty();
        requireAcyclic(beans);
    }

    @Test
    void rejectsAControlledServiceCycleButAllowsModelRelationships() {
        assertThatThrownBy(() -> requireAcyclic(Set.of(FirstService.class, SecondService.class)))
                .isInstanceOf(AssertionError.class);
        assertThatCode(() -> requireAcyclic(Set.of(ModelOne.class, ModelTwo.class)))
                .doesNotThrowAnyException();
    }

    private static void requireAcyclic(Set<Class<?>> types) {
        Map<Class<?>, Set<Class<?>>> graph = new HashMap<>();
        for (Class<?> bean : types) {
            if (!AnnotatedElementUtils.hasAnnotation(bean, Component.class)) continue;
            Set<Class<?>> dependencies = new HashSet<>();
            for (Class<?> current = bean; current != null; current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    List<Class<?>> fieldTypes = new ArrayList<>(List.of(field.getType()));
                    if (field.getGenericType() instanceof ParameterizedType parameterized) {
                        for (Type argument : parameterized.getActualTypeArguments())
                            if (argument instanceof Class<?> type) fieldTypes.add(type);
                    }
                    for (Class<?> candidate : types)
                        if (candidate != bean
                                && fieldTypes.stream()
                                        .anyMatch(type -> type.isAssignableFrom(candidate)))
                            dependencies.add(candidate);
                }
            }
            graph.put(bean, dependencies);
        }
        Set<Class<?>> finished = new HashSet<>();
        for (Class<?> bean : graph.keySet()) visit(bean, graph, finished, new LinkedHashSet<>());
    }

    private static void visit(
            Class<?> bean,
            Map<Class<?>, Set<Class<?>>> graph,
            Set<Class<?>> finished,
            Set<Class<?>> path) {
        if (finished.contains(bean)) return;
        assertThat(path).as("service collaboration path: " + path).doesNotContain(bean);
        path.add(bean);
        for (Class<?> dependency : graph.getOrDefault(bean, Set.of()))
            visit(dependency, graph, finished, path);
        path.remove(bean);
        finished.add(bean);
    }

    @Service
    static class FirstService {
        SecondService second;
    }

    @Service
    static class SecondService {
        FirstService first;
    }

    static class ModelOne {
        ModelTwo second;
    }

    static class ModelTwo {
        ModelOne first;
    }
}
