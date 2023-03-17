package com.veracode.jenkins.plugin.utils;

import com.veracode.apiwrapper.cli.VeracodeCommand;

import hudson.PluginWrapper;
import hudson.util.VersionNumber;
import jenkins.model.Jenkins;

/**
 * The UserAgentUtil is a utility class for getting version details.
 *
 */
public class UserAgentUtil {

    private static final String DEFAULT_USER_AGENT = "VeracodeScanJenkins";
    private static final String VERACODE_PLUGIN_CLASS = "veracode-scan";
    private static final String UNKNOWN_VALUE = "Unknown";
    private static final String USERAGENT_HEADER_FORMAT = "%s/%s (Jenkins/%s; Java/%s)";

    /**
     * Returns a String of version details of the plugin, Jenkins and Java.
     *
     * @return a {@link java.lang.String} object.
     */
    public static String getVersionDetails() {
        String pluginVersion = getPluginVersion();
        String jenkinsVersion = getJenkinsVersion();
        String javaVersion = getJavaVersion();

        return String.format(USERAGENT_HEADER_FORMAT, DEFAULT_USER_AGENT, pluginVersion,
                jenkinsVersion, javaVersion);
    }

    /**
     * Returns the retrieved Java version from Veracode API Wrapper.
     *
     * @return a {@link java.lang.String} object.
     */
    private static String getJavaVersion() {
        try {
            final String jreVersion = VeracodeCommand.getJreVersion();
            return (StringUtil.isNullOrEmpty(jreVersion) ? UNKNOWN_VALUE : jreVersion);
        } catch (Throwable t) {
            return UNKNOWN_VALUE;
        }
    }

    /**
     * Returns the plugin version.
     *
     * @return a {@link java.lang.String} object.
     */
    private static String getPluginVersion() {
        try {
            PluginWrapper pluginWrapper = Jenkins.get().getPluginManager()
                    .getPlugin(VERACODE_PLUGIN_CLASS);
            if (pluginWrapper == null) {
                throw new RuntimeException("Cannot locate the plugin.");
            }
            final String pluginVersion = pluginWrapper.getVersion();
            return (StringUtil.isNullOrEmpty(pluginVersion) ? UNKNOWN_VALUE : pluginVersion);
        } catch (Throwable t) {
            return UNKNOWN_VALUE;
        }
    }

    /**
     * Returns the Jenkins version.
     *
     * @return a {@link java.lang.String} object.
     */
    private static String getJenkinsVersion() {
        try {
            VersionNumber versionNumber = Jenkins.getVersion();
            if (versionNumber == null) {
                throw new RuntimeException("Cannot retrieve Jenkins version number.");
            }
            final String jenkinsVersion = versionNumber.toString();
            return (StringUtil.isNullOrEmpty(jenkinsVersion) ? UNKNOWN_VALUE : jenkinsVersion);
        } catch (Throwable t) {
            return UNKNOWN_VALUE;
        }
    }
}
