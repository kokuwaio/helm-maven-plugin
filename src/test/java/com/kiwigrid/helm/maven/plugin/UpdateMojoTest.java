package com.kiwigrid.helm.maven.plugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.kiwigrid.helm.maven.plugin.junit.MojoExtension;
import com.kiwigrid.helm.maven.plugin.junit.MojoProperty;
import com.kiwigrid.helm.maven.plugin.junit.SystemPropertyExtension;
import com.kiwigrid.helm.maven.plugin.pojo.HelmRepository;
import com.kiwigrid.helm.maven.plugin.pojo.RepoType;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({ SystemPropertyExtension.class, MojoExtension.class })
@MojoProperty(name = "helmDownloadUrl", value = "https://kubernetes-helm.storage.googleapis.com/helm-v2.9.0-linux-amd64.tar.gz")
@MojoProperty(name = "chartDirectory", value = "junit-helm")
@MojoProperty(name = "chartVersion", value = "0.0.1")
public class UpdateMojoTest {

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
		doReturn(tgzs).when(mojo).getChartTgzs(anyString());

		assertThrows(IllegalArgumentException.class, mojo::execute, "Missing credentials must fail.");
	}

	@Test
	public void verifyHttpConnectionForArtifactoryUpload(UploadMojo uploadMojo) throws IOException {
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
		HttpURLConnection httpURLConnection = uploadMojo.getConnectionForUploadToArtifactory(fileToUpload);

		// Verify
		assertEquals("PUT", httpURLConnection.getRequestMethod());
		String expectedUploadUrl = helmRepo.getUrl() + "/" + fileToUpload.getName();
		assertEquals(expectedUploadUrl, httpURLConnection.getURL().toString());

		String contentTypeHeader = httpURLConnection.getRequestProperty("Content-Type");
		assertNotNull(contentTypeHeader);
		assertEquals("application/gzip", contentTypeHeader);
	}

	@Test
	public void verifyHttpConnectionForChartmuseumUpload(UploadMojo uploadMojo) throws IOException {
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
		doReturn(tgzs).when(uploadMojo).getChartTgzs(anyString());

		HttpURLConnection urlConnectionMock = Mockito.mock(HttpURLConnection.class);
		doReturn(new NullOutputStream()).when(urlConnectionMock).getOutputStream();
		doReturn(new ByteArrayInputStream("ok".getBytes(StandardCharsets.UTF_8))).when(urlConnectionMock)
				.getInputStream();
		doNothing().when(urlConnectionMock).connect();
		doReturn(urlConnectionMock).when(uploadMojo).getConnectionForUploadToArtifactory(fileToUpload);

		// call Mojo
		uploadMojo.execute();

		verify(uploadMojo).getConnectionForUploadToArtifactory(fileToUpload);
	}

	/** Writes to nowhere */
	public class NullOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
		}
	}
}

