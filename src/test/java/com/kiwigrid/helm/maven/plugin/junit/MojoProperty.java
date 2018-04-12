package com.kiwigrid.helm.maven.plugin.junit;

import java.lang.annotation.*;

import com.kiwigrid.helm.maven.plugin.junit.MojoProperty.MojoProperties;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Repeatable(MojoProperties.class)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MojoProperty {

    String name();

    String value();

    @Target({ ElementType.METHOD, ElementType.TYPE })
    @Retention(RUNTIME)
    @Documented
    @interface MojoProperties {

        MojoProperty[] value();

    }
}