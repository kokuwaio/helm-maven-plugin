package io.kokuwa.maven.helm;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.kokuwa.maven.helm.exception.BadUploadException;
import io.kokuwa.maven.helm.junit.MojoExtension;
import io.kokuwa.maven.helm.junit.MojoProperty;
import io.kokuwa.maven.helm.junit.SystemPropertyExtension;
import io.kokuwa.maven.helm.pojo.HelmRepository;
import io.kokuwa.maven.helm.pojo.RepoType;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Server;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({ SystemPropertyExtension.class, MojoExtension.class })
@MojoProperty(name = "helmDownloadUrl", value = "https://get.helm.sh/helm-v2.14.3-linux-amd64.tar.gz")
@MojoProperty(name = "chartDirectory", value = "junit-helm")
@MojoProperty(name = "chartVersion", value = "0.0.1")
public class UploadMojoTest {

	@Test
	public void uploadToArtifactoryRequiresCredentials(UploadMojo mojo) throws MojoExecutionException {
		final HelmRepository helmRepo = new HelmRepository();
		helmRepo.setType(RepoType.ARTIFACTORY);
		helmRepo.setName("my-artifactory");
		helmRepo.setUrl("https://somwhere.com/repo");
		mojo.setUploadRepoStable(helmRepo);

		URL resource = this.getClass().getResource("app-0.1.0.tgz");
		final List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());

		doReturn(helmRepo).when(mojo).getHelmUploadRepo();
		doReturn(tgzs).when(mojo).getChartFiles(anyString());

		assertThrows(IllegalArgumentException.class, mojo::execute, "Missing credentials must fail.");
	}

	@Test
	public void uploadToArtifactoryWithRepositoryCredentials(UploadMojo mojo) throws IOException, MojoExecutionException {
		final HelmRepository helmRepo = new HelmRepository();
		helmRepo.setType(RepoType.ARTIFACTORY);
		helmRepo.setName("my-artifactory");
		helmRepo.setUrl("https://somwhere.com/repo");
		helmRepo.setUsername("foo");
		helmRepo.setPassword("bar");
		mojo.setUploadRepoStable(helmRepo);

		final URL resource = this.getClass().getResource("app-0.1.0.tgz");
		final File fileToUpload = new File(resource.getFile());
		final List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());

		doReturn(helmRepo).when(mojo).getHelmUploadRepo();
		doReturn(tgzs).when(mojo).getChartFiles(anyString());

		assertNotNull(mojo.getConnectionForUploadToArtifactory(fileToUpload, false));
	}

	@Test
	public void uploadToArtifactoryWithPlainCredentialsFromSettings(UploadMojo mojo) throws IOException, MojoExecutionException {
		final Server server = new Server();
		server.setId("my-artifactory");
		server.setUsername("foo");
		server.setPassword("bar");
		final List<Server> servers = new ArrayList<>();
		servers.add(server);
		mojo.getSettings().setServers(servers);

		final HelmRepository helmRepo = new HelmRepository();
		helmRepo.setType(RepoType.ARTIFACTORY);
		helmRepo.setName("my-artifactory");
		helmRepo.setUrl("https://somwhere.com/repo");
		mojo.setUploadRepoStable(helmRepo);

		final URL resource = this.getClass().getResource("app-0.1.0.tgz");
		final File fileToUpload = new File(resource.getFile());
		final List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());

		doReturn(helmRepo).when(mojo).getHelmUploadRepo();
		doReturn(tgzs).when(mojo).getChartFiles(anyString());

		assertNotNull(mojo.getConnectionForUploadToArtifactory(fileToUpload, false));
	}

	@Test
	public void uploadToArtifactoryWithEncryptedCredentialsFromSettings(UploadMojo mojo) throws IOException, MojoExecutionException {
		final Server server = new Server();
		server.setId("my-artifactory");
		server.setUsername("foo");
		server.setPassword("{GGhJc6qP+v0Hg2l+dei1MQFZt/55PzyFXY0MUMxcQdQ=}");
		final List<Server> servers = new ArrayList<>();
		servers.add(server);
		mojo.getSettings().setServers(servers);

		final HelmRepository helmRepo = new HelmRepository();
		helmRepo.setType(RepoType.ARTIFACTORY);
		helmRepo.setName("my-artifactory");
		helmRepo.setUrl("https://somwhere.com/repo");
		mojo.setUploadRepoStable(helmRepo);

		final URL resource = this.getClass().getResource("app-0.1.0.tgz");
		final File fileToUpload = new File(resource.getFile());
		final List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());

		doReturn(this.getClass().getResource("settings-security.xml").getFile()).when(mojo).getHelmSecurity();
		doReturn(helmRepo).when(mojo).getHelmUploadRepo();
		doReturn(tgzs).when(mojo).getChartFiles(anyString());

		assertNotNull(mojo.getConnectionForUploadToArtifactory(fileToUpload, false));

		final PasswordAuthentication pwd = Authenticator.requestPasswordAuthentication(InetAddress.getLocalHost(), 443, "https", "", "basicauth");
		assertEquals("foo", pwd.getUserName());
		assertEquals("bar", String.valueOf(pwd.getPassword()));
	}

	@Test
	public void verifyHttpConnectionForArtifactoryUpload(UploadMojo uploadMojo) throws IOException, MojoExecutionException {
		final URL resource = this.getClass().getResource("app-0.1.0.tgz");
		final File fileToUpload = new File(resource.getFile());

		final HelmRepository helmRepo = new HelmRepository();
		helmRepo.setType(RepoType.ARTIFACTORY);
		helmRepo.setName("my-artifactory");
		helmRepo.setUrl("https://somwhere.com/repo");
		helmRepo.setUsername("foo");
		helmRepo.setPassword("bar");
		uploadMojo.setUploadRepoStable(helmRepo);

		// Call
		HttpURLConnection httpURLConnection = uploadMojo.getConnectionForUploadToArtifactory(fileToUpload, false);

		// Verify
		assertEquals("PUT", httpURLConnection.getRequestMethod());
		String expectedUploadUrl = helmRepo.getUrl() + "/" + fileToUpload.getName();
		assertEquals(expectedUploadUrl, httpURLConnection.getURL().toString());

		String contentTypeHeader = httpURLConnection.getRequestProperty("Content-Type");
		assertNotNull(contentTypeHeader);
		assertEquals("application/gzip", contentTypeHeader);
	}

	@Test
	public void uploadToArtifactoryByGroupId(UploadMojo mojo) throws IOException, MojoExecutionException {
		final HelmRepository helmRepo = new HelmRepository();
		helmRepo.setType(RepoType.ARTIFACTORY);
		helmRepo.setName("my-artifactory");
		helmRepo.setUrl("https://somwhere.com/repo");
		helmRepo.setUsername("foo");
		helmRepo.setPassword("bar");
		helmRepo.setUseGroupId(true);
		mojo.setUploadRepoStable(helmRepo);
		final String projectGroupId = "example.foo.bar";
		final String projectVersion = "0.1.0";
		final String chartFileName = "app-0.1.0.tgz";
		final URL resource = this.getClass().getResource(chartFileName);
		final File fileToUpload = new File(resource.getFile());
		final List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());

		doReturn(helmRepo).when(mojo).getHelmUploadRepo();
		doReturn(tgzs).when(mojo).getChartFiles(anyString());
		doReturn(projectGroupId).when(mojo).getProjectGroupId();
		doReturn(projectVersion).when(mojo).getProjectVersion();

		HttpURLConnection connection = mojo.getConnectionForUploadToArtifactory(fileToUpload, helmRepo.isUseGroupId());
		assertEquals(
				helmRepo.getUrl() + "/"
						+ projectGroupId.replace(".", "/") + "/"
						+ projectVersion + "/"
						+ chartFileName
				, connection.getURL().toString());
	}

	@Test
	public void uploadToArtifactoryWithoutByGroupId(UploadMojo mojo) throws IOException, MojoExecutionException {
		final HelmRepository helmRepo = new HelmRepository();
		helmRepo.setType(RepoType.ARTIFACTORY);
		helmRepo.setName("my-artifactory");
		helmRepo.setUrl("https://somwhere.com/repo");
		helmRepo.setUsername("foo");
		helmRepo.setPassword("bar");
		helmRepo.setUseGroupId(false);
		mojo.setUploadRepoStable(helmRepo);
		final String projectGroupId = "example.foo.bar";
		final String projectVersion = "0.1.0";
		final String chartFileName = "app-0.1.0.tgz";
		final URL resource = this.getClass().getResource(chartFileName);
		final File fileToUpload = new File(resource.getFile());
		final List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());

		doReturn(helmRepo).when(mojo).getHelmUploadRepo();
		doReturn(tgzs).when(mojo).getChartFiles(anyString());
		doReturn(projectGroupId).when(mojo).getProjectGroupId();
		doReturn(projectVersion).when(mojo).getProjectVersion();

		HttpURLConnection connection = mojo.getConnectionForUploadToArtifactory(fileToUpload, helmRepo.isUseGroupId());
		assertEquals(
				helmRepo.getUrl() + "/" + chartFileName
				, connection.getURL().toString());
	}

	@Test
	public void verifyHttpConnectionForChartmuseumUpload(UploadMojo uploadMojo) throws IOException, MojoExecutionException {
		final HelmRepository helmRepo = new HelmRepository();
		helmRepo.setType(RepoType.CHARTMUSEUM);
		helmRepo.setName("my-chartmuseum");
		helmRepo.setUrl("https://somwhere.com/repo");
		uploadMojo.setUploadRepoStable(helmRepo);

		// Call
		HttpURLConnection httpURLConnection = uploadMojo.getConnectionForUploadToChartmuseum();

		// Verify
		assertEquals("POST", httpURLConnection.getRequestMethod());
		assertEquals(helmRepo.getUrl(), httpURLConnection.getURL().toString());

		String contentTypeHeader = httpURLConnection.getRequestProperty("Content-Type");
		assertNotNull(contentTypeHeader);
		assertEquals("application/gzip", contentTypeHeader);
	}

	@Test
	public void verifyUploadToArtifactory(UploadMojo uploadMojo) throws MojoExecutionException, IOException {
		final URL resource = this.getClass().getResource("app-0.1.0.tgz");
		final File fileToUpload = new File(resource.getFile());
		final List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());

		final HelmRepository helmRepo = new HelmRepository();
		helmRepo.setType(RepoType.ARTIFACTORY);
		helmRepo.setName("my-artifactory");
		helmRepo.setUrl("https://somwhere.com/repo");
		helmRepo.setUsername("foo");
		helmRepo.setPassword("bar");
		uploadMojo.setUploadRepoStable(helmRepo);

		doReturn(helmRepo).when(uploadMojo).getHelmUploadRepo();
		doReturn(tgzs).when(uploadMojo).getChartFiles(anyString());

		HttpURLConnection urlConnectionMock = Mockito.mock(HttpURLConnection.class);
		doReturn(new NullOutputStream()).when(urlConnectionMock).getOutputStream();
		doReturn(new ByteArrayInputStream("ok".getBytes(StandardCharsets.UTF_8))).when(urlConnectionMock)
				.getInputStream();
		doNothing().when(urlConnectionMock).connect();
		doReturn(urlConnectionMock).when(uploadMojo).getConnectionForUploadToArtifactory(fileToUpload, false);

		// call Mojo
		uploadMojo.execute();

		verify(uploadMojo).getConnectionForUploadToArtifactory(fileToUpload, false);
	}

	@Test
	public void repositoryTypeRequired(UploadMojo uploadMojo) throws MojoExecutionException {
		final HelmRepository helmRepo = new HelmRepository();
		helmRepo.setName("unknown-repo");
		helmRepo.setUrl("https://somwhere.com/repo");
		uploadMojo.setUploadRepoStable(helmRepo);

		URL resource = this.getClass().getResource("app-0.1.0.tgz");
		final List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());

		doReturn(helmRepo).when(uploadMojo).getHelmUploadRepo();
		doReturn(tgzs).when(uploadMojo).getChartFiles(anyString());

		assertThrows(IllegalArgumentException.class, uploadMojo::execute, "Missing repo type must fail.");
	}

	@Test
	public void verfifyNullErrorStreamOnFailedUpload(UploadMojo uploadMojo)
			throws IOException, MojoExecutionException
	{
		final HelmRepository helmRepo = new HelmRepository();
		helmRepo.setType(RepoType.CHARTMUSEUM);
		helmRepo.setName("my-chartmuseum");
		helmRepo.setUrl("https://somwhere.com/repo");
		uploadMojo.setUploadRepoStable(helmRepo);

		URL testChart = this.getClass().getResource("app-0.1.0.tgz");

		final HttpURLConnection urlConnectionMock = Mockito.mock(HttpURLConnection.class);
		doReturn(new NullOutputStream()).when(urlConnectionMock).getOutputStream();
		doReturn(301).when(urlConnectionMock).getResponseCode();
		doReturn(null).when(urlConnectionMock).getErrorStream();
		doReturn(null).when(urlConnectionMock).getInputStream();
		doNothing().when(urlConnectionMock).connect();
		doReturn(urlConnectionMock).when(uploadMojo).getConnectionForUploadToChartmuseum();

		try {
			uploadMojo.uploadSingle(testChart.getFile());
		} catch (BadUploadException e) {
			assertNotNull(e.getMessage(), "Exception must provide a message");
			return;
		}
		fail("BadUploadException expected on failed upload");
	}

	@Test
	public void uploadToNexusWithRepositoryCredentials(UploadMojo mojo) throws IOException, MojoExecutionException {
		final HelmRepository helmRepo = new HelmRepository();
		helmRepo.setType(RepoType.NEXUS);
		helmRepo.setName("my-nexus");
		helmRepo.setUrl("https://somwhere.com/repo");
		helmRepo.setUsername("foo");
		helmRepo.setPassword("bar");
		mojo.setUploadRepoStable(helmRepo);

		final URL resource = this.getClass().getResource("app-0.1.0.tgz");
		final File fileToUpload = new File(resource.getFile());
		final List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());

		doReturn(helmRepo).when(mojo).getHelmUploadRepo();
		doReturn(tgzs).when(mojo).getChartFiles(anyString());

		assertNotNull(mojo.getConnectionForUploadToNexus(fileToUpload));
	}

	@Test
	public void uploadToNexusWithPlainCredentialsFromSettings(UploadMojo mojo) throws IOException, MojoExecutionException {
		final Server server = new Server();
		server.setId("my-nexus");
		server.setUsername("foo");
		server.setPassword("bar");
		final List<Server> servers = new ArrayList<>();
		servers.add(server);
		mojo.getSettings().setServers(servers);

		final HelmRepository helmRepo = new HelmRepository();
		helmRepo.setType(RepoType.NEXUS);
		helmRepo.setName("my-nexus");
		helmRepo.setUrl("https://somwhere.com/repo");
		mojo.setUploadRepoStable(helmRepo);

		final URL resource = this.getClass().getResource("app-0.1.0.tgz");
		final File fileToUpload = new File(resource.getFile());
		final List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());

		doReturn(helmRepo).when(mojo).getHelmUploadRepo();
		doReturn(tgzs).when(mojo).getChartFiles(anyString());

		assertNotNull(mojo.getConnectionForUploadToNexus(fileToUpload));
	}

	@Test
	public void uploadToNexusWithEncryptedCredentialsFromSettings(UploadMojo mojo) throws IOException, MojoExecutionException {
		final Server server = new Server();
		server.setId("my-nexus");
		server.setUsername("foo");
		server.setPassword("{GGhJc6qP+v0Hg2l+dei1MQFZt/55PzyFXY0MUMxcQdQ=}");
		final List<Server> servers = new ArrayList<>();
		servers.add(server);
		mojo.getSettings().setServers(servers);

		final HelmRepository helmRepo = new HelmRepository();
		helmRepo.setType(RepoType.NEXUS);
		helmRepo.setName("my-nexus");
		helmRepo.setUrl("https://somwhere.com/repo");
		mojo.setUploadRepoStable(helmRepo);

		final URL resource = this.getClass().getResource("app-0.1.0.tgz");
		final File fileToUpload = new File(resource.getFile());
		final List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());

		doReturn(this.getClass().getResource("settings-security.xml").getFile()).when(mojo).getHelmSecurity();
		doReturn(helmRepo).when(mojo).getHelmUploadRepo();
		doReturn(tgzs).when(mojo).getChartFiles(anyString());

		assertNotNull(mojo.getConnectionForUploadToNexus(fileToUpload));

		final PasswordAuthentication pwd = Authenticator.requestPasswordAuthentication(InetAddress.getLocalHost(), 443, "https", "", "basicauth");
		assertEquals("foo", pwd.getUserName());
		assertEquals("bar", String.valueOf(pwd.getPassword()));
	}

	@Test
	public void uploadToNexusWithoutCredentials(UploadMojo mojo) throws IOException, MojoExecutionException {
		final HelmRepository helmRepo = new HelmRepository();
		helmRepo.setType(RepoType.NEXUS);
		helmRepo.setName("my-nexus");
		helmRepo.setUrl("https://somwhere.com/repo");
		mojo.setUploadRepoStable(helmRepo);

		final URL resource = this.getClass().getResource("app-0.1.0.tgz");
		final File fileToUpload = new File(resource.getFile());
		final List<String> tgzs = new ArrayList<>();
		tgzs.add(resource.getFile());

		doReturn(helmRepo).when(mojo).getHelmUploadRepo();
		doReturn(tgzs).when(mojo).getChartFiles(anyString());

		assertNotNull(mojo.getConnectionForUploadToNexus(fileToUpload));
	}

	@Test
	public void verifyHttpConnectionForNexusUpload(UploadMojo uploadMojo) throws IOException, MojoExecutionException {
		final URL resource = this.getClass().getResource("app-0.1.0.tgz");
		final File fileToUpload = new File(resource.getFile());

		final HelmRepository helmRepo = new HelmRepository();
		helmRepo.setType(RepoType.NEXUS);
		helmRepo.setName("my-nexus");
		helmRepo.setUrl("https://somwhere.com/repo");
		helmRepo.setUsername("foo");
		helmRepo.setPassword("bar");
		uploadMojo.setUploadRepoStable(helmRepo);

		// Call
		HttpURLConnection httpURLConnection = uploadMojo.getConnectionForUploadToNexus(fileToUpload);

		// Verify
		assertEquals("PUT", httpURLConnection.getRequestMethod());
		String expectedUploadUrl = helmRepo.getUrl() + "/" + fileToUpload.getName();
		assertEquals(expectedUploadUrl, httpURLConnection.getURL().toString());

		String contentTypeHeader = httpURLConnection.getRequestProperty("Content-Type");
		assertNotNull(contentTypeHeader);
		assertEquals("application/gzip", contentTypeHeader);
	}

	/** Writes to nowhere */
	public class NullOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
		}
	}
}

