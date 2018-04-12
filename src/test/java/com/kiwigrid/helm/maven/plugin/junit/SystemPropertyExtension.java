package com.kiwigrid.helm.maven.plugin.junit;

import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class SystemPropertyExtension implements BeforeAllCallback, BeforeEachCallback {

    private Properties backup;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        backup = new Properties(System.getProperties());
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Properties testProperties = new Properties(backup);
        Arrays.stream(ArrayUtils.addAll(
                context.getRequiredTestClass().getAnnotationsByType(SystemProperty.class),
                context.getRequiredTestMethod().getAnnotationsByType(SystemProperty.class)))
              .forEach(p -> testProperties.put(p.name(), p.value()));
        System.setProperties(testProperties);
    }
}