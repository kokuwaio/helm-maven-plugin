package io.kokuwa.maven.helm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.OS;

import io.kokuwa.maven.helm.junit.MojoExtension;
import io.kokuwa.maven.helm.pojo.K8SCluster;
import io.kokuwa.maven.helm.pojo.ValueOverride;

@DisplayName("helm:abstract")
public class HelmMojoTest extends AbstractMojoTest {

	@DisplayName("finding helm executable")
	@Nested
	class Executable {

		@DisplayName("fixed local executable")
		@Test
		@DisabledOnOs(OS.WINDOWS)
		void fixed(LintMojo mojo) {
			mojo.setUseLocalHelmBinary(true);
			mojo.setAutoDetectLocalHelmBinary(false);
			mojo.setHelmExecutableDirectory(MojoExtension.determineHelmExecutableDirectory().toFile());
			Path expected = MojoExtension.determineHelmExecutableDirectory().resolve(HELM);
			Path actual = assertDoesNotThrow(() -> mojo.getHelmExecutablePath());
			assertEquals(expected, actual);
		}

		@DisplayName("fixed local not found")
		@Test
		void fixedNotFound(LintMojo mojo) {
			mojo.setUseLocalHelmBinary(true);
			mojo.setAutoDetectLocalHelmBinary(false);
			mojo.setHelmExecutableDirectory(new File("src/nope"));
			MojoExecutionException exception = assertThrows(MojoExecutionException.class,
					() -> mojo.getHelmExecutablePath());
			assertEquals("Helm executable not found.", exception.getMessage());
		}

		@DisplayName("from path")
		@Test
		@EnabledIf("hasHelmFromPath")
		void path(LintMojo mojo) {
			mojo.setUseLocalHelmBinary(true);
			mojo.setAutoDetectLocalHelmBinary(true);
			Path expected = helmFromPath();
			Path actual = assertDoesNotThrow(() -> mojo.getHelmExecutablePath());
			assertEquals(expected, actual);
		}

		Path helmFromPath() {
			return Stream.of(System.getenv("PATH").split(Pattern.quote(File.pathSeparator)))
					.map(path -> Paths.get(path).resolve(HELM))
					.filter(Files::isRegularFile)
					.filter(Files::isExecutable)
					.findFirst().orElse(null);
		}

		boolean hasHelmFromPath() {
			return helmFromPath() != null;
		}
	}

	@DisplayName("flag exclude for getting chart directories")
	@Nested
	@DisabledOnOs(OS.WINDOWS)
	class Exclude {

		@DisplayName("empty")
		@Test
		void empty(LintMojo mojo) {
			mojo.setExcludes(new String[] {});
			mojo.setChartDirectory(new File("src/test/resources/dependencies"));
			assertHelm(mojo,
					"lint src/test/resources/dependencies/b",
					"lint src/test/resources/dependencies/a2",
					"lint src/test/resources/dependencies/a1",
					"lint src/test/resources/dependencies");
		}

		@DisplayName("single")
		@Test
		void single(LintMojo mojo) {
			mojo.setExcludes(new String[] { "src/test/resources/dependencies/a1" });
			mojo.setChartDirectory(new File("src/test/resources/dependencies"));
			assertHelm(mojo,
					"lint src/test/resources/dependencies/b",
					"lint src/test/resources/dependencies/a2",
					"lint src/test/resources/dependencies");
		}

		@DisplayName("multiple")
		@Test
		void multiple(LintMojo mojo) {
			mojo.setExcludes(
					new String[]
					{ "src/test/resources/dependencies/a1", "src/test/resources/dependencies/b" });
			mojo.setChartDirectory(new File("src/test/resources/dependencies"));
			assertHelm(mojo,
					"lint src/test/resources/dependencies/a2",
					"lint src/test/resources/dependencies");
		}

		@DisplayName("gobbling")
		@Test
		void gobbling(LintMojo mojo) {
			mojo.setExcludes(new String[] { "**/a*" });
			mojo.setChartDirectory(new File("src/test/resources/dependencies"));
			assertHelm(mojo,
					"lint src/test/resources/dependencies/b",
					"lint src/test/resources/dependencies");
		}
	}

	@DisplayName("all kinds of value overrides")
	@Nested
	class Override {

		@DisplayName("with override single set")
		@Test
		void overrideSetSingle(LintMojo mojo) {
			ValueOverride override = new ValueOverride().setOverrides(new LinkedHashMap<>());
			override.getOverrides().put("foo", "bar");
			mojo.setValues(override);
			assertHelm(mojo, "lint src/test/resources/simple --set foo=bar");
		}

		@DisplayName("with override multiple set")
		@Test
		void overrideSetMultiple(LintMojo mojo) {
			ValueOverride override = new ValueOverride().setOverrides(new LinkedHashMap<>());
			override.getOverrides().put("foo", "1");
			override.getOverrides().put("bar", "2");
			mojo.setValues(override);
			assertHelm(mojo, "lint src/test/resources/simple --set foo=1,bar=2");
		}

		@DisplayName("with override single string")
		@Test
		void overrideStringSingle(LintMojo mojo) {
			ValueOverride override = new ValueOverride().setStringOverrides(new LinkedHashMap<>());
			override.getStringOverrides().put("foo", "bar");
			mojo.setValues(override);
			assertHelm(mojo, "lint src/test/resources/simple --set-string foo=bar");
		}

		@DisplayName("with override multiple string")
		@Test
		void overrideStringMultiple(LintMojo mojo) {
			ValueOverride override = new ValueOverride().setStringOverrides(new LinkedHashMap<>());
			override.getStringOverrides().put("foo", "1");
			override.getStringOverrides().put("bar", "2");
			mojo.setValues(override);
			assertHelm(mojo, "lint src/test/resources/simple --set-string foo=1,bar=2");
		}

		@DisplayName("with override single file")
		@Test
		void overrideFileString(LintMojo mojo) {
			ValueOverride override = new ValueOverride().setFileOverrides(new LinkedHashMap<>());
			override.getFileOverrides().put("foo", "bar");
			mojo.setValues(override);
			assertHelm(mojo, "lint src/test/resources/simple --set-file foo=bar");
		}

		@DisplayName("with override multiple files")
		@Test
		void overrideFileMultiple(LintMojo mojo) {
			ValueOverride override = new ValueOverride().setFileOverrides(new LinkedHashMap<>());
			override.getFileOverrides().put("foo", "1");
			override.getFileOverrides().put("bar", "2");
			mojo.setValues(override);
			assertHelm(mojo, "lint src/test/resources/simple --set-file foo=1,bar=2");
		}

		@DisplayName("with values file")
		@Test
		void overrideFile(LintMojo mojo) {
			mojo.setValues(new ValueOverride().setYamlFile("values.yaml"));
			assertHelm(mojo, "lint src/test/resources/simple --values values.yaml");
		}

		@DisplayName("with values single file")
		@Test
		void overrideValuesSingle(LintMojo mojo) {
			mojo.setValues(new ValueOverride().setYamlFiles(Arrays.asList("foo")));
			assertHelm(mojo, "lint src/test/resources/simple --values foo");
		}

		@DisplayName("with values multiple files")
		@Test
		void overrideFileMultiple1(LintMojo mojo) {
			mojo.setValues(new ValueOverride().setYamlFiles(Arrays.asList("foo", "bar")));
			assertHelm(mojo, "lint src/test/resources/simple --values foo --values bar");
		}

		@DisplayName("with all overrides")
		@Test
		public void overrideAll(LintMojo mojo) {
			ValueOverride override = new ValueOverride()
					.setYamlFile("path/to/values.yaml")
					.setYamlFiles(Arrays.asList("path/to/values-1.yaml", "path/to/values-2.yaml"))
					.setOverrides(new LinkedHashMap<>())
					.setFileOverrides(new LinkedHashMap<>())
					.setStringOverrides(new LinkedHashMap<>());
			override.getOverrides().put("k1", "v1");
			override.getOverrides().put("k2", "v2");
			override.getStringOverrides().put("sk1", "sv1");
			override.getStringOverrides().put("sk2", "sv2");
			override.getFileOverrides().put("fk1", "path/to/file1.txt");
			override.getFileOverrides().put("fk2", "D:/absolute/path/to/file2.txt");
			mojo.setValues(override);
			assertHelm(mojo, "lint src/test/resources/simple"
					+ " --set k1=v1,k2=v2"
					+ " --set-string sk1=sv1,sk2=sv2"
					+ " --set-file fk1=path/to/file1.txt,fk2=D:/absolute/path/to/file2.txt"
					+ " --values path/to/values.yaml"
					+ " --values path/to/values-1.yaml"
					+ " --values path/to/values-2.yaml");
		}
	}

	@DisplayName("use k8s args")
	@Nested
	class K8SClusterArgs {

		@Test
		void empy(LintMojo mojo) {
			mojo.setK8sCluster(null);
			assertHelm(mojo, "lint src/test/resources/simple");
		}

		@Test
		void notConfigured(LintMojo mojo) {
			mojo.setK8sCluster(new K8SCluster());
			assertHelm(mojo, "lint src/test/resources/simple");
		}

		@Test
		void apiUrl(LintMojo mojo) {
			mojo.setK8sCluster(new K8SCluster().setApiUrl("custom-api-url"));
			assertHelm(mojo, "lint src/test/resources/simple --kube-apiserver custom-api-url");
		}

		@Test
		void asUser(LintMojo mojo) {
			mojo.setK8sCluster(new K8SCluster().setAsUser("custom-user"));
			assertHelm(mojo, "lint src/test/resources/simple --kube-as-user custom-user");
		}

		@Test
		void asGroup(LintMojo mojo) {
			mojo.setK8sCluster(new K8SCluster().setAsGroup("custom-group"));
			assertHelm(mojo, "lint src/test/resources/simple --kube-as-group custom-group");
		}

		@Test
		void token(LintMojo mojo) {
			mojo.setK8sCluster(new K8SCluster().setToken("custom-token"));
			assertHelm(mojo, "lint src/test/resources/simple --kube-token custom-token");
		}

		@Test
		void all(LintMojo mojo) {
			mojo.setK8sCluster(new K8SCluster()
					.setApiUrl("custom-api-url")
					.setAsUser("custom-user")
					.setAsGroup("custom-group")
					.setToken("custom-token"));
			assertHelm(mojo, "lint src/test/resources/simple"
					+ " --kube-apiserver custom-api-url"
					+ " --kube-as-user custom-user"
					+ " --kube-as-group custom-group"
					+ " --kube-token custom-token");
		}
	}
}
