package com.kiwigrid.helm.maven.plugin.junit;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

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