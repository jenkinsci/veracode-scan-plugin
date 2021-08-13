package com.veracode.jenkins.plugin;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.PrintStream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.veracode.jenkins.plugin.args.UploadAndScanArgs;
import com.veracode.jenkins.plugin.utils.FileUtil;
import com.veracode.jenkins.plugin.utils.RemoteScanUtil;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractItem;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        Job.class, Run.class, ItemGroup.class, FilePath.class, UploadAndScanArgs.class,
        RemoteScanUtil.class, Jenkins.class, VeracodePipelineRecorder.class, FileUtil.class
})
public class VeracodePipelineRecorderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test(expected = AbortException.class)
    public void testPerform() throws Exception {

        VeracodePipelineRecorder veracodePipelineRecorder = new VeracodePipelineRecorder("test_app",
                "medium", "test_sand_box", "scan1", false, 100, true, "test_team", true, true, true,
                true, "**/*.jar", "", "", "", "", "", true, false, "pHost", "pPort", "pUser",
                "pPassword", "vid", "vkey");
        
        Run run = PowerMockito.mock(Run.class);
        Job job = PowerMockito.mock(Job.class);
        ItemGroup itemGroup = PowerMockito.mock(ItemGroup.class);
        AbstractItem abstractItem = PowerMockito.mock(AbstractItem.class);
        Launcher launcher = PowerMockito.mock(Launcher.class);
        TaskListener taskListener = PowerMockito.mock(TaskListener.class);
        PrintStream printStream = PowerMockito.mock(PrintStream.class);
        EnvVars envVars = PowerMockito.mock(EnvVars.class);
        UploadAndScanArgs uploadAndScanArgs = PowerMockito.mock(UploadAndScanArgs.class);        
        PowerMockito.mockStatic(UploadAndScanArgs.class);
        
        PowerMockito.when(run.getEnvironment(taskListener)).thenReturn(envVars);
        PowerMockito.when(taskListener.getLogger()).thenReturn(printStream);
        PowerMockito.when(abstractItem.getParent()).thenReturn(itemGroup);
        PowerMockito.when(run.getParent()).thenReturn(job);
        PowerMockito.when(run.getDisplayName()).thenReturn("test_name");
        PowerMockito.when(job.getFullDisplayName()).thenReturn("job_name");
        PowerMockito.when(envVars.expand(any())).thenReturn("");
        
        File remoteFile = tempFolder.newFolder("remote_directory");
        File tempFile1 = new File(remoteFile, "vosp-api-wrappers-java1.jar");
        tempFile1.createNewFile();
        File tempFile2 = new File(remoteFile, "vosp-api-wrappers-java2.jar");
        tempFile2.createNewFile();
        File localFile = tempFolder.newFolder("local_directory");
        FilePath sampleFilePath = PowerMockito.spy(new FilePath(remoteFile));
        FilePath[] filePaths = new FilePath[2];
        filePaths[0] = new FilePath(tempFile1);
        filePaths[1] = new FilePath(tempFile2);
        
        when(job.getRootDir()).thenReturn(localFile);
        when(uploadAndScanArgs.newUploadAndScanArgs(anyBoolean(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyString(), anyBoolean(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), any(), any(), anyString(), anyBoolean(), any()))
                        .thenReturn(uploadAndScanArgs);
        when(run.getResult()).thenReturn(Result.FAILURE);
        
        veracodePipelineRecorder.perform(run, sampleFilePath, launcher, taskListener);
    }

    @Test
    public void testPerformWithoutUploadPatterns() throws Exception {

        // Pass null value for both upload include and exclude pattern
        VeracodePipelineRecorder veracodePipelineRecorder = new VeracodePipelineRecorder("test_app", "medium",
                "test_sand_box", "scan1", false, 100, true, "test_team", true, true, false, false, null, null, "", "",
                "", "", false, false, "pHost", "pPort", "pUser", "pPassword", "vid", "vkey");

        Run run = PowerMockito.mock(Run.class);
        Job job = PowerMockito.mock(Job.class);
        Launcher launcher = PowerMockito.mock(Launcher.class);
        TaskListener taskListener = PowerMockito.mock(TaskListener.class);
        EnvVars envVars = PowerMockito.mock(EnvVars.class);
        UploadAndScanArgs uploadAndScanArgs = PowerMockito.mock(UploadAndScanArgs.class);
        PowerMockito.mockStatic(UploadAndScanArgs.class);
        PrintStream printStream = PowerMockito.mock(PrintStream.class);
        FileUtil fileUtil = PowerMockito.mock(FileUtil.class);

        PowerMockito.when(taskListener.getLogger()).thenReturn(printStream);
        PowerMockito.when(run.getEnvironment(taskListener)).thenReturn(envVars);
        PowerMockito.when(run.getParent()).thenReturn(job);
        PowerMockito.when(run.getDisplayName()).thenReturn("test_name");
        PowerMockito.when(job.getFullDisplayName()).thenReturn("job_name");
        PowerMockito.when(envVars.expand(any())).thenReturn("");

        File remoteFile = tempFolder.newFolder("remote_directory");
        File tempFile1 = new File(remoteFile, "vosp-api-wrappers-java1.jar");
        tempFile1.createNewFile();
        File localFile = tempFolder.newFolder("local_directory");
        FilePath sampleFilePath = PowerMockito.spy(new FilePath(remoteFile));

        when(uploadAndScanArgs.newUploadAndScanArgs(anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(), anyString(), anyBoolean(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), any(), any(), anyString(),
                anyBoolean(), any())).thenReturn(uploadAndScanArgs);
        when(run.getResult()).thenReturn(Result.SUCCESS);

        veracodePipelineRecorder.perform(run, sampleFilePath, launcher, taskListener);

        // Validate null value is replaced with empty strings for both upload include
        // and exclude pattern
        Assert.assertNotNull("Upload Include Pattern should not be null",
                veracodePipelineRecorder.uploadIncludesPattern);
        Assert.assertNotNull("Upload Exclude Pattern should not be null",
                veracodePipelineRecorder.uploadExcludesPattern);

        // Verify #getStringFilePaths method executed without any errors
        Mockito.verify(fileUtil, Mockito.times(1)).getStringFilePaths(sampleFilePath
                .list(veracodePipelineRecorder.uploadIncludesPattern, veracodePipelineRecorder.uploadExcludesPattern));
    }
}