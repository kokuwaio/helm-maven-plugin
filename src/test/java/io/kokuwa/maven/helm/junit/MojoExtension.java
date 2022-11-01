package io.kokuwa.maven.helm.junit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.spy;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;

import io.kokuwa.maven.helm.AbstractHelmMojo;

public class MojoExtension implements ParameterResolver, BeforeAllCallback {

	private final Map<Class<AbstractHelmMojo>, MojoDescriptor> mojos = new HashMap<>();

	// lifecycle

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		if (mojos.isEmpty()) {

			// get plugin descriptor

			InputStream inputStream = AbstractHelmMojo.class.getResourceAsStream("/META-INF/maven/plugin.xml");
			assertNotNull(inputStream, "Plugin descriptor for not found, run 'mvn plugin:descriptor'.");
			PluginDescriptor plugin = new PluginDescriptorBuilder().build(new InterpolationFilterReader(
					new BufferedReader(new XmlStreamReader(inputStream)), new HashMap<>()));

			// get mojos

			for (MojoDescriptor mojo : plugin.getMojos()) {
				mojos.put((Class<AbstractHelmMojo>) Class.forName(mojo.getImplementation()), mojo);
			}
		}
	}

	// parameter

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
		return mojos.containsKey(parameterContext.getParameter().getType());
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
		try {

			// get descriptor

			Class<AbstractHelmMojo> mojoType = (Class<AbstractHelmMojo>) parameterContext.getParameter().getType();
			MojoDescriptor descriptor = mojos.get(mojoType);
			assertNotNull(descriptor, "Plugin descriptor for " + mojoType.getSimpleName() + " not found.");

			// create mojo

			AbstractHelmMojo mojo = spy(mojoType);
			mojo.setSettings(new Settings());
			mojo.setSecurityDispatcher(new DefaultSecDispatcher(new DefaultPlexusCipher()));
			mojo.setLog(new SystemStreamLog());
			mojo.setChartDirectory(new File("src/test/resources/simple")); // set some sane defaults for tests
			mojo.setHelmVersion("3.10.0"); // avoid github api

			// set parameter

			for (Parameter parameter : descriptor.getParameters()) {

				Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses(parameter.getName(), mojoType);
				field.setAccessible(true);

				if (parameter.isEditable() && parameter.getDefaultValue() != null) {
					String defaultValue = parameter.getDefaultValue()
							.replace("${project.build.directory}", "target")
							.replace("${java.io.tmpdir}", System.getProperty("java.io.tmpdir"));
					if (parameter.getType().equals("boolean")) {
						field.set(mojo, Boolean.parseBoolean(defaultValue));
					} else if (parameter.getType().equals(File.class.getName())) {
						field.set(mojo, new File(defaultValue));
					} else if (parameter.getType().equals(String.class.getName())) {
						field.set(mojo, defaultValue);
					} else {
						fail("unsupported type: " + parameter.getType());
					}
				}

				if (parameter.isRequired()) {
					assertNotNull(field.get(mojo), "Parameter '" + parameter.getName() + "' not set for mojo '"
							+ mojoType.getSimpleName() + "'.");
				}
			}

			return mojo;
		} catch (ReflectiveOperationException e) {
			throw new ParameterResolutionException("Failed to setup mockito.", e);
		}
	}
}
