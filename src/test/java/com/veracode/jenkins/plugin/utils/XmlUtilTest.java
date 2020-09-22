package com.veracode.jenkins.plugin.utils;

import org.junit.Assert;
import org.junit.Test;

public class XmlUtilTest {

    private static final String TEST_ACCT_ID = "12345";
    private static final String TEST_APP_ID = "123456";
    private static final String TEST_BUILD_ID = "1234567";
    private static final String TEST_SANDBOX_NAME = "abc'123@!";
    private static final String TEST_SANDBOX_ID = "6789";
    
    private static final String SAMPLE_BUILD_INFO_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
            + "<buildinfo account_id=\"" + TEST_ACCT_ID + "\" app_id=\"" + TEST_APP_ID
            + "\" build_id=\"" + TEST_BUILD_ID + "\">" + "<build build_id=\"" + TEST_BUILD_ID
            + "\">" + "<analysis_unit status=\"Results Ready\"/>" + "</build>" + "</buildinfo>";
    
    private static final String SAMPLE_GETSANDBOXLIST_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
            + "<sandboxlist xmlns=\"https://analysiscenter.veracode.com/schema/4.0/sandboxlist\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" account_id=\""
            + TEST_ACCT_ID + "\" app_id=\"" + TEST_APP_ID
            + "\" sandboxlist_version=\"1.0\" xsi:schemaLocation=\"https://analysiscenter.veracode.com/schema/4.0/sandboxlist https://analysiscenter.veracode.com/resource/4.0/sandboxlist.xsd\">"
            + "<sandbox auto_recreate=\"false\" expires=\"2020-12-09T10:40:27-05:00\" last_modified=\"2020-09-16T03:48:07-04:00\" owner=\"OWNER\" sandbox_id=\""
            + TEST_SANDBOX_ID + "\" sandbox_name=\"" + TEST_SANDBOX_NAME + "\">"
            + "<customfield name=\"Custom 1\" value=\"\"/>"
            + "<customfield name=\"Custom 2\" value=\"\"/>"
            + "<customfield name=\"Custom 3\" value=\"\"/>"
            + "<customfield name=\"Custom 4\" value=\"\"/>"
            + "<customfield name=\"Custom 5\" value=\"\"/>" + "</sandbox>" + "</sandboxlist>";

    @Test
    public void testParseBuildId() throws Exception {
        final String buildId = XmlUtil.parseBuildId(SAMPLE_BUILD_INFO_XML);
        Assert.assertFalse("Incorrect Build ID", !buildId.equals(TEST_BUILD_ID));
    }

    @Test
    public void testParseSandboxIdForAvailableSandboxName() throws Exception {
        final String sandboxId = XmlUtil.parseSandboxId("abc'123@!", SAMPLE_GETSANDBOXLIST_XML);
        Assert.assertEquals("Sandbox should be located", TEST_SANDBOX_ID, sandboxId);
    }

    @Test
    public void testParseSandboxIdForUnavailableSandboxName() throws Exception {
        final String sandboxId = XmlUtil.parseSandboxId("xyz'456@!", SAMPLE_GETSANDBOXLIST_XML);
        Assert.assertEquals("Sandbox should not be available", "", sandboxId);
    }

    @Test
    public void testParseSandboxIdForCaseInsensitiveSandboxName() throws Exception {
        final String sandboxId = XmlUtil.parseSandboxId("AbC'123@!", SAMPLE_GETSANDBOXLIST_XML);
        Assert.assertEquals("Sandbox should be located", TEST_SANDBOX_ID, sandboxId);
    }
}