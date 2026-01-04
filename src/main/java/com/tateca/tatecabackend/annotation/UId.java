package com.tateca.tatecabackend.annotation;

import io.swagger.v3.oas.annotations.Parameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject authenticated user's UID from SecurityContext.
 * The UID is automatically extracted from Firebase JWT Bearer token.
 * This parameter is hidden from OpenAPI specification as it's not part of the public API contract.
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Parameter(hidden = true)
public @interface UId {
}