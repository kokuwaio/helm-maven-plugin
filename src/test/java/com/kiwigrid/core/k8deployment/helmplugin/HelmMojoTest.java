package com.kiwigrid.core.k8deployment.helmplugin;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doNothing;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.kiwigrid.core.k8deployment.helmplugin.junit.MojoExtension;
import com.kiwigrid.core.k8deployment.helmplugin.junit.MojoProperty;
import com.kiwigrid.core.k8deployment.helmplugin.junit.SystemPropertyExtension;

@ExtendWith({ SystemPropertyExtension.class, MojoExtension.class })
@MojoProperty(name = "helmDownloadUrl", value = "https://kubernetes-helm.storage.googleapis.com/helm-v2.7.2-linux-amd64.tar.gz")
@MojoProperty(name = "helmUploadUrl", value = "none")
@MojoProperty(name = "helmRepoUrl", value = "none")
@MojoProperty(name = "chartDirectory", value = "junit-helm")
public class HelmMojoTest {

    @DisplayName("Init helm with different download urls.")
    @ParameterizedTest
    @ValueSource(strings = { "darwin", "linux", "windows" })
    public void testInitMojo(String os, InitMojo mojo) throws Exception {

        // prepare execution

        doNothing().when(mojo).callCli(contains("helm "), anyString(), anyBoolean());
        mojo.setHelmDownloadUrl("https://kubernetes-helm.storage.googleapis.com/helm-v2.7.2-" + os + "-amd64.tar.gz");

        // run init

        mojo.execute();

        // check helm file

        Path helm = Paths.get(mojo.getHelmExecuteableDirectory(), "windows".equals(os) ? "helm.exe" : "helm").toAbsolutePath();
        assertTrue(Files.exists(helm), "Helm executable not found at: " + helm);
    }
}