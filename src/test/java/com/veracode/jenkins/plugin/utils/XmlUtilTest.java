package com.veracode.jenkins.plugin.utils;

import org.junit.Assert;
import org.junit.Test;

public class XmlUtilTest {

    private static final String TEST_ACCT_ID = "12345";
    private static final String TEST_APP_ID = "123456";
    private static final String TEST_BUILD_ID = "1234567";
    private static final String SAMPLE_BUILD_INFO_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
            + "<buildinfo account_id=\"" + TEST_ACCT_ID + "\" app_id=\"" + TEST_APP_ID
            + "\" build_id=\"" + TEST_BUILD_ID + "\">" + "<build build_id=\"" + TEST_BUILD_ID
            + "\">" + "<analysis_unit status=\"Results Ready\"/>" + "</build>" + "</buildinfo>";

    @Test
    public void testParseBuildId() throws Exception {
        final String buildId = XmlUtil.parseBuildId(SAMPLE_BUILD_INFO_XML);
        Assert.assertFalse("Incorrect Build ID", !buildId.equals(TEST_BUILD_ID));
    }
}