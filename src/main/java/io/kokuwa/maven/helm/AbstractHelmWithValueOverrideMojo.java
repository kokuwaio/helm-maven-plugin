package io.kokuwa.maven.helm;

import io.kokuwa.maven.helm.pojo.ValueOverride;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Locale;
import java.util.Map;

import static java.lang.String.format;

@Setter
public abstract class AbstractHelmWithValueOverrideMojo extends AbstractHelmMojo {

    @Parameter(property = "helm.values")
    private ValueOverride values;

    protected String getValuesOptions() {
        StringBuilder setValuesOptions = new StringBuilder();
        if (values != null) {
            if (isNotEmpty(values.getOverrides())) {
                setValuesOptions.append(" --set ");
                appendOverrideMap(setValuesOptions, values.getOverrides());
            }
            if (isNotEmpty(values.getStringOverrides())) {
                setValuesOptions.append(" --set-string ");
                appendOverrideMap(setValuesOptions, values.getStringOverrides());
            }
            if (isNotEmpty(values.getFileOverrides())) {
                setValuesOptions.append(" --set-file ");
                appendOverrideMap(setValuesOptions, values.getFileOverrides());
            }
            if (StringUtils.isNotBlank(values.getYamlFile())) {
                setValuesOptions.append(" --values ").append(values.getYamlFile());
            }
            if (values.getYamlFiles() != null) {
                for (String yamlFile : values.getYamlFiles()) {
                    if (StringUtils.isNotBlank(yamlFile)) {
                        setValuesOptions.append(" --values ").append(yamlFile);
                    }
                }
            }
        }
        return setValuesOptions.toString();
    }

    protected final String getCommand(String action, String inputDirectory)
            throws MojoExecutionException {
        return getCommand(action, "", inputDirectory);
    }

    protected final String getCommand(String action, String args, String inputDirectory) throws MojoExecutionException {
        return new StringBuilder(getHelmCommand(action, args))
                       .append(StringUtils.isNotEmpty(getReleaseName()) ? format(" %s ", getReleaseName()) : " --generate-name ")
                       .append(inputDirectory)
                       .append(StringUtils.isNotEmpty(getNamespace())
                                       ? format(" -n %s ", getNamespace().toLowerCase(Locale.ROOT))
                                       : "")
                       .append(isDebug() ? " --debug " : "")
                       .append(StringUtils.isNotEmpty(getRegistryConfig())
                                       ? format(" --registry-config %s ", getRegistryConfig())
                                       : "")
                       .append(StringUtils.isNotEmpty(getRepositoryCache())
                                       ? format(" --repository-cache %s ", getRepositoryCache())
                                       : "")
                       .append(StringUtils.isNotEmpty(getRepositoryConfig())
                                       ? format(" --repository-config %s ", getRepositoryConfig())
                                       : "")
                       .append(getValuesOptions())
                       .toString();
    }

    private void appendOverrideMap(StringBuilder setValues, Map<String, String> overrides) {
        boolean first = true;
        for (Map.Entry<String, String> valueEntry : overrides.entrySet()) {
            if (first) {
                first = false;
            } else {
                setValues.append(',');
            }
            setValues.append(valueEntry.getKey()).append('=').append(valueEntry.getValue());
        }
    }

    private static <K, V> boolean isNotEmpty(Map<K, V> map) {
        return map != null && !map.isEmpty();
    }
}
