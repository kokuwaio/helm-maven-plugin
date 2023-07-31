package io.kokuwa.maven.helm.pojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/** Wrapper for helm executeable to execute commands. */
@RequiredArgsConstructor
public class HelmExecutable {

	private final Log log;
	@Getter
	private final Path executable;
	private final List<String> arguments = new ArrayList<>();
	private final Set<String> sensitiveFlags = new HashSet<>(Arrays.asList("password", "kube-token"));
	private final Map<String, List<String>> flags = new LinkedHashMap<>();
	@Setter
	private String stdin;

	public HelmExecutable arguments(Object... args) {
		Stream.of(args).filter(Objects::nonNull).map(Object::toString).forEach(arguments::add);
		return this;
	}

	public HelmExecutable flag(String key) {
		return flag(key, true);
	}

	public HelmExecutable flag(String key, boolean value) {
		if (value) {
			flags.put(key, Arrays.asList());
		}
		return this;
	}

	public HelmExecutable flag(String key, File file) {
		if (file != null) {
			flag(key, file.toPath());
		}
		return this;
	}

	public HelmExecutable flag(String key, Path path) {
		if (path != null) {
			flags.computeIfAbsent(key, k -> new ArrayList<>()).add(path.toString());
		}
		return this;
	}

	public HelmExecutable flag(String key, String value) {
		if (StringUtils.isNotBlank(value)) {
			flags.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
		}
		return this;
	}

	/**
	 * Execute helm command.
	 *
	 * @param errorMessage Error message if exit code is not zero.
	 * @throws MojoExecutionException Failed to execute helm command.
	 */
	public void execute(String errorMessage) throws MojoExecutionException {

		String command = Stream.of(toCommand(true)).collect(Collectors.joining(" "));
		log.debug("Execute: " + command);

		try {

			Process process = Runtime.getRuntime().exec(toCommand(false));

			// write to stdin

			if (StringUtils.isNotEmpty(stdin)) {
				try (OutputStream output = process.getOutputStream()) {
					output.write(stdin.getBytes(StandardCharsets.UTF_8));
				} catch (IOException e) {
					log.error("failed to write to stdin of helm", e);
				}
			}

			// redirect helm output to maven log

			new Thread(() -> {
				try (InputStream input = process.getInputStream()) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
					String line;
					while ((line = reader.readLine()) != null) {
						log.info(line);
					}
				} catch (IOException e) {
					log.error("Failed to redirect input", e);
				}
			}).start();
			new Thread(() -> {
				try (InputStream error = process.getErrorStream()) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(error, StandardCharsets.UTF_8));
					String line;
					while ((line = reader.readLine()) != null) {
						log.error(line);
					}
				} catch (IOException e) {
					log.error("Failed to redirect errors", e);
				}
			}).start();

			// wait for process to finish

			if (process.waitFor() != 0) {
				throw new MojoExecutionException(errorMessage);
			}

		} catch (IOException | InterruptedException e) {
			throw new MojoExecutionException("Error processing command: " + command, e);
		}
	}

	/**
	 * Returns this command as strings for {@link Runtime#exec(String[])}
	 *
	 * @param stripSensitiveFlags <code>true</code> if passwords and tokens should be masked.
	 * @return Strings for helm command.
	 */
	private String[] toCommand(boolean stripSensitiveFlags) {
		List<String> command = new ArrayList<>();
		command.add(executable.toString());
		command.addAll(arguments);
		for (Entry<String, List<String>> entry : flags.entrySet()) {
			String flag = entry.getKey();
			List<String> values = entry.getValue();
			if (values.isEmpty()) {
				command.add("--" + flag);
			}
			for (String value : values) {
				command.add("--" + flag);
				command.add(stripSensitiveFlags && sensitiveFlags.contains(flag) ? "********" : value);
			}
		}
		return command.toArray(new String[command.size()]);
	}
}
