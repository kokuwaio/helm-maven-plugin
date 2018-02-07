package com.kiwigrid.core.k8deployment.helmplugin.junit;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SystemProperty {

    String name();

    String value() default "";

    boolean unset() default false;

    @Target({ ElementType.METHOD, ElementType.TYPE })
    @Retention(RUNTIME)
    @Documented
    @interface SystemProperties {

        SystemProperty[] value();

    }
}