package io.kokuwa.maven.helm;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Parameter;

import io.kokuwa.maven.helm.pojo.ValueOverride;
import lombok.Setter;

@Setter
public abstract class AbstractHelmWithValueOverrideMojo extends AbstractHelmMojo {

	/**
	 * Additional values to set.
	 *
	 * @since 5.6
	 */
	@Parameter
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
