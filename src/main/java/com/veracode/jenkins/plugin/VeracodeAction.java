package com.veracode.jenkins.plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.veracode.jenkins.plugin.common.Constant;
import com.veracode.jenkins.plugin.data.BuildHistory;
import com.veracode.jenkins.plugin.data.FindingCounts;
import com.veracode.jenkins.plugin.data.SCAComponent;
import com.veracode.jenkins.plugin.data.SCAScanHistory;
import com.veracode.jenkins.plugin.data.ScanHistory;
import com.veracode.jenkins.plugin.enums.SeverityLevel;

import hudson.model.Run;
import jenkins.model.RunAction2;

/**
 * This class represents the post build Veracode step on the build page.
 *
 */
public class VeracodeAction implements RunAction2 {

    // The object to store the scan history for Jenkins builds.
    private final ScanHistory scanHistory;

    // The Jenkins build containing this action
    private transient Run<?, ?> build;
    
    // The object to store the specific region url
    private final String xmlApiHost;

    /**
     * <p>Constructor for VeracodeAction.</p>
     */
    public VeracodeAction() {
        scanHistory = null;
        xmlApiHost = null;
        build = null;
    }

    /**
     * <p>Constructor for VeracodeAction.</p>
     *
     * @param scanHistory a {@link com.veracode.jenkins.plugin.data.ScanHistory} object.
     */
    public VeracodeAction(ScanHistory scanHistory, String xmlApiHost) {
        if (null == scanHistory || null == xmlApiHost) {
            throw new IllegalArgumentException(
                    "Missing required information to create a VeracodeAction.");
        }
        this.scanHistory = scanHistory;
        this.xmlApiHost = xmlApiHost;
        build = null;
    }

    /**
     * Use by Jenkins framework to display our logo on the left panel on on the
     * build page
     *
     * @return URI to the 24x24 Veracode logo icon
     */
    @Override
    public String getIconFileName() {
        return Constant.PLUGIN_ICONS_URI_PREFIX + Constant.VERACODE_ICON_24X24;
    }

    @Override
    public String getDisplayName() {
        return "Veracode Static Scan";
    }

    @Override
    public String getUrlName() {
        return "veracode";
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        setBuild(r);
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        setBuild(r);
    }

    /**
     * Get the policy name
     *
     * @return the policy name
     */
    public String getPolicyName() {
        return scanHistory.getPolicyName();
    }

    /**
     * Get the policy name suitable for displaying in HTML
     *
     * @return policy name escaped for HTML
     */
    public String getPolicyNameForHTML() {
        return StringEscapeUtils.escapeHtml(scanHistory.getPolicyName());
    }

    /**
     * Get the policy compliance status
     *
     * @return policy compliance status
     */
    public String getPolicyComplianceStatus() {
        return scanHistory.getPolicyComplianceStatus();
    }

    /**
     * Get the policy compliance status for displaying in HTML Note that the "PASS"
     * status is returned as "Passed" for displaying purpose.
     *
     * @return policy compliance status escaped for HTML
     */
    public String getPolicyComplianceStatusForHTML() {
        String complianceStatus = scanHistory.getPolicyComplianceStatus();
        return StringEscapeUtils.escapeHtml(
                complianceStatus.equalsIgnoreCase(Constant.PASSED) ? "Passed" : complianceStatus);
    }

    /**
     * Get the Veracode level
     *
     * @return Veracode level
     */
    public String getVeracodeLevel() {
        return scanHistory.getVeracodeLevel();
    }

    /**
     * Get the Veracode level to be displayed in HTML
     *
     * @return Veracode level escaped for HTML
     */
    public String getVeracodeLevelForHTML() {
        return StringEscapeUtils.escapeHtml(scanHistory.getVeracodeLevel());
    }

    public int getAnalysisScore() {
        return scanHistory.getScore();
    }

    public String getScanOverdueStatus() {
        return scanHistory.isScanOverdue() ? "Did not pass" : "Passed";
    }

    /**
     * Use by summary.jelly for VeracodeAction to display our logo
     *
     * @return URI to the 48x48 Veracode logo icon
     */
    public String getVeracodeLogo48() {
        return Constant.PLUGIN_ICONS_URI_PREFIX + Constant.VERACODE_ICON_48X48;
    }

    /**
     * Use by summary.jelly for VeracodeAction to display the correct status icon
     * (16x16)
     *
     * @return relative URI of the status icon
     */
    public String getPolicyComplianceStatusIconUri16() {
        String complianceStatus = scanHistory.getPolicyComplianceStatus();
        String iconName;
        if (complianceStatus.equalsIgnoreCase(Constant.PASSED)) {
            iconName = Constant.SHIELD_GREEN_16X16;
        } else if (complianceStatus.equalsIgnoreCase(Constant.DID_NOT_PASS)) {
            iconName = Constant.SHIELD_RED_16X16;
        } else if (complianceStatus.equalsIgnoreCase(Constant.CONDITIONAL_PASS)) {
            iconName = Constant.SHIELD_YELLOW_16X16;
        } else {
            iconName = Constant.SHIELD_GRAY_16X16;
        }

        return Constant.PLUGIN_ICONS_URI_PREFIX + iconName;
    }

    /**
     * Use by summary.jelly for VeracodeAction to display the correct status icon
     * (24x24)
     *
     * @return relative URI of the status icon
     */
    public String getPolicyComplianceStatusIconUri24() {
        String complianceStatus = scanHistory.getPolicyComplianceStatus();
        String iconName;
        if (complianceStatus.equalsIgnoreCase(Constant.PASSED)) {
            iconName = Constant.SHIELD_GREEN_24X24;
        } else if (complianceStatus.equalsIgnoreCase(Constant.DID_NOT_PASS)) {
            iconName = Constant.SHIELD_RED_24X24;
        } else if (complianceStatus.equalsIgnoreCase(Constant.CONDITIONAL_PASS)) {
            iconName = Constant.SHIELD_YELLOW_24X24;
        } else {
            iconName = Constant.SHIELD_GRAY_24X24;
        }

        return Constant.PLUGIN_ICONS_URI_PREFIX + iconName;
    }

    /**
     * Used by summary.jelly for VeracodeAction to display the correct status icon
     * (32x32)
     *
     * @return relative URI of the status icon
     */
    public String getPolicyComplianceStatusIconUri32() {
        String complianceStatus = scanHistory.getPolicyComplianceStatus();
        String iconName;
        if (complianceStatus.equalsIgnoreCase(Constant.PASSED)) {
            iconName = Constant.SHIELD_GREEN_32X32;
        } else if (complianceStatus.equalsIgnoreCase(Constant.DID_NOT_PASS)) {
            iconName = Constant.SHIELD_RED_32X32;
        } else if (complianceStatus.equalsIgnoreCase(Constant.CONDITIONAL_PASS)) {
            iconName = Constant.SHIELD_YELLOW_32X32;
        } else {
            iconName = Constant.SHIELD_GRAY_32X32;
        }

        return Constant.PLUGIN_ICONS_URI_PREFIX + iconName;
    }

    /**
     * Used by summary.jelly for VeracodeAction to display the correct status icon
     *
     * @return relative URI of the status icon
     */
    public String getPolicyComplianceStatusIconUri48() {
        String complianceStatus = scanHistory.getPolicyComplianceStatus();
        String iconName;
        if (complianceStatus.equalsIgnoreCase(Constant.PASSED)) {
            iconName = Constant.SHIELD_GREEN_48X48;
        } else if (complianceStatus.equalsIgnoreCase(Constant.DID_NOT_PASS)) {
            iconName = Constant.SHIELD_RED_48X48;
        } else if (complianceStatus.equalsIgnoreCase(Constant.CONDITIONAL_PASS)) {
            iconName = Constant.SHIELD_YELLOW_48X48;
        } else {
            iconName = Constant.SHIELD_GRAY_48X48;
        }

        return Constant.PLUGIN_ICONS_URI_PREFIX + iconName;
    }

    /**
     * Use by index.jelly for VeracodeAction to display the open new window icon
     *
     * @return String
     */
    public String getOpenNewWindow16() {
        return Constant.PLUGIN_ICONS_URI_PREFIX + Constant.OPEN_NEW_WINDOW;
    }

    /**
     * Find out if the flaw count of the given severity is lower than the actual
     * count due to mitigation
     *
     * @param severity - A severity level
     * @return true if the count is lower due to mitigation, false otherwise.
     */
    public boolean isStaticSevLevelMitigated(int severity) {
        boolean result = false;
        try {
            result = scanHistory.getMitigateFlag(severity);
        } catch (IllegalArgumentException iae) {
            result = false;
        }
        return result;
    }

    public String getFlawsCount(int severity) {
        String displayFlawsCount;
        try {
            int flawsCount = scanHistory.getFlawsCount(severity);

            // Adding "*" to count number if there is a mitigated flaw in that severity
            // level
            if (isStaticSevLevelMitigated(severity)) {
                displayFlawsCount = flawsCount + "*";
            } else {
                displayFlawsCount = flawsCount > 0 ? Integer.toString(flawsCount) : "";
            }
        } catch (IllegalArgumentException iae) {
            displayFlawsCount = "";
        }

        return displayFlawsCount;
    }

    public int getTotalFlawsCount() {
        return scanHistory.getTotalFlawsCount();
    }

    public int getTotalNewFlawsCount() {
        return scanHistory.getTotalNewFlawsCount();
    }

    public int getTotalNetChangeCount() {
        return scanHistory.getTotalNetChangeCount();
    }

    public String getNetChange(int severity) {

        int netChange = scanHistory.getNetChange(severity);
        String displayNetChange = ""; // empty string for 0

        // Adding "+" to positive number
        if (netChange > 0) {
            displayNetChange = "+" + Integer.toString(netChange);
        } else if (netChange < 0) {
            displayNetChange = Integer.toString(netChange);
        }

        return displayNetChange;
    }

    public String getNewFlaws(int severity) {
        int newFlaw = scanHistory.getNewFlaws(severity);

        // Display empty string for 0 and negative number
        return newFlaw > 0 ? Integer.toString(newFlaw) : "";
    }

    public List<Map<String, Long>> getFlawsCountHistory() {
        return scanHistory.getFlawsCountHistory();
    }

    /**
     * Creates a trend chart with scan history.
     *
     * @param request  a {@link org.kohsuke.stapler.StaplerRequest} object.
     * @param response a {@link org.kohsuke.stapler.StaplerResponse} object.
     */
    public void doGraph(StaplerRequest request, StaplerResponse response) {
        try {
            Collection<BuildHistory> buildHistoryList = new ArrayList<BuildHistory>();
            BuildHistory staticBuildHistory = new BuildHistory("Static Flaws",
                    scanHistory.getFlawsCountHistory());
            buildHistoryList.add(staticBuildHistory);

            // SCA vulnerabilities is optional
            if (null != getVulCountHistory()) {
                BuildHistory vulnerBuildHistory = new BuildHistory("SCA Vulnerabilities",
                        getVulCountHistory());
                buildHistoryList.add(vulnerBuildHistory);
            }
            TrendChart trendChart = new TrendChart(System.currentTimeMillis(), 600, 400,
                    buildHistoryList);
            trendChart.doPng(request, response);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to generate the Flaw trend graph.");
        }
    }

    /**
     * Get the URL to the Detailed Report for this scan that is escaped for HTML
     * attribute
     *
     * @return Detailed Report URL escaped for HTML attribute
     */
    public String getDetailedReportURLForHTMLAttr() {
        String escapedAcctId = StringEscapeUtils.escapeHtml(scanHistory.getAccountId());
        String escapedAppId = StringEscapeUtils.escapeHtml(scanHistory.getAppId());
        String escapedBuildId = StringEscapeUtils.escapeHtml(scanHistory.getBuildId());
        return String.format(Constant.VIEW_REPORT_URI_PREFIX,
                null == xmlApiHost ? Constant.DEFAULT_XML_API_HOST : xmlApiHost) + ":"
                + escapedAcctId + ":" + escapedAppId + ":" + escapedBuildId;
    }

    public boolean isScanHistoryAvailable() {
        return null != scanHistory;
    }

    private void setBuild(Run<?, ?> build) {
        this.build = build;
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    /**
     * Determine if the SCA History is available
     *
     * @return true if available. False otherwise
     */
    public boolean isSCAHistoryAvailable() {
        return (null != scanHistory) ? scanHistory.hasSCAHistory() : false;
    }

    /**
     * Determine if the account used for the build is subscribed to SCA or not
     *
     * @return true if subscribed. False if not subscribed or data not available
     */
    public boolean isSubscribedToSCA() {
        return (isSCAHistoryAvailable()) ? scanHistory.getScaHistory().isSubscribed() : false;
    }

    /**
     * Get the max CVSS score among all the SCA components.
     *
     * @return the max CVSS score if available. Otherwise, -1 for none of the SCA
     *         components has a CVSS score -2 for SCA data is not available
     */
    public double getMaxCVSSScore() {
        double score = -2;
        if (isSCAHistoryAvailable()) {
            score = scanHistory.getScaHistory().getMaxCVSSScore();
        }
        return score;
    }

    /**
     * Returns the display on the build page based on the Max CVSS Score
     *
     * @return the max CVSS score if available. Otherwise, "-" if none of the SCA
     *         components has a CVSS score "" if SCA data is not available
     */
    public String getMaxCVSSScoreForHTML() {
        double score = getMaxCVSSScore();
        String result = "";
        if (-1 == score) {
            result = "-";
        } else if (-2 != score) {
            result = String.valueOf(score);
        }
        return result;
    }

    /**
     * Get the number of blacklisted components.
     *
     * @return the number of blacklisted components if available. Otherwise, -1 if
     *         SCA data is not available
     */
    public int getBlacklistedCompsCount() {
        return (isSCAHistoryAvailable())
                ? scanHistory.getScaHistory().getBlacklistedComponentsCount()
                : -1;
    }

    /**
     * Returns the display of the number of blacklisted components on the build page
     *
     * @return the number of blacklisted components if available. Otherwise, "0" if
     *         SCA data is not available or no SCA blacklisted components
     */
    public String getBlacklistedCompsCountForHTML() {
        int count = getBlacklistedCompsCount();
        return (count == -1) ? "0" : String.valueOf(count);
    }

    private FindingCounts getCountBySeverity(int severity) {
        if (!isSCAHistoryAvailable()) {
            return null;
        }

        FindingCounts result = null;
        try {
            result = scanHistory.getScaHistory()
                    .getCountBySeverity(SeverityLevel.findSevLevel(severity));
        } catch (IllegalArgumentException iae) {
            result = null;
        }

        return result;
    }

    /**
     * Get the vulnerability count of a given severity level (0 - 5). If the count
     * is lower than actual (due to mitigation), then the returned count will be
     * followed by an asterisk
     *
     * @param severity - A severity level
     * @return the vulnerability count
     */
    public String getVulCountForDisplayBySeverity(int severity) {
        FindingCounts fc = getCountBySeverity(severity);
        String result = "";
        if (null != fc) {
            result = fc.isMitigated() ? fc.getCount() + "*"
                    : (fc.getCount() > 0 ? Integer.toString(fc.getCount()) : "");
        } else {
            result = null;
        }

        return result;
    }

    /**
     * Get if the each severity level contains flaws which affect policy compliance
     *
     * @param severity - A severity level
     * @return policy affection
     */
    public boolean getPolicyAffection(int severity) {
        boolean result = false;
        try {
            result = scanHistory.getPolicyAffection(severity);
        } catch (IllegalArgumentException iae) {
            result = false;
        } catch (NullPointerException ex) {
            result = false;
        }
        return result;
    }

    /**
     * Get the new vulnerability count of a given severity level (0 - 5). If the
     * count zero, then an empty string will be returned. If the count is not
     * available, null will be returned.
     *
     * @param severity - A severity level
     * @return the vulnerability count
     */
    public String getNewVulCountForDisplayBySeverity(int severity) {
        FindingCounts fc = getCountBySeverity(severity);
        String result = "";
        if (null != fc) {
            result = fc.getNewCount() > 0 ? Integer.toString(fc.getNewCount()) : "";
        } else {
            result = null;
        }

        return result;
    }

    /**
     * Get the net vulnerability count of a given severity level (0 - 5). If the
     * count zero, then an empty string will be returned. If the count is not
     * available, null will be returned.
     *
     * @param severity - A severity level
     * @return the vulnerability count
     */
    public String getNetVulCountForDisplayBySeverity(int severity) {
        FindingCounts fc = getCountBySeverity(severity);
        String result = "";
        if (null != fc) {
            if (fc.getNetCount() == 0) {
                result = "";
            } else {
                result = fc.getNetCount() > 0 ? "+" + fc.getNetCount()
                        : Integer.toString(fc.getNetCount());
            }
        } else {
            result = null;
        }

        return result;
    }

    /**
     * Find out if the vulnerability count of the given severity is lower than the
     * actual count due to mitigation
     *
     * @param severity - A severity level
     * @return true if the count is lower due to mitigation, false otherwise.
     */
    public boolean isSCASevLevelMitigated(int severity) {
        if (!isSCAHistoryAvailable()) {
            return false;
        }

        boolean result = false;
        try {
            FindingCounts fc = scanHistory.getScaHistory()
                    .getCountBySeverity(SeverityLevel.findSevLevel(severity));
            if (null != fc) {
                result = fc.isMitigated();
            }
        } catch (IllegalArgumentException iae) {
            result = false;
        }

        return result;
    }

    /**
     * Get the total number of vulnerabilities across all severity levels.
     *
     * @return the total number of vulnerabilities if available. Otherwise, -1 if
     *         SCA data is not available
     */
    public int getTotalVulCount() {
        return (isSCAHistoryAvailable()) ? scanHistory.getScaHistory().getTotalVulCount() : -1;
    }

    /**
     * Returns the display of the total number of vulnerabilities across all
     * severity levels.
     *
     * @return the total number of vulnerabilities
     */
    public String getTotalVulCountForDisplay() {
        int count = getTotalVulCount();
        return count > 0 ? String.valueOf(count) : "0";
    }

    /**
     * Get the total number of new vulnerabilities across all severity levels.
     *
     * @return the total number of new vulnerabilities if available. Otherwise, -1
     *         if SCA data is not available
     */
    public int getTotalNewVulCount() {
        return (isSCAHistoryAvailable()) ? scanHistory.getScaHistory().getTotalNewVulCount() : -1;
    }

    /**
     * Returns the display of the total number of new vulnerabilities across all
     * severity levels.
     *
     * @return the total number of new vulnerabilities
     */
    public String getTotalNewVulCountForDisplay() {
        int count = getTotalNewVulCount();
        return count > 0 ? String.valueOf(count) : "0";
    }

    /**
     * Get the total number of net vulnerabilities across all severity levels.
     *
     * @return the total number of net vulnerabilities if available. Otherwise, null
     *         if SCA data is not available
     */
    public Integer getTotalNetVulCount() {
        return (isSCAHistoryAvailable()) ? scanHistory.getScaHistory().getTotalNetVulCount() : null;
    }

    /**
     * Returns the display of the total number of net vulnerabilities across all
     * severity levels.
     *
     * @return the total number of net vulnerabilities
     */
    public String getTotalNetVulCountForDisplay() {

        String totalNetVulCount = "0";
        if (null != getTotalNetVulCount()) {
            totalNetVulCount = String.valueOf(getTotalNetVulCount());
        }
        return totalNetVulCount;
    }

    /**
     * Used by index.jelly for VeracodeAction to determine if there are new SCA
     * components since the previous build.
     *
     * @return boolean whether or not there are new SCA components
     */
    public boolean isNewSCAComponents() {

        boolean isNewComponents = false;

        if (isSCAHistoryAvailable()) {
            /* Check if any new SCA components since previous build */
            for (SCAComponent component : scanHistory.getScaHistory().getSCAComponents()) {
                if (component.isNew()) {
                    isNewComponents = true;
                    break;
                }
            }
        }
        return isNewComponents;
    }

    /**
     * Used by index.jelly for VeracodeAction to display the SCA components which
     * are new since the previous build. Builds an array of the new SCA components
     * that passed policy or failed policy.
     *
     * @param isViolatedPolicy boolean
     * @return array of SCA component names that either passed or failed policy
     */
    public ArrayList<String> getNewSCAComponentsByPolicyStatus(boolean isViolatedPolicy) {

        ArrayList<String> componentArray = new ArrayList<String>();

        /* Add SCA component names based on specified passed or failed policy status */
        for (SCAComponent component : scanHistory.getScaHistory().getSCAComponents()) {
            if (component.isNew() && (isViolatedPolicy == component.isViolatedPolicy())) {
                componentArray.add(component.getName());
            }
        }
        /* Sort component array by alphabetic order */
        Collections.sort(componentArray);
        return componentArray;
    }

    /**
     * Used by index.jelly for VeracodeAction to display the policy compliance icon
     * for the SCA component.
     *
     * @param isViolatedPolicy boolean
     * @return relative URI of the status icon
     */
    public String getPolicyComplianceComponentIconUri(boolean isViolatedPolicy) {

        String iconName = "";

        if (isViolatedPolicy) {
            iconName = Constant.FAILED_POLICY_COMPONENT_ICON;
        } else {
            iconName = Constant.PASSED_POLICY_COMPONENT_ICON;
        }
        return Constant.PLUGIN_ICONS_URI_PREFIX + iconName;
    }

    /**
     * Get the vulnerability count history
     *
     * @return the count history or null if it is unavailable
     */
    public List<Map<String, Long>> getVulCountHistory() {
        return isSCAHistoryAvailable() ? scanHistory.getScaHistory().getVulCountHistory() : null;
    }

    public SCAScanHistory getSCAScanHistory() {
        return isSCAHistoryAvailable() ? scanHistory.getScaHistory() : null;
    }
}
