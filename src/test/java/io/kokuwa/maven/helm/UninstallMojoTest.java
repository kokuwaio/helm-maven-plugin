package io.kokuwa.maven.helm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link UninstallMojoTest}
 *
 * @author stephan.schnabel@posteo.de
 * @since 6.10.0
 */
@DisplayName("helm:uninstall")
public class UninstallMojoTest extends AbstractMojoTest {

	@DisplayName("default values")
	@Test
	void install(UninstallMojo mojo) {
		mojo.setSkipUninstall(false);
		assertHelm(mojo, "uninstall simple");
	}

	@DisplayName("with flag skip")
	@Test
	void skip(UninstallMojo mojo) {
		assertHelm(mojo);
		assertHelm(mojo.setSkipUninstall(false).setSkip(true));
		assertHelm(mojo.setSkipUninstall(true).setSkip(false));
		assertHelm(mojo.setSkipUninstall(true).setSkip(true));
	}

	@DisplayName("with flag keep-history")
	@Test
	void keepHistory(UninstallMojo mojo) {
		mojo.setSkipUninstall(false);
		mojo.setUninstallKeepHistory(true);
		assertHelm(mojo, "uninstall simple --keep-history");
	}

	@DisplayName("with flag no-hooks")
	@Test
	void noHooks(UninstallMojo mojo) {
		mojo.setSkipUninstall(false);
		mojo.setUninstallNoHooks(true);
		assertHelm(mojo, "uninstall simple --no-hooks");
	}

	@DisplayName("with flag ignore-not-found")
	@Test
	void ignoreNotFound(UninstallMojo mojo) {
		mojo.setSkipUninstall(false);
		mojo.setUninstallIgnoreNotFound(true);
		assertHelm(mojo, "uninstall simple --ignore-not-found");
	}

	@DisplayName("with flags cascade background")
	@Test
	void cascade(UninstallMojo mojo) {
		mojo.setSkipUninstall(false);
		mojo.setUninstallCascade("background");
		assertHelm(mojo, "uninstall simple --cascade background");
	}

	@DisplayName("with flags wait and timeout")
	@Test
	void timeout(UninstallMojo mojo) {
		mojo.setSkipUninstall(false);
		mojo.setUninstallWait(true);
		mojo.setUninstallTimeout(41);
		assertHelm(mojo, "uninstall simple --wait --timeout 41s");
	}

	@DisplayName("with release name")
	@Test
	void releaseName(UninstallMojo mojo) {
		mojo.setSkipUninstall(false);
		mojo.setReleaseName("foo");
		assertHelm(mojo, "uninstall foo");
	}

	@DisplayName("with release name and multiple charts")
	@Test
	void releaseNameWithMultipleCharts(UninstallMojo mojo) {
		mojo.setSkipUninstall(false);
		mojo.setReleaseName("foo");
		mojo.setChartDirectory(new File("src/test/resources/dependencies"));
		String message = assertThrows(MojoExecutionException.class, mojo::execute).getMessage();
		assertEquals("For multiple charts releaseName is not supported.", message);
	}
}
