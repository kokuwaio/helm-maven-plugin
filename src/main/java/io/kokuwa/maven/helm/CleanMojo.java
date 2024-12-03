package io.kokuwa.maven.helm;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Cleanups helm specific directory {@code charts} and {@code Chart.lock} file.
 *
 * @author Slawomir Jaranowski
 * @since 6.17.0
 */
@Mojo(name = "clean", defaultPhase = LifecyclePhase.CLEAN, threadSafe = true)
public class CleanMojo extends AbstractChartDirectoryMojo {

	/**
	 * Set this to <code>true</code> to skip invoking clean goal.
	 *
	 * @since 6.17.0
	 */
	@Parameter(property = "helm.clean.skip", defaultValue = "false")
	private boolean skipClean;

	/**
	 * Indicates whether the build will continue even if there are clean errors.
	 *
	 * @since 6.17.0
	 */
	@Parameter(property = "helm.clean.failOnError", defaultValue = "true")
	private boolean cleanFailOnError;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipClean) {
			getLog().info("Skip clean");
			return;
		}

		for (Path chartDirectory : getChartDirectories()) {
			getLog().info("Cleanups chart " + chartDirectory + "...");
			Path chartsPath = chartDirectory.resolve("charts");
			if (Files.exists(chartsPath)) {
				try {
					try (Stream<Path> paths = Files.walk(chartsPath)) {
						paths.sorted(Comparator.reverseOrder()).forEach(this::delete);
					}
					delete(chartDirectory.resolve("Chart.lock"));
				} catch (IOException e) {
					throw new MojoExecutionException("Failed to cleanups chart " + chartDirectory, e);
				}
			}
		}
	}

	private void delete(Path path) {
		try {
			if (getLog().isDebugEnabled()) {
				getLog().debug("Deleting " + path);
			}
			Files.deleteIfExists(path);
		} catch (IOException e) {
			if (cleanFailOnError) {
				throw new UncheckedIOException(e);
			} else {
				if (getLog().isDebugEnabled()) {
					getLog().debug("Failed to delete " + path, e);
				} else {
					getLog().warn("Failed to delete " + path + ": " + e.getMessage());
				}
			}
		}
	}
}
