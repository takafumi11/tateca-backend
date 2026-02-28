package com.tateca.tatecabackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject authenticated user's UID from SecurityContext.
 * The UID is automatically extracted from Firebase JWT Bearer token.
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface UId {
}
