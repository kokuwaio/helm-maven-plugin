package com.kiwigrid.core.k8deployment.helmplugin.junit;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.kiwigrid.core.k8deployment.helmplugin.junit.MojoProperty.MojoProperties;

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