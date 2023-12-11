package io.kokuwa.maven.helm.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.kokuwa.maven.helm.pojo.HelmChart;
import io.kokuwa.maven.helm.pojo.HelmChart.Dependency;

/**
 * Utility class for overwriting a local path charts within a chart's dependencies.
 *
 * @since 6.10.0
 */
public class DependencyOverwriter {

	private static final ObjectMapper MAPPER = new YAMLMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	private final Log log;
	private final String overwriteRepository;
	private final String overwriteVersion;
	private Path writeDirectory;

	/**
	 * Constructor for setting constants
	 *
	 * @param overwriteRepository used to overwrite a local path chart's repository
	 * @param overwriteVersion    used to overwrite a local path chart's version
	 * @param log                 used to write output from the util
	 */
	public DependencyOverwriter(String overwriteRepository, String overwriteVersion, Log log) {
		this.overwriteRepository = overwriteRepository;
		this.overwriteVersion = overwriteVersion;
		this.log = log;
	}

	/**
	 * Used in testing for setting a custom filePath to write the new Chart.yaml to
	 *
	 * @param writeDirectory {@link Path} directory to write the new Chart.yaml to
	 */
	public void setWriteDirectory(Path writeDirectory) {
		this.writeDirectory = writeDirectory;
	}

	/**
	 * Utility class for overwriting local chart dependencies when making use of helm.overwriteLocalDependencies
	 *
	 * @param directory {@link Path} directory to overwrite the Chart.yaml
	 * @throws MojoExecutionException Must set a value for helm.overwriteDependencyRepository
	 */
	public void execute(Path directory) throws MojoExecutionException {

		// read dependencies from chart file

		Path chartFile = directory.resolve("Chart.yaml");
		if (!Files.isRegularFile(chartFile)) {
			log.warn("No Charts detected - no Chart.yaml files found below " + directory);
			return;
		}
		List<Dependency> dependencies;
		try {
			dependencies = MAPPER.readValue(chartFile.toFile(), HelmChart.class).getDependencies();
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to read chart dependencies from " + chartFile, e);
		}
		if (dependencies == null || dependencies.isEmpty()) {
			log.info("No dependencies found for " + chartFile);
			return;
		}

		// Sets the boolean for the {@link Dependency} that need to be overwritten

		for (Dependency dependency : dependencies) {
			if (dependency.getRepository() != null && dependency.getRepository().startsWith("file://")) {
				dependency.setOverwrite(true);
			}
		}

		// check if any dependencies need to be overwritten

		if (dependencies.stream().anyMatch(Dependency::isOverwrite)) {
			overwriteDependencies(dependencies, directory, chartFile);
		}
	}

	/**
	 * Overwrite the dependencies with their new values to the Chart.yaml
	 *
	 * @param dependencies {@link ArrayList}<{@link Dependency}>
	 * @param chartFile    Chart.yaml file to read
	 * @param directory    Directory to write the new Chart.yaml to
	 * @throws MojoExecutionException unable to read/write Chart.yaml
	 */
	private void overwriteDependencies(List<Dependency> dependencies, Path directory, Path chartFile)
			throws MojoExecutionException {

		// delete the outdated Chart.lock

		Path chartLock = directory.resolve("Chart.lock");
		try {
			if (Files.deleteIfExists(chartLock)) {
				log.info("Lock deleted at " + chartLock);
			} else {
				log.info("No lock file found at " + chartLock);
			}
		} catch (IOException e) {
			log.warn("Failed to delete " + chartLock);
		}

		List<String> overwrittenChart = new ArrayList<>();
		List<String> originalChart;
		try {
			originalChart = Files.readAllLines(chartFile, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to read " + chartFile, e);
		}

		// Used for determining when a dependency has been read
		boolean name, repository, version, isDependencies;
		name = version = repository = isDependencies = false;

		Iterator<Dependency> dependencyIterator = dependencies.iterator();
		Dependency currentDependency = dependencyIterator.next();

		for (String line : originalChart) {
			// check for lines that need to be overwritten
			if (isDependencies) {
				if (line.contains("version:")) {
					version = true;

					// if no overwrite value is provided for the version, the original will be used
					if (currentDependency.isOverwrite() && overwriteVersion != null && !overwriteVersion.isEmpty()) {
						log.info("Overwriting dependency '" + currentDependency.getName() + "' with new version:");
						log.info("\tOld Value:");
						log.info(line);

						// preserve any characters/spacing that may appear around the attribute
						line = line.replace(currentDependency.getVersion(), overwriteVersion);

						log.info("\tNew Value:");
						log.info(line);
					}
				} else if (line.contains("repository:")) {
					repository = true;

					if (currentDependency.isOverwrite()) {
						log.info("Overwriting dependency '" + currentDependency.getName() + "' with new repository:");
						log.info("\tOld Value:");
						log.info(line);

						// preserve any characters/spacing that may appear around the attribute
						line = line.replace(currentDependency.getRepository(), overwriteRepository);

						log.info("\tNew Value:");
						log.info(line);
					}
				} else if (line.contains("name:")) {
					name = true;
				}

				// Repository is not a required field so could be null
				if (currentDependency.getRepository() == null) {
					repository = true;
				}

				// check if the current dependency has been read completely
				if (name && version && repository) {
					name = version = repository = false;

					// check if there are any remaining dependencies
					if (dependencyIterator.hasNext()) {
						currentDependency = dependencyIterator.next();
					} else {
						isDependencies = false;
					}
				}
			} else if (line.contains("dependencies:")) {
				// determines whether currently in the dependencies section of the yaml
				isDependencies = true;
			}

			overwrittenChart.add(line);
		}

		// overwrite the existing Chart.yaml with the new values
		try {
			Path path = writeDirectory != null ? writeDirectory.resolve("Chart.yaml") : chartFile;
			Files.write(path, overwrittenChart, StandardCharsets.UTF_8);
			log.info("Overwriting successful for " + path);
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to overwrite Chart.yaml at " + directory, e);
		}
	}
}
