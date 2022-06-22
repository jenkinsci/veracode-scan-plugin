package com.veracode.jenkins.plugin.args;

import static org.mockito.Matchers.any;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.veracode.jenkins.plugin.VeracodeNotifier;
import com.veracode.jenkins.plugin.VeracodeNotifier.VeracodeDescriptor;
import com.veracode.jenkins.plugin.data.CredentialsBlock;
import com.veracode.jenkins.plugin.utils.FormValidationUtil;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AbstractBuild.class, UploadAndScanArgs.class, FormValidationUtil.class,
        VeracodeNotifier.class, VeracodeDescriptor.class, FilePath.class
})
public class UploadAndScanArgsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testNewUploadAndScanArgs() throws IOException {
        
        String appName = "test_app";
        boolean createProfile = true;
        String teams = "test_team";
        String criticality = "High";
        String sandboxName = "test_sandbox";
        boolean createSandbox = true;
        String version = "1.0";
        String filenamePattern = "**/*.jar";
        String replacementPattern = "";
        String uploadIncludesPattern = "";
        String uploadExcludesPattern = "";
        String scanIncludesPattern = "";
        String scanExcludesPattern = "";
        boolean waitForScan = true;
        String timeout = "60";
        String deleteIncompleteScan = "1";
        boolean bRemoteScan = true;

        AbstractBuild build = PowerMockito.mock(AbstractBuild.class);
        EnvVars envVars = PowerMockito.mock(EnvVars.class);
        VeracodeNotifier.VeracodeDescriptor veracodeDescriptor = PowerMockito
                .mock(VeracodeNotifier.VeracodeDescriptor.class);
        AbstractProject abstractProject = PowerMockito.mock(AbstractProject.class);
        PowerMockito.mockStatic(FormValidationUtil.class);
        CredentialsBlock credentials = new CredentialsBlock("v_id", "v_key", "v_user", "v_pass");
        String[] filePaths = new String[2];
        PowerMockito.when(FormValidationUtil.formatTimeout(any())).thenReturn("60");
        VeracodeNotifier veracodeNotifier = PowerMockito.spy(new VeracodeNotifier(appName,
                createProfile, teams, criticality, sandboxName, createSandbox, version,
                filenamePattern, replacementPattern, uploadIncludesPattern, uploadExcludesPattern,
                scanIncludesPattern, scanExcludesPattern, waitForScan, timeout, deleteIncompleteScan, credentials));

        FilePath filePath = new FilePath(tempFolder.newFolder("tempDir"));

        PowerMockito.doReturn(veracodeDescriptor).when(veracodeNotifier).getDescriptor();
        PowerMockito.when(veracodeDescriptor.getAutoappname()).thenReturn(true);
        PowerMockito.when(veracodeDescriptor.getAutodescription()).thenReturn(true);
        PowerMockito.when(veracodeDescriptor.getAutoversion()).thenReturn(true);
        PowerMockito.when(veracodeDescriptor.getProxy()).thenReturn(false);
        PowerMockito.when(build.getDisplayName()).thenReturn(appName);
        PowerMockito.when(build.getProject()).thenReturn(abstractProject);
        PowerMockito.when(abstractProject.getDisplayName()).thenReturn("project_name");
        PowerMockito.doReturn("High").when(veracodeNotifier).getCriticality();
        PowerMockito.doReturn("**/*.jar").when(veracodeNotifier).getScanincludespattern();
        PowerMockito.doReturn("").when(veracodeNotifier).getScanexcludespattern();
        PowerMockito.doReturn("").when(veracodeNotifier).getFilenamepattern();
        PowerMockito.doReturn("").when(veracodeNotifier).getReplacementpattern();
        PowerMockito.doReturn("60").when(veracodeNotifier).getTimeout();
        PowerMockito.doReturn("1").when(veracodeNotifier).getDeleteIncompleteScan();
        PowerMockito.doReturn(filePath).when(build).getWorkspace();
        PowerMockito.doReturn(appName).when(envVars).expand(appName);
        PowerMockito.when(veracodeDescriptor.getDebug()).thenReturn(true);
        
        UploadAndScanArgs uploadAndScanArgs = UploadAndScanArgs
                .newUploadAndScanArgs(veracodeNotifier, build, envVars, filePaths, bRemoteScan);

        Assert.assertTrue("action is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains("-action"));
        Assert.assertTrue("UploadAndScan is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains("UploadAndScan"));
        Assert.assertTrue("scantimeout is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains("-scantimeout"));
        Assert.assertTrue(timeout + " is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains(timeout));
        Assert.assertTrue("appname is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains("-appname"));
        Assert.assertTrue(appName + " is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains(appName));
        Assert.assertTrue("createprofile is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains("-createprofile"));
        Assert.assertTrue("teams is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains("-teams"));
        Assert.assertTrue(teams + " is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains(teams));
        Assert.assertTrue("criticality is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains("-criticality"));
        Assert.assertTrue("action is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains(criticality));
        Assert.assertTrue("autoscan is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains("-autoscan"));
        Assert.assertTrue("maxretrycount is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains("-maxretrycount"));
        Assert.assertTrue("5 is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains("5"));
        Assert.assertTrue("debug is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains("-debug"));
        Assert.assertTrue("deleteincompletescan is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains("-deleteincompletescan"));
    }

    // Freestyle - DeleteIncompleteScan

    @Test
    public void testNewUploadAndScanArgsForFreestyleWithDeleteIncompleteScan_False() throws IOException {

        UploadAndScanArgs uploadAndScanArgs = testNewUploadAndScanArgsForFreestyleWithDeleteIncompleteScan(
                String.valueOf(false));

        Assert.assertTrue("deleteincompletescan is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains("-deleteincompletescan"));
        Assert.assertTrue("deleteincompletescan is not false",
                uploadAndScanArgs.list.get(uploadAndScanArgs.list.indexOf("-deleteincompletescan") + 1).equals("0"));
    }

    @Test
    public void testNewUploadAndScanArgsForFreestyleWithDeleteIncompleteScan_True() throws IOException {

        UploadAndScanArgs uploadAndScanArgs = testNewUploadAndScanArgsForFreestyleWithDeleteIncompleteScan(
                String.valueOf(true));

        Assert.assertTrue("deleteincompletescan is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains("-deleteincompletescan"));
        Assert.assertTrue("deleteincompletescan is not true",
                uploadAndScanArgs.list.get(uploadAndScanArgs.list.indexOf("-deleteincompletescan") + 1).equals("1"));
    }

    @Test
    public void testNewUploadAndScanArgsForFreestyleWithDeleteIncompleteScan_0() throws IOException {

        UploadAndScanArgs uploadAndScanArgs = testNewUploadAndScanArgsForFreestyleWithDeleteIncompleteScan("0");

        Assert.assertTrue("deleteincompletescan is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains("-deleteincompletescan"));
        Assert.assertTrue("deleteincompletescan is not 0",
                uploadAndScanArgs.list.get(uploadAndScanArgs.list.indexOf("-deleteincompletescan") + 1).equals("0"));
    }

    @Test
    public void testNewUploadAndScanArgsForFreestyleWithDeleteIncompleteScan_1() throws IOException {

        UploadAndScanArgs uploadAndScanArgs = testNewUploadAndScanArgsForFreestyleWithDeleteIncompleteScan("1");

        Assert.assertTrue("deleteincompletescan is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains("-deleteincompletescan"));
        Assert.assertTrue("deleteincompletescan is not 1",
                uploadAndScanArgs.list.get(uploadAndScanArgs.list.indexOf("-deleteincompletescan") + 1).equals("1"));
    }

    @Test
    public void testNewUploadAndScanArgsForFreestyleWithDeleteIncompleteScan_2() throws IOException {

        UploadAndScanArgs uploadAndScanArgs = testNewUploadAndScanArgsForFreestyleWithDeleteIncompleteScan("2");

        Assert.assertTrue("deleteincompletescan is not visible in uploadAndScanArgs",
                uploadAndScanArgs.list.contains("-deleteincompletescan"));
        Assert.assertTrue("deleteincompletescan is not 2",
                uploadAndScanArgs.list.get(uploadAndScanArgs.list.indexOf("-deleteincompletescan") + 1).equals("2"));
    }


    // Pipeline - DeleteIncompleteScan

    @Test
    public void testNewUploadAndScanArgsForPipelineWithDeleteIncompleteScan_False() throws IOException {

        UploadAndScanArgs uploadAndScanArgs = testNewUploadAndScanArgsForPipelineWithDeleteIncompleteScan("false");

        Assert.assertTrue("deleteincompletescan flag is not visible in upload and scan argument list",
                uploadAndScanArgs.list.contains("-deleteincompletescan"));
        Assert.assertTrue("deleteincompletescan flag is not set to false",
                uploadAndScanArgs.list.get(uploadAndScanArgs.list.indexOf("-deleteincompletescan") + 1).equals("0"));
    }

    @Test
    public void testNewUploadAndScanArgsForPipelineWithDeleteIncompleteScan_True() throws IOException {

        UploadAndScanArgs uploadAndScanArgs = testNewUploadAndScanArgsForPipelineWithDeleteIncompleteScan("true");

        Assert.assertTrue("deleteincompletescan flag is not visible in upload and scan argument list",
                uploadAndScanArgs.list.contains("-deleteincompletescan"));
        Assert.assertTrue("deleteincompletescan flag is not set to true",
                uploadAndScanArgs.list.get(uploadAndScanArgs.list.indexOf("-deleteincompletescan") + 1).equals("1"));
    }

    @Test
    public void testNewUploadAndScanArgsForPipelineWithDeleteIncompleteScan_0() throws IOException {

        UploadAndScanArgs uploadAndScanArgs = testNewUploadAndScanArgsForPipelineWithDeleteIncompleteScan("0");

        Assert.assertTrue("deleteincompletescan flag is not visible in upload and scan argument list",
                uploadAndScanArgs.list.contains("-deleteincompletescan"));
        Assert.assertTrue("deleteincompletescan flag is not set to 0",
                uploadAndScanArgs.list.get(uploadAndScanArgs.list.indexOf("-deleteincompletescan") + 1).equals("0"));
    }

    @Test
    public void testNewUploadAndScanArgsForPipelineWithDeleteIncompleteScan_1() throws IOException {

        UploadAndScanArgs uploadAndScanArgs = testNewUploadAndScanArgsForPipelineWithDeleteIncompleteScan("1");

        Assert.assertTrue("deleteincompletescan flag is not visible in upload and scan argument list",
                uploadAndScanArgs.list.contains("-deleteincompletescan"));
        Assert.assertTrue("deleteincompletescan flag is not set to 1",
                uploadAndScanArgs.list.get(uploadAndScanArgs.list.indexOf("-deleteincompletescan") + 1).equals("1"));
    }

    @Test
    public void testNewUploadAndScanArgsForPipelineWithDeleteIncompleteScan_2() throws IOException {

        UploadAndScanArgs uploadAndScanArgs = testNewUploadAndScanArgsForPipelineWithDeleteIncompleteScan("2");

        Assert.assertTrue("deleteincompletescan flag is not visible in upload and scan argument list",
                uploadAndScanArgs.list.contains("-deleteincompletescan"));
        Assert.assertTrue("deleteincompletescan flag is not set to 2",
                uploadAndScanArgs.list.get(uploadAndScanArgs.list.indexOf("-deleteincompletescan") + 1).equals("2"));
    }

    private UploadAndScanArgs testNewUploadAndScanArgsForFreestyleWithDeleteIncompleteScan(String deleteIncompleteScan) {

        AbstractBuild build = PowerMockito.mock(AbstractBuild.class);
        EnvVars envVars = PowerMockito.mock(EnvVars.class);
        VeracodeDescriptor veracodeDescriptor = PowerMockito.mock(VeracodeDescriptor.class);
        AbstractProject abstractProject = PowerMockito.mock(AbstractProject.class);
        PowerMockito.mockStatic(FormValidationUtil.class);
        CredentialsBlock credentials = new CredentialsBlock("v_id", "v_key", null, null);
        PowerMockito.when(FormValidationUtil.formatTimeout(any())).thenReturn("60");
        VeracodeNotifier veracodeNotifier = PowerMockito
                .spy(new VeracodeNotifier("test_app", true, "test_team", "High", "test_sandbox", true, "1.0",
                        "**/*.jar", "", "", "", "", "", true, "60", deleteIncompleteScan, credentials));

        PowerMockito.doReturn(veracodeDescriptor).when(veracodeNotifier).getDescriptor();
        PowerMockito.when(veracodeDescriptor.getAutoappname()).thenReturn(true);
        PowerMockito.when(veracodeDescriptor.getAutodescription()).thenReturn(true);
        PowerMockito.when(veracodeDescriptor.getAutoversion()).thenReturn(true);
        PowerMockito.when(veracodeDescriptor.getProxy()).thenReturn(false);
        PowerMockito.when(build.getDisplayName()).thenReturn("project_name");
        PowerMockito.when(build.getProject()).thenReturn(abstractProject);
        PowerMockito.when(abstractProject.getDisplayName()).thenReturn("project_name");
        PowerMockito.doReturn("High").when(veracodeNotifier).getCriticality();
        PowerMockito.doReturn("60").when(veracodeNotifier).getTimeout();
        PowerMockito.when(build.getWorkspace()).thenReturn(PowerMockito.mock(FilePath.class));
        PowerMockito.when(envVars.expand(any())).thenReturn("anyString");
        PowerMockito.when(veracodeDescriptor.getDebug()).thenReturn(true);
        return UploadAndScanArgs.newUploadAndScanArgs(veracodeNotifier, build, envVars, new String[2], true);
    }

    private UploadAndScanArgs testNewUploadAndScanArgsForPipelineWithDeleteIncompleteScan(String deleteIncompleteScan) {

        EnvVars envVars = PowerMockito.mock(EnvVars.class);
        AbstractBuild build = PowerMockito.mock(AbstractBuild.class);
        FilePath filePath = PowerMockito.mock(FilePath.class);
        PowerMockito.when(build.getWorkspace()).thenReturn(filePath);
        PowerMockito.when(envVars.expand(any())).thenReturn("anyString");

        if (deleteIncompleteScan.equals("false")) {
            deleteIncompleteScan = "0";
        } else if (deleteIncompleteScan.equals("true")) {
            deleteIncompleteScan = "1";
        }

        return UploadAndScanArgs.newUploadAndScanArgs(false, false, false, false, false, false, "", false, "vid",
                "vkey", "buildnum", "sample_project", "sample_app", "sample_sandbox", "scan", "High", "**/**.java", "",
                "", "", "phost", "pport", "puser", "pcredential", filePath, envVars, "60", deleteIncompleteScan, true,
                new String[2]);
    }
}