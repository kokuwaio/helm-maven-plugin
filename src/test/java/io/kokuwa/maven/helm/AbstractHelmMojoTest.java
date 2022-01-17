package io.kokuwa.maven.helm;

import io.kokuwa.maven.helm.pojo.K8SCluster;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static java.nio.file.Files.write;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

class AbstractHelmMojoTest {
    @Spy
    @InjectMocks
    private NoopHelmMojo subjectSpy = new NoopHelmMojo();
    private Path testPath;
    private Path testHelmExecutablePath;

    private String chartDir;
    private String excludeDir1;
    private String excludeDir2;

    private static final LocalDate LOCAL_DATE = LocalDate.of(2000, 01, 01);

    @Mock
    private Clock clock;

    private Clock fixedClock;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        fixedClock = Clock.fixed(LOCAL_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        chartDir = getBaseChartsDirectory().toString();
        excludeDir1 = chartDir + File.separator + "exclude1";
        excludeDir2= chartDir + File.separator + "exclude2";

        testPath = Files.createTempDirectory("test").toAbsolutePath();
        testHelmExecutablePath = testPath.resolve(SystemUtils.IS_OS_WINDOWS ? "helm.exe" : "helm");
    }

    @Test
    void getChartDirectoriesReturnChartDirectories() throws MojoExecutionException {

        List<String> chartDirectories = subjectSpy.getChartDirectories(chartDir);
        List<String> expected = asList(chartDir, excludeDir1, excludeDir2);

        assertTrue(chartDirectories.containsAll(expected), "Charts dirs: " + chartDirectories + ", should contain all expected dirs: " + expected);
    }

    @Nested
    class K8SClusterArgs{

        @Test
        void k8sClusterArg_WhenNull(){
            assertNull(subjectSpy.getK8SCluster());
            assertEquals("", subjectSpy.getK8SArgs());
        }

        @Test
        void k8sClusterArg_NotConfigured(){
            final K8SCluster k8SCluster = new K8SCluster();
            subjectSpy.setK8SCluster(k8SCluster);
            assertEquals("", subjectSpy.getK8SArgs());
        }

        @Test
        void k8sClusterArg_ApiUrl(){
            final K8SCluster k8SCluster = new K8SCluster();
            subjectSpy.setK8SCluster(k8SCluster);
            k8SCluster.setApiUrl("custom-api-url");
            assertEquals(" --kube-apiserver custom-api-url", subjectSpy.getK8SArgs());
        }

        @Test
        void k8sClusterArg_AsUser(){
            final K8SCluster k8SCluster = new K8SCluster();
            subjectSpy.setK8SCluster(k8SCluster);
            k8SCluster.setAsUser("custom-user");
            assertEquals(" --kube-as-user custom-user", subjectSpy.getK8SArgs());
        }

        @Test
        void k8sClusterArg_AsGroup(){
            final K8SCluster k8SCluster = new K8SCluster();
            subjectSpy.setK8SCluster(k8SCluster);
            k8SCluster.setAsGroup("custom-group");
            assertEquals(" --kube-as-group custom-group", subjectSpy.getK8SArgs());
        }

        @Test
        void k8sClusterArg_Namespace(){
            final K8SCluster k8SCluster = new K8SCluster();
            subjectSpy.setK8SCluster(k8SCluster);
            k8SCluster.setNamespace("custom-ns");
            assertEquals(" --namespace custom-ns", subjectSpy.getK8SArgs());
        }

        @Test
        void k8sClusterArg_Token(){
            final K8SCluster k8SCluster = new K8SCluster();
            subjectSpy.setK8SCluster(k8SCluster);
            k8SCluster.setToken("custom-token");
            assertEquals(" --kube-token custom-token", subjectSpy.getK8SArgs());
        }
        @Test
        void k8sClusterArg_All(){
            final K8SCluster k8SCluster = new K8SCluster();
            subjectSpy.setK8SCluster(k8SCluster);
            k8SCluster.setApiUrl("custom-api-url");
            k8SCluster.setAsUser("custom-user");
            k8SCluster.setAsGroup("custom-group");
            k8SCluster.setToken("custom-token");
            k8SCluster.setNamespace("custom-ns");
            k8SCluster.setToken("custom-token");
            assertEquals(
                    " --kube-apiserver custom-api-url"
                            + " --namespace custom-ns"
                            + " --kube-as-user custom-user"
                            + " --kube-as-group custom-group"
                            + " --kube-token custom-token"
                    , subjectSpy.getK8SArgs());
        }

    }

    @Nested
    class TimeStampAsVersion {


        @Test
        void timestamp_Formats() {
            doReturn("yyyy MM dd HH:mm:ss").when(subjectSpy).getTimestampFormat();
            assertEquals("2000 01 01 00:00:00", subjectSpy.getCurrentTimestamp());

            doReturn("yyyy MM dd HHmmss").when(subjectSpy).getTimestampFormat();
            assertEquals("2000 01 01 000000", subjectSpy.getCurrentTimestamp());
        }

        @Test
        void timestampAsVersion() {
            final String timeStamp = "2000-01-01 00:00:00";
            doReturn("yyyy-MM-dd HH:mm:ss").when(subjectSpy).getTimestampFormat();
            doReturn(timeStamp).when(subjectSpy).getCurrentTimestamp();
            doReturn(true).when(subjectSpy).isTimestampOnSnapshot();
            final String chartVersion = "0.0.0-SNAPSHOT";
            subjectSpy.setChartVersion(chartVersion);
            assertEquals(chartVersion.replace("SNAPSHOT",timeStamp), subjectSpy.getChartVersionWithProcessing());

            doReturn(false).when(subjectSpy).isTimestampOnSnapshot();
            assertEquals(chartVersion, subjectSpy.getChartVersionWithProcessing());
        }

        @Test
        void snapshotAsVersion() {
            doReturn("yyyy-MM-dd HH:mm:ss").when(subjectSpy).getTimestampFormat();
            doReturn("2000-01-01 00:00:00").when(subjectSpy).getCurrentTimestamp();
            doReturn(true).when(subjectSpy).isTimestampOnSnapshot();
            final String chartVersion = "0.0.0-SNAPSHOT";
            subjectSpy.setChartVersion(chartVersion);

            doReturn(false).when(subjectSpy).isTimestampOnSnapshot();
            assertEquals(chartVersion, subjectSpy.getChartVersionWithProcessing());
        }

        @Test
        void releaseVersions() {
            doReturn("yyyy-MM-dd HH:mm:ss").when(subjectSpy).getTimestampFormat();
            doReturn("2000-01-01 00:00:00").when(subjectSpy).getCurrentTimestamp();
            doReturn(true).when(subjectSpy).isTimestampOnSnapshot();
            final String chartVersion = "0.0.0";
            subjectSpy.setChartVersion(chartVersion);

            doReturn(false).when(subjectSpy).isTimestampOnSnapshot();
            assertEquals(chartVersion, subjectSpy.getChartVersionWithProcessing());

            doReturn(true).when(subjectSpy).isTimestampOnSnapshot();
            assertEquals(chartVersion, subjectSpy.getChartVersionWithProcessing());
        }
    }

    @Test
    void getChartDirectoriesReturnChartDirectoriesWithPlainExclusion() throws MojoExecutionException {
        Path baseChartsDirectory = getBaseChartsDirectory();

        subjectSpy.setExcludes(new String[]{excludeDir1});

        List<String> chartDirectories = subjectSpy.getChartDirectories(baseChartsDirectory.toString());

        assertFalse(chartDirectories.contains(excludeDir1), "Charts dirs [" + chartDirectories + "] should not contain excluded dirs [" + excludeDir1 + "]");
        assertTrue(chartDirectories.contains(excludeDir2), "Charts dirs [" + chartDirectories + "] should contain not excluded dirs [" + excludeDir2 + "]");
    }


    @Test
    void getChartDirectoriesReturnChartDirectoriesWithAntPatternsExclusion() throws MojoExecutionException {
        Path baseChartsDirectory = getBaseChartsDirectory();

        subjectSpy.setExcludes(new String[]{"**" + File.separator + "exclude*"});

        List<String> chartDirectories = subjectSpy.getChartDirectories(baseChartsDirectory.toString());

        assertFalse(chartDirectories.contains(excludeDir1), "Charts dirs [" + chartDirectories + "] should not contain excluded dirs [" + excludeDir1 + "]");
        assertFalse(chartDirectories.contains(excludeDir2), "Charts dirs [" + chartDirectories + "] should not contain excluded dirs [" + excludeDir2 + "]");
    }

    @Test
    void formatIfValueIsNotEmpty() {
        String formatString = "Test %s data";
        String value = "format";

        String formatWithValue = subjectSpy.formatIfValueIsNotEmpty(formatString, value);
        String formatWithNullValue = subjectSpy.formatIfValueIsNotEmpty(formatString, null);
        String formatWithEmptyValue = subjectSpy.formatIfValueIsNotEmpty(formatString, "");

        assertEquals("Test format data", formatWithValue);
        assertEquals("", formatWithNullValue);
        assertEquals("", formatWithEmptyValue);
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

    private Path getBaseChartsDirectory() {
    	return new File(getClass().getResource("Chart.yaml").getFile()).toPath().getParent();
    }

    @AfterEach
    void tearDown() { deleteQuietly(testPath.toFile()); }

    private static class NoopHelmMojo extends AbstractHelmMojo {

        @Override
        public void execute() { /* Noop. */ }
    }
}
