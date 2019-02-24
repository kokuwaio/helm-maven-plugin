package com.kiwigrid.helm.maven.plugin;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.Files.write;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

class AbstractHelmMojoTest {

    private NoopHelmMojo subjectSpy;
    private Path testPath;
    private Path testHelmExecutablePath;

    @BeforeEach
    void setUp() throws IOException {

        subjectSpy = Mockito.spy(new NoopHelmMojo());
        testPath = Files.createTempDirectory("test").toAbsolutePath();
        testHelmExecutablePath = testPath.resolve(SystemUtils.IS_OS_WINDOWS ? "helm.exe" : "helm");
    }

    @Nested
    class WhenUseLocalBinaryAndAutoDetectIsEnabled {

        @BeforeEach
        void setUp() {

            subjectSpy.setUseLocalHelmBinary(true);
            subjectSpy.setAutoDetectLocalHelmBinary(true);
            doReturn(new String[]{ testPath.toAbsolutePath().toString() }).when(subjectSpy).getPathsFromEnvironmentVariables();
        }

        @Test
        void helmIsAutoDetectedFromPATH() throws MojoExecutionException, IOException {

            final Path expectedPath = addHelmToTestPath();
            assertEquals(expectedPath, subjectSpy.getHelmExecuteablePath());
        }

        @Test
        void executionFailsWhenHelmIsNotFoundInPATH() {

            final MojoExecutionException exception = assertThrows(MojoExecutionException.class, subjectSpy::getHelmExecuteablePath);
            assertTrue(exception.getMessage().contains("not found"));
        }

        @Test
        void helmIsAutoDetectedEvenWhenExecutableDirectoryIsConfigured() throws IOException, MojoExecutionException {

            final String explicitExecutableDirectory = "/fish/in/da/sea";
            subjectSpy.setHelmExecutableDirectory(explicitExecutableDirectory);
            final Path expectedPath = addHelmToTestPath();
            assertEquals(expectedPath, subjectSpy.getHelmExecuteablePath());
            assertNotEquals(explicitExecutableDirectory, subjectSpy.getHelmExecuteablePath());
        }
    }

    @Nested
    class WhenExecutableDirectoryIsSpecifiedAndUseLocalBinaryIsDisabled {

        @BeforeEach
        void setUp() {

            subjectSpy.setUseLocalHelmBinary(false);
            subjectSpy.setHelmExecutableDirectory(testPath.toString());
        }

        @Test
        void helmIsInTheExplicitlyConfiguredDirectory() throws MojoExecutionException, IOException {

            final Path expectedPath = addHelmToTestPath();
            assertEquals(expectedPath, subjectSpy.getHelmExecuteablePath());
        }

        @Test
        void executionFailsWhenHelmIsNotFoundInConfiguredDirectory() {

            final MojoExecutionException exception = assertThrows(MojoExecutionException.class, subjectSpy::getHelmExecuteablePath);
            assertTrue(exception.getMessage().contains("not found"));
        }
    }

    private Path addHelmToTestPath() throws IOException { return write(testHelmExecutablePath, new byte[]{}); }

    @AfterEach
    void tearDown() { deleteQuietly(testPath.toFile()); }

    private static class NoopHelmMojo extends AbstractHelmMojo {

        @Override
        public void execute() { /* Noop. */ }
    }
}
