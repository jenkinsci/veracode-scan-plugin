package io.jenkins.plugins.veracode.data;

import java.util.List;
import java.util.Map;

public class DAScanHistory extends ScanHistory {

	public DAScanHistory(String accountId, String appId, String buildId, String policyName,
			String policyComplianceStatus, int score, String veracodeLevel, boolean scanOverdue, int totalFlawsCount,
			int[] flawsCount, boolean[] mitigateFlag, int[] netChange, List<Map<String, Long>> flawsCountHistory,
			boolean[] policyaffect) {

		super(accountId, appId, buildId, policyName, policyComplianceStatus, score, veracodeLevel, scanOverdue, totalFlawsCount,
				flawsCount, mitigateFlag, netChange, flawsCountHistory, null, policyaffect);
	}

	// SCA is not applicable for Dynamic Analysis
    public boolean hasSCAHistory() {
        return false;
    }

    public SCAScanHistory getScaHistory() {
    	 throw new IllegalArgumentException("SCA Scan History is not available for Dynamic Analysis.");
    }

}
