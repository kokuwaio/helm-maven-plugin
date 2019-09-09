package com.kiwigrid.helm.maven.plugin;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import com.kiwigrid.helm.maven.plugin.junit.MojoExtension;
import com.kiwigrid.helm.maven.plugin.junit.MojoProperty;
import com.kiwigrid.helm.maven.plugin.junit.SystemPropertyExtension;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.Os;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

@ExtendWith({ SystemPropertyExtension.class, MojoExtension.class })
@MojoProperty(name = "helmDownloadUrl", value = "https://get.helm.sh/helm-v2.14.3-linux-amd64.tar.gz")
@MojoProperty(name = "chartDirectory", value = "junit-helm")
@MojoProperty(name = "chartVersion", value = "0.0.1")
public class InitMojoTest {

	@DisplayName("Init helm with different download urls.")
	@ParameterizedTest
	@ValueSource(strings = { "darwin", "linux", "windows" })
	public void initMojoHappyPathWhenDownloadHelm(String os, InitMojo mojo) throws Exception {

		// prepare execution
		doNothing().when(mojo).callCli(contains("helm "), anyString(), anyBoolean());
		// getHelmExecuteablePath is system-depending and has to be mocked for that reason
		// as SystemUtils.IS_OS_WINDOWS will always return false on a *NIX system
		doReturn(Paths.get("dummy/path/to/helm").toAbsolutePath()).when(mojo).getHelmExecuteablePath();
		mojo.setHelmDownloadUrl(getOsSpecificDownloadURL(os));

		// run init
		mojo.execute();

		// check helm file
		Path helm = Paths.get(mojo.getHelmExecutableDirectory(), "windows".equals(os) ? "helm.exe" : "helm")
				.toAbsolutePath();
		assertTrue(Files.exists(helm), "Helm executable not found at: " + helm);
	}

	@Test
	public void verifyDefaultInitCommandWhenDownloadingHelm(InitMojo mojo) throws Exception {

		// prepare execution
		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).callCli(helmCommandCaptor.capture(), anyString(), anyBoolean());
		mojo.setHelmDownloadUrl(getOsSpecificDownloadURL());

		// run init
		mojo.execute();

		// check captured argument
		String helmInitCommand = helmCommandCaptor.getAllValues()
				.stream()
				.filter(cmd -> cmd.contains(Os.OS_FAMILY == Os.FAMILY_WINDOWS ? "helm.exe init" : "helm init"))
				.findAny().orElseThrow(() -> new IllegalArgumentException("Only one helm init command expected"));

		assertTrue(helmInitCommand.contains("--client-only"), "Option 'client-only' expected");
		assertFalse(helmInitCommand.contains("--skip-refresh"), "Option 'skip-refresh' must not be active by default.");
	}

	@Test
	public void initMojoSkipRefreshIfConfigured(InitMojo mojo) throws Exception {

		// prepare execution
		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).callCli(helmCommandCaptor.capture(), anyString(), anyBoolean());
		mojo.setHelmDownloadUrl(getOsSpecificDownloadURL());
		mojo.setSkipRefresh(true);

		// run init
		mojo.execute();

		// check captured argument
		List<String> helmCommands = helmCommandCaptor.getAllValues()
				.stream()
				.filter(cmd -> cmd.contains(Os.OS_FAMILY == Os.FAMILY_WINDOWS ? "helm.exe " : "helm "))
				.collect(Collectors.toList());
		assertEquals(1, helmCommands.size(), "Only helm init command expected");
		String helmInitCommand = helmCommands.get(0);
		assertTrue(helmInitCommand.contains("--skip-refresh"), "Option 'skip-refresh' expected");
	}

	@Test
	public void verifyLocalHelmBinaryUsage(InitMojo mojo) throws MojoExecutionException {
		// Because the download URL is hardcoded to linux, only proceed if the OS is indeed linux.
		assumeTrue(isOSUnix());

		final URL resource = this.getClass().getResource("helm.tar.gz");
		final String helmExecutableDir = new File(resource.getFile()).getParent();
		mojo.callCli("tar -xf "
				+ helmExecutableDir
				+ File.separator
				// flatten directory structure using --strip to get helm executeable on basedir, see https://www.systutorials.com/docs/linux/man/1-tar/#lbAS
				+ "helm.tar.gz --strip=1 --directory="
				+ helmExecutableDir, "Unable to unpack helm to " + helmExecutableDir, false);

		// configure mojo
		mojo.setUseLocalHelmBinary(true);
		mojo.setHelmExecutableDirectory(helmExecutableDir);

		// execute
		mojo.execute();
	}

	private boolean isOSUnix() {
		return System.getProperty("os.name").matches(".*n[i|u]x.*");
	}

	private String getOsSpecificDownloadURL() {
		return getOsSpecificDownloadURL(Os.OS_FAMILY == Os.FAMILY_UNIX ? "linux" : Os.OS_FAMILY);
	}

	private String getOsSpecificDownloadURL(final String os) {
		return "https://get.helm.sh/helm-v2.14.3-" + os + "-amd64." + ("windows".equals(os) ? "zip" : "tar.gz");
	}
}
