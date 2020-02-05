package io.jenkins.plugins.veracode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import io.jenkins.plugins.veracode.common.Constant;
import io.jenkins.plugins.veracode.data.BuildHistory;
import io.jenkins.plugins.veracode.data.DAScanHistory;
import org.apache.commons.lang.StringEscapeUtils;

import hudson.model.Run;
import jenkins.model.RunAction2;

/**
 * This class represents the post build Veracode step on the build page.
 *
 */
public class DynamicAnalysisResultsAction implements RunAction2 {

    // The object to store the scan history for Jenkins builds.
    private final DAScanHistory scanHistory;

    // The Jenkins build containing this action
    private transient Run<?,?> build;

    public DynamicAnalysisResultsAction() {
        scanHistory = null;
        build = null;
    }

    public DynamicAnalysisResultsAction(DAScanHistory scanHistory) {
        if (null == scanHistory) {
            throw new IllegalArgumentException("Missing required information to create a DynamicAnalysisResultsAction.");
        }
        this.scanHistory = scanHistory;
        build = null;
    }

    /**
     * Use by Jenkins framework to display our logo on the left panel on
     * on the build page
     *
     * @return URI to the 24x24 Veracode logo icon
     */
    @Override
    public String getIconFileName() {
        return Constant.PLUGIN_ICONS_URI_PREFIX + Constant.VERACODE_ICON_24X24;
    }

    @Override
    public String getDisplayName() {
        return "Veracode Dynamic Analysis";
    }

    @Override
    public String getUrlName() {
        return "veracodeDA";
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
     * Get the policy compliance status for displaying in HTML
     * Note that the "PASS" status is returned as "Passed" for displaying purpose.
     * @return policy compliance status escaped for HTML
     */
    public String getPolicyComplianceStatusForHTML() {
        String complianceStatus = scanHistory.getPolicyComplianceStatus();
        return StringEscapeUtils.escapeHtml(complianceStatus.equalsIgnoreCase(Constant.PASSED) ? "Passed" : complianceStatus);
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
     * Use by summary.jelly for DynamicAnalysisResultsAction to display our logo
     *
     * @return URI to the 48x48 Veracode logo icon
     */
    public String getVeracodeLogo48() {
        return Constant.PLUGIN_ICONS_URI_PREFIX + Constant.VERACODE_ICON_48X48;
    }

    /**
     * Use by summary.jelly for DynamicAnalysisResultsAction to display the correct status icon (16x16)
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
     * Use by summary.jelly for DynamicAnalysisResultsAction to display the correct status icon (24x24)
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
     * Used by summary.jelly for DynamicAnalysisResultsAction to display the correct status icon (32x32)
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
     * Used by summary.jelly for DynamicAnalysisResultsAction to display the correct status icon
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
     * Use by index.jelly for DynamicAnalysisResultsAction to display the open new window icon
     * @return String
     */
    public String getOpenNewWindow16() {
        return Constant.PLUGIN_ICONS_URI_PREFIX + Constant.OPEN_NEW_WINDOW;
    }

    /**
     * Find out if the flaw count of the given severity is
     * lower than the actual count due to mitigation
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

	        // Adding "*" to count number if there is a mitigated flaw in that severity level
	        if (isStaticSevLevelMitigated(severity)) {
	            displayFlawsCount = flawsCount + "*";
	        } else {
	            displayFlawsCount = flawsCount > 0? Integer.toString(flawsCount) : "";
	        }
		} catch (IllegalArgumentException iae) {
		    displayFlawsCount = "";
		}

        return displayFlawsCount;
    }

    public int getFlawsCountInt(int severity) {
        return scanHistory.getFlawsCount(severity);
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
        return newFlaw > 0? Integer.toString(newFlaw) : "";
    }

    public List<Map<String, Long>> getFlawsCountHistory() {
        return scanHistory.getFlawsCountHistory();
    }

    public void doGraph(StaplerRequest request, StaplerResponse response) {
        try {
            BuildHistory buildHistory = new BuildHistory("Dynamic Vulnerabilities", scanHistory.getFlawsCountHistory());
            Collection<BuildHistory> buildHistoryList = new ArrayList<BuildHistory>();
            buildHistoryList.add(buildHistory);
            TrendChart trendChart = new TrendChart(System.currentTimeMillis(), 600, 400, buildHistoryList);
            trendChart.doPng(request, response);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to generate the Flaw trend graph.");
        }
    }

    /**
     * Get the URL to the Detailed Report for this scan that is escaped for HTML attribute
     *
     * @return Detailed Report URL escaped for HTML attribute
     */
    public String getDetailedReportURLForHTMLAttr() {
        String escapedAcctId = StringEscapeUtils.escapeHtml(scanHistory.getAccountId());
        String escapedAppId = StringEscapeUtils.escapeHtml(scanHistory.getAppId());
        String escapedBuildId = StringEscapeUtils.escapeHtml(scanHistory.getBuildId());
        return Constant.VIEW_REPORT_URI_PREFIX + ":" + escapedAcctId + ":" + escapedAppId + ":" + escapedBuildId;
    }

    public boolean isScanHistoryAvailable() {
        return null != scanHistory;
    }


    private void setBuild(Run<?,?> build) {
        this.build = build;
    }

    public Run<?,?> getBuild() {
        return build;
    }

    /**
     * Get if the each severity level contains flaws which affect policy compliance
     * @param severity - A severity level
     * @return policy affection
     */
    public boolean getPolicyAffection(int severity){
    	 boolean result = false;
         try {
             result = scanHistory.getPolicyAffection(severity);
         } catch (IllegalArgumentException iae) {
             result = false;
         } catch (NullPointerException ex){
        	 result = false;
         }
         return result;
    }

    /**
     * Used by index.jelly for DynamicResultsAction to display the policy compliance icon
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
}
