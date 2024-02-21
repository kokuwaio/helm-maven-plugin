package io.kokuwa.maven.helm.junit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.spy;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.DefaultMavenProjectHelper;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockito.Mockito;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;

import io.kokuwa.maven.helm.AbstractChartDirectoryMojo;
import io.kokuwa.maven.helm.AbstractHelmMojo;

@SuppressWarnings("unchecked")
public class MojoExtension implements ParameterResolver, BeforeAllCallback {

	private final Map<Class<AbstractHelmMojo>, MojoDescriptor> mojos = new HashMap<>();

	// lifecycle

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		if (mojos.isEmpty()) {

			// get plugin descriptor

			InputStream inputStream = AbstractHelmMojo.class.getResourceAsStream("/META-INF/maven/plugin.xml");
			assertNotNull(inputStream, "Plugin descriptor for not found, run 'mvn plugin:descriptor'.");
			HashMap<String, Object> variables = new HashMap<>();
			variables.put("project.build.directory", "target");
			variables.put("java.io.tmpdir", System.getProperty("java.io.tmpdir"));
			PluginDescriptor plugin = new PluginDescriptorBuilder().build(new InterpolationFilterReader(
					new BufferedReader(new XmlStreamReader(inputStream)), variables));

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
			mojo.setMavenProjectHelper(Mockito.mock(DefaultMavenProjectHelper.class));
			mojo.setMavenProject(new MavenProject());
			mojo.getMavenProject().getBuild().setDirectory("target");
			mojo.setLog(new SystemStreamLog());

			// set parameter

			for (Parameter parameter : descriptor.getParameters()) {

				Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses(parameter.getName(), mojoType);
				field.setAccessible(true);

				if (parameter.isEditable() && parameter.getDefaultValue() != null) {
					if (parameter.getType().equals("boolean")) {
						field.set(mojo, Boolean.parseBoolean(parameter.getDefaultValue()));
					} else if (parameter.getType().equals(File.class.getName())) {
						field.set(mojo, new File(parameter.getDefaultValue()));
					} else if (parameter.getType().equals(String.class.getName())) {
						field.set(mojo, parameter.getDefaultValue());
					} else {
						fail("unsupported type: " + parameter.getType());
					}
				}
			}

			// preconfigure

			if (mojo instanceof AbstractChartDirectoryMojo) {
				((AbstractChartDirectoryMojo) mojo)
					.setChartDirectory(new File("src/test/resources/simple")); // set some sane defaults for tests
			}
			mojo.setHelmExecutableDirectory(determineHelmExecutableDirectory().toFile()); // avoid download helm
			mojo.setHelmVersion("3.12.0"); // avoid github api

			return mojo;
		} catch (ReflectiveOperationException e) {
			throw new ParameterResolutionException("Failed to setup mockito.", e);
		}
	}

	/**
	 * Determines which helm executable to use based on the machine's architecture.
	 *
	 * @return location of appropriate helm executable
	 */
	public static Path determineHelmExecutableDirectory() {
		return Paths.get("src/bin/" + System.getProperty("os.arch")).toAbsolutePath();
	}
}
