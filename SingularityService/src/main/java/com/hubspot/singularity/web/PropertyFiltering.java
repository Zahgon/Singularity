package com.hubspot.singularity.web;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks an answer whose entity may be cut down to the properties the request asks for.
 */
@Target({ METHOD, TYPE })
@Retention(RUNTIME)
public @interface PropertyFiltering {
  String[] always() default {};
}
