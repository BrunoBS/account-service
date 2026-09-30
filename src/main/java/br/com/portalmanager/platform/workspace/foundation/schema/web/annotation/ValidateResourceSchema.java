package br.com.portalmanager.platform.workspace.foundation.schema.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the resource schema binding used to validate the raw HTTP request body
 * before Spring deserializes it into the controller request type.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateResourceSchema {
    String type();
    String code();
}
