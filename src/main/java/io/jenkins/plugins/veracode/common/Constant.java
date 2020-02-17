/*******************************************************************************
 * Copyright (c) 2017 Veracode, Inc. All rights observed.
 *
 * Available for use by Veracode customers as described in the accompanying license agreement.
 *
 * Send bug reports or enhancement requests to support@veracode.com.
 *
 * See the license agreement for conditions on submitted materials.
 ******************************************************************************/
package io.jenkins.plugins.veracode.common;

/**
 * Commonly used strings in both extentions
 * 
 * @author harsha
 *
 */
public class Constant {

    public static final String nodeJarFileDir = "veracode-jenkins-plugin";
    public static final String inclusive = "vosp-api-wrappers-java*.jar";
    public static final String execJarFile = "VeracodeJavaAPI";
    public static final String regex = "(vosp-api-wrappers).*?(.jar)";

    public static final String RESULTS_READY = "Results Ready";

    public static final String CONDITIONAL_PASS = "Conditional pass";
    public static final String DID_NOT_PASS = "Did not pass";
    public static final String PASSED = "Pass";
    public static final String PLUGIN_NAME = "veracode-jenkins-plugin";
    public static final String PLUGIN_ICONS_URI_PREFIX = "/plugin/" + PLUGIN_NAME + "/icons/";
    public static final String VIEW_REPORT_URI_PREFIX = "https://analysiscenter.veracode.com/auth/index.jsp#ViewReportsDetailedReport";

    // Open new window icon
    public static final String OPEN_NEW_WINDOW = "open-new-window-16x16.png";

    // Veracode logo icon
    public static final String VERACODE_ICON_24X24 = "veracode-24x24.png";
    public static final String VERACODE_ICON_48X48 = "veracode-48x48.png";

    // Shield icons for passed/did not pass/unknown compliance status
    public static final String SHIELD_RED_16X16 = "shield-red-16x16.png";
    public static final String SHIELD_GREEN_16X16 = "shield-green-16x16.png";
    public static final String SHIELD_YELLOW_16X16 = "shield-yellow-16x16.png";
    public static final String SHIELD_GRAY_16X16 = "shield-gray-16x16.png";

    public static final String SHIELD_RED_24X24 = "shield-red-24x24.png";
    public static final String SHIELD_GREEN_24X24 = "shield-green-24x24.png";
    public static final String SHIELD_YELLOW_24X24 = "shield-yellow-24x24.png";
    public static final String SHIELD_GRAY_24X24 = "shield-gray-24x24.png";

    public static final String SHIELD_RED_32X32 = "shield-red-32x32.png";
    public static final String SHIELD_GREEN_32X32 = "shield-green-32x32.png";
    public static final String SHIELD_YELLOW_32X32 = "shield-yellow-32x32.png";
    public static final String SHIELD_GRAY_32X32 = "shield-gray-32x32.png";

    public static final String SHIELD_RED_48X48 = "shield-red-48x48.png";
    public static final String SHIELD_GREEN_48X48 = "shield-green-48x48.png";
    public static final String SHIELD_YELLOW_48X48 = "shield-yellow-48x48.png";
    public static final String SHIELD_GRAY_48X48 = "shield-gray-48x48.png";

    public static final String PASSED_POLICY_COMPONENT_ICON = "pass-policy-component.png";
    public static final String FAILED_POLICY_COMPONENT_ICON = "fail-policy-component.png";

    public static final String NEWLINE = System.lineSeparator();

    public static final String AUTHENTICATION_ERROR = "Please verify the following:" + NEWLINE
            + "1. The login API credentials are valid." + NEWLINE
            + "2. The account is configured with sufficient privilege." + NEWLINE
            + "3. The account is not locked." + NEWLINE
            + "4. This machine's internet-facing IP address is not restricted." + NEWLINE;

    public static final String PRIVILEGE_ERROR = "Please verify the account is configured with sufficient privilege.";
    public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    public static final String GATEWAY_TIMEOUT_ERROR = "Gateway Timeout Error";

    public static final String STARTING_POST_BUILD_ACTION_LOG = "------------------------------------------------------------------------"
            + NEWLINE + "Starting Post Build Action: %s" + NEWLINE
            + "------------------------------------------------------------------------";
    public static final String FINISHED_POST_BUILD_ACTION_LOG = "Finished Post Build Action: %s";
    public static final String POST_BUILD_ACTION_DISPLAY_TEXT_RESUBMIT = "Resubmit Veracode Dynamic Analysis";
    public static final String POST_BUILD_ACTION_DISPLAY_TEXT_REVIEW = "Review Veracode Dynamic Analysis Results";
    public static final int DEFAULT_VALUE_DA_MAX_DURATION_HOURS = 72;
    public static final int DEFAULT_VALUE_DA_WAIT_FOR_RESULTS_MINUTES = 60;
    public static final int UPLOADANDSCAN_MAX_RETRY_COUNT = 5;
}