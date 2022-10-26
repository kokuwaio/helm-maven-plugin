package io.kokuwa.maven.helm.pojo;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * POJO for extra value override configuration (typically passed with --set or -f options)
 *
 * @author Emile de Weerd
 * @since 5.6
 */
@Data
public class ValueOverride {
	/**
	 * Values that will be passed with the option --set of helm command line.
	 */
	private Map<String, String> overrides;

	/**
	 * Values that will be passed with the option --set-string to the helm command line.
	 *
	 * <p>
	 * This option forces the values to be transformed and manipulated as strings by Go template.
	 * </p>
	 */
	private Map<String, String> stringOverrides;

	/**
	 * Values that will be passed with the option --set-file to the helm command line.
	 *
	 * <p>
	 * Values here point to files that contain the content you want to inject. Very useful to use with en entire script
	 * you want to insert optionally somewhere for instance.
	 * </p>
	 */
	private Map<String, String> fileOverrides;

	/**
	 * Value YAML file that will be passed with option --values or -f of the helm command line.
	 *
	 * <p>
	 * It can be seen as creating a temporary extending chart with its dedicated values.yaml.
	 * </p>
	 */
	private String yamlFile;

	/**
	 * Multiple Value YAML files that will be passed with option --values or -f of the helm command line.
	 *
	 * <p>
	 * It can be seen as creating a temporary extending chart with its dedicated values.yaml.
	 * </p>
	 */
	private List<String> yamlFiles;
}
