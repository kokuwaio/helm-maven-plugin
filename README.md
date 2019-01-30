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

By default the plugin downloads HELM in a specific version. Next to that it is possible to specify a local HELM binary.
In both cases HELM will be executed in the background.

Add following dependency to your pom.xml:
```
<dependency>
  <groupId>com.kiwigrid</groupId>
  <artifactId>helm-maven-plugin</artifactId>
  <version>4.0</version>
</dependency>
```

## Configuration Examples

### Usage with Downloaded Binary
```xml
<build>
  <plugins>
  ...
    <plugin>
      <groupId>com.kiwigrid</groupId>
      <artifactId>helm-maven-plugin</artifactId>
      <version>4.0</version>
      <configuration>
        <chartDirectory>${project.basedir}</chartDirectory>
        <chartVersion>${project.version}</chartVersion>
        <!-- This is the related section when using binary download -->
        <helmDownloadUrl>https://storage.googleapis.com/kubernetes-helm/helm-v2.12.3-linux-amd64.tar.gz</helmDownloadUrl>
        <helmHomeDirectory>${project.basedir}/target/helm/home</helmHomeDirectory>
      </configuration>
    </plugin>
  ...
  </plugins>
</build>
```

### Usage with Local Binary
```xml
<build>
  <plugins>
  ...
    <plugin>
      <groupId>com.kiwigrid</groupId>
      <artifactId>helm-maven-plugin</artifactId>
      <version>4.0</version>
      <configuration>
        <chartDirectory>${project.basedir}</chartDirectory>
        <chartVersion>${project.version}</chartVersion>
        <!-- This is the related section to use local binary -->
        <useLocalHelmBinary>true</useLocalHelmBinary>
        <helmExecutableDirectory>/usr/local/bin</helmExecutableDirectory>        
      </configuration>
    </plugin>
  ...
  </plugins>
</build>
```

### Configure Plugin to Use Credentials from settings.xml for Upload
```xml
<build>
  <plugins>
  ...
    <plugin>
      <groupId>com.kiwigrid</groupId>
      <artifactId>helm-maven-plugin</artifactId>
      <version>3.4</version>
      <configuration>
        <chartDirectory>${project.basedir}</chartDirectory>
        <chartVersion>${project.version}</chartVersion>
        <!-- This is the related section to configure upload repos -->
        <uploadRepoStable>
            <name>stable-repo</name>
            <url>https://repo.example.com/artifactory/helm-stable</url>
            <type>ARTIFACTORY</type>
        </uploadRepoStable>
        <uploadRepoSnapshot>
            <name>snapshot-repo</name>
            <url>https://my.chart.museum:8080/api/charts</url>
            <type>CHARTMUSEUM</type>
        </uploadRepoSnapshot>
        <helmDownloadUrl>https://storage.googleapis.com/kubernetes-helm/helm-v2.12.3-linux-amd64.tar.gz</helmDownloadUrl>
        <helmHomeDirectory>${project.basedir}/target/helm/home</helmHomeDirectory>
      </configuration>
    </plugin>
  ...
  </plugins>
</build>
```

### More Complex Example
```xml
<build>
  <plugins>
  ...
    <plugin>
      <groupId>com.kiwigrid</groupId>
      <artifactId>helm-maven-plugin</artifactId>
      <version>3.4</version>
      <configuration>
        <chartDirectory>${project.basedir}</chartDirectory>
        <chartVersion>${project.version}</chartVersion>
        <uploadRepoStable>
            <name>stable-repo</name>
            <url>https://repo.example.com/artifactory/helm-stable</url>
            <!-- Artifacotry requires basic authentication --> 
            <!-- which is supported from HELM version >= 2.9 -->
            <type>ARTIFACTORY</type>
            <username>foo</username>
            <password>bar</password>
        </uploadRepoStable>
        <uploadRepoSnapshot>
            <name>snapshot-repo</name>
            <url>https://my.chart.museum/api/charts</url>
            <type>CHARTMUSEUM</type>
        </uploadRepoSnapshot>
        <helmDownloadUrl>https://storage.googleapis.com/kubernetes-helm/helm-v2.12.3-linux-amd64.tar.gz</helmDownloadUrl>
        <helmHomeDirectory>${project.basedir}/target/helm/home</helmHomeDirectory>
        <!-- Skip a single goal -->
        <skipRefresh>false</skipRefresh>
        <!-- Exclude a directory to avoid processing -->
        <excludes>
          <exclude>${project.basedir}/excluded</exclude>
        </excludes>
        <!-- Add an additional repo -->
        <helmExtraRepos>
          <helmRepo>
            <name>kiwigrid</name>
            <url>https://kiwigrid.github.io</url>
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
- Repository names are interpreted as server IDs to retrieve basic authentication from server list in settings.xml.

# Usage

## Goals

- `helm:init` initializes Helm by downloading a specific version
- `helm:dependency-build` resolves the chart dependencies  
- `helm:package` packages the given charts (chart.tar.gz)
- `helm:lint` tests the given charts
- `helm:dry-run` simulates an install
- `helm:upload` upload charts via HTTP PUT

## Configuration

Parameter | Type | User Property | Required | Description
--- | --- | --- | --- | ---
`<chartDirectory>` | string | helm.chartDirectory | true | root directory of your charts
`<chartVersion>` | string | helm.chartVersion | true | Version of the charts. The version have to be in the [SEMVER-Format](https://semver.org/), required by helm.
`<appVersion>` | string | helm.appVersion | false | The version of the app. This needn't be SemVer.
`<helmDownloadUrl>` | string | helm.downloadUrl | false | URL to download helm
`<excludes>` | list of strings | helm.excludes | false | list of chart directories to exclude
`<useLocalHelmBinary>` | boolean | helm.useLocalHelmBinary | false | Controls whether a local binary should be used instead of downloading it. If set to `true` path has to be set with property `executableDirectory`
`<helmExecutableDirectory>` | string | helm.executableDirectory | false | directory of your helm installation (default: `${project.build.directory}/helm`)
`<outputDirectory>` | string | helm.outputDirectory | false | chart output directory (default: `${project.build.directory}/helm/repo`)
`<helmHomeDirectory>` | string | helm.homeDirectory | false | path to helm home directory; useful for concurrent Jenkins builds! (default: `~/.helm`)
`<helmExtraRepos>` | list of [HelmRepository](./src/main/java/com/kiwigrid/helm/maven/plugin/HelmRepository.java) | helm.extraRepos | false | adds extra repositories while init
`<uploadRepoStable>`| [HelmRepository](./src/main/java/com/kiwigrid/helm/maven/plugin/HelmRepository.java) | helm.uploadRepo.stable | true | Upload repository for stable charts
`<uploadRepoSnapshot>`| [HelmRepository](./src/main/java/com/kiwigrid/helm/maven/plugin/HelmRepository.java) | helm.uploadRepo.snapshot | false | Upload repository for snapshot charts (determined by version postfix 'SNAPSHOT')
`<skip>` | boolean | helm.skip | false | skip plugin execution
`<skipRefresh>` | boolean | helm.init.skipRefresh | false | do not refresh (download) the local repository cache while init
`<skipInit>` | boolean | helm.init.skip | false | skip init goal
`<skipLint>` | boolean | helm.lint.skip | false | skip lint goal
`<skipDryRun>` | boolean | helm.dry-run.skip | false | skip dry-run goal
`<skipDependencyBuild>` | boolean | helm.dependency-build.skip | false | skip dependency-build goal
`<skipPackage>` | boolean | helm.package.skip | false | skip package goal
`<skipUpload>` | boolean | helm.upload.skip | false | skip upload goal
`<security>` | string | helm.security | false | path to your [settings-security.xml](https://maven.apache.org/guides/mini/guide-encryption.html) (default: `~/.m2/settings-security.xml`)

## Packaging with the Helm Lifecycle

To keep your pom files small you can use 'helm' packaging. This binds `helm:init` to the initialize phase, `helm:lint` to the test phase, `helm:package` to the package phase and `helm:upload` to the deploy phase.

```xml
<pom>
  <artifactId>my-helm-charts</artifactId>
  <version>0.0.1</version>
  <packaging>helm</packaging>
  ...
  <build>
    <plugins>
      <plugin>
        <groupId>com.kiwigrid</groupId>
        <artifactId>helm-maven-plugin</artifactId>
        <!-- Mandatory when you use a custom lifecycle -->
        <extensions>true</extensions>
        <configuration>
          ...
        </configuration>
      </plugin>
    </plugins>
    ....
  </build>
</pom>
```
