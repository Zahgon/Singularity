package com.hubspot.singularity.web;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The value a parameter is read as when the request does not carry it at all.
 */
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface DefaultValue {
  String value();
}
