package com.veracode.jenkins.plugin.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.veracode.jenkins.plugin.utils.StringUtil;

/**
 * This XML-binded ScanHistory class represents the scan history for a Jenkins
 * current/past builds.
 *
 */
public class ScanHistory {

    public static final String BUILD_DATE = "BUILD_DATE";
    public static final String FLAWS_COUNT = "FLAWS_COUNT";

    private String accountId;
    private String appId;
    private String buildId;

    private String policyName;
    private String policyComplianceStatus;
    private int score;
    private String veracodeLevel;
    private boolean scanOverdue;

    private int[] flawsCount;
    private boolean[] mitigateFlag;
    private boolean[] policyaffect;
    private final int totalFlawsCount;
    private final int totalNewFlawsCount;
    private final int totalNetChangeCount;

    private int[] netChange;

    private final List<Map<String, Long>> flawsCountHistory;

    private final SCAScanHistory scaHistory;

    /**
     * Constructor for ScanHistory.
     *
     * @param accountId              a {@link java.lang.String} object.
     * @param appId                  a {@link java.lang.String} object.
     * @param buildId                a {@link java.lang.String} object.
     * @param policyName             a {@link java.lang.String} object.
     * @param policyComplianceStatus a {@link java.lang.String} object.
     * @param score                  a int.
     * @param veracodeLevel          a {@link java.lang.String} object.
     * @param scanOverdue            a boolean.
     * @param totalFlawsCount        a int.
     * @param flawsCount             an array of {@link int} objects.
     * @param mitigateFlag           an array of {@link boolean} objects.
     * @param netChange              an array of {@link int} objects.
     * @param flawsCountHistory      a {@link java.util.List} object.
     * @param scaHistory             a
     *                               {@link com.veracode.jenkins.plugin.data.SCAScanHistory}
     *                               object.
     * @param policyaffect           an array of {@link boolean} objects.
     */
    public ScanHistory(String accountId, String appId, String buildId, String policyName,
            String policyComplianceStatus, int score, String veracodeLevel, boolean scanOverdue,
            int totalFlawsCount, int[] flawsCount, boolean[] mitigateFlag, int[] netChange,
            List<Map<String, Long>> flawsCountHistory, SCAScanHistory scaHistory,
            boolean[] policyaffect) {

        if (StringUtil.isNullOrEmpty(policyName) || StringUtil.isNullOrEmpty(policyComplianceStatus)
                || StringUtil.isNullOrEmpty(veracodeLevel)) {
            throw new IllegalArgumentException(
                    "Missing required information to create a scan history.");
        }
        this.accountId = accountId;
        this.appId = appId;
        this.buildId = buildId;

        this.policyName = policyName;
        this.policyComplianceStatus = policyComplianceStatus;
        this.score = score;
        this.veracodeLevel = veracodeLevel;
        this.scanOverdue = scanOverdue;
        this.flawsCount = flawsCount.clone();
        this.mitigateFlag = mitigateFlag.clone();
        this.totalFlawsCount = totalFlawsCount;
        this.netChange = netChange.clone();

        int totalNetChangeCount = 0;
        int totalNewFlawsCount = 0;
        for (int thisCount : netChange) {
            totalNetChangeCount += thisCount;
            if (thisCount > 0) {
                totalNewFlawsCount += thisCount;
            }
        }
        this.totalNetChangeCount = totalNetChangeCount;
        this.totalNewFlawsCount = totalNewFlawsCount;

        this.flawsCountHistory = new ArrayList<>(flawsCountHistory);
        this.scaHistory = scaHistory;
        this.policyaffect = policyaffect.clone();
    }

    public String getPolicyName() {
        return policyName;
    }

    public String getPolicyComplianceStatus() {
        return policyComplianceStatus;
    }

    public int getScore() {
        return score;
    }

    public String getVeracodeLevel() {
        return veracodeLevel;
    }

    public boolean isScanOverdue() {
        return scanOverdue;
    }

    public int getFlawsCount(int severity) {
        if (severity < 0 || severity > 5) {
            throw new IllegalArgumentException(
                    "Invalid severity. Severity must be between 0 and 5");
        }
        return flawsCount[severity];
    }

    public boolean getMitigateFlag(int severity) {
        if (severity < 0 || severity > 5) {
            throw new IllegalArgumentException(
                    "Invalid severity. Severity must be between 0 and 5");
        }
        return mitigateFlag[severity];
    }

    public boolean getPolicyAffection(int severity) {
        if (severity < 0 || severity > 5) {
            throw new IllegalArgumentException(
                    "Invalid severity. Severity must be between 0 and 5");
        }
        return policyaffect[severity];
    }

    public int getTotalFlawsCount() {
        return this.totalFlawsCount;
    }

    public int getTotalNewFlawsCount() {
        return this.totalNewFlawsCount;
    }

    public int getTotalNetChangeCount() {
        return this.totalNetChangeCount;
    }

    public int getNewFlaws(int severity) {
        if (severity < 0 || severity > 5) {
            throw new IllegalArgumentException(
                    "Invalid severity. Severity must be between 0 and 5");
        }
        int netChange = getNetChange(severity);
        return Math.max(netChange, 0);
    }

    public int getNetChange(int severity) {
        if (severity < 0 || severity > 5) {
            throw new IllegalArgumentException(
                    "Invalid severity. Severity must be between 0 and 5");
        }
        return netChange[severity];
    }

    public List<Map<String, Long>> getFlawsCountHistory() {
        return flawsCountHistory;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getAppId() {
        return appId;
    }

    public String getBuildId() {
        return buildId;
    }

    public boolean hasSCAHistory() {
        return null != scaHistory;
    }

    public SCAScanHistory getScaHistory() {
        return scaHistory;
    }
}
