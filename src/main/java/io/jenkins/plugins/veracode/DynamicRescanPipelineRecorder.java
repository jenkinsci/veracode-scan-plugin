/*******************************************************************************
 * Copyright (c) 2017 Veracode, Inc. All rights observed.
 *
 * Available for use by Veracode customers as described in the accompanying license agreement.
 *
 * Send bug reports or enhancement requests to support@veracode.com.
 *
 * See the license agreement for conditions on submitted materials.
 ******************************************************************************/
package io.jenkins.plugins.veracode;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

import com.veracode.apiwrapper.cli.VeracodeCommand.VeracodeParser;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ArgumentListBuilder;
import io.jenkins.plugins.veracode.args.DynamicRescanArgs;
import io.jenkins.plugins.veracode.common.Constant;
import io.jenkins.plugins.veracode.utils.FileUtil;
import io.jenkins.plugins.veracode.utils.RemoteScanUtil;
import io.jenkins.plugins.veracode.utils.StringUtil;
import jenkins.tasks.SimpleBuildStep;

public class DynamicRescanPipelineRecorder extends Recorder implements SimpleBuildStep {

    @DataBoundSetter
    public final String applicationName;
    @DataBoundSetter
    public final boolean dvrEnabled;
    @DataBoundSetter
    public final boolean canFailJob;
    @DataBoundSetter
    public final boolean debug;

    // Credentials
    @DataBoundSetter
    public final String vid;
    @DataBoundSetter
    public final String vkey;
    // Proxy
    @DataBoundSetter
    public final boolean useProxy;
    @DataBoundSetter
    public final String pHost;
    @DataBoundSetter
    public final int pPort;
    @DataBoundSetter
    public final String pUser;
    @DataBoundSetter
    public final String pPassword;

    /**
     * {@link org.kohsuke.stapler.DataBoundConstructor DataBoundContructor}
     *
     * @param applicationName String
     * @param dvrEnabled      boolean
     * @param canFailJob      boolean
     * @param debug           boolean
     * @param vid             String
     * @param vkey            String
     * @param useProxy        boolean
     * @param pHost           String
     * @param pPort           int
     * @param pUser           String
     * @param pPassword       String
     * @param vid             String
     * @param vkey            String
     */
    @org.kohsuke.stapler.DataBoundConstructor
    public DynamicRescanPipelineRecorder(String applicationName, boolean dvrEnabled,
            boolean canFailJob, boolean debug, boolean useProxy, String pHost, int pPort,
            String pUser, String pPassword, String vid, String vkey) {

        this.canFailJob = canFailJob;
        this.debug = debug;
        this.applicationName = applicationName;
        this.dvrEnabled = dvrEnabled;
        this.vid = vid;
        this.vkey = vkey;
        this.useProxy = useProxy;
        this.pHost = pHost;
        this.pPort = pPort;
        this.pUser = pUser;
        this.pPassword = pPassword;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {

        PrintStream ps = listener.getLogger();

        ps.println("------------------------------------------------------------------------");
        ps.println(PipelineDynamicRescanDescriptorImpl.PostBuildActionDisplayText);
        ps.println("------------------------------------------------------------------------");

        if (debug) {
            ps.println("\r\n[Debug mode is on]\r\n");

            ps.println(String.format("Can Fail Job: %s%n", this.canFailJob));

            try {
                Method method = com.veracode.apiwrapper.cli.VeracodeCommand.class
                        .getDeclaredMethod("getVersionString");
                method.setAccessible(true);
                String version = (String) method.invoke(null);
                if (!StringUtil.isNullOrEmpty(version)) {
                    ps.println(String.format("Version information:%n%s", version));
                }
            } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                ps.println("Could not retrieve API wrapper's version information.");
            }
            try {
                String location = this.getClass().getProtectionDomain().getCodeSource()
                        .getLocation().toString();
                if (!StringUtil.isNullOrEmpty(location)) {
                    ps.println("\r\nHPI location: ");
                    location = location.replace("file:/", "");
                    listener.hyperlink("file://" + location, location);
                }
            } catch (Exception e) {
                ps.println("\r\nCould not retrieve hpi file's directory.");
            }
        }

        boolean isRemoteWorkspace = workspace.isRemote();

        if (debug) {
            ps.println(String.format("%n%nProcessing files in [%s] workspace: ",
                    isRemoteWorkspace ? "remote" : "local"));
            String workspaceDir = workspace.getRemote();
            workspaceDir = workspaceDir.replace("\\", "/");
            listener.hyperlink("file://" + workspaceDir, workspaceDir);
        }

        File localWorkspaceDir = null;

        try {
            if (isRemoteWorkspace) {

                if (copyJarRemoteBuild(workspace, listener)) {
                    // remote scan if we can copy the veracode java wrapper
                    if (!runScanFromRemote(run, workspace, listener, ps)) {

                        if (this.canFailJob) {
                            run.setResult(Result.FAILURE);
                        }

                    }
                } else // set build failure
                {

                    if (this.canFailJob) {
                        ps.println("Could not copy Veracode libs");
                        run.setResult(Result.FAILURE);
                    }

                }

                return;
            }

            if (debug) {
                ps.print("\r\n\r\nBuilding arguments. ");
            }

            boolean autoApplicationName = false;
            boolean autoScanName = false;
            boolean createAutoApplicationDescription = false;

            DynamicRescanArgs pipelineScanArguments = DynamicRescanArgs.pipelineRescanArgs(
                    autoApplicationName, createAutoApplicationDescription, autoScanName, useProxy,
                    vid, vkey, run.getDisplayName(), run.getParent().getFullDisplayName(),
                    applicationName, dvrEnabled, pHost, Integer.toString(pPort), pUser, pPassword,
                    workspace, run.getEnvironment(listener));

            if (debug) {
                ps.println(String.format("Calling wrapper with arguments:%n%s%n",
                        Arrays.toString(pipelineScanArguments.getMaskedArguments())));
            }

            try {
                VeracodeParser parser = new VeracodeParser();
                parser.setOutputWriter(ps);
                parser.setErrorWriter(ps);
                parser.throwExceptions(true);
                int retCode = parser.parse(pipelineScanArguments.getArguments());
                if (retCode != 0) {
                    if (this.canFailJob) {
                        ps.print("\r\n\r\nError- Returned code from wrapper:" + retCode + "\r\n\n");
                    }
                }
            } catch (Exception e) {
                ps.print("\r\n\r\n" + e.getMessage());
            }
            return;
        } finally {
            if (isRemoteWorkspace) {
                try {
                    if (localWorkspaceDir != null && localWorkspaceDir.exists()) {
                        FileUtil.deleteDirectory(localWorkspaceDir);
                    }
                } catch (Exception e) {
                    ps.print("\r\n\r\n" + e.getMessage());
                }
            }
        }
    }

    @Override
    public PipelineDynamicRescanDescriptorImpl getDescriptor() {
        return (PipelineDynamicRescanDescriptorImpl) super.getDescriptor();
    }

    @Symbol("veracodeDynamicRescan")
    @hudson.Extension
    public static final class PipelineDynamicRescanDescriptorImpl
            extends BuildStepDescriptor<Publisher> {
        public static final String PostBuildActionDisplayText = "Dynamic Rescan with Veracode Pipeline";

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return false;
        }

        @Override
        public String getDisplayName() {
            return PostBuildActionDisplayText;
        }

    }

    // copy the wrapper to the remote location
    private boolean copyJarFiles(Node node, FilePath local, FilePath remote, PrintStream ps)
            throws Exception {
        boolean bRet = false;
        try {
            local.copyRecursiveTo(Constant.inclusive, null, remote);

            // now make a copy of the jar as 'VeracodeJavaAPI.jar' as the name of the
            // jarfile in the plugin
            // will change depending on the wrapper version it has been built with

            FilePath[] files = remote.list(Constant.inclusive);
            String jarName = files[0].getRemote();
            FilePath oldJar = new FilePath(node.getChannel(), jarName);
            String newJarName = jarName.replaceAll(Constant.regex, Constant.execJarFile + "$2");
            FilePath newjarFilePath = new FilePath(node.getChannel(), newJarName);
            oldJar.copyToWithPermission(newjarFilePath);
            bRet = true;
        } catch (RuntimeException ex) {
            if (this.canFailJob) {
                ps.print("Failed to copy the veracode java-wrapper libaries\n");
            }
        }

        return bRet;
    }

    private boolean copyJarRemoteBuild(FilePath workspace, TaskListener listener) {
        boolean bRet = false;
        PrintStream ps = listener.getLogger();

        boolean isRemoteWorkspace = workspace.isRemote();

        // only copy if remote workspace and copyRemoteFiles set true in groovy script
        if (isRemoteWorkspace) {
            Computer comp = workspace.toComputer();
            if (comp == null) {
                throw new RuntimeException("Cannot locate the remote workspace.");
            }
            Node node = comp.getNode();
            if (node == null) {
                throw new RuntimeException("Cannot locate the remote node.");
            }
            try {
                FilePath localWorkspaceFilePath = FileUtil.getLocalWorkspaceFilepath();
                FilePath remoteVeracodeFilePath = RemoteScanUtil.getRemoteVeracodePath(node);
                if (remoteVeracodeFilePath == null) {
                    throw new RuntimeException("Cannot retrieve the remote file path.");
                }

                // create the directory (where we want to copy the javawrapper jar) if it does
                // not exist
                if (!remoteVeracodeFilePath.exists()) {
                    if (debug)
                        ps.println("Making remote dir");

                    remoteVeracodeFilePath.mkdirs();
                }

                FilePath[] files = remoteVeracodeFilePath.list(Constant.inclusive);

                // copy the jar if it does not exist
                if (files.length == 0) {
                    bRet = copyJarFiles(node, localWorkspaceFilePath, remoteVeracodeFilePath, ps);
                } else // if file exits
                {
                    FilePath[] newfiles = localWorkspaceFilePath.list(Constant.inclusive);
                    String newjarName = newfiles[0].getRemote();
                    int newVersion = RemoteScanUtil.getJarVersion(newjarName);
                    String oldjarName = files[0].getRemote();
                    int oldVersion = RemoteScanUtil.getJarVersion(oldjarName);

                    // also copy the jar if there is a newer version in the plugin directory and
                    // delete the old one
                    if (newVersion > oldVersion) {
                        if (debug) {
                            ps.println(
                                    "Newer veracode library version, copying it to remote machine");
                        }

                        remoteVeracodeFilePath.deleteContents();
                        bRet = copyJarFiles(node, localWorkspaceFilePath, remoteVeracodeFilePath,
                                ps);
                    } else // just make sure we have our jarfile (defensive coding)
                    {
                        String jarName = files[0].getRemote();
                        String newJarName = jarName.replaceAll(Constant.regex,
                                Constant.execJarFile + "$2");
                        FilePath newjarFilePath = new FilePath(node.getChannel(), newJarName);

                        if (newjarFilePath.exists())
                            bRet = true;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                if (this.canFailJob) {
                    ps.println(ex.getMessage());
                }
            }
        } else {
            bRet = true;
        }

        return bRet;
    }

    // invoking the CLI from remote node
    private boolean runScanFromRemote(Run<?, ?> run, FilePath workspace, TaskListener listener,
            PrintStream ps) {
        boolean bRet = false;
        boolean autoApplicationName = false;
        boolean autoScanName = true;
        boolean createAutoApplicationDescription = false;

        Computer comp = workspace.toComputer();
        if (comp == null) {
            throw new RuntimeException("Cannot locate the remote workspace.");
        }
        Node node = comp.getNode();
        if (node == null) {
            throw new RuntimeException("Cannot locate the remote node.");
        }
        FilePath remoteVeracodeFilePath = RemoteScanUtil.getRemoteVeracodePath(node);
        if (remoteVeracodeFilePath == null) {
            throw new RuntimeException("Cannot retrieve the remote file path.");
        }
        String jarFilePath = remoteVeracodeFilePath.getRemote();
        String remoteworkspace = workspace.getRemote();

        String sep = RemoteScanUtil.getPathSeparator(remoteworkspace);

        try {

            DynamicRescanArgs pipelineScanArguments = DynamicRescanArgs.pipelineRescanArgs(
                    autoApplicationName, createAutoApplicationDescription, autoScanName, useProxy,
                    vid, vkey, run.getDisplayName(), run.getParent().getFullDisplayName(),
                    applicationName, dvrEnabled, pHost, Integer.toString(pPort), pUser, pPassword,
                    workspace, run.getEnvironment(listener));

            String jarPath = jarFilePath + sep + Constant.execJarFile + ".jar";
            String cmd = "java -jar " + jarPath;
            String[] cmds = pipelineScanArguments.getArguments();

            StringBuilder result = new StringBuilder();
            result.append(cmd);
            for (String _cmd : cmds) {
                _cmd = RemoteScanUtil.formatParameterValue(_cmd);
                result.append(" " + _cmd);
            }

            ArgumentListBuilder command = new ArgumentListBuilder();
            command.addTokenized(result.toString());

            List<String> remoteCmd = command.toList();
            int iSize = remoteCmd.size();
            Integer[] iPos = RemoteScanUtil.getMaskPosition(remoteCmd);
            int iPosPassword = iPos[0];
            int iPosKey = iPos[1];
            int iPosProxyPassword = iPos[2];

            Launcher launcher = node.createLauncher(listener);
            ProcStarter procStart = launcher.new ProcStarter();

            // masking the password related information
            boolean[] masks = new boolean[iSize];
            for (int i = 0; i < iSize; i++) {
                if (iPosPassword != -1) {
                    if (iPosPassword == i)
                        masks[i] = true;
                } else if (iPosKey != -1) {
                    if (iPosKey == i)
                        masks[i] = true;
                } else if (iPosProxyPassword != -1) {
                    if (iPosProxyPassword == i)
                        masks[i] = true;
                } else
                    masks[i] = false;
            }

            procStart = procStart.cmds(command).masks(masks).stdout(listener).quiet(true);

            if (this.debug) {
                procStart.quiet(false);
                ps.print("\nInvoking the following command in remote workspace:\n");
            }
            Proc proc = launcher.launch(procStart);

            int retcode = proc.join();

            if (retcode != 0) {
                if (this.canFailJob) {
                    ps.print("\r\n\r\nError- Returned code from wrapper:" + retcode + "\r\n\n");
                }
            } else if (retcode == 0)
                bRet = true;

        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            if (this.canFailJob) {
                ps.print("\r\n\r\n" + ex.getMessage());
            }
        }

        return bRet;
    }
}