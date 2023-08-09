package io.kokuwa.maven.helm.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.kokuwa.maven.helm.pojo.Dependencies;
import io.kokuwa.maven.helm.pojo.Dependencies.Dependency;

/**
 * Utility class for overwriting a local path charts within a chart's dependencies.
 *
 * @since 6.9.1
 */
public class DependencyOverwriter {

	private File writeDirectory;

	private String overwriteRepository;

	private String overwriteVersion;

	private Log log;

	/**
	 * Constructor for setting constants
	 * @param overwriteRepository used to overwrite a local path chart's repository
	 * @param overwriteVersion used to overwrite a local path chart's version
	 * @param log used to write output from the util
	 */
	public DependencyOverwriter(String overwriteRepository, String overwriteVersion, Log log) {
		this.overwriteRepository = overwriteRepository;
		this.overwriteVersion = overwriteVersion;
		this.log = log;
	}
	
	/**
	 * Used in testing for setting a custom filePath to write the new Chart.yaml to
	 * @param directory {@link File} directory to write the new Chart.yaml to
	 */
	public void setWriteDirectory(File directory) {
		this.writeDirectory = directory;
	}

	/**
	 * Read in the dependencies from the Chart.yaml into the {@link Dependencies} POJO
	 * @param chartFile Chart.yaml file to read
	 * @return {@link ArrayList}<{@link Dependency}>
	 * @throws MojoExecutionException Unable to read Chart.yaml dependencies
	 */
	private ArrayList<Dependency> getDependencies(File chartFile) throws MojoExecutionException {
		ObjectMapper yamlMapper = new YAMLMapper();
		yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		Dependencies dependencies;
		try {
			dependencies = yamlMapper.readValue(chartFile, Dependencies.class);
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to read chart dependencies from " + chartFile, e);
		}

		return dependencies.getDependencies();
	}

	/**
	 * Sets the boolean for the {@link Dependency} that need to be overwritten
	 * @param dependencies {@Link ArrayList}<{@link Dependency}>
	 */
	private void updateDependencies(ArrayList<Dependency> dependencies) {
		for (Dependency dependency : dependencies) {
			if (dependency.getRepository() != null && dependency.getRepository().startsWith("file://")) {
				dependency.setOverwrite(true);
			}
		}
	}

	/**
	 * Overwrite the dependencies with their new values to the Chart.yaml 
	 * @param dependencies {@link ArrayList}<{@link Dependency}>
	 * @param chartFile Chart.yaml file to read
	 * @param directory Directory to write the new Chart.yaml to
	 * @throws MojoExecutionException unable to read/write Chart.yaml
	 */
	private void overwriteDependencies(ArrayList<Dependency> dependencies, File chartFile, Path directory) 
		throws MojoExecutionException {

		//delete the outdated Chart.lock
		File chartLock = new File(directory.toString() + "/Chart.lock");
		
		if (chartLock.exists()) {
			chartLock.delete();
			this.log.info("Chart.lock deleted at " + directory);
		} else {
			this.log.info("No Chart.lock file found at " + directory);
		}

		List<String> overwrittenChart = new ArrayList<>();
		List<String> originalChart;
		try {
			originalChart = Files.readAllLines(Paths.get(chartFile.toURI()), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to read Chart.yaml at " + directory, e);
		}

		//Used for determining when a dependency has been read
		boolean name, repository, version, isDependencies;
		name = version = repository = isDependencies = false;

		Iterator<Dependency> dependencyIterator = dependencies.iterator();
		Dependency currentDependency = dependencyIterator.next();

		for (String line : originalChart) {
			//check for lines that need to be overwritten
			if (isDependencies) {
				if (line.contains("version:")) { 
					version = true;
					
					//if no overwrite value is provided for the version, the original will be used
					if (currentDependency.isOverwrite() && this.overwriteVersion != null 
						&& !this.overwriteVersion.isEmpty()) {
						this.log.info("Overwriting dependency '" + currentDependency.getName() + 
							"' with new version:");
						this.log.info("\tOld Value:");
						this.log.info(line);

						//preserve any characters/spacing that may appear around the attribute
						line = line.replace(currentDependency.getVersion(), this.overwriteVersion);

						this.log.info("\tNew Value:");
						this.log.info(line);
					}
				} else if (line.contains("repository:")) {
					repository = true;

					if (currentDependency.isOverwrite()) {
						this.log.info("Overwriting dependency '" + currentDependency.getName() + 
							"' with new repository:");
						this.log.info("\tOld Value:");
						this.log.info(line);

						//preserve any characters/spacing that may appear around the attribute
						line = line.replace(currentDependency.getRepository(), this.overwriteRepository);

						this.log.info("\tNew Value:");
						this.log.info(line);
					}
				} else if (line.contains("name:")) { 
						name = true;
				}

				//Repository is not a required field so could be null
				if (currentDependency.getRepository() == null) {
					repository = true;
				}

				//check if the current dependency has been read completely
				if (name && version && repository) {
					name = version = repository = false;

					//check if there are any remaining dependencies
					if (dependencyIterator.hasNext()) {
						currentDependency = dependencyIterator.next();
					} else {
						isDependencies = false;
					}
				}
			} 
			//determines whether currently in the dependencies section of the yaml
			else if (line.contains("dependencies:")) {
				isDependencies = true;
			}

			overwrittenChart.add(line);
		}

		//overwrite the existing Chart.yaml with the new values
		try {
			File newFile;
			if (this.writeDirectory != null) {
				newFile = new File(this.writeDirectory.toString() + "/Chart.yaml");
			} else {
				newFile = new File(directory.toString() + "/Chart.yaml");
			}
			Files.write(Paths.get(newFile.toURI()), overwrittenChart, StandardCharsets.UTF_8);
			this.log.info("Overwriting successful for " + newFile);
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to overwrite Chart.yaml at " + directory, e);
		}
	}

	/**
	 * Utility class for overwriting local chart dependencies when making use of helm.overwriteLocalDependencies
	 * @param directory {@link Path} directory to overwrite the Chart.yaml
	 * @throws MojoExecutionException Must set a value for helm.overwriteDependencyRepository
	 */
	public void execute(Path directory) throws MojoExecutionException {
		if (this.overwriteRepository == null) {
			throw new MojoExecutionException("Null value for 'overwriteDependencyRepository' is " +
			"not allowed when using 'overwriteLocalDependencies'. See the README for more details.");
		} else {
			this.log.info("Overwriting dependencies that contain local path charts...");
		}

		File chartYamlFile = new File(directory.toString() + "/Chart.yaml");

		if (chartYamlFile.exists()) {
			ArrayList<Dependency> dependencies = this.getDependencies(chartYamlFile);

			//check if the chart has any dependencies 
			if (dependencies != null && dependencies.size() > 0) {
				this.updateDependencies(dependencies);

				//check if any dependencies need to be overwritten
				if (dependencies.stream().anyMatch(dependency -> dependency.isOverwrite())) {
					this.overwriteDependencies(dependencies, chartYamlFile, directory);
				} else {
					this.log.info("No dependencies to update for " + chartYamlFile);
				}
			}
			else {
				this.log.info("No dependencies found for " + chartYamlFile);
			}
		} else {
			this.log.warn("No Charts detected - no Chart.yaml files found below " + directory);
		}
	}
}
