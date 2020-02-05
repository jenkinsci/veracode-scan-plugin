package io.jenkins.plugins.veracode.enums;

/**
 * Severity levels of flaws or vulnerability
 *
 */
public enum SeverityLevel {
    // NOTE: When adding new enum whose name containing multiple words, use
    //       underscore as separator so that the toString() method will work
    //       correctly
    INFORMATIONAL(0), VERY_LOW(1), LOW(2), MEDIUM(3), HIGH(4), VERY_HIGH(5);

    private final int severityLevel; // Level in the detailed report

    private SeverityLevel(int level) {
        severityLevel = level;
    }

    /**
     * Retrieve the Enum value corresponding to the given level. Level can be an integer between 0 and 5
     *
     * @param level - A severity level
     * @return Enum value corresponding to the given level
     * @throws IllegalArgumentException if level is out of range
     */
    public static SeverityLevel findSevLevel(int level) {
        SeverityLevel[] allLevels = SeverityLevel.values();
        if (level < 0 || level >= allLevels.length) {
            throw new IllegalArgumentException("Invalid severity level.");
        }

        SeverityLevel result = null;
        for (SeverityLevel sl : allLevels) {
            if (sl.getSevLevel() == level) {
                result = sl;
                break;
            }
        }
        return result;
    }

    public int getSevLevel() {
      return severityLevel;
    }

    @Override
    public String toString() {
        // Capitalize the first letter and replace underscore with space
        String s = super.toString();
        String result = s.substring(0, 1) + ((s.length() > 1)? s.substring(1).toLowerCase() : "");
        return result.replace('_', ' ');
    }
}
