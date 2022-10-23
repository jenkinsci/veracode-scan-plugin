package com.veracode.jenkins.plugin.args;

import com.veracode.jenkins.plugin.DynamicRescanNotifier;
import com.veracode.jenkins.plugin.VeracodeNotifier;
import com.veracode.jenkins.plugin.VeracodeNotifier.VeracodeDescriptor;
import com.veracode.jenkins.plugin.utils.StringUtil;
import com.veracode.jenkins.plugin.utils.UserAgentUtil;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import jenkins.model.Jenkins;

/**
 * The DynamicRescanArgs class builds the command line argument passed to the
 * Veracode API wrapper that causes it to create dynamic scan request and submit
 * the created dynamic scan.
 *
 */
public final class DynamicRescanArgs extends AbstractArgs {

    private static final String APPNAME = SWITCH + "appname";
    private static final String VERSION = SWITCH + "version";
    private static final String FLAWONLY = SWITCH + "flawonly";
    private static final String CUSTOM_PROJECT_NAME_VAR = "projectname";

    /**
     * Constructor for DynamicRescanArgs.
     */
    private DynamicRescanArgs() {
        addAction("CreateAndSubmitDynamicRescan");
    }

    /**
     * Processes arguments.
     *
     * @param dynamicScanDescriptor a
     *                              {@link com.veracode.jenkins.plugin.DynamicRescanNotifier}
     *                              object.
     * @param build                 a {@link hudson.model.AbstractBuild} object.
     * @param environment           a {@link hudson.EnvVars} object.
     * @return a {@link com.veracode.jenkins.plugin.args.DynamicRescanArgs} object.
     */
    public static DynamicRescanArgs dynamicScanArgs(DynamicRescanNotifier dynamicScanDescriptor,
            AbstractBuild<?, ?> build, EnvVars environment) {
        VeracodeDescriptor veracodeDescriptor = (VeracodeDescriptor) Jenkins.get()
                .getDescriptor(VeracodeNotifier.class);

        DynamicRescanArgs args = new DynamicRescanArgs();
        String vid = null;
        String vkey = null;
        if (veracodeDescriptor != null) {
            vid = veracodeDescriptor.getGvid();
            vkey = veracodeDescriptor.getGvkey();
        }
        String appname = dynamicScanDescriptor.getAppname();
        boolean isDVREnabled = dynamicScanDescriptor.getDvrenabled();
        boolean autoAppName = dynamicScanDescriptor.getDescriptor().getAutoappname();
        boolean autoversion = dynamicScanDescriptor.getDescriptor().getAutoversion();
        String scanName = StringUtil.getEmptyIfNull(build.getDisplayName());

        environment.put(CUSTOM_PROJECT_NAME_VAR,
                StringUtil.getEmptyIfNull(build.getProject().getDisplayName()));

        if (!StringUtil.isNullOrEmpty(vid)) {
            vid = environment.expand(vid);
        }

        if (!StringUtil.isNullOrEmpty(vkey)) {
            vkey = environment.expand(vkey);
        }

        // application profile name
        if (!StringUtil.isNullOrEmpty(appname)) {
            appname = environment.expand(appname);
        }

        if (!StringUtil.isNullOrEmpty(appname)) {
            appname = environment.expand(appname);
        } else if (autoAppName) {
            appname = environment.get(CUSTOM_PROJECT_NAME_VAR);
        }

        args.addApiCredentials(vid, vkey);

        args.addStdArguments(appname, scanName, isDVREnabled, autoversion);
        if (veracodeDescriptor != null) {
            args.setProxy(veracodeDescriptor, args);
        }
        args.addUserAgent(UserAgentUtil.getVersionDetails());

        return args;

    }

    /**
     * Adds arguments to the argument list.
     *
     * @param appname     a {@link java.lang.String} object.
     * @param version     a {@link java.lang.String} object.
     * @param isDvr       a {@link java.lang.Boolean} object.
     * @param autoVersion a {@link java.lang.Boolean} object.
     */
    private void addStdArguments(String appname, String version, Boolean isDvr,
            Boolean autoVersion) {
        if (!StringUtil.isNullOrEmpty(appname)) {
            list.add(APPNAME);
            list.add(appname);

            if (!StringUtil.isNullOrEmpty(version) && autoVersion) {
                list.add(VERSION);
                list.add(version);
            }

            list.add(FLAWONLY);
            list.add(String.valueOf(isDvr));
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
     * Sets proxy settings.
     *
     * @param veracodeDescriptor a
     *                           {@link com.veracode.jenkins.plugin.VeracodeNotifier.VeracodeDescriptor}
     *                           object.
     * @param args               a
     *                           {@link com.veracode.jenkins.plugin.args.DynamicRescanArgs}
     *                           object.
     */
    private void setProxy(VeracodeDescriptor veracodeDescriptor, DynamicRescanArgs args) {
        String phost = null;
        String pport = null;
        String puser = null;
        String ppsword = null;

        if (veracodeDescriptor.getProxy()) {
            phost = veracodeDescriptor.getPhost();
            pport = veracodeDescriptor.getPport();
            puser = veracodeDescriptor.getPuser();
            ppsword = veracodeDescriptor.getPpassword();
        }

        args.addProxyCredentials(puser, ppsword);
        args.addProxyConfiguration(phost, pport);
    }

    /**
     * Creates argument for pipeline in dynamicRescan.
     *
     * @param autoApplicationName a boolean.
     * @param autoDescription     a boolean.
     * @param autoScanName        a boolean.
     * @param useProxy            a boolean.
     * @param vId                 a {@link java.lang.String} object.
     * @param vKey                a {@link java.lang.String} object.
     * @param version             a {@link java.lang.String} object.
     * @param projectName         a {@link java.lang.String} object.
     * @param applicationName     a {@link java.lang.String} object.
     * @param DVREnabled          a boolean.
     * @param pHost               a {@link java.lang.String} object.
     * @param pPort               a {@link java.lang.String} object.
     * @param pUser               a {@link java.lang.String} object.
     * @param pCredential         a {@link java.lang.String} object.
     * @param workspace           a {@link hudson.FilePath} object.
     * @param envVars             a {@link hudson.EnvVars} object.
     * @return a {@link com.veracode.jenkins.plugin.args.DynamicRescanArgs} object.
     */
    public static DynamicRescanArgs pipelineRescanArgs(boolean autoApplicationName,
            boolean autoDescription, boolean autoScanName, boolean useProxy, String vId,
            String vKey, String version, String projectName, String applicationName,
            boolean DVREnabled, String pHost, String pPort, String pUser, String pCredential,
            FilePath workspace, hudson.EnvVars envVars) {

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

        envVars.put(CUSTOM_PROJECT_NAME_VAR, StringUtil.getEmptyIfNull(projectName));

        // application profile name
        if (!StringUtil.isNullOrEmpty(applicationName)) {
            applicationName = envVars.expand(applicationName);
        } else if (autoApplicationName) {
            applicationName = envVars.get(CUSTOM_PROJECT_NAME_VAR);
        }
        // proxy settings
        if (useProxy) {
            phost = pHost;
            pport = pPort;
            puser = pUser;
            ppsword = pCredential;
        }
        DynamicRescanArgs args = new DynamicRescanArgs();
        // We know whether we are using the global or job credentials because
        // of the initial initialization statements therefore no add'l logic req'd.
        args.addApiCredentials(vId, vKey);
        args.addProxyCredentials(puser, ppsword);
        args.addProxyConfiguration(phost, pport);
        args.addStdArguments(applicationName, version, DVREnabled, autoScanName);
        args.addUserAgent(UserAgentUtil.getVersionDetails());

        return args;
    }
}
