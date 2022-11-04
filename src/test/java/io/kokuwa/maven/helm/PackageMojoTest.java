package io.kokuwa.maven.helm;

import static org.mockito.Mockito.when;

import java.io.File;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("helm:package")
public class PackageMojoTest extends AbstractMojoTest {

	@DisplayName("default values")
	@Test
	void packageExecute(PackageMojo mojo) {
		assertHelm(mojo, "package src/test/resources/simple --destination target/helm/repo");
	}

	@DisplayName("with flag skip")
	@Test
	void skip(PackageMojo mojo) {
		assertHelm(mojo.setSkipPackage(false).setSkip(true));
		assertHelm(mojo.setSkipPackage(true).setSkip(false));
		assertHelm(mojo.setSkipPackage(true).setSkip(true));
	}

	@DisplayName("with dependencies")
	@Test
	void dependencies(PackageMojo mojo) {
		mojo.setChartDirectory(new File("src/test/resources/dependencies"));
		assertHelm(mojo,
				"package src/test/resources/dependencies/b --destination target/helm/repo",
				"package src/test/resources/dependencies/a2 --destination target/helm/repo",
				"package src/test/resources/dependencies/a1 --destination target/helm/repo",
				"package src/test/resources/dependencies --destination target/helm/repo");
	}

	@DisplayName("with appVersion")
	@Test
	void appVersion(PackageMojo mojo) {
		mojo.setAppVersion("0815");
		assertHelm(mojo, "package src/test/resources/simple --destination target/helm/repo --app-version 0815");
	}

	@DisplayName("with chartVersion stable")
	@Test
	void chartVersionStable(PackageMojo mojo) {
		mojo.setChartVersion("0.0.1");
		assertHelm(mojo, "package src/test/resources/simple --destination target/helm/repo --version 0.0.1");
	}

	@DisplayName("with chartVersion snapshot")
	@Test
	void chartVersionSnapshot(PackageMojo mojo) {
		mojo.setChartVersion("0.0.1-SNAPSHOT");
		assertHelm(mojo, "package src/test/resources/simple --destination target/helm/repo --version 0.0.1-SNAPSHOT");
	}

	@DisplayName("with chartVersion timestamped snapshot")
	@Test
	void chartVersionSnapshotTimestamp(PackageMojo mojo) {
		when(mojo.getTimestamp()).thenReturn(LocalDateTime.of(1996, 10, 15, 23, 56, 12));
		mojo.setChartVersion("0.0.1-SNAPSHOT");
		mojo.setTimestampOnSnapshot(true);
		assertHelm(mojo,
				"package src/test/resources/simple --destination target/helm/repo --version 0.0.1-19961015235612");
	}

	@DisplayName("with version timestamped snapshot and custom format")
	@Test
	void versionSnapshotTimestampCustom(PackageMojo mojo) {
		when(mojo.getTimestamp()).thenReturn(LocalDateTime.of(1996, 10, 15, 23, 56, 12));
		mojo.setChartVersion("0.0.1-SNAPSHOT");
		mojo.setTimestampOnSnapshot(true);
		mojo.setTimestampFormat("yyyyMMdd-HHmmss");
		assertHelm(mojo,
				"package src/test/resources/simple --destination target/helm/repo --version 0.0.1-19961015-235612");
	}

	@DisplayName("with keyring without password")
	@Test
	void keyringWithoutPassword(PackageMojo mojo) {
		mojo.setKeyring("foo");
		mojo.setKey("bar");
		assertHelm(mojo,
				"package src/test/resources/simple --destination target/helm/repo --sign --keyring foo --key bar");
	}

	@DisplayName("with keyring with password")
	@Test
	void keyringWithPassword(PackageMojo mojo) {
		mojo.setKeyring("foo");
		mojo.setKey("bar");
		mojo.setPassphrase("secret");
		assertHelm(mojo, "package src/test/resources/simple --destination target/helm/repo"
				+ " --sign --keyring foo --key bar --passphrase-file -");
	}
}
