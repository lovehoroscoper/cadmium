package com.meltmedia.cadmium.core;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * com.meltmedia.cadmium.core.ISJBoss
 *
 * @author jmcentire
 */
@BindingAnnotation
@Target({FIELD, METHOD, PARAMETER})
@Retention(RUNTIME)
public @interface ISJBoss {
}
