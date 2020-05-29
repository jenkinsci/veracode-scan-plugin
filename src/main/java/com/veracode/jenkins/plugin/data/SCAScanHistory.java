package com.veracode.jenkins.plugin.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.veracode.jenkins.plugin.enums.SeverityLevel;

/**
 * This SCAScanHistory class represents the SCA Scan history for a Jenkins
 * current/past build.
 *
 */
public class SCAScanHistory {

    private final boolean subscribed;
    private final int blComponentsCount; // number of blacklisted components
    private final double maxCVSSScore;

    private final List<Map<String, Long>> vulCountHistory;

    private final int totalVulCount; // Total vulnerability count of this build across all severity
                                     // levels
    private final int totalNewVulCount; // Total new vulnerability count against last build across
                                        // all severity levels
    private final int totalNetVulCount; // Total delta of vulnerability count between this and last
                                        // build across all severity levels

    private final FindingCounts[] vulCounts; // An array to contain Vulnerability counts for each
                                             // severity level.
    private final Set<SCAComponent> scaComponents;

    /**
     * Constructor for SCAScanHistory.
     */
    public SCAScanHistory() {
        subscribed = false;
        blComponentsCount = 0;
        maxCVSSScore = 0;
        vulCounts = null;
        scaComponents = null;
        vulCountHistory = null;
        totalVulCount = 0;
        totalNewVulCount = 0;
        totalNetVulCount = 0;
    }

    /**
     * Constructor for SCAScanHistory.
     *
     * @param vulCountHistory a {@link java.util.List} object.
     */
    public SCAScanHistory(List<Map<String, Long>> vulCountHistory) {
        subscribed = false;
        blComponentsCount = 0;
        maxCVSSScore = 0;
        vulCounts = null;
        scaComponents = null;
        this.vulCountHistory = new ArrayList<Map<String, Long>>(vulCountHistory);
        totalVulCount = 0;
        totalNewVulCount = 0;
        totalNetVulCount = 0;
    }

    /**
     * Constructor for SCAScanHistory.
     *
     * @param maxCVSSScore      a double.
     * @param blComponentsCount a int.
     * @param vulCounts         a {@link java.util.Set} object.
     * @param scaComponents     a {@link java.util.Set} object.
     * @param vulCountHistory   a {@link java.util.List} object.
     */
    public SCAScanHistory(double maxCVSSScore, int blComponentsCount, Set<FindingCounts> vulCounts,
            Set<SCAComponent> scaComponents, List<Map<String, Long>> vulCountHistory) {
        this.subscribed = true;
        this.maxCVSSScore = maxCVSSScore;
        this.scaComponents = new HashSet<SCAComponent>(scaComponents);
        this.vulCounts = new FindingCounts[SeverityLevel.values().length];

        int totalVulCount = 0;
        int totalNewVulCount = 0;
        int totalNetVulCount = 0;
        for (FindingCounts vc : vulCounts) {
            if (null != vc && null == this.vulCounts[vc.getSevLevel().getSevLevel()]) {
                this.vulCounts[vc.getSevLevel().getSevLevel()] = vc;
                totalVulCount += vc.getCount();
                totalNewVulCount += vc.getNewCount();
                totalNetVulCount += vc.getNetCount();
            }
        }
        this.totalVulCount = totalVulCount;
        this.totalNewVulCount = totalNewVulCount;
        this.totalNetVulCount = totalNetVulCount;
        this.blComponentsCount = blComponentsCount;

        this.vulCountHistory = new ArrayList<Map<String, Long>>(vulCountHistory);
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public int getBlacklistedComponentsCount() {
        return blComponentsCount;
    }

    public double getMaxCVSSScore() {
        return maxCVSSScore;
    }

    public Set<SCAComponent> getSCAComponents() {
        return new HashSet<SCAComponent>(scaComponents);
    }

    public FindingCounts getCountBySeverity(SeverityLevel sevLevel) {
        if (null == sevLevel) {
            throw new IllegalArgumentException(
                    "Must provide a severity level to get the finding counts.");
        }

        return vulCounts[sevLevel.getSevLevel()];
    }

    public int getTotalVulCount() {
        return totalVulCount;
    }

    public int getTotalNewVulCount() {
        return totalNewVulCount;
    }

    public int getTotalNetVulCount() {
        return totalNetVulCount;
    }

    public List<Map<String, Long>> getVulCountHistory() {
        return vulCountHistory;
    }

    /**
     * Determine if there is any SCA vulnerability count in the history
     *
     * @return true if there is, false otherwise.
     */
    public boolean hasVulCountHistory() {
        if (null == vulCountHistory) {
            return false;
        }

        boolean foundCountHistory = false;
        for (Map<String, Long> thisCountHistory : vulCountHistory) {
            if (null != thisCountHistory && null != thisCountHistory.get(ScanHistory.FLAWS_COUNT)) {
                foundCountHistory = true;
                break;
            }
        }
        return foundCountHistory;
    }
}
