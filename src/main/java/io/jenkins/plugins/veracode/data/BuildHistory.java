package io.jenkins.plugins.veracode.data;

import java.util.List;
import java.util.Map;

/**
 * This class represents the build history data for a particular scan type (Static, SCA, Dynamic Analysis)
 *
 */
public class BuildHistory {

    private String buildType;
    private final List<Map<String, Long>> buildList;

    public BuildHistory(String buildType, List<Map<String, Long>> buildList) {
        this.buildType = buildType;
        this.buildList = buildList;
    }

    public String getBuildType() {
    	return buildType;
    }

    public List<Map<String, Long>> getBuildList() {
    	return buildList;
    }
}
