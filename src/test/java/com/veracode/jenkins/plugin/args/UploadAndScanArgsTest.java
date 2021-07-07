package com.veracode.jenkins.plugin.args;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.veracode.jenkins.plugin.VeracodeNotifier;
import com.veracode.jenkins.plugin.data.CredentialsBlock;
import com.veracode.jenkins.plugin.utils.FormValidationUtil;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AbstractBuild.class, UploadAndScanArgs.class, FormValidationUtil.class,
        VeracodeNotifier.VeracodeDescriptor.class
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
        boolean bRemoteScan = true;

        AbstractBuild build = PowerMockito.mock(AbstractBuild.class);
        EnvVars envVars = PowerMockito.mock(EnvVars.class);
        VeracodeNotifier.VeracodeDescriptor veracodeDescriptor = PowerMockito
                .mock(VeracodeNotifier.VeracodeDescriptor.class);
        AbstractProject abstractProject = PowerMockito.mock(AbstractProject.class);
        PowerMockito.mockStatic(FormValidationUtil.class);
        CredentialsBlock credentials = new CredentialsBlock("v_id", "v_key", "v_user", "v_pass");
        String[] filePaths = new String[2];
        PowerMockito.when(FormValidationUtil.formatTimeout(Matchers.any())).thenReturn("60");
        VeracodeNotifier veracodeNotifier = PowerMockito.spy(new VeracodeNotifier(appName,
                createProfile, teams, criticality, sandboxName, createSandbox, version,
                filenamePattern, replacementPattern, uploadIncludesPattern, uploadExcludesPattern,
                scanIncludesPattern, scanExcludesPattern, waitForScan, timeout, credentials));

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
    }
}