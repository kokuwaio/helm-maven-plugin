package io.kokuwa.maven.helm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

import java.net.URL;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.codehaus.plexus.util.Os;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import io.kokuwa.maven.helm.junit.MojoExtension;
import io.kokuwa.maven.helm.junit.MojoProperty;
import io.kokuwa.maven.helm.pojo.HelmRepository;
import io.kokuwa.maven.helm.pojo.RepoType;

@ExtendWith(MojoExtension.class)
@MojoProperty(name = "helmDownloadUrl", value = "https://get.helm.sh/helm-v3.0.0-linux-amd64.tar.gz")
@MojoProperty(name = "chartDirectory", value = "junit-helm")
@MojoProperty(name = "chartVersion", value = "0.0.1")
public class InitMojoTest {

	@Test
	public void verifyAddingStableByDefault(InitMojo mojo) throws Exception {

		// prepare execution
		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).helm(helmCommandCaptor.capture(), anyString(), any());
		mojo.setHelmDownloadUrl(getOsSpecificDownloadURL());
		mojo.setAddDefaultRepo(true);
		mojo.setAddUploadRepos(false);

		// run init
		mojo.execute();

		// check captured argument
		String helmDefaultCommand = helmCommandCaptor.getAllValues()
				.stream()
				.filter(cmd -> cmd.contains("repo"))
				.findAny().orElseThrow(() -> new IllegalArgumentException("Only one helm repo command expected"));

		assertTrue(helmDefaultCommand.contains("repo add stable " + InitMojo.STABLE_HELM_REPO),
				"Adding stable repo by default expected");
	}

	@Test
	public void verifyAddingUploadSnapshotRepoNoDefaultRepo(InitMojo mojo) throws Exception {

		// prepare execution
		HelmRepository helmRepo = new HelmRepository();
		helmRepo.setType(RepoType.ARTIFACTORY);
		helmRepo.setName("my-artifactory-snapshot");
		helmRepo.setUrl("https://somwhere.com/repo");
		mojo.setUploadRepoSnapshot(helmRepo);
		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).helm(helmCommandCaptor.capture(), anyString(), any());
		mojo.setHelmDownloadUrl(getOsSpecificDownloadURL());
		mojo.setAddDefaultRepo(false);
		mojo.setAddUploadRepos(true);

		// run init
		mojo.execute();

		// check captured argument
		String helmCommand = helmCommandCaptor.getAllValues()
				.stream()
				.filter(cmd -> cmd.contains("repo"))
				.findAny().orElseThrow(() -> new IllegalArgumentException("Only one helm repo command expected"));

		assertTrue(helmCommand.contains("repo add my-artifactory-snapshot https://somwhere.com/repo"),
				"Adding upload snapshot repo expected");
	}

	@Test
	public void verifyAddingUploadStableRepoNoDefaultRepo(InitMojo mojo) throws Exception {

		// prepare execution
		HelmRepository helmRepo = new HelmRepository();
		helmRepo.setType(RepoType.ARTIFACTORY);
		helmRepo.setName("my-artifactory-stable");
		helmRepo.setUrl("https://somwhere.com/repo/stable");
		mojo.setUploadRepoStable(helmRepo);
		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).helm(helmCommandCaptor.capture(), anyString(), any());
		mojo.setHelmDownloadUrl(getOsSpecificDownloadURL());
		mojo.setAddDefaultRepo(false);
		mojo.setAddUploadRepos(true);

		// run init
		mojo.execute();

		// check captured argument
		String helmCommand = helmCommandCaptor.getAllValues()
				.stream()
				.filter(cmd -> cmd.contains("repo"))
				.findAny().orElseThrow(() -> new IllegalArgumentException("Only one helm repo command expected"));

		assertTrue(helmCommand.contains("repo add my-artifactory-stable https://somwhere.com/repo/stable"),
				"Adding upload stable repo expected");
	}

	@Test
	public void verifyAddingUploadReposButNoRepoDefined(InitMojo mojo) throws Exception {

		// prepare execution
		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).helm(helmCommandCaptor.capture(), anyString(), any());
		mojo.setHelmDownloadUrl(getOsSpecificDownloadURL());
		mojo.setAddDefaultRepo(false);
		mojo.setAddUploadRepos(true);

		// run init
		mojo.execute();

		// check captured argument
		Optional<String> helmCommand = helmCommandCaptor.getAllValues()
				.stream()
				.filter(cmd -> cmd.contains("repo"))
				.findFirst();

		assertFalse(helmCommand.isPresent(), "Adding repos not expected");
	}

	@Test
	public void verifyAddingUploadSnapshotStableRepoAndDefaultRepo(InitMojo mojo) throws Exception {

		// prepare execution
		HelmRepository helmUploadStableRepo = new HelmRepository();
		helmUploadStableRepo.setType(RepoType.ARTIFACTORY);
		helmUploadStableRepo.setName("my-artifactory-stable");
		helmUploadStableRepo.setUrl("https://somwhere.com/repo/stable");
		mojo.setUploadRepoStable(helmUploadStableRepo);
		HelmRepository helmUploadSnapshotRepo = new HelmRepository();
		helmUploadSnapshotRepo.setType(RepoType.ARTIFACTORY);
		helmUploadSnapshotRepo.setName("my-artifactory-snapshot");
		helmUploadSnapshotRepo.setUrl("https://somwhere.com/repo/snapshot");
		mojo.setUploadRepoSnapshot(helmUploadSnapshotRepo);
		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).helm(helmCommandCaptor.capture(), anyString(), any());
		mojo.setHelmDownloadUrl(getOsSpecificDownloadURL());
		mojo.setAddDefaultRepo(true);
		mojo.setAddUploadRepos(true);

		// run init7
		mojo.execute();

		// check captured commands
		Set<String> helmCommands = helmCommandCaptor.getAllValues()
				.stream()
				.filter(cmd -> cmd.contains("repo"))
				.collect(Collectors.toSet());

		assertEquals(3, helmCommands.size(), "Expected 3 helm commands");
		assertTrue(helmCommands.contains("repo add my-artifactory-stable https://somwhere.com/repo/stable"),
				"Adding upload stable repo expected");
		assertTrue(helmCommands.contains("repo add my-artifactory-snapshot https://somwhere.com/repo/snapshot"),
				"Adding upload snapshot repo expected");
		assertTrue(helmCommands.contains("repo add stable " + InitMojo.STABLE_HELM_REPO),
				"Adding helm stable repo expected");
	}

	@Test
	public void verifyAddingUploadSnapshotStableRepoSameRepoName(InitMojo mojo) throws Exception {

		// prepare execution
		HelmRepository helmUploadStableRepo = new HelmRepository();
		helmUploadStableRepo.setType(RepoType.ARTIFACTORY);
		helmUploadStableRepo.setName("my-artifactory");
		helmUploadStableRepo.setUrl("https://somwhere.com/repo");
		mojo.setUploadRepoStable(helmUploadStableRepo);
		HelmRepository helmUploadSnapshotRepo = new HelmRepository();
		helmUploadSnapshotRepo.setType(RepoType.ARTIFACTORY);
		helmUploadSnapshotRepo.setName("my-artifactory");
		helmUploadSnapshotRepo.setUrl("https://somwhere.com/repo");
		mojo.setUploadRepoSnapshot(helmUploadSnapshotRepo);
		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).helm(helmCommandCaptor.capture(), anyString(), any());
		mojo.setHelmDownloadUrl(getOsSpecificDownloadURL());
		mojo.setAddDefaultRepo(true);
		mojo.setAddUploadRepos(true);

		// run init7
		mojo.execute();

		// check captured commands
		Set<String> helmCommands = helmCommandCaptor.getAllValues()
				.stream()
				.filter(cmd -> cmd.contains("repo"))
				.collect(Collectors.toSet());

		assertEquals(2, helmCommands.size(), "Expected 2 helm commands");
		assertTrue(helmCommands.contains("repo add my-artifactory https://somwhere.com/repo"),
				"Adding upload stable repo expected");
		assertTrue(helmCommands.contains("repo add stable " + InitMojo.STABLE_HELM_REPO),
				"Adding helm stable repo expected");
	}

	@Test
	public void verifyAddingUploadSnapshotRepoStableNotPresent(InitMojo mojo) throws Exception {

		// prepare execution
		HelmRepository helmUploadSnapshotRepo = new HelmRepository();
		helmUploadSnapshotRepo.setType(RepoType.ARTIFACTORY);
		helmUploadSnapshotRepo.setName("my-artifactory-snapshot");
		helmUploadSnapshotRepo.setUrl("https://somwhere.com/repo/snapshot");
		mojo.setUploadRepoSnapshot(helmUploadSnapshotRepo);
		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).helm(helmCommandCaptor.capture(), anyString(), any());
		mojo.setHelmDownloadUrl(getOsSpecificDownloadURL());
		mojo.setAddDefaultRepo(false);
		mojo.setAddUploadRepos(true);

		// run init
		mojo.execute();

		// check captured commands
		Set<String> helmCommands = helmCommandCaptor.getAllValues()
				.stream()
				.filter(cmd -> cmd.contains("repo"))
				.collect(Collectors.toSet());

		assertEquals(1, helmCommands.size(), "Expected 1 helm command");
		assertTrue(helmCommands.contains("repo add my-artifactory-snapshot https://somwhere.com/repo/snapshot"),
				"Adding upload snapshot repo expected");
	}

	@Test
	public void verifyAddingUploadStableRepoSnapshotNotPresent(InitMojo mojo) throws Exception {

		// prepare execution
		HelmRepository helmUploadStableRepo = new HelmRepository();
		helmUploadStableRepo.setType(RepoType.ARTIFACTORY);
		helmUploadStableRepo.setName("my-artifactory-stable");
		helmUploadStableRepo.setUrl("https://somwhere.com/repo/stable");
		mojo.setUploadRepoStable(helmUploadStableRepo);
		ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
		doNothing().when(mojo).helm(helmCommandCaptor.capture(), anyString(), any());
		mojo.setHelmDownloadUrl(getOsSpecificDownloadURL());
		mojo.setAddDefaultRepo(false);
		mojo.setAddUploadRepos(true);

		// run init
		mojo.execute();

		// check captured commands
		Set<String> helmCommands = helmCommandCaptor.getAllValues()
				.stream()
				.filter(cmd -> cmd.contains("repo"))
				.collect(Collectors.toSet());

		assertEquals(1, helmCommands.size(), "Expected 1 helm command");
		assertTrue(helmCommands.contains("repo add my-artifactory-stable https://somwhere.com/repo/stable"),
				"Adding upload stable repo expected");
	}

	private URL getOsSpecificDownloadURL() {
		String osForDownload;
		switch (Os.OS_FAMILY) {
			case Os.FAMILY_UNIX:
				osForDownload = "linux";
				break;
			case Os.FAMILY_MAC:
				osForDownload = "darwin";
				break;
			default:
				osForDownload = Os.OS_FAMILY;
		}

		return getOsSpecificDownloadURL(osForDownload);
	}

	private URL getOsSpecificDownloadURL(String os) {
		return assertDoesNotThrow(() -> new URL(
				"https://get.helm.sh/helm-v3.0.0-" + os + "-amd64." + ("windows".equals(os) ? "zip" : "tar.gz")));
	}
}
