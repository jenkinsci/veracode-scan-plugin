package com.veracode.jenkins.plugin.data;

import java.util.List;
import java.util.Map;

/**
 * The DAScanHistory class represents the Dynamic Analysis history for a Jenkins
 * current/past build.
 *
 */
public class DAScanHistory extends ScanHistory {

    /**
     * Constructor for DAScanHistory.
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
     * @param policyaffect           an array of {@link boolean} objects.
     */
    public DAScanHistory(String accountId, String appId, String buildId, String policyName,
            String policyComplianceStatus, int score, String veracodeLevel, boolean scanOverdue,
            int totalFlawsCount, int[] flawsCount, boolean[] mitigateFlag, int[] netChange,
            List<Map<String, Long>> flawsCountHistory, boolean[] policyaffect) {

        super(accountId, appId, buildId, policyName, policyComplianceStatus, score, veracodeLevel,
                scanOverdue, totalFlawsCount, flawsCount, mitigateFlag, netChange,
                flawsCountHistory, null, policyaffect);
    }

    /**
     * SCA scan history is not applicable for Dynamic Analysis.
     *
     * @return a boolean.
     */
    public boolean hasSCAHistory() {
        return false;
    }

    /**
     * SCA scan history is not applicable for Dynamic Analysis.
     *
     * @return a {@link com.veracode.jenkins.plugin.data.SCAScanHistory} object.
     */
    public SCAScanHistory getScaHistory() {
        throw new IllegalArgumentException(
                "SCA Scan History is not available for Dynamic Analysis.");
    }
}