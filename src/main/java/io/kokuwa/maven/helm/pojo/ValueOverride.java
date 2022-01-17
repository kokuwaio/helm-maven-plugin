package io.kokuwa.maven.helm.pojo;

import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.annotations.Parameter;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * POJO for extra value override configuration (typically passed with --set or -f options)
 *
 * @author Emile de Weerd
 * @since October 12th 2020
 */
@Getter
@Setter
@ToString
public class ValueOverride {
    /**
     * Values that will be passed with the option --set of helm command line.
     */
    @Parameter(property = "helm.values.overrides")
    private Map<String, String> overrides;

    /**
     * Values that will be passed with the option --set-string to the helm command line.
     *
     * <p>
     * This option forces the values to be transformed and manipulated as strings by Go template.
     * </p>
     */
    @Parameter(property = "helm.values.stringOverrides")
    private Map<String, String> stringOverrides;

    /**
     * Values that will be passed with the option --set-file to the helm command line.
     *
     * <p>
     * Values here point to files that contain the content you want to inject. Very useful to use with en entire script
     * you want to insert optionally somewhere for instance.
     * </p>
     */
    @Parameter(property = "helm.values.fileOverrides")
    private Map<String, String> fileOverrides;

    /**
     * Value YAML file that will be passed with option --values or -f of the helm command line.
     *
     * <p>
     * It can be seen as creating a temporary extending chart with its dedicated values.yaml.
     * </p>
     */
    @Parameter(property = "helm.values.yamlFile")
    private String yamlFile;

    /**
     * Multiple Value YAML files that will be passed with option --values or -f of the helm command line.
     *
     * <p>
     * It can be seen as creating a temporary extending chart with its dedicated values.yaml.
     * </p>
     */
    @Parameter(property = "helm.values.yamlFiles")
    private List<String> yamlFiles;
}
