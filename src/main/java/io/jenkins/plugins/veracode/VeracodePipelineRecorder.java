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

import hudson.EnvVars;
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
import io.jenkins.plugins.veracode.args.UploadAndScanArgs;
import io.jenkins.plugins.veracode.data.ProxyBlock;
import io.jenkins.plugins.veracode.data.ScanHistory;
import io.jenkins.plugins.veracode.utils.FileUtil;
import io.jenkins.plugins.veracode.utils.FormValidationUtil;
import io.jenkins.plugins.veracode.utils.RemoteScanUtil;
import io.jenkins.plugins.veracode.utils.StringUtil;
import io.jenkins.plugins.veracode.utils.WrapperUtil;
import io.jenkins.plugins.veracode.utils.XmlUtil;
import jenkins.tasks.SimpleBuildStep;

public class VeracodePipelineRecorder extends Recorder implements SimpleBuildStep {

    @DataBoundSetter
    public final String applicationName;
    @DataBoundSetter
    public final String criticality;
    @DataBoundSetter
    public final String sandboxName;
    @DataBoundSetter
    public final String scanName;
    @DataBoundSetter
    public final boolean waitForScan;
    @DataBoundSetter
    public final Integer timeout;
    @DataBoundSetter
    public final boolean createProfile;
    @DataBoundSetter
    public final String teams;
    @DataBoundSetter
    public final boolean createSandbox;
    @DataBoundSetter
    public final boolean timeoutFailsJob;
    @DataBoundSetter
    public final boolean canFailJob;
    @DataBoundSetter
    public final boolean debug;
    @DataBoundSetter
    public final boolean copyRemoteFiles;
    // Patterns
    @DataBoundSetter
    public final String uploadIncludesPattern;
    @DataBoundSetter
    public final String uploadExcludesPattern;
    @DataBoundSetter
    public final String scanIncludesPattern;
    @DataBoundSetter
    public final String scanExcludesPattern;
    @DataBoundSetter
    public final String fileNamePattern;
    @DataBoundSetter
    public final String replacementPattern;
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
    public final String pPort;
    @DataBoundSetter
    public final String pUser;
    @DataBoundSetter
    public final String pPassword;

    private static String inclusive = "vosp-api-wrappers-java*.jar";
    private static String execJarFile = "VeracodeJavaAPI";
    private static String regex = "(vosp-api-wrappers).*?(.jar)";

    /**
     * {@link org.kohsuke.stapler.DataBoundConstructor DataBoundContructor}
     *
     * @param applicationName       String
     * @param criticality           String
     * @param sandboxName           String
     * @param scanName              String
     * @param waitForScan           boolean
     * @param timeout               int
     * @param createProfile         boolean
     * @param teams                 String
     * @param createSandbox         boolean
     * @param timeoutFailsJob       boolean
     * @param canFailJob            boolean
     * @param debug                 boolean
     * @param uploadIncludesPattern String
     * @param uploadExcludesPattern String
     * @param scanIncludesPattern   String
     * @param scanExcludesPattern   String
     * @param fileNamePattern       String
     * @param replacementPattern    String
     * @param vid                   String
     * @param vkey                  String
     * @param copyRemoteFiles       String
     * @param useProxy              boolean
     * @param pHost                 String
     * @param pPort                 String
     * @param pUser                 String
     * @param pPassword             String
     * @param vid                   String
     * @param vkey                  String
     */
    @org.kohsuke.stapler.DataBoundConstructor
    public VeracodePipelineRecorder(String applicationName, String criticality, String sandboxName,
            String scanName, boolean waitForScan, int timeout, boolean createProfile, String teams,
            boolean createSandbox, boolean timeoutFailsJob, boolean canFailJob, boolean debug,
            String uploadIncludesPattern, String uploadExcludesPattern, String scanIncludesPattern,
            String scanExcludesPattern, String fileNamePattern, String replacementPattern,
            boolean copyRemoteFiles, boolean useProxy, String pHost, String pPort, String pUser,
            String pPassword, String vid, String vkey) {

        this.applicationName = applicationName;
        this.criticality = criticality;
        this.sandboxName = sandboxName;
        this.scanName = scanName;
        this.timeoutFailsJob = timeoutFailsJob;
        this.waitForScan = waitForScan;
        this.timeout = waitForScan && timeout > 0 ? timeout : null;
        this.createProfile = createProfile;
        this.teams = teams;
        this.createSandbox = createSandbox;
        this.canFailJob = canFailJob;
        this.debug = debug;
        this.copyRemoteFiles = copyRemoteFiles;
        this.uploadIncludesPattern = uploadIncludesPattern;
        this.uploadExcludesPattern = uploadExcludesPattern;
        this.scanIncludesPattern = scanIncludesPattern;
        this.scanExcludesPattern = scanExcludesPattern;
        this.fileNamePattern = fileNamePattern;
        this.replacementPattern = replacementPattern;
        this.vid = vid;
        this.vkey = vkey;
        this.useProxy = useProxy;
        this.pHost = useProxy ? pHost : null;
        this.pPort = useProxy ? pPort : null;
        this.pUser = useProxy ? pUser : null;
        this.pPassword = useProxy ? pPassword : null;
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
        ps.println(PipelineDescriptorImpl.PostBuildActionDisplayText);
        ps.println("------------------------------------------------------------------------");

        EnvVars envVars = run.getEnvironment(listener);
        UploadAndScanArgs.setEnvVars(envVars, run.getDisplayName(),
                run.getParent().getFullDisplayName());
        String uploadincludePattern = envVars.expand(this.uploadIncludesPattern);
        String uploadexcludePattern = envVars.expand(this.uploadExcludesPattern);
        if (debug) {
            ps.println("\r\n[Debug mode is on]\r\n");

            ps.println(String.format("Can Fail Job: %s%n", this.canFailJob));
            if (this.timeout != null) {
                ps.println(String.format("Timeout: %s%n", this.timeout));
            }

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

        String[] uploadAndScanFilePaths = null;
        File localWorkspaceDir = null;

        try {
            if (isRemoteWorkspace) {
                if (debug) {
                    // ps.print(String.format("\r\n\r\nCopy remote files to [local]
                    // workspace?\r\n%s", this.copyRemoteFiles));
                    if (!copyRemoteFiles) {
                        ps.print(String.format("%n%nPerforming scan from [remote] workspace?%n%s",
                                !copyRemoteFiles));
                        ps.print("\n");
                    } else {
                        ps.print(String.format(
                                "%n%nPerforming scan [local] workspace after copying remote files?%n%s",
                                copyRemoteFiles));
                        ps.print("\n");
                    }
                }

                if (this.copyRemoteFiles) {
                    localWorkspaceDir = new File(run.getParent().getRootDir(),
                            "temp-veracode-local-workspace");

                    if (debug) {
                        ps.print(
                                "\r\n\r\nAttempting to copy remote files to [local] workspace:\r\n");
                        String p = localWorkspaceDir.getCanonicalPath().replace("\\", "/");
                        listener.hyperlink("file://" + p, p);
                    }

                    try {
                        if (localWorkspaceDir.exists()) {
                            FileUtil.deleteDirectory(localWorkspaceDir);
                        }
                        boolean dirCreated = localWorkspaceDir.mkdir();
                        if (!dirCreated) {
                            ps.print("\r\n\r\nFailed to create temporary local workspace.\r\n");
                            if (this.canFailJob || (this.timeout != null && this.timeout > 0)) {
                                run.setResult(Result.FAILURE);
                            }
                        }

                        FilePath localWorkspaceFilePath = new FilePath(localWorkspaceDir);
                        workspace.copyRecursiveTo(uploadincludePattern, uploadexcludePattern,
                                localWorkspaceFilePath);

                        // obtain the String file paths, using the includes/excludes patterns a 2nd
                        // time
                        uploadAndScanFilePaths = FileUtil.getStringFilePaths(localWorkspaceFilePath
                                .list(uploadincludePattern, uploadexcludePattern));
                    } catch (Exception e) {
                        String msg = e.getMessage();
                        ps.print("\r\n\r\nFailed to copy remote files to the [local] workspace:\r\n"
                                + e.getClass().getName() + (msg != null ? ": " + msg : "")
                                + "\r\n\r\n");
                        return;
                    }
                } else {

                    if (copyJarRemoteBuild(workspace, listener)) {
                        // remote scan if we can copy the veracode java wrapper
                        if (!runScanFromRemote(run, workspace, listener, ps)) {
                            if (this.canFailJob || (this.timeout != null && this.timeout > 0)) {
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
            } else {
                uploadAndScanFilePaths = FileUtil.getStringFilePaths(
                        workspace.list(uploadincludePattern, uploadexcludePattern));
            }

            if (debug) {
                ps.print("\r\n\r\nBuilding arguments. ");
            }

            //
            // Placeholders
            // We may want to implement these in the future, but I don't think it is
            // necessary for pipeline.
            // The only thing we're really missing is autoapplicationdescription. We may
            // want to allow the user to update the veracode description of the application.
            // Currently createAutoApplicationDescription = true will not resolve the host
            // name for Jenkins properly on jenkins 2.x. Not sure why.
            ///
            boolean autoApplicationName = false;
            boolean autoScanName = false;
            boolean createAutoApplicationDescription = false;
            String str_timeout = "";
            if (timeout != null) {
                str_timeout = Integer.toString(timeout);
            }

            UploadAndScanArgs uploadAndScanArguments = UploadAndScanArgs.newUploadAndScanArgs(false,
                    autoApplicationName, createAutoApplicationDescription, autoScanName,
                    createSandbox, createProfile, teams, useProxy, vid, vkey, run.getDisplayName(),
                    run.getParent().getFullDisplayName(), applicationName, sandboxName, scanName,
                    criticality, scanIncludesPattern, scanExcludesPattern, fileNamePattern,
                    replacementPattern, pHost, pPort, pUser, pPassword, workspace,
                    run.getEnvironment(listener), str_timeout, uploadAndScanFilePaths);

            if (debug) {
                ps.println(String.format("Calling wrapper with arguments:%n%s%n",
                        Arrays.toString(uploadAndScanArguments.getMaskedArguments())));
            }

            try {
                VeracodeParser parser = new VeracodeParser();
                parser.setOutputWriter(ps);
                parser.setErrorWriter(ps);
                parser.throwExceptions(true);
                parser.setScanCompleteTimeout(
                        this.timeout != null ? this.timeout.toString() : null);
                final int retCode = parser.parse(uploadAndScanArguments.getArguments());
                try {
                    // Starting from 17.9.4.6, the Java wrapper returns code (4) when a scan
                    // did not pass policy compliance. Therefore, we need to generate the scan
                    // result for both return code 0 and 4.
                    if (null != this.timeout) {
                        if (4 == retCode || 0 == retCode) {
                            getScanResults(run, listener, autoApplicationName);
                        } else {
                            run.addAction(new VeracodeAction());
                        }
                    }
                } catch (Exception e) {
                    ps.println();
                    ps.println(String.format(
                            "Ran into problem when generating scan results in Jenkins. Error: [%s, %s]",
                            e.getClass().getSimpleName(), e.getMessage()));
                    e.printStackTrace(ps);
                } finally { // Make sure setting the build status correctly according to the retCode
                    if (retCode != 0) {
                        if (this.canFailJob) {
                            ps.println();
                            ps.println("Error- Returned code from wrapper:" + retCode);
                        }

                        if (this.canFailJob || (this.timeout != null && this.timeout > 0)) {
                            run.setResult(Result.FAILURE);
                        }
                    }
                }
            } catch (Exception e) {
                if (this.canFailJob || (this.timeout != null && this.timeout > 0)) {
                    run.setResult(Result.FAILURE);
                }
            }

            ps.println();
            return;
        } finally {
            if (isRemoteWorkspace && this.copyRemoteFiles) {
                try {
                    if (localWorkspaceDir != null && localWorkspaceDir.exists()) {
                        FileUtil.deleteDirectory(localWorkspaceDir);
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    public PipelineDescriptorImpl getDescriptor() {
        return (PipelineDescriptorImpl) super.getDescriptor();
    }

    @Symbol("veracode")
    @hudson.Extension
    public static final class PipelineDescriptorImpl extends BuildStepDescriptor<Publisher> {
        public static final String PostBuildActionDisplayText = "Upload and Scan with Veracode Pipeline";

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
            local.copyRecursiveTo(inclusive, null, remote);

            // now make a copy of the jar as 'VeracodeJavaAPI.jar' as the name of the
            // jarfile in the plugin
            // will change depending on the wrapper version it has been built with

            FilePath[] files = remote.list(inclusive);
            String jarName = files[0].getRemote();
            FilePath oldJar = new FilePath(node.getChannel(), jarName);
            String newJarName = jarName.replaceAll(regex, execJarFile + "$2");
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

        boolean copyRemoteFiles = this.copyRemoteFiles;
        boolean isRemoteWorkspace = workspace.isRemote();

        // only copy if remote workspace and copyRemoteFiles set true in groovy script
        if (isRemoteWorkspace && !copyRemoteFiles) {
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

                FilePath[] files = remoteVeracodeFilePath.list(inclusive);

                // copy the jar if it does not exist
                if (files.length == 0) {
                    bRet = copyJarFiles(node, localWorkspaceFilePath, remoteVeracodeFilePath, ps);
                } else { // if file exits
                    FilePath[] newfiles = localWorkspaceFilePath.list(inclusive);
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
                    } else { // just make sure we have our jarfile (defensive coding)
                        String jarName = files[0].getRemote();
                        String newJarName = jarName.replaceAll(regex, execJarFile + "$2");
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
            PrintStream ps) throws IOException, InterruptedException {
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

        EnvVars envVars = run.getEnvironment(listener);
        UploadAndScanArgs.setEnvVars(envVars, run.getDisplayName(),
                run.getParent().getFullDisplayName());
        String uploadincludePattern = envVars.expand(this.uploadIncludesPattern);
        String uploadexcludePattern = envVars.expand(this.uploadExcludesPattern);

        // obtain the String file paths, using the includes/excludes patterns a 2nd time
        try {
            String[] uploadAndScanFilePaths = FileUtil
                    .getStringFilePaths(workspace.list(uploadincludePattern, uploadexcludePattern));

            String str_timeout = "";
            if (timeout != null) {
                str_timeout = Integer.toString(timeout);
            }

            UploadAndScanArgs uploadAndScanArguments = UploadAndScanArgs.newUploadAndScanArgs(true,
                    autoApplicationName, createAutoApplicationDescription, autoScanName,
                    createSandbox, createProfile, teams, useProxy, vid, vkey, run.getDisplayName(),
                    run.getParent().getFullDisplayName(), applicationName, sandboxName, scanName,
                    criticality, scanIncludesPattern, scanExcludesPattern, fileNamePattern,
                    replacementPattern, pHost, pPort, pUser, pPassword, workspace,
                    run.getEnvironment(listener), str_timeout, uploadAndScanFilePaths);

            String jarPath = jarFilePath + sep + execJarFile + ".jar";
            String cmd = "java -jar " + jarPath;
            String[] cmds = uploadAndScanArguments.getArguments();

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
                    if (iPosProxyPassword == i) {
                        masks[i] = true;
                    }
                } else {
                    masks[i] = false;
                }
            }

            procStart = procStart.cmds(command).masks(masks).stdout(listener).quiet(true);

            if (this.debug) {
                procStart.quiet(false);
                ps.print("\nInvoking the following command in remote workspace:\n");
            }
            Proc proc = launcher.launch(procStart);

            int retcode = proc.join();

            if (retcode != 0 && this.canFailJob) {
                ps.print("\r\n\r\nError- Returned code from wrapper:" + retcode + "\r\n\n");
            } else {
                bRet = true;
            }

            try {
                // Starting from 17.9.4.6, the Java wrapper returns code (4) when a scan
                // did not pass policy compliance. Therefore, we need to generate the scan
                // result for both return code 0 and 4.
                if (null != this.timeout) {
                    if (4 == retcode || 0 == retcode) {
                        getScanResults(run, listener, autoApplicationName);
                    } else {
                        run.addAction(new VeracodeAction());
                    }
                }
            } catch (Exception e) {
                ps.println();
                ps.println(String.format(
                        "Ran into problem when generating scan results in Jenkins. Error: [%s, %s]",
                        e.getClass().getSimpleName(), e.getMessage()));
                e.printStackTrace(ps);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (this.canFailJob) {
                ps.print("\r\n\r\n" + ex.getMessage());
            }
        }

        return bRet;
    }

    /**
     * Construct the scan result from Detailed Report
     *
     * @param run                 - the current Jenkins build
     * @param listener            - listener of this task
     * @param autoApplicationName - automatically generate application name of not
     * @throws Exception when error happened during the operation
     */
    private void getScanResults(Run<?, ?> run, TaskListener listener, boolean autoApplicationName)
            throws Exception {
        ProxyBlock proxy = null;
        if (useProxy) {
            proxy = new ProxyBlock(pHost, pPort, pUser, pPassword);
        }

        EnvVars envVars = run.getEnvironment(listener);
        UploadAndScanArgs.setEnvVars(envVars, run.getDisplayName(),
                run.getParent().getFullDisplayName());
        String appName = applicationName;
        // application profile name
        if (!StringUtil.isNullOrEmpty(appName)) {
            appName = envVars.expand(appName);
        } else if (autoApplicationName) {
            appName = envVars.get(UploadAndScanArgs.CUSTOM_PROJECT_NAME_VAR);
        }

        String resolvedSandboxName = !StringUtil.isNullOrEmpty(sandboxName)
                ? envVars.expand(sandboxName)
                : sandboxName;
        try {
            String buildInfoXML = WrapperUtil.getBuildInfo(appName, resolvedSandboxName, vid, vkey,
                    proxy);
            String buildId = XmlUtil.parseBuildId(buildInfoXML);
            String detailedReportXML = WrapperUtil.getDetailedReport(buildId, vid, vkey, proxy);
            ScanHistory scanHistory = XmlUtil.newScanHistory(buildInfoXML, detailedReportXML, run);
            run.addAction(new VeracodeAction(scanHistory));
        } catch (Exception e) {
            run.addAction(new VeracodeAction());
            throw e;
        }
    }

    private final int getTimeout() {
        return Integer.parseInt(FormValidationUtil.formatTimeout(String.valueOf(timeout)));
    }
}