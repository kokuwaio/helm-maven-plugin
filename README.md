# What?

This is a Maven plugin for testing, packaging and uploading HELM charts.

"HELM is a tool for managing Kubernetes charts. Charts are packages of pre-configured Kubernetes resources." 

Visit https://docs.helm.sh for detailed information.

Currently the upload to [ChartMuseum](https://github.com/kubernetes-helm/chartmuseum) and [Artifactory](https://jfrog.com/artifactory/) is supported.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.kiwigrid/helm-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.kiwigrid/helm-maven-plugin)

[![Build Status](https://travis-ci.org/kiwigrid/helm-maven-plugin.svg?branch=master)](https://travis-ci.org/kiwigrid/helm-maven-plugin)

# Why?

Currently (October 2017) there is no simple Maven plugin to package existing HELM charts.

# How?

The plugin downloads HELM in a specific version and runs the tool in the background.

Add following dependency to your pom.xml:
```
<dependency>
  <groupId>com.kiwigrid</groupId>
  <artifactId>helm-maven-plugin</artifactId>
  <version>2.4</version>
</dependency>
```

Configure plugin:
```
...
<properties>
  <helm.download.url>https://storage.googleapis.com/kubernetes-helm/helm-v2.9.0-rc3-linux-amd64.tar.gz</helm.download.url>
  <repoBaseUrl>>https://repo.example.com/artifactory</repoBaseUrl>
</properties>
...
<build>
  <plugins>
  ...
    <plugin>
      <groupId>com.kiwigrid</groupId>
      <artifactId>helm-maven-plugin</artifactId>
      <version>2.1</version>
      <configuration>
        <chartDirectory>${project.basedir}</chartDirectory>
        <chartVersion>${project.version}</chartVersion>
        <uploadRepoStable>
            <name>stable-repo</name>
            <url>${repoBaseUrl}/helm-stable</url>
            <!-- Artifacotry requieres basic authentication --> 
            <!-- which is supported from HELM version >= 2.9 -->
            <type>ARTIFACTORY</type>
            <username>foo</username>
            <password>bar</password>
        </uploadRepoStable>
        <uploadRepoSnapshot>
            <name>snapshot-repo</name>
            <url>${repoBaseUrl}/helm-snapshots</url>
            <type>CHARTMUSEUM</type>
        </uploadRepoSnapshot>
        <helmDownloadUrl>${helm.download.url}</helmDownloadUrl>
        <helmHomeDirectory>${project.basedir}/target/helm/home</helmHomeDirectory>
        <excludes>
          <exclude>${project.basedir}/excluded</exclude>
        </excludes>
        <helmExtraRepos>
          <helmRepo>
            <name>incubator</name>
            <url>https://kubernetes-charts-incubator.storage.googleapis.com</url>
          </helmRepo>
        </helmExtraRepos>
      </configuration>
    </plugin>
  ...
  </plugins>
</build>
```

# Features

- Package Helm charts from standard folder structure
- Test Helm charts (Helm lint)
- Recursive chart detection (subcharts)
- Helm does not need to be installed
- Upload to [ChartMuseum](https://github.com/kubernetes-helm/chartmuseum) or [Artifactory](https://jfrog.com/artifactory/)

# Usage

## Goals

- `helm:init` initializes Helm by downloading a specific version
- `helm:dependency-build` resolves the chart dependencies  
- `helm:package` packages the given charts (chart.tar.gz)
- `helm:lint` tests the given charts
- `helm:dry-run` simulates an install
- `helm:upload` upload charts via HTTP PUT

## Configuration

- `<chartDirectory>` 
  - description: root directory of your charts
  - required: true
  - type: string
  - user property: helm.chartDirectory

- `<chartVersion>`
  - description: Version of the charts. The version have to be in the [SEMVER-Format](https://semver.org/), required by helm.
  - required: true
  - type: string
  - user property: helm.chartVersion

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

- `<helmExtraRepos>`
  - description: adds extra repositories while init
  - required: false
  - type: list of [HelmRepository](./src/main/java/com/kiwigrid/helm/maven/plugin/HelmRepository.java)
  - user property: helm.extraRepos
  
- `<uploadRepoStable>`
  - description: Upload repository for stable charts
  - required: true
  - type: [HelmRepository](./src/main/java/com/kiwigrid/helm/maven/plugin/HelmRepository.java)
  - user property: helm.uploadRepo.stable
  
- `<uploadRepoSnapshot>`
  - description: Upload repository for snapshot charts (determined by version postfix 'SNAPSHOT')
  - required: false
  - type: [HelmRepository](./src/main/java/com/kiwigrid/helm/maven/plugin/HelmRepository.java)
  - user property: helm.uploadRepo.snapshot
