package com.meltmedia.cadmium.search;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used to bind the class of the search content processor.
 *
 * @author John McEntire
 */
@BindingAnnotation
@Target({FIELD, METHOD, PARAMETER})
@Retention(RUNTIME)
public @interface SearchContentPreprocessorClass {
}
