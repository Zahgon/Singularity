package com.hubspot.singularity.web;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks the parameter of a resource method that is the user the request is answered for. The service works
 * the user out of the request the way its configuration says it may be named.
 */
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Auth {
}
