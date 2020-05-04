package io.jenkins.plugins.veracode.utils;

import com.veracode.apiwrapper.cli.VeracodeCommand;

import hudson.PluginWrapper;
import hudson.util.VersionNumber;
import jenkins.model.Jenkins;

/**
 * A utility class for getting version details.
 *
 *
 */
public class UserAgentUtil {

    private static final String DEFAULT_USER_AGENT = "VeracodeScanJenkins";
    private static final String VERACODE_PLUGIN_CLASS = "veracode-scan";
    private static final String UNKNOWN_VALUE = "Unknown";
    private static final String USERAGENT_HEADER_FORMAT = "%s/%s (Jenkins/%s; Java/%s)";

    public static String getVersionDetails() {
        String pluginVersion = getPluginVersion();
        String jenkinsVersion = getJenkinsVersion();
        String javaVersion = getJavaVersion();

        return String.format(USERAGENT_HEADER_FORMAT, DEFAULT_USER_AGENT, pluginVersion,
                jenkinsVersion, javaVersion);
    }

    // Retrieve Java version from Java Wrapper
    private static String getJavaVersion() {
        try {
            final String jreVersion = VeracodeCommand.getJreVersion();
            return (StringUtil.isNullOrEmpty(jreVersion) ? UNKNOWN_VALUE : jreVersion);
        } catch (Throwable t) {
            return UNKNOWN_VALUE;
        }
    }

    // Retrieve plugin version
    private static String getPluginVersion() {
        try {
            PluginWrapper pluginWrapper = Jenkins.getInstance().getPluginManager()
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

    // Retrieve Jenkins version
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