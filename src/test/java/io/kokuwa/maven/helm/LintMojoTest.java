package io.kokuwa.maven.helm;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import java.nio.file.Paths;

import org.codehaus.plexus.util.Os;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import io.kokuwa.maven.helm.junit.MojoExtension;
import io.kokuwa.maven.helm.junit.MojoProperty;
import io.kokuwa.maven.helm.pojo.ValueOverride;

@ExtendWith(MojoExtension.class)
@MojoProperty(name = "helmDownloadUrl", value = "https://get.helm.sh/helm-v2.14.3-linux-amd64.tar.gz")
@MojoProperty(name = "chartDirectory", value = "junit-chart")
@MojoProperty(name = "chartVersion", value = "0.0.1")
public class LintMojoTest {

	@Test
	public void valuesFile(LintMojo mojo) throws Exception {
		ValueOverride override = new ValueOverride();
		override.setYamlFile("overrideValues.yaml");
		mojo.setValues(override);
		mojo.setChartDirectory(Paths.get(getClass().getResource("Chart.yaml").toURI()).getParent().toString());

		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).helm(helmCommandCaptor.capture(), anyString());
		doReturn(Paths.get("helm" + (Os.OS_FAMILY == Os.FAMILY_WINDOWS ? ".exe" : ""))).when(mojo)
				.getHelmExecuteablePath();

		mojo.execute();

		assertTrue(helmCommandCaptor.getValue().contains("--values overrideValues.yaml"));
	}

	@Test
	public void lintWithNamespace(LintMojo mojo) throws Exception {
		mojo.setLintNamespace("default");
		mojo.setChartDirectory(Paths.get(getClass().getResource("Chart.yaml").toURI()).getParent().toString());

		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).helm(helmCommandCaptor.capture(), anyString());
		doReturn(Paths.get("helm" + (Os.OS_FAMILY == Os.FAMILY_WINDOWS ? ".exe" : ""))).when(mojo)
				.getHelmExecuteablePath();

		mojo.execute();

		assertTrue(helmCommandCaptor.getValue().contains("--namespace=default"));
	}
}
