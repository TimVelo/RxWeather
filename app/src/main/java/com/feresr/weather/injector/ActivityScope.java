package com.feresr.weather.injector;

import java.lang.annotation.Retention;

import javax.inject.Scope;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by Fernando on 18/10/2015.
 */
@Scope
@Retention(RUNTIME)
public @interface ActivityScope {
}