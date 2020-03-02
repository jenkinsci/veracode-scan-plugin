package io.jenkins.plugins.veracode.args;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import io.jenkins.plugins.veracode.DynamicRescanNotifier;
import io.jenkins.plugins.veracode.VeracodeNotifier;
import io.jenkins.plugins.veracode.VeracodeNotifier.VeracodeDescriptor;
import io.jenkins.plugins.veracode.utils.StringUtil;
import io.jenkins.plugins.veracode.utils.UserAgentUtil;
import jenkins.model.Jenkins;

/**
 * Builds the command line argument passed to the Veracode API wrapper that
 * causes it to create dynamic scan request and submit the created dynamic scan.
 */
public final class DynamicRescanArgs extends AbstractArgs {

    private static final String APPNAME = SWITCH + "appname";
    private static final String VERSION = SWITCH + "version";
    private static final String FLAWONLY = SWITCH + "flawonly";
    private static final String CUSTOM_PROJECT_NAME_VAR = "projectname";

    private DynamicRescanArgs() {
        addAction("CreateAndSubmitDynamicRescan");
    }

    /**
     * process arguments
     *
     * @param dynamicScanDescriptor DynamicRescanNotifier
     * @param build                 AbstractBuild
     * @param environment           EnvVars
     * @return DynamicRescanArgs
     */
    public static DynamicRescanArgs dynamicScanArgs(DynamicRescanNotifier dynamicScanDescriptor,
            AbstractBuild<?, ?> build, EnvVars environment) {
        VeracodeDescriptor veracodeDescriptor = (VeracodeDescriptor) Jenkins.getInstance()
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
     * add argument to the argument list
     *
     * @param appname     String
     * @param version     String
     * @param isDvr       Boolean
     * @param autoVersion Boolean
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
     * Add user agent details got through api
     * 
     * @param userAgent String
     */
    protected void addUserAgent(String userAgent) {
        if (!StringUtil.isNullOrEmpty(userAgent)) {
            list.add(USERAGENT);
            list.add(userAgent);
        }
    }

    /**
     * set proxy setting
     *
     * @param veracodeDescriptor VeracodeDescriptor
     * @param args               DynamicRescanArgs
     */
    private void setProxy(VeracodeDescriptor veracodeDescriptor, DynamicRescanArgs args) {
        String phost = null;
        String pport = null;
        String puser = null;
        String ppassword = null;

        if (veracodeDescriptor.getProxy()) {
            phost = veracodeDescriptor.getPhost();
            pport = veracodeDescriptor.getPport();
            puser = veracodeDescriptor.getPuser();
            ppassword = veracodeDescriptor.getPpassword();
        }

        args.addProxyCredentials(puser, ppassword);
        args.addProxyConfiguration(phost, pport);
    }

    /**
     * create argument for pipeline in dynamicRescan
     * 
     * @param autoApplicationName boolean
     * @param autoDescription     boolean
     * @param autoScanName        boolean
     * @param useProxy            boolean
     * @param vId                 String
     * @param vKey                String
     * @param version             String
     * @param projectName         String
     * @param applicationName     String
     * @param DVREnabled          boolean
     * @param pHost               String
     * @param pPort               String
     * @param pUser               String
     * @param pCredential         String
     * @param workspace           FilePath
     * @param envVars             EnvVars
     * @return DynamicRescanArgs
     */
    public static DynamicRescanArgs pipelineRescanArgs(boolean autoApplicationName,
            boolean autoDescription, boolean autoScanName, boolean useProxy, String vId,
            String vKey, String version, String projectName, String applicationName,
            boolean DVREnabled, String pHost, String pPort, String pUser, String pCredential,
            FilePath workspace, hudson.EnvVars envVars) {

        String phost = null;
        String pport = null;
        String puser = null;
        String ppassword = null;

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
            ppassword = pCredential;
        }
        DynamicRescanArgs args = new DynamicRescanArgs();
        // We know whether we are using the global or job credentials because
        // of the initial initialization statements therefore no add'l logic req'd.
        args.addApiCredentials(vId, vKey);
        args.addProxyCredentials(puser, ppassword);
        args.addProxyConfiguration(phost, pport);
        args.addStdArguments(applicationName, version, DVREnabled, autoScanName);
        args.addUserAgent(UserAgentUtil.getVersionDetails());

        return args;
    }
}