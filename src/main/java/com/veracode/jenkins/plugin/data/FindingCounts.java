package com.veracode.jenkins.plugin.data;

import com.veracode.jenkins.plugin.enums.SeverityLevel;

/**
 * The FindingCounts class represents the counts and mitigated status of
 * findings at a severity level.
 *
 */
public class FindingCounts {

    private final SeverityLevel sevLevel;

    private final int count;
    private final int newCount;
    private final int netCount;
    private final boolean mitigated;

    /**
     * Constructor for FindingCounts.
     *
     * @param sevLevel  a {@link com.veracode.jenkins.plugin.enums.SeverityLevel}
     *                  object.
     * @param count     a int.
     * @param newCount  a int.
     * @param netCount  a int.
     * @param mitigated a boolean.
     */
    public FindingCounts(SeverityLevel sevLevel, int count, int newCount, int netCount,
            boolean mitigated) {
        this.sevLevel = sevLevel;
        this.count = count;
        this.newCount = newCount;
        this.netCount = netCount;
        this.mitigated = mitigated;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sevLevel == null) ? 0 : sevLevel.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FindingCounts other = (FindingCounts) obj;
        if (sevLevel != other.sevLevel)
            return false;
        return true;
    }

    public SeverityLevel getSevLevel() {
        return sevLevel;
    }

    public int getCount() {
        return count;
    }

    public int getNewCount() {
        return newCount;
    }

    public int getNetCount() {
        return netCount;
    }

    public boolean isMitigated() {
        return mitigated;
    }
}
