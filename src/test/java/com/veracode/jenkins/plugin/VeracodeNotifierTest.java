package com.veracode.jenkins.plugin;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.PrintStream;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.veracode.http.Credentials;
import com.veracode.http.Region;
import com.veracode.jenkins.plugin.args.UploadAndScanArgs;
import com.veracode.jenkins.plugin.data.ScanHistory;
import com.veracode.jenkins.plugin.utils.FileUtil;
import com.veracode.jenkins.plugin.utils.RemoteScanUtil;
import com.veracode.jenkins.plugin.utils.WrapperUtil;
import com.veracode.jenkins.plugin.utils.XmlUtil;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AbstractBuild.class, Credentials.class, FilePath.class, FileUtil.class, Node.class, ProcStarter.class,
        RemoteScanUtil.class, UploadAndScanArgs.class, WrapperUtil.class, XmlUtil.class })
public class VeracodeNotifierTest {

    @Test
    public void testRunScanFromRemote() throws Exception {

        AbstractBuild abstractBuild = PowerMockito.mock(AbstractBuild.class);
        BuildListener buildListener = PowerMockito.mock(BuildListener.class);
        PrintStream printStream = PowerMockito.mock(PrintStream.class);
        EnvVars envVars = PowerMockito.mock(EnvVars.class);
        AbstractProject abstractProject = PowerMockito.mock(AbstractProject.class);

        FilePath filePath = PowerMockito.mock(FilePath.class);
        VirtualChannel virtualChannel = PowerMockito.mock(VirtualChannel.class);
        Node node = PowerMockito.mock(Node.class);
        UploadAndScanArgs uploadAndScanArgs = PowerMockito.mock(UploadAndScanArgs.class);
        Launcher launcher = PowerMockito.mock(Launcher.class);
        ProcStarter procStarter = PowerMockito.mock(ProcStarter.class);
        Proc proc = PowerMockito.mock(Proc.class);
        ScanHistory scanHistory = PowerMockito.mock(ScanHistory.class);
        Credentials credentials = PowerMockito.mock(Credentials.class);
        Region region = PowerMockito.mock(Region.class);
        VeracodeAction veracodeAction = PowerMockito.mock(VeracodeAction.class);

        PowerMockito.mockStatic(FileUtil.class);
        PowerMockito.mockStatic(RemoteScanUtil.class);
        PowerMockito.mockStatic(UploadAndScanArgs.class);
        PowerMockito.mockStatic(WrapperUtil.class);
        PowerMockito.mockStatic(XmlUtil.class);
        PowerMockito.mockStatic(Credentials.class);

        when(abstractBuild.getEnvironment(buildListener)).thenReturn(envVars);
        when(abstractBuild.getDisplayName()).thenReturn("DisplayName");
        PowerMockito.when(abstractBuild.getProject()).thenReturn(abstractProject);
        when(abstractProject.getDisplayName()).thenReturn("DisplayName");
        PowerMockito.when(envVars.expand(any())).thenReturn("Test");

        when(abstractBuild.getBuiltOn()).thenReturn(node);
        when(RemoteScanUtil.getRemoteVeracodePath(abstractBuild)).thenReturn(filePath);
        PowerMockito.when(abstractBuild.getWorkspace()).thenReturn(filePath);
        when(filePath.getRemote()).thenReturn("Workspace");
        when(node.getChannel()).thenReturn(virtualChannel);
        PowerMockito.whenNew(FilePath.class).withAnyArguments().thenReturn(filePath);

        when(FileUtil.getStringFilePaths(any())).thenReturn(new String[0]);
        when(UploadAndScanArgs.newUploadAndScanArgs(any(VeracodeNotifier.class), any(AbstractBuild.class),
                any(EnvVars.class), any(String[].class), anyBoolean())).thenReturn(uploadAndScanArgs);
        when(uploadAndScanArgs.getArguments()).thenReturn(new String[0]);
        when(RemoteScanUtil.formatParameterValue(anyString())).thenCallRealMethod();

        when(RemoteScanUtil.getMaskPosition(any())).thenCallRealMethod();

        when(node.createLauncher(buildListener)).thenReturn(launcher);
        PowerMockito.whenNew(ProcStarter.class).withNoArguments().thenReturn(procStarter);
        when(procStarter.pwd(any(FilePath.class))).thenReturn(procStarter);
        when(procStarter.cmds(any(ArgumentListBuilder.class))).thenReturn(procStarter);
        when(procStarter.masks(Matchers.anyVararg())).thenReturn(procStarter);
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

        Method runScanFromRemoteMethod = VeracodeNotifier.class.getDeclaredMethod("runScanFromRemote",
                AbstractBuild.class, BuildListener.class, PrintStream.class, boolean.class);
        runScanFromRemoteMethod.setAccessible(true);
        VeracodeNotifier notifier = new VeracodeNotifier("appname", true, null, "criticality", null, false, "version",
                null, null, "**/**.*", null, "**/**.jar", "**/**.war", true, null, null);
        boolean success = (boolean) runScanFromRemoteMethod.invoke(notifier, abstractBuild, buildListener, printStream,
                true);
        Assert.assertTrue(success);
    }
}
