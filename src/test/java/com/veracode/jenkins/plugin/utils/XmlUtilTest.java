package com.veracode.jenkins.plugin.utils;

import java.util.Calendar;

import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.w3c.dom.Document;

import com.veracode.apiwrapper.dynamicanalysis.model.client.LinkedAppData;
import com.veracode.apiwrapper.dynamicanalysis.model.client.ScanOccurrenceInfo;
import com.veracode.apiwrapper.dynamicanalysis.model.client.ScanOccurrenceStatusInfo;
import com.veracode.jenkins.plugin.data.DAScanHistory;
import com.veracode.jenkins.plugin.data.SCAScanHistory;
import com.veracode.jenkins.plugin.data.ScanHistory;
import com.veracode.jenkins.plugin.testutils.XmlDocumentGenerator;

import hudson.model.Run;

public class XmlUtilTest {

	private static final String TEST_ACCT_ID = "12345";
	private static final String TEST_APP_ID = "123456";
	private static final String TEST_APP_NAME = "abcd'1234@!";
	private static final String TEST_BUILD_ID = "1234567";
	private static final String TEST_SANDBOX_NAME = "abc'123@!";
	private static final String TEST_SANDBOX_ID = "6789";
	private static final String SAMPLE_ERROR = "Sample error";

	@Test
	public void testNewScanHistory() throws Exception {
		Run<?, ?> run = PowerMockito.mock(Run.class);
		Calendar calendar = PowerMockito.mock(Calendar.class);
		PowerMockito.when(run.getTimestamp()).thenReturn(calendar);
		PowerMockito.when(calendar.getTimeInMillis()).thenReturn(1620666022149L);
		String buildInfoXml = XmlDocumentGenerator.getGetBuildInfoXmlDocument(TEST_ACCT_ID, TEST_APP_ID,
				TEST_SANDBOX_ID, TEST_BUILD_ID, null, null);
		String detailedReportXml = XmlDocumentGenerator.getDetailedReportXmlDocument(TEST_ACCT_ID, TEST_APP_ID,
				TEST_BUILD_ID);
		ScanHistory scanHistory = XmlUtil.newScanHistory(buildInfoXml, detailedReportXml, run);
		Assert.assertEquals("Scan history details account id is incorrect", TEST_ACCT_ID, scanHistory.getAccountId());
		Assert.assertEquals("Scan history details app id is incorrect", TEST_APP_ID, scanHistory.getAppId());
		Assert.assertEquals("Scan history details policy name is incorrect", "Veracode Transitional Very High",
				scanHistory.getPolicyName());
	}

	@Test
	public void testNewSCAHistory() throws Exception {
		Run<?, ?> run = PowerMockito.mock(Run.class);
		Calendar calendar = PowerMockito.mock(Calendar.class);
		SCAScanHistory scaScanHistory = PowerMockito.mock(SCAScanHistory.class);
		PowerMockito.when(run.getTimestamp()).thenReturn(calendar);
		PowerMockito.when(calendar.getTimeInMillis()).thenReturn(1620666022149L);
		String scaDetailedReport = XmlDocumentGenerator.getSCADetailedReportXmlDocument();
		SCAScanHistory scaHistory = XmlUtil.newSCAHistory(scaDetailedReport, 1620666022149L, scaScanHistory);
		Assert.assertTrue("Could not find SCA related document", scaHistory.isSubscribed());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNewSCAHistory_IllegalArgumentException() throws Exception {
		SCAScanHistory scaScanHistory = PowerMockito.mock(SCAScanHistory.class);
		XmlUtil.newSCAHistory(null, 1620666022149L, scaScanHistory);
	}

	@Test
	public void testNewDAScanHistory() throws Exception {
		Run run = PowerMockito.mock(Run.class);
		ScanOccurrenceInfo scanOccurrenceInfo = new ScanOccurrenceInfo(null, null, null, null, 4, 1, 2, 3, 31,
				new LinkedAppData(TEST_BUILD_ID, null, "resultImportRequestStatus"), null, null, null, null,
				new ScanOccurrenceStatusInfo(ScanOccurrenceStatusInfo.StatusTypeEnum.SUBMITTED));
		Calendar calendar = PowerMockito.mock(Calendar.class);
		PowerMockito.when(run.getTimestamp()).thenReturn(calendar);
		PowerMockito.when(calendar.getTimeInMillis()).thenReturn(1620666022149L);
		PowerMockito.when(run.getPreviousBuild()).thenReturn(run);
		String daDetailedReportXml = XmlDocumentGenerator.getDADetailedReportXmlDocument(TEST_ACCT_ID, TEST_APP_ID);
		DAScanHistory daHistory = XmlUtil.newDAScanHistory(daDetailedReportXml, scanOccurrenceInfo, run);
		Assert.assertEquals("Scan history details are incorrect", TEST_ACCT_ID, daHistory.getAccountId());
		Assert.assertEquals("Scan history details are incorrect", "Veracode Recommended High",
				daHistory.getPolicyName());
	}

	@Test
	public void testParseAppId() throws Exception {
		String appListXml = XmlDocumentGenerator.getGetAppListXmlDocument(TEST_APP_ID, TEST_APP_NAME);
		String appId = XmlUtil.parseAppId(TEST_APP_NAME, appListXml);
		Assert.assertEquals("Issue in collecting app id", TEST_APP_ID, appId);

	}

	@Test
	public void testParseSandboxId_ValidSandboxName() throws Exception {
		String sandboxListXml = XmlDocumentGenerator.getGetSandboxListXmlDocument(TEST_APP_ID, TEST_SANDBOX_ID,
				TEST_SANDBOX_NAME);
		final String sandboxId = XmlUtil.parseSandboxId(TEST_SANDBOX_NAME, sandboxListXml);
		Assert.assertEquals("Sandbox should be located", TEST_SANDBOX_ID, sandboxId);
	}

	@Test
	public void testParseSandboxId_InvalidSandboxName() throws Exception {
		String sandboxListXml = XmlDocumentGenerator.getGetSandboxListXmlDocument(TEST_APP_ID, TEST_SANDBOX_ID,
				TEST_SANDBOX_NAME);
		final String sandboxId = XmlUtil.parseSandboxId("xyz'456@!", sandboxListXml);
		Assert.assertEquals("Sandbox should not be available", "", sandboxId);
	}

	@Test
	public void testParseSandboxId_CaseInsensitiveSandboxName() throws Exception {
		String sandboxListXml = XmlDocumentGenerator.getGetSandboxListXmlDocument(TEST_APP_ID, TEST_SANDBOX_ID,
				TEST_SANDBOX_NAME);
		final String sandboxId = XmlUtil.parseSandboxId("AbC'123@!", sandboxListXml);
		Assert.assertEquals("Sandbox should be located", TEST_SANDBOX_ID, sandboxId);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseSandboxId_EmptySandboxListXml() throws Exception {
		XmlUtil.parseSandboxId("AbC'123@!", "");
	}

	@Test
	public void testGetXmlDocument() throws Exception {
		String appListXml = XmlDocumentGenerator.getGetAppListXmlDocument(TEST_APP_ID, TEST_APP_NAME);
		Document document = XmlUtil.getXmlDocument(appListXml);
		Assert.assertEquals("Issue in expected XML version", "1.0", document.getXmlVersion());
	}

	@Test
	public void testParseBuildId() throws Exception {
		String buildInfoXml = XmlDocumentGenerator.getGetBuildInfoXmlDocument(TEST_ACCT_ID, TEST_APP_ID,
				TEST_SANDBOX_ID, TEST_BUILD_ID, null, null);
		final String buildId = XmlUtil.parseBuildId(buildInfoXml);
		Assert.assertFalse("Incorrect Build ID", !buildId.equals(TEST_BUILD_ID));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseBuildId_IllegalArgumentException() throws Exception {
		XmlUtil.parseBuildId(null);
	}

	@Test
	public void testGetErrorString() {
		String errorXml = XmlDocumentGenerator.getErrorXmlDocument(SAMPLE_ERROR);
		String errorString = XmlUtil.getErrorString(errorXml);
		Assert.assertEquals("Issue in error element", SAMPLE_ERROR, errorString);
		String nullInput = XmlUtil.getErrorString(null);
		Assert.assertEquals("Issue in error element", "", nullInput);
	}
}