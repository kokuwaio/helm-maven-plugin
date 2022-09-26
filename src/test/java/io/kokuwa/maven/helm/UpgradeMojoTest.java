package io.kokuwa.maven.helm;

import io.kokuwa.maven.helm.junit.MojoExtension;
import io.kokuwa.maven.helm.junit.MojoProperty;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.Os;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MojoExtension.class)
@MojoProperty(name = "helmDownloadUrl", value = "https://get.helm.sh/helm-v2.14.3-linux-amd64.tar.gz")
@MojoProperty(name = "chartDirectory", value = "junit-helm")
@MojoProperty(name = "chartVersion", value = "0.0.1")
@MojoProperty(name = "releaseName", value = "myRel")
public class UpgradeMojoTest {

	@Test
	public void verifyUpgradeCommand(UpgradeMojo mojo) throws MojoExecutionException {
		URL resource = this.getClass().getResource("app-0.1.0.tgz");
		List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());
		doReturn(tgzs).when(mojo).getChartDirectories(anyString());

		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).helm(helmCommandCaptor.capture(), anyString());
		doReturn(Paths.get("helm" + (Os.OS_FAMILY == Os.FAMILY_WINDOWS ? ".exe" : ""))).when(mojo)
				.getHelmExecuteablePath();

		mojo.execute();

		String helmDefaultCommand = helmCommandCaptor.getAllValues()
				.stream()
				.filter(cmd -> cmd.contains("upgrade"))
				.findAny().orElseThrow(() -> new IllegalArgumentException("Only one helm repo command expected"))
				.trim();

		assertEquals("upgrade myRel " + tgzs.get(0), helmDefaultCommand);
	}

	@Test
	public void verifyDryRunUpgradeCommand(UpgradeMojo mojo) throws MojoExecutionException {
		URL resource = this.getClass().getResource("app-0.1.0.tgz");
		List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());
		doReturn(tgzs).when(mojo).getChartDirectories(anyString());

		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).helm(helmCommandCaptor.capture(), anyString());
		doReturn(Paths.get("helm" + (Os.OS_FAMILY == Os.FAMILY_WINDOWS ? ".exe" : ""))).when(mojo)
				.getHelmExecuteablePath();

		mojo.setUpgradeDryRun(true);

		mojo.execute();

		String helmDefaultCommand = helmCommandCaptor.getAllValues()
				.stream()
				.filter(cmd -> cmd.contains("upgrade"))
				.findAny().orElseThrow(() -> new IllegalArgumentException("Only one helm repo command expected"))
				.trim();

		assertEquals("upgrade myRel " + tgzs.get(0) + " --dry-run", helmDefaultCommand);
	}

	@Test
	public void verifyInstallUpgradeCommand(UpgradeMojo mojo) throws MojoExecutionException {
		URL resource = this.getClass().getResource("app-0.1.0.tgz");
		List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());
		doReturn(tgzs).when(mojo).getChartDirectories(anyString());

		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).helm(helmCommandCaptor.capture(), anyString());
		doReturn(Paths.get("helm" + (Os.OS_FAMILY == Os.FAMILY_WINDOWS ? ".exe" : ""))).when(mojo)
				.getHelmExecuteablePath();

		mojo.setUpgradeWithInstall(true);

		mojo.execute();

		String helmDefaultCommand = helmCommandCaptor.getAllValues()
				.stream()
				.filter(cmd -> cmd.contains("upgrade"))
				.findAny().orElseThrow(() -> new IllegalArgumentException("Only one helm repo command expected"))
				.trim();

		assertEquals("upgrade myRel " + tgzs.get(0) + " --install", helmDefaultCommand);
	}

	@Test
	public void verifyDryRunAndInstallUpgradeCommand(UpgradeMojo mojo) throws MojoExecutionException {
		URL resource = this.getClass().getResource("app-0.1.0.tgz");
		List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());
		doReturn(tgzs).when(mojo).getChartDirectories(anyString());

		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).helm(helmCommandCaptor.capture(), anyString());
		doReturn(Paths.get("helm" + (Os.OS_FAMILY == Os.FAMILY_WINDOWS ? ".exe" : ""))).when(mojo)
				.getHelmExecuteablePath();

		mojo.setUpgradeWithInstall(true);
		mojo.setUpgradeDryRun(true);

		mojo.execute();

		String helmDefaultCommand = helmCommandCaptor.getAllValues()
				.stream()
				.filter(cmd -> cmd.contains("upgrade"))
				.findAny().orElseThrow(() -> new IllegalArgumentException("Only one helm repo command expected"))
				.trim();

		assertEquals("upgrade myRel " + tgzs.get(0) + " --install --dry-run", helmDefaultCommand);
	}

	@Test
	public void verifyNothingHappenWhenSkip(UpgradeMojo mojo) throws MojoExecutionException {
		URL resource = this.getClass().getResource("app-0.1.0.tgz");
		List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());
		doReturn(tgzs).when(mojo).getChartDirectories(anyString());

		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).helm(helmCommandCaptor.capture(), anyString());
		doReturn(Paths.get("helm" + (Os.OS_FAMILY == Os.FAMILY_WINDOWS ? ".exe" : ""))).when(mojo)
				.getHelmExecuteablePath();

		mojo.setSkip(true);

		mojo.execute();

		assertEquals(0, helmCommandCaptor.getAllValues().size());
	}

	@Test
	public void verifyNothingHappenWhenSkipUpgrade(UpgradeMojo mojo) throws MojoExecutionException {
		URL resource = this.getClass().getResource("app-0.1.0.tgz");
		List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());
		doReturn(tgzs).when(mojo).getChartDirectories(anyString());

		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).helm(helmCommandCaptor.capture(), anyString());
		doReturn(Paths.get("helm" + (Os.OS_FAMILY == Os.FAMILY_WINDOWS ? ".exe" : ""))).when(mojo)
				.getHelmExecuteablePath();

		mojo.setSkipUpgrade(true);

		mojo.execute();

		assertEquals(0, helmCommandCaptor.getAllValues().size());
	}
}
