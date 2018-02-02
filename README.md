# What?

This is a Maven plugin for testing, packaging and uploading HELM charts.

"HELM is a tool for managing Kubernetes charts. Charts are packages of pre-configured Kubernetes resources." 

Visit https://docs.helm.sh for detailed information.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.kiwigrid/helm-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.kiwigrid/helm-maven-plugin)

# Why?

Currently (October 2017) there is no simple Maven plugin to package existing HELM charts.

# How?

The plugin downloads HELM in a specific version and runs the tool in the background.

Add following dependency to your pom.xml:
```
<dependency>
  <groupId>com.kiwigrid</groupId>
  <artifactId>helm-maven-plugin</artifactId>
  <version>1.6</version>
</dependency>
```

Configure plugin:
```
...
<properties>
  <helm.download.url>https://kubernetes-helm.storage.googleapis.com/helm-v2.6.1-linux-amd64.tar.gz</helm.download.url>
  <helm.repo.url>https://repo.example.com/artifactory/helm</helm.repo.url>
</properties>
...
<build>
  <plugins>
  ...
    <plugin>
      <groupId>com.kiwigrid</groupId>
      <artifactId>helm-maven-plugin</artifactId>
      <version>1.2</version>
      <configuration>
        <chartDirectory>${project.basedir}</chartDirectory>
        <chartVersion>${project.version}</chartVersion>
        <helmRepoUrl>${helm.repo.url}</helmRepoUrl>
        <helmUploadUrl>${helm.repo.url}/api/charts</helmUploadUrl>
        <helmDownloadUrl>${helm.download.url}</helmDownloadUrl>
        <indexFileForMerge>${project.basedir}/target/helm/current_index.yaml</indexFileForMerge>
        <helmHomeDirectory>${project.basedir}/target/helm/home</helmHomeDirectory>
      </configuration>
    </plugin>
  ...
  </plugins>
</build>
```

# Features

- Package Helm charts from standard folder structure
- Merge index.yaml files from remote repositories, so the local repository is updated
- Test Helm charts (Helm lint)
- Recursive chart detection (subcharts)
- Helm does not need to be installed
- Upload via POST

# Usage

## Goals

- `helm:init` initializes Helm by downloading a specific version
- `helm:package` packages the given charts (chart.tar.gz)
- `helm:dependency-build` resolves the chart dependencies  
- `helm:lint` tests the given charts
- `helm:dry-run` simulate an install
- `helm:index` creates and merges the index.yaml file
- `helm:upload` upload charts via HTTP POST

## Configuration

- `<chartDirectory>` 
  - description: root directory of your charts
  - required: true
  - type: string
  - user property: helm.chartDirectory

- `<chartVersion>`
  - description: Version of the charts. The version have to be in the [SEMVER-Format](https://semver.org/), required by helm.
  - required: false
  - type: string
  - user property: helm.chartVersion

- `<helmRepoUrl>`
  - description: URL to your helm repository
  - required: true
  - type: string
  - user property: helm.repoUrl

- `<helmUploadUrl>`
  - description: URL to your helm repository
  - required: true
  - type: string
  - user property: helm.uploadUrl

- `<indexFileForMerge>`
  - description: path to a index.yaml file that will be merged
  - required: false
  - type: string
  - user property: helm.indexFileForMerge

- `<helmDownloadUrl>`
  - description: URL to download helm
  - required: false
  - type: string
  - user property: helm.downloadUrl

- `<excludes>`
  - description: list of chart directories to exclude
  - required: false
  - type: list of strings
  - user property: helm.excludes

- `<helmExecutableDirectory>`
  - description: directory of your helm installation
  - required: false
  - default value: ${project.build.directory}/helm
  - type: string
  - user property: helm.executableDirectory

- `<helmExecutable>`
  - description: path to your helm executable
  - required: false
  - default value: "${project.build.directory}/helm/linux-amd64/helm
  - type: string
  - user property: helm.executable

- `<outputDirectory>`
  - description: chart output directory
  - required: false
  - default value: ${project.build.directory}/helm/repo
  - type: string
  - user property: helm.outputDirectory

- `<helmHomeDirectory>`
  - description: path to helm home directory; useful for concurrent Jenkins builds!
  - required: false
  - default value: ~/.helm
  - type: string
  - user property: helm.homeDirectory
