package com.kiwigrid.helm.maven.plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import com.kiwigrid.helm.maven.plugin.junit.MojoExtension;
import com.kiwigrid.helm.maven.plugin.junit.MojoProperty;
import com.kiwigrid.helm.maven.plugin.junit.SystemPropertyExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;

@ExtendWith({ SystemPropertyExtension.class, MojoExtension.class })
@MojoProperty(name = "helmDownloadUrl", value = "https://kubernetes-helm.storage.googleapis.com/helm-v2.7.2-linux-amd64.tar.gz")
@MojoProperty(name = "chartDirectory", value = "junit-helm")
@MojoProperty(name = "chartVersion", value = "0.0.1")
public class HelmMojoTest {

	@DisplayName("Init helm with different download urls.")
	@ParameterizedTest
	@ValueSource(strings = { "darwin", "linux", "windows" })
	public void initMojoHappyPath(String os, InitMojo mojo) throws Exception {

		// prepare execution

		doNothing().when(mojo).callCli(contains("helm "), anyString(), anyBoolean());
		mojo.setHelmDownloadUrl("https://kubernetes-helm.storage.googleapis.com/helm-v2.9.1-" + os + "-amd64.tar.gz");

		// run init

		mojo.execute();

		// check helm file

		Path helm = Paths.get(mojo.getHelmExecuteableDirectory(), "windows".equals(os) ? "helm.exe" : "helm")
				.toAbsolutePath();
		assertTrue(Files.exists(helm), "Helm executable not found at: " + helm);
	}

	@Test
	public void verifyDefaultInitCommand(InitMojo mojo) throws Exception {

		// prepare execution
		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).callCli(helmCommandCaptor.capture(), anyString(), anyBoolean());
		mojo.setHelmDownloadUrl("https://kubernetes-helm.storage.googleapis.com/helm-v2.9.1-linux-amd64.tar.gz");

		// run init
		mojo.execute();

		// check captured argument
		List<String> helmCommands = helmCommandCaptor.getAllValues()
				.stream()
				.filter(cmd -> cmd.contains("helm "))
				.collect(Collectors.toList());

		assertEquals(1, helmCommands.size(), "Only helm init command expected");
		String helmInitCommand = helmCommands.get(0);
		assertTrue(helmInitCommand.contains("--client-only"), "Option 'client-only' expected");
		assertFalse(helmInitCommand.contains("--skip-refresh"), "Option 'skip-refresh' must not be active by default.");
	}

	@Test
	public void initMojoSkipRefreshIfConfigured(InitMojo mojo) throws Exception {

		// prepare execution
		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).callCli(helmCommandCaptor.capture(), anyString(), anyBoolean());
		mojo.setHelmDownloadUrl("https://kubernetes-helm.storage.googleapis.com/helm-v2.9.1-linux-amd64.tar.gz");
		mojo.setSkipRefresh(true);

		// run init
		mojo.execute();

		// check captured argument
		List<String> helmCommands = helmCommandCaptor.getAllValues()
				.stream()
				.filter(cmd -> cmd.contains("helm "))
				.collect(Collectors.toList());
		assertEquals(1, helmCommands.size(), "Only helm init command expected");
		String helmInitCommand = helmCommands.get(0);
		assertTrue(helmInitCommand.contains("--skip-refresh"), "Option 'skip-refresh' expected");
	}
}