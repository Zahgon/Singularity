package com.hubspot.singularity.web;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The name under which a resource method's parameter is read.
 */
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface PathParam {
  String value();
}
