package io.kokuwa.maven.helm;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import io.kokuwa.maven.helm.pojo.HelmExecutable;
import io.kokuwa.maven.helm.pojo.ValueOverride;
import lombok.Setter;

@Setter
public abstract class AbstractHelmWithValueOverrideMojo extends AbstractChartDirectoryMojo {

	/**
	 * Additional values to set.
	 *
	 * @since 5.6
	 */
	@Parameter
	private ValueOverride values;

	@Override
	HelmExecutable helm() throws MojoExecutionException {
		HelmExecutable command = super.helm();
		if (values != null) {
			if (isNotEmpty(values.getOverrides())) {
				command.flag("set", values.getOverrides()
						.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
						.collect(Collectors.joining(",")));
			}
			if (isNotEmpty(values.getStringOverrides())) {
				command.flag("set-string", values.getStringOverrides()
						.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
						.collect(Collectors.joining(",")));
			}
			if (isNotEmpty(values.getFileOverrides())) {
				command.flag("set-file", values.getFileOverrides()
						.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
						.collect(Collectors.joining(",")));
			}
			if (values.getYamlFile() != null) {
				command.flag("values", values.getYamlFile());
			}
			if (values.getYamlFiles() != null) {
				values.getYamlFiles().forEach(yamlFile -> command.flag("values", yamlFile));
			}
		}
		return command;
	}

	private static <K, V> boolean isNotEmpty(Map<K, V> map) {
		return map != null && !map.isEmpty();
	}
}
