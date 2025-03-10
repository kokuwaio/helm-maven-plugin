# Helm Maven Plugin

[![Maven Central](https://img.shields.io/maven-central/v/io.kokuwa.maven/helm-maven-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/namespace/io.kokuwa.maven)
[![Build](https://img.shields.io/github/actions/workflow/status/kokuwaio/helm-maven-plugin/build.yaml?label=Build)](https://github.com/kokuwaio/helm-maven-plugin/actions/workflows/build.yaml?label=Build)

This is a Maven plugin for testing, packaging and uploading HELM charts.
"HELM is a tool for managing Kubernetes charts. Charts are packages of pre-configured Kubernetes resources."

Visit <https://docs.helm.sh> for detailed information.

Currently the upload to [ChartMuseum](https://github.com/kubernetes-helm/chartmuseum), [Artifactory](https://jfrog.com/artifactory/) and [Nexus](https://github.com/sonatype/nexus-public) is supported.

## Helm v3

From version **5.0** Helm v3 is required.
There is no longer support for Helm v2.
For convenience reasons the stable repo is added by default.

Helm v2 users can still use plugin version [4.13](https://search.maven.org/artifact/io.kokuwa.maven/helm-maven-plugin/4.13/maven-plugin).

## Why?

Currently (October 2017) there is no simple Maven plugin to package existing HELM charts.

## How?

By default, the plugin automatically downloads Helm at the specified version. You can also manually specify the download URL.
Next to that it is possible to specify a local Helm binary. In all cases Helm will be executed in the background.

Add following dependency to your pom.xml:

```xml
<dependency>
  <groupId>io.kokuwa.maven</groupId>
  <artifactId>helm-maven-plugin</artifactId>
  <version>6.17.0</version>
</dependency>
```

## Configuration Examples

### Helm URL Auto Detection

The default setting is to construct the Helm download URL based upon the detected OS and architecture:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>io.kokuwa.maven</groupId>
      <artifactId>helm-maven-plugin</artifactId>
      <version>6.17.0</version>
      <configuration>
        <chartDirectory>${project.basedir}</chartDirectory>
        <chartVersion>${project.version}</chartVersion>
      </configuration>
    </plugin>
  </plugins>
</build>
```

If you leave `helmVersion` and `helmDownloadUrl` empty the plugin will determine the latest version based on [https://api.github.com/repos/helm/helm/releases/latest].

### Usage with Downloaded Binary

```xml
<build>
  <plugins>
  ...
    <plugin>
      <groupId>io.kokuwa.maven</groupId>
      <artifactId>helm-maven-plugin</artifactId>
      <version>6.17.0</version>
      <configuration>
        <chartDirectory>${project.basedir}</chartDirectory>
        <chartVersion>${project.version}</chartVersion>
        <!-- This is the related section when using binary download -->
        <helmDownloadUrl>https://get.helm.sh/helm-v3.8.1-linux-amd64.tar.gz</helmDownloadUrl>
      </configuration>
    </plugin>
  ...
  </plugins>
</build>
```

### Usage with Local Binary

When `useLocalHelmBinary` is enabled, the plugin by default will search for the `helm` executable in `PATH`:

```xml
<build>
  <plugins>
  ...
    <plugin>
      <groupId>io.kokuwa.maven</groupId>
      <artifactId>helm-maven-plugin</artifactId>
      <version>6.17.0</version>
      <configuration>
        <chartDirectory>${project.basedir}</chartDirectory>
        <chartVersion>${project.version}</chartVersion>
        <!-- This is the related section to use local binary with auto-detection enabled. -->
        <useLocalHelmBinary>true</useLocalHelmBinary>
      </configuration>
    </plugin>
  ...
  </plugins>
</build>
```

The following is an example configuration that explicitly sets the directory in which to look for the `helm` executable,
and disables the auto-detection feature:

```xml
<build>
  <plugins>
  ...
    <plugin>
      <groupId>io.kokuwa.maven</groupId>
      <artifactId>helm-maven-plugin</artifactId>
      <version>6.17.0</version>
      <configuration>
        <chartDirectory>${project.basedir}</chartDirectory>
        <chartVersion>${project.version}</chartVersion>
        <!-- This is the related section to use local binary with auto-detection disabled. -->
        <useLocalHelmBinary>true</useLocalHelmBinary>
        <autoDetectLocalHelmBinary>false</autoDetectLocalHelmBinary>
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
      <groupId>io.kokuwa.maven</groupId>
      <artifactId>helm-maven-plugin</artifactId>
      <version>6.17.0</version>
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
        <helmDownloadUrl>https://get.helm.sh/helm-v3.8.1-linux-amd64.tar.gz</helmDownloadUrl>
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
      <groupId>io.kokuwa.maven</groupId>
      <artifactId>helm-maven-plugin</artifactId>
      <version>6.17.0</version>
      <configuration>
        <chartDirectory>${project.basedir}</chartDirectory>
        <chartVersion>${project.version}</chartVersion>
        <uploadRepoStable>
            <name>stable-repo</name>
            <url>https://repo.example.com/artifactory/helm-stable</url>
            <type>ARTIFACTORY</type>
            <username>foo</username>
            <password>bar</password>
        </uploadRepoStable>
        <uploadRepoSnapshot>
            <name>snapshot-repo</name>
            <url>https://my.chart.museum/api/charts</url>
            <type>CHARTMUSEUM</type>
        </uploadRepoSnapshot>
        <helmDownloadUrl>https://get.helm.sh/helm-v3.8.2-linux-amd64.tar.gz</helmDownloadUrl>
        <registryConfig>~/.config/helm/registry.json</registryConfig>
        <repositoryCache>~/.cache/helm/repository</repositoryCache>
        <repositoryConfig>~/.config/helm/repositories.yaml</repositoryConfig>
        <!-- Add a gpg signature to the chart -->
        <keyring>~/.gpg/secring.gpg</keyring>
        <key>MySigningKey</key>
        <passphrase>SecretPassPhrase</passphrase>
        <!-- Lint with strict mode -->
        <lintStrict>true</lintStrict>
        <!-- Disable adding of default repo stable https://charts.helm.sh/stable -->
        <addDefaultRepo>false</addDefaultRepo>
        <!-- Exclude a directory to avoid processing -->
        <excludes>
          <exclude>${project.basedir}/excluded</exclude>
          <exclude>${project.basedir}/**/excluded*</exclude>
        </excludes>
        <!-- Add an additional repo -->
        <helmExtraRepos>
          <helmRepo>
            <name>kokuwa</name>
            <url>https://kokuwa.github.io</url>
          </helmRepo>
        </helmExtraRepos>
        <!-- extra value settings for the lint command -->
        <values>
          <overrides>
            <component1.install.path>/opt/component1</component1.install.path>
          </overrides>
          <yamlFile>${project.basedir}/src/test/resources/myOverrides.yaml</yamlFile>
          <yamlFiles>
              <yamlFile>${project.basedir}/src/test/resources/myOverrides-1.yaml</yamlFile>
              <yamlFile>${project.basedir}/src/test/resources/myOverrides-2.yaml</yamlFile>
          </yamlFiles>
        </values>
      </configuration>
    </plugin>
  ...
  </plugins>
</build>
```

## Features

- Package Helm charts from standard folder structure
- Test Helm charts (Helm lint)
- Recursive chart detection (subcharts)
- Helm does not need to be installed
- Upload to [ChartMuseum](https://github.com/kubernetes-helm/chartmuseum) or [Artifactory](https://jfrog.com/artifactory/)
- Repository names are interpreted as server IDs to retrieve basic authentication from server list in settings.xml.

## Usage

## Goals

- `helm:clean` cleanups helm specific directory `charts` and `Chart.lock` file
- `helm:init` initializes Helm by downloading a specific version
- `helm:dependency-build` resolves the chart dependencies
- `helm:dependency-update` verifies that the required chart dependencies are present
- `helm:package` packages the given charts (chart.tar.gz)
- `helm:lint` tests the given charts
- `helm:template` Locally render templates
- `helm:dry-run` simulates an install
- `helm:upload` upload charts via HTTP PUT
- `helm:registry-login` login into docker registry
- `helm:registry-logout` login from docker registry
- `helm:push` push charts to OCI (docker registry)
- `helm:upgrade` upgrade an already existing installation

## Configuration

Parameter | Type | User Property | Required | Description
--- | --- | --- | --- | ---
`<chartDirectory>` | string | helm.chartDirectory | true | root directory of your charts
`<chartVersion>` | string | helm.chartVersion | true | Version of the charts. The version have to be in the [SEMVER-Format](https://semver.org/), required by helm.
`<appVersion>` | string | helm.appVersion | false | The version of the app. This needn't be SemVer.
`<helmDownloadUrl>` | string | helm.downloadUrl | false | URL to download helm. Leave empty to autodetect URL based upon OS and architecture.
`<helmDownloadUser>` | string | helm.downloadUser | false | Username used to authenticate while downloading helm binary package
`<helmDownloadPassword>` | string | helm.downloadPassword | false | Password used to authenticate while downloading helm binary package
`<helmDownloadServerId>` | string | helm.downloadServerId | false | Server Id in `settings.xml` which has username and password used to authenticate while downloading helm binary package
`<helmVersion>` | string | helm.version | false | Version of helm to download.
`<githubUserAgent>` | string | helm.githubUserAgent | false | To determine latest helm version this plugin uses the Github API. Therefore a [user agent](https://docs.github.com/en/rest/overview/resources-in-the-rest-api#user-agent-required) is needed. Defaults to `kokuwaio/helm-maven-plugin`
`<cacheValidityDays>` | int | helm.cacheValidityDays | false | Skip accessing the Github API for helm version if cache is newer than given days. Defaults to 1.
`<tmpDir>` | string | helm.tmpDir | false | Directory where to store cached Github responses. Defaults to `${java.io.tmpdir}/helm-maven-plugin`
`<excludes>` | list of strings | helm.excludes | false | list of chart directories to exclude
`<useLocalHelmBinary>` | boolean | helm.useLocalHelmBinary | false | Controls whether a local binary should be used instead of downloading it. If set to `true` path has to be set with property `executableDirectory`
`<autoDetectLocalHelmBinary>` | boolean | helm.autoDetectLocalHelmBinary | true | Controls whether the local binary should be auto-detected from `PATH` environment variable. If set to `false`, the binary in `<helmExecutableDirectory>` is used only. This property has no effect unless `<useLocalHelmBinary>` is set to `true`.
`<helmExecutableDirectory>` | string | helm.executableDirectory | false | directory of your helm installation (default: `${project.build.directory}/helm`). If defined, directory is used also for `<autoDetectLocalHelmBinary>` as a last resort.
`<outputDirectory>` | string | helm.outputDirectory | false | chart output directory (default: `${project.build.directory}/helm/repo`)
`<debug>` | boolean | helm.debug | false | add debug to helm
`<registryConfig>` | string | helm.registryConfig | false | path to the registry config file
`<repositoryCache>` | string | helm.repositoryCache | false | path to the file containing cached repository indexes
`<repositoryConfig>` | string | helm.repositoryConfig | false | path to the file containing repository names and URLs
`<repositoryAddForceUpdate>`| boolean | helm.repo.add.force-update | false | If `true`, replaces (overwrite) the repo if they already exists.
`<repositoryAddPassCredentials>`| boolean | helm.repo.add.pass-credentials  | false | If `true`, pass credentials to all domains
`<helmExtraRepos>` | list of [HelmRepository](./src/main/java/io/kokuwa/maven/helm/pojo/HelmRepository.java) | | false | adds extra repositories while init
`<uploadRepoStable>`| [HelmRepository](./src/main/java/io/kokuwa/maven/helm/pojo/HelmRepository.java) | | false | Upload repository for stable charts
`<uploadRepoSnapshot>`| [HelmRepository](./src/main/java/io/kokuwa/maven/helm/pojo/HelmRepository.java) | | false | Upload repository for snapshot charts (determined by version postfix 'SNAPSHOT')
`<lintStrict>` | boolean | helm.lint.strict | false | run lint command with strict option (fail on lint warnings)
`<lintQuiet>` | boolean | helm.lint.quiet | false | run lint command with quiet option (print only warnings and errors)
`<addDefaultRepo>` | boolean | helm.init.add-default-repo | true | If true, stable repo (<https://charts.helm.sh/stable>) will be added
`<addUploadRepos>` | boolean | helm.init.add-upload-repos | false | If true, upload repos (uploadRepoStable, uploadRepoSnapshot) will be added, if configured
`<skip>` | boolean | helm.skip | false | skip plugin execution
`<skipClean>` | boolean | helm.clean.skip | false | skip clean goal
`<skipInit>` | boolean | helm.init.skip | false | skip init goal
`<skipLint>` | boolean | helm.lint.skip | false | skip lint goal
`<skipTemplate>` | boolean | helm.template.skip | false | skip template goal. Default value is true due to the dry-run goal
`<skipDryRun>` | boolean | helm.dry-run.skip | false | skip dry-run goal
`<skipDependencyBuild>` | boolean | helm.dependency-build.skip | false | skip dependency-build goal
`<skipDependencyBuildRepoRefresh>` | boolean | helm.dependency-build.skip-repo-refresh | false | skip local repository refresh in dependency-build goal
`<skipDependencyUpdate>` | boolean | helm.dependency-update.skip | false | skip dependency-update goal
`<skipDependencyUpdateRepoRefresh>` | boolean | helm.dependency-update.skip-repo-refresh | false | skip local repository refresh in dependency-update goal
`<skipPackage>` | boolean | helm.package.skip | false | skip package goal
`<skipUpload>` | boolean | helm.upload.skip | false | skip upload goal
`<skipCatalog>` | boolean | helm.upload.skip.catalog | true | Skips creation of a catalog file with a list of helm chart upload details
`<insecure>` | boolean | helm.upload.insecure | false | Skip tls certificate checks for the chart upload.
`<uploadVerification>` | boolean | helm.upload.verification | false | wait for the chart to be added to the repository index before continuing
`<uploadVerificationTimeout>` | Integer | helm.upload.timeout | false | set the timeout limit (in seconds) for verification to be attempted
`<skipInstall>` | boolean | helm.install.skip | false | skip install goal
`<skipUninstall>` | boolean | helm.uninstall.skip | false | skip uninstall goal
`<security>` | string | helm.security | false | path to your [settings-security.xml](https://maven.apache.org/guides/mini/guide-encryption.html) (default: `~/.m2/settings-security.xml`)
`<keyring>` | string | helm.package.keyring | false | path to gpg secret keyring for signing
`<key>` | string  | helm.package.key | false | name of gpg key in keyring
`<passphrase>` | string | helm.package.passphrase | false | passphrase for gpg key (requires helm 3.4 or newer)
`<skipPushLogin>` | boolean | helm.push.skipPushLogin | false | Skip login, usefull if already logged via `helm:registry-login`
`<values>` | [ValueOverride](./src/main/java/io/kokuwa/maven/helm/pojo/ValueOverride.java) | | false | override some values for linting with helm.values.overrides (--set option), helm.values.stringOverrides (--set-string option), helm.values.fileOverrides (--set-file option) and last but not least helm.values.yamlFile (--values option)
`<namespace>` | string | helm.namespace | false | namespace scope for helm command
`<kubeApiServer>` | string | helm.kubeApiServer | false | the address and the port for the Kubernetes API server
`<kubeInsecure>` | string | helm.kubeInsecure | false | Skip tls certificate checks for the operation. Also known as `helm --kube-insecure-skip-tls-verify`.
`<kubeAsUser>` | string | helm.kubeAsUser | false | username to impersonate for the operation
`<kubeAsGroup>` | string | helm.kubeAsGroup | false | group to impersonate for the operation, this flag can be repeated to specify multiple groups
`<kubeToken>` | string | helm.kubeToken | false | bearer token used for authentication
`<releaseName>` | string | helm.releaseName | false | Name of the release to handle.
`<installForce>` | boolean | helm.install.force | false | Force resource updates through a replacement strategy.
`<installAtomic>` | boolean | helm.install.atomic | false | Set this to `true` to delete the installation on failure.
`<installTimeout>` | boolean | helm.upgrade.imeout | false | Time in seconds to wait for any individual Kubernetes operation during install process. The default is 300 seconds (from helm) if `installAtomic` is set to `true`.
`<installPlainHttp>` | boolean | helm.install.plain-http | false | Use insecure HTTP connections for the chart download.
`<upgradeForce>` | boolean | helm.upgrade.force | false | Force resource updates through a replacement strategy.
`<upgradeAtomic>` | boolean | helm.upgrade.atomic | false | Set this to `true` to rollback changes made in case of failed upgrade.
`<upgradeTimeout>` | boolean | helm.upgrade.imeout | false | Time in seconds to wait for any individual Kubernetes operation during upgrade process. The default is 300 seconds (from helm) if `upgradeTimeout` is set to `true`.
`<upgradeDryRun>` | boolean | helm.upgrade.dryRun | false | Run upgrade goal only in dry run mode
`<upgradePlainHttp>` | boolean | helm.upgrade.plain-http | false | Use insecure HTTP connections for the chart download.
`<uninstallWait>` | boolean | helm.uninstall.wait | false | If set, will wait until all the resources are deleted before returning. It will wait for as long as `uninstallTimeout`.
`<uninstallTimeout>` | boolean | helm.uninstall.timeout | false | Time to wait for any individual Kubernetes operation (like Jobs for hooks) (default 5m0s).
`<uninstallNoHooks>` | boolean | helm.uninstall.no-hooks | false | Prevent hooks from running during uninstallation.
`<uninstallCascade>` | boolean | helm.uninstall.cascade | false | Must be "background", "orphan", or "foreground". Selects the deletion cascading strategy for the dependents. Defaults to background. (default "background" from helm)
`<uninstallKeepHistory>` | boolean | helm.uninstall.keep-history | false | Remove all associated resources and mark the release as deleted, but retain the release history.
`<uninstallIgnoreNotFound>` | boolean | helm.uninstall.ignore-not-found | false | Treat "release not found" as a successful uninstall.
`<templateOutputDir>` | file | helm.template.output-dir | false | Writes the executed templates to files in output-dir instead of stdout.
`<templateTemplateName>` | string | helm.template.name-template | false | Specify template used to name the release.
`<templateGenerateName>` | boolean | helm.template.generate-name | false | Generate the name (and omit the NAME parameter).
`<templatePlainHttp>` | boolean | helm.template.plain-http | false | Use insecure HTTP connections for the chart download.
`<pushPlainHttp>` | boolean | helm.push.plain-http | false | Use insecure HTTP connections for the chart download.
`<plainHttp>` | boolean | helm.plain-http | false | Global Flag: Use insecure HTTP connections for the chart download.
`<caFile>` | boolean | helm.push.caFile | false | Verify certificates of HTTPS-enabled servers using this CA bundle.
`<insecure>` | boolean | helm.push.insecure | false | Skip tls certificate checks for the chart upload. Also known as `helm push --insecure-skip-tls-verify`
`<fallbackBinaryDownload>` | boolean | helm.fallbackBinaryDownload | false | Controls whether a download should occur when local helm binary is not found. This property has no effect unless `<useLocalHelmBinary>` is set to `true`.
`<overwriteLocalDependencies>` | boolean | helm.overwriteLocalDependencies | false | Controls whether a local path chart should be used for a chart dependency. When set to `true`, chart dependencies on a local path chart will be overwritten with the respective properties set by `overwriteDependencyVersion` and `overwriteDependencyRepository`. This is helpful for deploying charts with intra repository dependencies, while still being able to use local path dependencies for development builds. Example usage: for development use `mvn clean install` and for deployment use `mvn clean deploy -Dhelm.overwriteLocalDependencies=true`
`<overwriteDependencyVersion>` | string | helm.overwriteDependencyVersion | false |  Value used to overwrite a local path chart's version within a chart's dependencies. The property `overwriteLocalDependencies` must be set to `true` for this to apply.
`<overwriteDependencyRepository>` | string | helm.overwriteDependencyRepository | false | Value used to overwrite a local path chart's repository within a chart's dependencies. The property `overwriteLocalDependencies` must be set to `true` for this to apply.
`<cleanFailOnError>` | boolean | helm.clean.failOnError | false | Indicates whether the build will continue even if there are clean errors. (default true)

## Packaging with the Helm Lifecycle

To keep your pom files small you can use 'helm' packaging.

This binds:

- `helm:clean` to clean phase
- `helm:init` to the initialize phase
- `helm:dependency-build` to the process-resources phase
- `helm:lint` to the test phase
- `helm:package` to the package phase
- `helm:upload` to the deploy phase

```xml
<pom>
  <artifactId>my-helm-charts</artifactId>
  <version>0.0.1</version>
  <packaging>helm</packaging>
  ...
  <build>
    <plugins>
      <plugin>
        <groupId>io.kokuwa.maven</groupId>
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
