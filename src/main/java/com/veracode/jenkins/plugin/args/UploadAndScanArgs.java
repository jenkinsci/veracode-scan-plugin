package com.veracode.jenkins.plugin.args;

import com.veracode.jenkins.plugin.VeracodeNotifier;
import com.veracode.jenkins.plugin.VeracodeNotifier.VeracodeDescriptor;
import com.veracode.jenkins.plugin.common.Constant;
import com.veracode.jenkins.plugin.utils.FileUtil;
import com.veracode.jenkins.plugin.utils.StringUtil;
import com.veracode.jenkins.plugin.utils.UserAgentUtil;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;

/**
 * The UploadAndScanArgs class builds the command line argument passed to the
 * Veracode API wrapper that causes it to upload binaries, start the pre-scan,
 * and if the pre-scan is successful, start the scan.
 *
 */
public final class UploadAndScanArgs extends AbstractArgs {

    private static final String APPNAME = SWITCH + "appname";
    private static final String DESCRIPTION = SWITCH + "description";
    private static final String CREATEPROFILE = SWITCH + "createprofile";
    private static final String TEAMS = SWITCH + "teams";
    private static final String CRITICALITY = SWITCH + "criticality";
    private static final String SANDBOXNAME = SWITCH + "sandboxname";
    private static final String CREATESANDBOX = SWITCH + "createsandbox";
    private static final String VERSION = SWITCH + "version";
    private static final String INCLUDE = SWITCH + "include";
    private static final String EXCLUDE = SWITCH + "exclude";
    private static final String AUTOSCAN = SWITCH + "autoscan";
    private static final String PATTERN = SWITCH + "pattern";
    private static final String REPLACEMENT = SWITCH + "replacement";
    private static final String FILEPATH = SWITCH + "filepath";
    private static final String TIMEOUT = SWITCH + "scantimeout";
    private static final String DELETEINCOMPLETESCAN = SWITCH + "deleteincompletescan";
    private static final String MAXRETRYCOUNT = SWITCH + "maxretrycount";
    private static final String DEBUG = SWITCH + "debug";

    private static final String CUSTOM_TIMESTAMP_VAR = "timestamp";
    private static final String CUSTOM_BUILD_NUMBER_VAR = "buildnumber";
    public static final String CUSTOM_PROJECT_NAME_VAR = "projectname";

    /**
     * Constructor for UploadAndScanArgs.
     */
    private UploadAndScanArgs() {
        addAction("UploadAndScan");
    }

    /**
     * Adds the specified arguments and switches for those arguments to the command
     * line arguments list.
     *
     * @param bRemoteScan   a boolean.
     * @param appname       a {@link java.lang.String} object.
     * @param description   a {@link java.lang.String} object.
     * @param createprofile a boolean.
     * @param teams         a {@link java.lang.String} object.
     * @param criticality   a {@link java.lang.String} object.
     * @param sandboxname   a {@link java.lang.String} object.
     * @param createsandbox a boolean.
     * @param version       a {@link java.lang.String} object.
     * @param include       a {@link java.lang.String} object.
     * @param exclude       a {@link java.lang.String} object.
     * @param pattern       a {@link java.lang.String} object.
     * @param replacement   a {@link java.lang.String} object.
     * @param timeOut       a {@link java.lang.String} object.
     * @param deleteIncompleteScan  a {@link java.lang.String} object.
     * @param debug         a boolean.
     * @param filepath      a {@link java.lang.String} object.
     */
    private void addStdArguments(boolean bRemoteScan, String appname, String description,
            boolean createprofile, String teams, String criticality, String sandboxname,
            boolean createsandbox, String version, String include, String exclude, String pattern,
            String replacement, String timeOut, String deleteIncompleteScan, boolean debug, String... filepath) {
        // only add scantimeout if scan takes place from remote
        if (bRemoteScan) {
            if (!StringUtil.isNullOrEmpty(timeOut)) {
                list.add(TIMEOUT);
                list.add(timeOut);
            }
        }

        addStdArguments(appname, description, createprofile, teams, criticality, sandboxname,
                createsandbox, version, include, exclude, pattern, replacement, deleteIncompleteScan, debug, filepath);
    }

    /**
     * Adds the specified arguments and switches for those arguments to the command
     * line arguments list.
     *
     * @param appname       a {@link java.lang.String} object.
     * @param description   a {@link java.lang.String} object.
     * @param createprofile a boolean.
     * @param teams         a {@link java.lang.String} object.
     * @param criticality   a {@link java.lang.String} object.
     * @param sandboxname   a {@link java.lang.String} object.
     * @param createsandbox a boolean.
     * @param version       a {@link java.lang.String} object.
     * @param include       a {@link java.lang.String} object.
     * @param exclude       a {@link java.lang.String} object.
     * @param pattern       a {@link java.lang.String} object.
     * @param replacement   a {@link java.lang.String} object.
     * @param deleteIncompleteScan  a {@link java.lang.String} object.
     * @param debug         a boolean.
     * @param filepath      a {@link java.lang.String} object.
     */
    private void addStdArguments(String appname, String description, boolean createprofile,
            String teams, String criticality, String sandboxname, boolean createsandbox,
            String version, String include, String exclude, String pattern, String replacement,
            String deleteIncompleteScan, boolean debug, String... filepath) {
        if (!StringUtil.isNullOrEmpty(appname)) {
            list.add(APPNAME);
            list.add(appname);

            list.add(CREATEPROFILE);
            list.add(String.valueOf(createprofile));
        }

        if (!StringUtil.isNullOrEmpty(description)) {
            list.add(DESCRIPTION);
            list.add(description);
        }

        if (!StringUtil.isNullOrEmpty(teams)) {
            list.add(TEAMS);
            list.add(teams);
        }

        if (!StringUtil.isNullOrEmpty(criticality)) {
            list.add(CRITICALITY);
            list.add(criticality);
        }

        if (!StringUtil.isNullOrEmpty(sandboxname)) {
            list.add(SANDBOXNAME);
            list.add(sandboxname);

            // relevant only if sandbox name was provided
            list.add(CREATESANDBOX);
            list.add(String.valueOf(createsandbox));
        }

        if (!StringUtil.isNullOrEmpty(version)) {
            list.add(VERSION);
            list.add(version);
        }

        boolean canAutoScan = true;

        if (!StringUtil.isNullOrEmpty(include)) {
            list.add(INCLUDE);
            list.add(include);
            canAutoScan = false;
        }
        if (!StringUtil.isNullOrEmpty(exclude)) {
            list.add(EXCLUDE);
            list.add(exclude);
            canAutoScan = false;
        }

        if (canAutoScan) {
            list.add(AUTOSCAN);
            list.add(Boolean.toString(true));
        }

        if (!StringUtil.isNullOrEmpty(pattern) && !StringUtil.isNullOrEmpty(replacement)) {
            list.add(PATTERN);
            list.add(pattern);

            list.add(REPLACEMENT);
            list.add(replacement);
        }

        if (!StringUtil.isNullOrEmpty(deleteIncompleteScan)) {
            list.add(DELETEINCOMPLETESCAN);
            list.add(deleteIncompleteScan);
        }

        list.add(MAXRETRYCOUNT);
        list.add(String.valueOf(Constant.UPLOADANDSCAN_MAX_RETRY_COUNT));

        if (debug) {
            list.add(DEBUG);
        }

        if (filepath != null) {
            for (String s : filepath) {
                if (!StringUtil.isNullOrEmpty(s)) {
                    list.add(FILEPATH);
                    list.add(s);
                }
            }
        }
    }

    /**
     * Adds user agent details got through API.
     *
     * @param userAgent a {@link java.lang.String} object.
     */
    protected void addUserAgent(String userAgent) {
        if (!StringUtil.isNullOrEmpty(userAgent)) {
            list.add(USERAGENT);
            list.add(userAgent);
        }
    }

    /**
     * Builds the argument passed to the description switch. It includes the
     * Jenkins-URL, the host-name, and the workspace-path.
     * <p>
     * This argument is used when the plugin is configured to include this
     * information when it creates application profiles.
     *
     * @param workspaceFilePath a {@link hudson.FilePath} object.
     * @return a {@link java.lang.String} object.
     */
    private static String createDescriptionArg(FilePath workspaceFilePath) {
        String description_3fs = "Jenkins-URL: %s Host-Name: %s Workspace-Path: %s (Auto-generated by Veracode Jenkins Plugin)";

        String url = null;
        String hostName = null;
        String path = null;

        try {
            String rootUrl = Hudson.get().getRootUrl();
            if (!StringUtil.isNullOrEmpty(rootUrl)) {
                url = rootUrl;
            } else {
                url = "Not set in global config.";
            }
        } catch (Exception e) {
            url = "Not found.";
        }

        try {
            hostName = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostName = "Not found.";
        }

        try {
            path = FileUtil.getStringFilePath(workspaceFilePath);
        } catch (Exception e) {
            path = "Not found.";
        }

        return String.format(description_3fs, url, hostName, path);
    }

    /**
     * Returns an UploadAndScanArgs object initialized with the specified arguments.
     * Used by the notifier
     *
     * @param notifier    a {@link com.veracode.jenkins.plugin.VeracodeNotifier}
     *                    object.
     * @param build       a {@link hudson.model.AbstractBuild} object.
     * @param envVars     a {@link hudson.EnvVars} object.
     * @param filePaths   an array of {@link java.lang.String} objects.
     * @param bRemoteScan a boolean.
     * @return a {@link com.veracode.jenkins.plugin.args.UploadAndScanArgs} object.
     */
    public static UploadAndScanArgs newUploadAndScanArgs(VeracodeNotifier notifier,
            AbstractBuild<?, ?> build, hudson.EnvVars envVars, String[] filePaths,
            boolean bRemoteScan) {
        VeracodeDescriptor descriptor = notifier.getDescriptor();

        // credentials if(getCredentials = null) means that the credentials block is
        // null or alternatively,
        // it means we have selected the option (use global credentials) therefore seek
        // global credentials.
        String vId = (notifier.getCredentials() == null) ? descriptor.getGvid() : notifier.getVid();
        String vKey = (notifier.getCredentials() == null) ? descriptor.getGvkey()
                : notifier.getVkey();

        String phost = null;
        String pport = null;
        String puser = null;
        String ppsword = null;

        // proxy settings
        if (descriptor.getProxy()) {
            phost = descriptor.getPhost();
            pport = descriptor.getPport();
            puser = descriptor.getPuser();
            ppsword = descriptor.getPpassword();
        }

        return newUploadAndScanArgs(bRemoteScan, descriptor.getAutoappname(),
                descriptor.getAutodescription(), descriptor.getAutoversion(),
                notifier.getCreatesandbox(), notifier.getCreateprofile(), notifier.getTeams(),
                descriptor.getProxy(), vId, vKey, build.getDisplayName(),
                build.getProject().getDisplayName(), notifier.getAppname(),
                notifier.getSandboxname(), notifier.getVersion(), notifier.getCriticality(),
                notifier.getScanincludespattern(), notifier.getScanexcludespattern(),
                notifier.getFilenamepattern(), notifier.getReplacementpattern(), phost, pport,
                puser, ppsword, build.getWorkspace(), envVars, notifier.getTimeout(),
                notifier.getDeleteIncompleteScan(), descriptor.getDebug(), filePaths);
    }

    /**
     * Returns an UploadAndScanArgs object initialized with the specified arguments.
     * Used by the pipeline recorder.
     *
     * @param bRemoteScan         a boolean.
     * @param autoApplicationName a boolean.
     * @param autoDescription     a boolean.
     * @param autoScanName        a boolean.
     * @param createSandbox       a boolean.
     * @param createProfile       a boolean.
     * @param teams               a {@link java.lang.String} object.
     * @param useProxy            a boolean.
     * @param vId                 a {@link java.lang.String} object.
     * @param vKey                a {@link java.lang.String} object.
     * @param buildNumber         a {@link java.lang.String} object.
     * @param projectName         a {@link java.lang.String} object.
     * @param applicationName     a {@link java.lang.String} object.
     * @param sandboxName         a {@link java.lang.String} object.
     * @param scanName            a {@link java.lang.String} object.
     * @param criticality         a {@link java.lang.String} object.
     * @param scanIncludesPattern a {@link java.lang.String} object.
     * @param scanExcludesPattern a {@link java.lang.String} object.
     * @param fileNamePattern     a {@link java.lang.String} object.
     * @param replacementPattern  a {@link java.lang.String} object.
     * @param pHost               a {@link java.lang.String} object.
     * @param pPort               a {@link java.lang.String} object.
     * @param pUser               a {@link java.lang.String} object.
     * @param pCredential         a {@link java.lang.String} object.
     * @param workspace           a {@link hudson.FilePath} object.
     * @param envVars             a {@link hudson.EnvVars} object.
     * @param debug               a boolean.
     * @param timeOut             a {@link java.lang.String} object.
     * @param deleteIncompleteScan  a {@link java.lang.String} object.
     * @param filePaths           an array of {@link java.lang.String} objects.
     * @return a {@link com.veracode.jenkins.plugin.args.UploadAndScanArgs} object.
     */
    public static UploadAndScanArgs newUploadAndScanArgs(boolean bRemoteScan,
            boolean autoApplicationName, boolean autoDescription, boolean autoScanName,
            boolean createSandbox, boolean createProfile, String teams, boolean useProxy,
            String vId, String vKey, String buildNumber, String projectName, String applicationName,
            String sandboxName, String scanName, String criticality, String scanIncludesPattern,
            String scanExcludesPattern, String fileNamePattern, String replacementPattern,
            String pHost, String pPort, String pUser, String pCredential, FilePath workspace,
            hudson.EnvVars envVars, String timeOut, String deleteIncompleteScan, boolean debug, String[] filePaths) {

        String description = null;

        String phost = null;
        String pport = null;
        String puser = null;
        String ppsword = null;

        if (!StringUtil.isNullOrEmpty(vId)) {
            vId = envVars.expand(vId);
        }

        if (!StringUtil.isNullOrEmpty(vKey)) {
            vKey = envVars.expand(vKey);
        }

        // note that the API wrapper would likely not call the createbuild.do (or
        // createapp.do) web API until a few seconds later
        UploadAndScanArgs.setEnvVars(envVars, StringUtil.getEmptyIfNull(buildNumber),
                StringUtil.getEmptyIfNull(projectName));

        // application profile name
        if (!StringUtil.isNullOrEmpty(applicationName)) {
            applicationName = envVars.expand(applicationName);
        } else if (autoApplicationName) {
            applicationName = envVars.get(CUSTOM_PROJECT_NAME_VAR);
        }

        // sandbox name
        if (!StringUtil.isNullOrEmpty(sandboxName)) {
            sandboxName = envVars.expand(sandboxName);
        }

        // version (build name / scan name)
        if (!StringUtil.isNullOrEmpty(scanName)) {
            scanName = envVars.expand(scanName);
        } else if (autoScanName) {
            scanName = envVars.get(CUSTOM_BUILD_NUMBER_VAR);
        }

        if (!StringUtil.isNullOrEmpty(scanIncludesPattern)) {
            scanIncludesPattern = envVars.expand(scanIncludesPattern);
        }

        if (!StringUtil.isNullOrEmpty(scanExcludesPattern)) {
            scanExcludesPattern = envVars.expand(scanExcludesPattern);
        }

        // application description
        if (autoDescription) {
            description = createDescriptionArg(workspace.getParent());
        }
        // proxy settings
        if (useProxy) {
            phost = pHost;
            pport = pPort;
            puser = pUser;
            ppsword = pCredential;
        }
        UploadAndScanArgs args = new UploadAndScanArgs();
        // We know whether we are using the global or job credentials because
        // of the initial initialization statements therefore no add'l logic req'd.
        args.addApiCredentials(vId, vKey);
        args.addProxyCredentials(puser, ppsword);
        args.addProxyConfiguration(phost, pport);
        args.addStdArguments(bRemoteScan, applicationName, description, createProfile, teams, criticality, sandboxName,
                createSandbox, scanName, scanIncludesPattern, scanExcludesPattern, fileNamePattern, replacementPattern,
                timeOut, getDeleteIncompleteScan(deleteIncompleteScan), debug, filePaths);
        args.addUserAgent(UserAgentUtil.getVersionDetails());

        return args;
    }

    /**
     * Sets our custom environment variables.
     *
     * @param envVars     a {@link hudson.EnvVars} object - environment of the
     *                    build/run.
     * @param buildNumber a {@link java.lang.String} object.
     * @param projectName a {@link java.lang.String} object.
     */
    public static void setEnvVars(hudson.EnvVars envVars, String buildNumber, String projectName) {
        if (null == envVars) {
            return;
        }

        // note that the API wrapper would likely not call the createbuild.do (or
        // createapp.do) web API until a few seconds later
        envVars.put(CUSTOM_TIMESTAMP_VAR,
                new java.text.SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new java.util.Date()));
        envVars.put(CUSTOM_BUILD_NUMBER_VAR, StringUtil.getEmptyIfNull(buildNumber));
        envVars.put(CUSTOM_PROJECT_NAME_VAR, StringUtil.getEmptyIfNull(projectName));
    }

    /**
     * This method will handle the backward compatibility of deleteIncompleteScan.
     * If deleteIncompleteScan is false then return "0". If deleteIncompleteScan is
     * true then return "1".
     * 
     * @param deleteIncompleteScan a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getDeleteIncompleteScan(String deleteIncompleteScan) {
        if (deleteIncompleteScan == null) {
            return null;
        }

        if (deleteIncompleteScan.equals("false")) {
            return "0";
        } else if (deleteIncompleteScan.equals("true")) {
            return "1";
        } else {
            return deleteIncompleteScan;
        }
    }
}
