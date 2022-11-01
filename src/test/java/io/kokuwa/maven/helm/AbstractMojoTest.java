package io.kokuwa.maven.helm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.Authenticator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.settings.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import io.kokuwa.maven.helm.junit.MojoExtension;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@ExtendWith(MojoExtension.class)
public abstract class AbstractMojoTest {

	/** Path of helm executeable. */
	static final Path HELM = new LintMojo().getHelmExecuteableName();
	/** Basic auth for foo:secret. */
	static final String BASIC_FOO_SECRET = "Basic Zm9vOnNlY3JldA==";
	/** Encrypted string "secret" using {@link #SETTINGS_SECURITY_XML} */
	static final String SECRET_ENCRYPTED = "{wdKoxh4nz/EHtUjMHT2D80nzmLMVmhtb/Ote2nPOQtQ=}";
	/** Path for security settings used in tests. */
	static final String SETTINGS_SECURITY_XML = "src/test/resources/settings-security.xml";

	@BeforeEach
	void reset(InitMojo mojo) throws IOException {
		Files.deleteIfExists(Paths.get(mojo.getOutputDirectory()).resolve("app-0.1.0.tgz"));
		Authenticator.setDefault(null);
	}

	static void assertHelm(AbstractHelmMojo mojo, String... commands) {

		ArgumentCaptor<String> commandCaptor = ArgumentCaptor.forClass(String.class);
		assertDoesNotThrow(() -> Mockito.doNothing()
				.when(mojo)
				.helm(commandCaptor.capture(), ArgumentMatchers.anyString(), ArgumentMatchers.any()), "mockito failed");
		assertDoesNotThrow(() -> mojo.execute(), "failed to execute mojo");

		List<String> actual = commandCaptor.getAllValues().stream()
				// do some sanitizing on spaces
				.map(command -> command.trim().replaceAll(" ( )+", " "))
				// replace windows path
				.map(command -> command.replaceAll(Pattern.quote("\\"), "/"))
				.collect(Collectors.toList());
		List<String> expected = Stream.of(commands)
				// replace windows path
				.map(command -> command.replaceAll(Pattern.quote("\\"), "/"))
				.collect(Collectors.toList());
		assertEquals(expected, actual, "got commands: \n" + actual.stream().collect(Collectors.joining("\n")));
	}

	static Server getServer(String id, String username, String password) {
		Server server = new Server();
		server.setId(id);
		server.setUsername(username);
		server.setPassword(password);
		return server;
	}

	static Path copyPackagedHelmChartToOutputdirectory(AbstractHelmMojo mojo) {
		Path source = Paths.get("src/test/resources/app-0.1.0.tgz");
		Path target = Paths.get(mojo.getOutputDirectory()).resolve("app-0.1.0.tgz");
		assertDoesNotThrow(() -> Files.createDirectories(target.getParent()));
		assertDoesNotThrow(() -> Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING));
		return target;
	}
}
