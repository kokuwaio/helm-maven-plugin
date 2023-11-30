package io.kokuwa.maven.helm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.settings.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.kokuwa.maven.helm.junit.MojoExtension;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@ExtendWith(MojoExtension.class)
public abstract class AbstractMojoTest {

	/** Path of helm executable. */
	static final Path HELM = Paths.get(SystemUtils.IS_OS_WINDOWS ? "helm.exe" : "helm");
	/** Basic auth for foo:secret. */
	static final String BASIC_FOO_SECRET = "Basic Zm9vOnNlY3JldA==";
	/** Encrypted string "secret" using {@link #SETTINGS_SECURITY_XML} */
	static final String SECRET_ENCRYPTED = "{wdKoxh4nz/EHtUjMHT2D80nzmLMVmhtb/Ote2nPOQtQ=}";
	/** Path for security settings used in tests. */
	static final String SETTINGS_SECURITY_XML = "src/test/resources/settings-security.xml";

	@BeforeEach
	void reset(InitMojo mojo) throws IOException {
		Files.deleteIfExists(mojo.getOutputDirectory().resolve("app-0.1.0.tgz"));
		Authenticator.setDefault(null);
	}

	static void assertHelm(AbstractHelmMojo mojo, String... commands) {

		// setup log

		ArgumentCaptor<CharSequence> messages = ArgumentCaptor.forClass(CharSequence.class);
		Log log = Mockito.spy(SystemStreamLog.class);
		Mockito.doReturn(true).when(log).isDebugEnabled();
		Mockito.doReturn(true).when(log).isInfoEnabled();
		Mockito.doReturn(true).when(log).isWarnEnabled();
		Mockito.doReturn(true).when(log).isErrorEnabled();
		Mockito.doNothing().when(log).debug(messages.capture());
		Mockito.doNothing().when(log).info(messages.capture());
		Mockito.doNothing().when(log).warn(messages.capture());
		Mockito.doNothing().when(log).error(messages.capture());
		Mockito.doNothing().when(log).debug(messages.capture(), ArgumentMatchers.any(Throwable.class));
		Mockito.doNothing().when(log).info(messages.capture(), ArgumentMatchers.any(Throwable.class));
		Mockito.doNothing().when(log).warn(messages.capture(), ArgumentMatchers.any(Throwable.class));
		Mockito.doNothing().when(log).error(messages.capture(), ArgumentMatchers.any(Throwable.class));

		// setup runtime

		ArgumentCaptor<String[]> actualCommands = ArgumentCaptor.forClass(String[].class);
		Runtime runtime = Mockito.mock(Runtime.class);
		Process process = Mockito.mock(Process.class);
		Mockito.doReturn(new ByteArrayInputStream(new byte[0])).when(process).getInputStream();
		Mockito.doReturn(new ByteArrayInputStream(new byte[0])).when(process).getErrorStream();
		Mockito.doReturn(new ByteArrayOutputStream()).when(process).getOutputStream();
		assertDoesNotThrow(() -> Mockito.doReturn(process).when(runtime).exec(actualCommands.capture()));
		assertDoesNotThrow(() -> Mockito.doReturn(0).when(process).waitFor());

		// execute mojo

		try (MockedStatic<Runtime> mockedRuntime = Mockito.mockStatic(Runtime.class)) {
			mockedRuntime.when(Runtime::getRuntime).thenReturn(runtime);
			mojo.setLog(log);
			mojo.execute();
		} catch (Exception e) {
			fail("Failed to execute mojo", e);
		}

		// check logs for secrets and passwords

		assertEquals(Collections.emptySet(), messages.getAllValues().stream()
				.map(String::valueOf).map(String::toLowerCase)
				.filter(message -> message.contains("secret"))
				.collect(Collectors.toSet()),
				"found secrets in log statements");

		// check commands

		List<String> actual = actualCommands.getAllValues().stream()
				.map(command -> Stream.of(command)
						// remove helm executable as first entry
						.skip(1)
						// replace windows path
						.map(part -> part.replaceAll(Pattern.quote("\\"), "/"))
						// join to simply test inputs
						.collect(Collectors.joining(" ")))
				.collect(Collectors.toList());
		List<String> expected = Stream.of(commands)
				// replace windows path
				.map(part -> part.replaceAll(Pattern.quote("\\"), "/"))
				.collect(Collectors.toList());
		assertEquals(expected, actual, "commands differ");
	}

	static Server getServer(String id, String username, String password) {
		Server server = new Server();
		server.setId(id);
		server.setUsername(username);
		server.setPassword(password);
		return server;
	}

	static Path copyPackagedHelmChartToOutputdirectory(AbstractHelmMojo mojo) {
		Path source = Paths.get("src/test/resources/__files/app-0.1.0.tgz");
		Path target = mojo.getOutputDirectory().resolve("app-0.1.0.tgz");
		assertDoesNotThrow(() -> Files.createDirectories(target.getParent()));
		assertDoesNotThrow(() -> Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING));
		return target;
	}
}
