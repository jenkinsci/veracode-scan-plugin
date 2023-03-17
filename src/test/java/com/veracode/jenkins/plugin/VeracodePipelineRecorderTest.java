package com.veracode.jenkins.plugin;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.veracode.http.Credentials;
import com.veracode.http.Region;
import com.veracode.jenkins.plugin.VeracodeNotifier.VeracodeDescriptor;
import com.veracode.jenkins.plugin.args.UploadAndScanArgs;
import com.veracode.jenkins.plugin.data.ScanHistory;
import com.veracode.jenkins.plugin.utils.FileUtil;
import com.veracode.jenkins.plugin.utils.RemoteScanUtil;
import com.veracode.jenkins.plugin.utils.WrapperUtil;
import com.veracode.jenkins.plugin.utils.XmlUtil;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.AbstractItem;
import hudson.model.Computer;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        Job.class, Run.class, ItemGroup.class, FilePath.class, UploadAndScanArgs.class,
        RemoteScanUtil.class, Jenkins.class, VeracodePipelineRecorder.class, FileUtil.class,
        ProcStarter.class, WrapperUtil.class, XmlUtil.class, Credentials.class, Jenkins.class,
        VeracodeDescriptor.class
})
public class VeracodePipelineRecorderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test(expected = AbortException.class)
    public void testPerform() throws Exception {

        VeracodePipelineRecorder veracodePipelineRecorder = new VeracodePipelineRecorder("test_app",
                "medium", "test_sand_box", "scan1", false, 100, "0", true, "test_team", true, true, true,
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
                anyString(), anyString(), anyString(), any(), any(), anyString(), anyString(), anyBoolean(), any()))
                        .thenReturn(uploadAndScanArgs);
        when(run.getResult()).thenReturn(Result.FAILURE);
        
        veracodePipelineRecorder.perform(run, sampleFilePath, launcher, taskListener);
    }

    @Test
    public void testPerformWithoutUploadPatterns() throws Exception {

        // Pass null value for both upload include and exclude pattern
        VeracodePipelineRecorder veracodePipelineRecorder = new VeracodePipelineRecorder("test_app", "medium",
                "test_sand_box", "scan1", false, 100, "0", true, "test_team", true, true, false, false, null, null, "", "",
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
                anyString(), anyBoolean(), any())).thenReturn(uploadAndScanArgs);
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

    @Test
    public void testRunScanFromRemote() throws Exception {

        Run run = PowerMockito.mock(Run.class);
        FilePath filePath = PowerMockito.mock(FilePath.class);
        TaskListener taskListener = PowerMockito.mock(TaskListener.class);
        PrintStream printStream = PowerMockito.mock(PrintStream.class);
        Computer computer = Mockito.mock(Computer.class);
        Node node = Mockito.mock(Node.class);
        EnvVars envVars = PowerMockito.mock(EnvVars.class);
        Job job = PowerMockito.mock(Job.class);
        Launcher launcher = PowerMockito.mock(Launcher.class);
        ProcStarter procStarter = PowerMockito.mock(ProcStarter.class);
        Proc proc = PowerMockito.mock(Proc.class);
        ScanHistory scanHistory = PowerMockito.mock(ScanHistory.class);
        Credentials credentials = PowerMockito.mock(Credentials.class);
        Region region = PowerMockito.mock(Region.class);
        VeracodeAction veracodeAction = PowerMockito.mock(VeracodeAction.class);
        Jenkins jenkins = PowerMockito.mock(Jenkins.class);

        PowerMockito.mockStatic(RemoteScanUtil.class);
        PowerMockito.mockStatic(FileUtil.class);
        PowerMockito.mockStatic(WrapperUtil.class);
        PowerMockito.mockStatic(XmlUtil.class);
        PowerMockito.mockStatic(Credentials.class);
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.mockStatic(VeracodeDescriptor.class);

        when(filePath.toComputer()).thenReturn(computer);
        when(computer.getNode()).thenReturn(node);
        when(RemoteScanUtil.getRemoteVeracodePath(node)).thenReturn(filePath);
        when(run.getEnvironment(taskListener)).thenReturn(envVars);
        when(run.getDisplayName()).thenReturn("DisplayName");
        PowerMockito.when(run.getParent()).thenReturn(job);
        when(job.getFullDisplayName()).thenReturn("FullDisplayName");
        PowerMockito.when(envVars.expand(any())).thenReturn("Test");

        when(FileUtil.getStringFilePaths(any())).thenReturn(new String[0]);
        when(RemoteScanUtil.formatParameterValue(anyString())).thenCallRealMethod();
        when(RemoteScanUtil.getMaskPosition(any())).thenCallRealMethod();

        when(computer.isUnix()).thenReturn(false);
        when(RemoteScanUtil.addArgumentsToCommand(any(), anyVararg(), anyBoolean())).thenCallRealMethod();

        when(node.createLauncher(taskListener)).thenReturn(launcher);
        PowerMockito.whenNew(ProcStarter.class).withNoArguments().thenReturn(procStarter);
        when(procStarter.pwd(any(FilePath.class))).thenReturn(procStarter);
        when(procStarter.cmds(any(ArgumentListBuilder.class))).thenReturn(procStarter);
        when(procStarter.envs(anyMap())).thenReturn(procStarter);
        when(procStarter.stdout(any(TaskListener.class))).thenReturn(procStarter);
        when(procStarter.quiet(anyBoolean())).thenReturn(procStarter);
        when(launcher.launch(any(ProcStarter.class))).thenReturn(proc);
        when(proc.join()).thenReturn(0);

        when(WrapperUtil.getBuildInfo(anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn("buildInfoXML");
        when(XmlUtil.parseBuildId(anyString())).thenReturn("buildId");
        when(WrapperUtil.getDetailedReport(anyString(), anyString(), anyString(), any()))
                .thenReturn("detailedReportXML");
        when(XmlUtil.newScanHistory(anyString(), anyString(), any())).thenReturn(scanHistory);
        when(Credentials.create(anyString(), anyString())).thenReturn(credentials);
        when(credentials.getRegion()).thenReturn(region);
        when(region.getXmlApiHost()).thenReturn("xmlApiHost");
        PowerMockito.whenNew(VeracodeAction.class).withAnyArguments().thenReturn(veracodeAction);

        when(Jenkins.get()).thenReturn(jenkins);

        Method runScanFromRemoteMethod = VeracodePipelineRecorder.class.getDeclaredMethod("runScanFromRemote",
                Run.class, FilePath.class, TaskListener.class, PrintStream.class);
        runScanFromRemoteMethod.setAccessible(true);
        VeracodePipelineRecorder recorder = new VeracodePipelineRecorder("applicationName", "criticality", null,
                "scanName", true, 60, "0", true, null, false, false, true, true, "**/**.*", null, "**/**.jar", "**/**.war",
                null, null, false, true, null, null, null, null, "vid", "vkey");
        boolean success = (boolean) runScanFromRemoteMethod.invoke(recorder, run, filePath, taskListener, printStream);
        Assert.assertTrue(success);
    }
}