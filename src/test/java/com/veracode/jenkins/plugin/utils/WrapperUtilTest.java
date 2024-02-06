package com.veracode.jenkins.plugin.utils;

import static org.mockito.ArgumentMatchers.any;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.veracode.apiwrapper.AbstractAPIWrapper;
import com.veracode.apiwrapper.wrapper.cli.exceptions.ApiException;
import com.veracode.apiwrapper.wrappers.ResultsAPIWrapper;
import com.veracode.apiwrapper.wrappers.SandboxAPIWrapper;
import com.veracode.apiwrapper.wrappers.UploadAPIWrapper;
import com.veracode.jenkins.plugin.data.ProxyBlock;
import com.veracode.jenkins.plugin.testutils.XmlDocumentGenerator;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ProxyBlock.class, ResultsAPIWrapper.class, WrapperUtil.class, SandboxAPIWrapper.class,
		UploadAPIWrapper.class, XmlUtil.class })
public class WrapperUtilTest {

	private static final String API_ID = "abcd";
	private static final String API_KEY = "1234";
	private static final String APP_ID = "10032";
	private static final String APP_NAME = "app123";
	private static final String BUILD_ID = "123456";
	private static final String BUILD_NAME = "build123";
	private static final String SANDBOX_ID = "10021";
	private static final String SANDBOX_NAME = "sandboxN123";
	private static final String POLICY_STATUS = "high";
	private static final String STATUS = "complete";
	private static final String ERROR_STRING = "sampleError";

	@Test
	public void testSetupCredential() {
		AbstractAPIWrapper abstractAPIWrapper = PowerMockito.mock(AbstractAPIWrapper.class);
		WrapperUtil.setupCredential(null, API_ID, API_KEY);
		Mockito.verify(abstractAPIWrapper, Mockito.times(0)).setUpApiCredentials(API_ID, API_KEY);
		WrapperUtil.setupCredential(abstractAPIWrapper, API_ID, API_KEY);
		Mockito.verify(abstractAPIWrapper, Mockito.times(1)).setUpApiCredentials(API_ID, API_KEY);
	}

	@Test
	public void testSetupProxy() {
		AbstractAPIWrapper abstractAPIWrapper = PowerMockito.mock(AbstractAPIWrapper.class);
		ProxyBlock proxyBlock = PowerMockito.mock(ProxyBlock.class);
		WrapperUtil.setupProxy(null, null);
		Mockito.verify(abstractAPIWrapper, Mockito.times(0)).setUpProxy(null, null, null, null);
		WrapperUtil.setupProxy(abstractAPIWrapper, proxyBlock);
		Mockito.verify(abstractAPIWrapper, Mockito.times(1)).setUpProxy(proxyBlock.getPhost(), proxyBlock.getPport(),
				proxyBlock.getPuser(), proxyBlock.getPpassword());
	}

	@Test
	public void testGetBuildInfo() throws Exception {
		String appListXml = XmlDocumentGenerator.getGetAppListXmlDocument(APP_ID, APP_NAME);
		String sandboxListXml = XmlDocumentGenerator.getGetSandboxListXmlDocument(APP_ID, SANDBOX_ID, SANDBOX_NAME);
		String buildInfoXml = XmlDocumentGenerator.getGetBuildInfoXmlDocument(null, APP_ID, SANDBOX_ID, BUILD_ID,
				BUILD_NAME, STATUS);

		ProxyBlock proxyBlock = PowerMockito.mock(ProxyBlock.class);
		UploadAPIWrapper uploadAPIWrapper = PowerMockito.mock(UploadAPIWrapper.class);
		SandboxAPIWrapper sandboxAPIWrapper = PowerMockito.mock(SandboxAPIWrapper.class);
		PowerMockito.whenNew(UploadAPIWrapper.class).withNoArguments().thenReturn(uploadAPIWrapper);
		PowerMockito.when(uploadAPIWrapper.getAppList()).thenReturn(appListXml);
		PowerMockito.whenNew(SandboxAPIWrapper.class).withNoArguments().thenReturn(sandboxAPIWrapper);
		PowerMockito.when(sandboxAPIWrapper.getSandboxList(any())).thenReturn(sandboxListXml);
		PowerMockito.when(uploadAPIWrapper.getBuildInfo(APP_ID, null, SANDBOX_ID)).thenReturn(buildInfoXml);

		String buildInfoWithoutProxy = WrapperUtil.getBuildInfo(APP_NAME, SANDBOX_NAME, API_ID, API_KEY, null);
		Assert.assertEquals("Errors in build info", buildInfoXml, buildInfoWithoutProxy);
		String buildInfo = WrapperUtil.getBuildInfo(APP_NAME, SANDBOX_NAME, API_ID, API_KEY, proxyBlock);
		Assert.assertEquals("Errors in build info", buildInfoXml, buildInfo);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetBuildInfo_EmptyAppName() throws Exception {
		WrapperUtil.getBuildInfo("", SANDBOX_NAME, API_ID, API_KEY, null);
	}

	@Test(expected = ApiException.class)
	public void testGetBuildInfo_XmlError() throws Exception {
		String errorXmlDocument = XmlDocumentGenerator.getErrorXmlDocument(ERROR_STRING);
		UploadAPIWrapper uploadAPIWrapper = PowerMockito.mock(UploadAPIWrapper.class);
		PowerMockito.whenNew(UploadAPIWrapper.class).withNoArguments().thenReturn(uploadAPIWrapper);
		PowerMockito.when(uploadAPIWrapper.getAppList()).thenReturn(errorXmlDocument);
		WrapperUtil.getBuildInfo(APP_NAME, SANDBOX_NAME, API_ID, API_KEY, null);
	}

	@Test(expected = ApiException.class)
	public void testGetBuildInfo_InvalidAppId() throws Exception {
		String appListXml = XmlDocumentGenerator.getGetAppListXmlDocument(APP_ID, APP_NAME);
		UploadAPIWrapper uploadAPIWrapper = PowerMockito.mock(UploadAPIWrapper.class);
		PowerMockito.whenNew(UploadAPIWrapper.class).withNoArguments().thenReturn(uploadAPIWrapper);
		PowerMockito.when(uploadAPIWrapper.getAppList()).thenReturn(appListXml);
		WrapperUtil.getBuildInfo("invalidAppName", SANDBOX_NAME, API_ID, API_KEY, null);
	}

	@Test(expected = ApiException.class)
	public void testGetBuildInfo_ErrorSandBoxXml() throws Exception {
		String appListXml = XmlDocumentGenerator.getGetAppListXmlDocument(APP_ID, APP_NAME);
		String buildInfoXml = XmlDocumentGenerator.getGetBuildInfoXmlDocument(null, APP_ID, SANDBOX_ID, BUILD_ID,
				BUILD_NAME, STATUS);
		String errorXml = XmlDocumentGenerator.getErrorXmlDocument(ERROR_STRING);

		UploadAPIWrapper uploadAPIWrapper = PowerMockito.mock(UploadAPIWrapper.class);
		SandboxAPIWrapper sandboxAPIWrapper = PowerMockito.mock(SandboxAPIWrapper.class);
		PowerMockito.whenNew(UploadAPIWrapper.class).withNoArguments().thenReturn(uploadAPIWrapper);
		PowerMockito.when(uploadAPIWrapper.getAppList()).thenReturn(appListXml);
		PowerMockito.whenNew(SandboxAPIWrapper.class).withNoArguments().thenReturn(sandboxAPIWrapper);
		PowerMockito.when(sandboxAPIWrapper.getSandboxList(any())).thenReturn(errorXml);
		PowerMockito.when(uploadAPIWrapper.getBuildInfo(APP_ID, null, SANDBOX_ID)).thenReturn(buildInfoXml);
		WrapperUtil.getBuildInfo(APP_NAME, SANDBOX_NAME, API_ID, API_KEY, null);
	}

	@Test(expected = ApiException.class)
	public void testGetBuildInfo_ErrorBuildInfoXml() throws Exception {
		String sandboxListXml = XmlDocumentGenerator.getGetSandboxListXmlDocument(APP_ID, SANDBOX_ID, SANDBOX_NAME);
		String appListXml = XmlDocumentGenerator.getGetAppListXmlDocument(APP_ID, APP_NAME);
		String errorXml = XmlDocumentGenerator.getErrorXmlDocument(ERROR_STRING);

		UploadAPIWrapper uploadAPIWrapper = PowerMockito.mock(UploadAPIWrapper.class);
		SandboxAPIWrapper sandboxAPIWrapper = PowerMockito.mock(SandboxAPIWrapper.class);
		PowerMockito.whenNew(UploadAPIWrapper.class).withNoArguments().thenReturn(uploadAPIWrapper);
		PowerMockito.when(uploadAPIWrapper.getAppList()).thenReturn(appListXml);
		PowerMockito.whenNew(SandboxAPIWrapper.class).withNoArguments().thenReturn(sandboxAPIWrapper);
		PowerMockito.when(sandboxAPIWrapper.getSandboxList(any())).thenReturn(sandboxListXml);
		PowerMockito.when(uploadAPIWrapper.getBuildInfo(APP_ID, null, null)).thenReturn(errorXml);
		WrapperUtil.getBuildInfo(APP_NAME, "invalidSandboxName", API_ID, API_KEY, null);
	}

	@Test
	public void testGetBuildInfoByAppIdBuildId() throws Exception {
		String buildInfoXml = XmlDocumentGenerator.getGetBuildInfoXmlDocument(null, APP_ID, SANDBOX_ID, BUILD_ID,
				BUILD_NAME, STATUS);
		ProxyBlock proxyBlock = PowerMockito.mock(ProxyBlock.class);
		UploadAPIWrapper uploadAPIWrapper = PowerMockito.mock(UploadAPIWrapper.class);
		PowerMockito.whenNew(UploadAPIWrapper.class).withNoArguments().thenReturn(uploadAPIWrapper);
		PowerMockito.when(uploadAPIWrapper.getBuildInfo(APP_ID, BUILD_ID)).thenReturn(buildInfoXml);
		String buildInfoWithoutProxy = WrapperUtil.getBuildInfoByAppIdBuildId(APP_ID, BUILD_ID, API_ID, API_KEY, null);
		Assert.assertEquals("Errors in build info xml", buildInfoXml, buildInfoWithoutProxy);
		String buildInfo = WrapperUtil.getBuildInfoByAppIdBuildId(APP_ID, BUILD_ID, API_ID, API_KEY, proxyBlock);
		Assert.assertEquals("Errors in build info xml", buildInfoXml, buildInfo);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetBuildInfoByAppIdBuildId_EmptyAppId() throws Exception {
		WrapperUtil.getBuildInfoByAppIdBuildId(null, BUILD_ID, API_ID, API_KEY, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetBuildInfoByAppIdBuildId_EmptyBuildId() throws Exception {
		WrapperUtil.getBuildInfoByAppIdBuildId(APP_ID, null, API_ID, API_KEY, null);
	}

	@Test(expected = ApiException.class)
	public void testGetBuildInfoByAppIdBuildId_ErrorXml() throws Exception {
		String errorXml = XmlDocumentGenerator.getErrorXmlDocument(ERROR_STRING);
		UploadAPIWrapper uploadAPIWrapper = PowerMockito.mock(UploadAPIWrapper.class);
		PowerMockito.whenNew(UploadAPIWrapper.class).withNoArguments().thenReturn(uploadAPIWrapper);
		PowerMockito.when(uploadAPIWrapper.getBuildInfo(APP_ID, BUILD_ID)).thenReturn(errorXml);
		WrapperUtil.getBuildInfoByAppIdBuildId(APP_ID, BUILD_ID, API_ID, API_KEY, null);
	}

	@Test
	public void testGetDetailedReport() throws Exception {
		String buildInfoXml = XmlDocumentGenerator.getGetBuildInfoXmlDocument(null, APP_ID, SANDBOX_ID, BUILD_ID,
				BUILD_NAME, STATUS);
		ProxyBlock proxyBlock = PowerMockito.mock(ProxyBlock.class);
		ResultsAPIWrapper resultsApiWrapper = PowerMockito.mock(ResultsAPIWrapper.class);
		PowerMockito.whenNew(ResultsAPIWrapper.class).withNoArguments().thenReturn(resultsApiWrapper);
		PowerMockito.when(resultsApiWrapper.detailedReport(any())).thenReturn(buildInfoXml);
		String buildInfo = WrapperUtil.getDetailedReport(BUILD_ID, API_ID, API_KEY, proxyBlock);
		Assert.assertEquals("Errors in buildInfo xml", buildInfoXml, buildInfo);
		String buildInfoWithOutProxy = WrapperUtil.getDetailedReport(BUILD_ID, API_ID, API_KEY, null);
		Assert.assertEquals("Errors in buildInfo xml", buildInfoXml, buildInfoWithOutProxy);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetDetailedReport_EmptyBuild() throws Exception {
		WrapperUtil.getDetailedReport("", API_ID, API_KEY, null);
	}

	@Test(expected = ApiException.class)
	public void testGetDetailedReport_ApiException() throws Exception {
		String errorXmlDocument = XmlDocumentGenerator.getErrorXmlDocument(ERROR_STRING);
		ProxyBlock proxyBlock = PowerMockito.mock(ProxyBlock.class);
		ResultsAPIWrapper resultsApiWrapper = PowerMockito.mock(ResultsAPIWrapper.class);
		PowerMockito.whenNew(ResultsAPIWrapper.class).withNoArguments().thenReturn(resultsApiWrapper);
		PowerMockito.when(resultsApiWrapper.detailedReport(any())).thenReturn(errorXmlDocument);
		WrapperUtil.getDetailedReport(BUILD_ID, API_ID, API_KEY, proxyBlock);
	}

	@Test
	public void testGetSandboxList() throws Exception {
		String sandboxListXml = XmlDocumentGenerator.getGetSandboxListXmlDocument(APP_ID, SANDBOX_ID, SANDBOX_NAME);
		ProxyBlock proxyBlock = PowerMockito.mock(ProxyBlock.class);
		SandboxAPIWrapper sandboxAPIWrapper = PowerMockito.mock(SandboxAPIWrapper.class);
		PowerMockito.whenNew(SandboxAPIWrapper.class).withNoArguments().thenReturn(sandboxAPIWrapper);
		PowerMockito.when(sandboxAPIWrapper.getSandboxList(any())).thenReturn(sandboxListXml);

		String sandboxList = WrapperUtil.getSandboxList(APP_ID, API_ID, API_KEY, proxyBlock);
		Assert.assertEquals("Errors in sandbox list", sandboxListXml, sandboxList);
		String sandboxListWithOutProxy = WrapperUtil.getSandboxList(APP_ID, API_ID, API_KEY, null);
		Assert.assertEquals("Errors in sandbox list", sandboxListXml, sandboxListWithOutProxy);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetSandboxList_IllegalArgumentException() throws Exception {
		WrapperUtil.getSandboxList(null, API_ID, API_KEY, null);
	}

	@Test
	public void testGetSummaryReport() throws Exception {
		String summaryReportXml = XmlDocumentGenerator.getSummaryReportXmlDocument(APP_ID, BUILD_ID, BUILD_NAME,
				POLICY_STATUS);
		ProxyBlock proxyBlock = PowerMockito.mock(ProxyBlock.class);
		ResultsAPIWrapper resultsAPIWrapper = PowerMockito.mock(ResultsAPIWrapper.class);
		PowerMockito.whenNew(ResultsAPIWrapper.class).withNoArguments().thenReturn(resultsAPIWrapper);
		PowerMockito.when(resultsAPIWrapper.summaryReport(any())).thenReturn(summaryReportXml);

		String summaryReport = WrapperUtil.getSummaryReport(BUILD_ID, API_ID, API_KEY, proxyBlock);
		Assert.assertEquals("Errors in summary report", summaryReportXml, summaryReport);
		String summaryReportWithProxy = WrapperUtil.getSummaryReport(BUILD_ID, API_ID, API_KEY, null);
		Assert.assertEquals("Errors in summary report", summaryReportXml, summaryReportWithProxy);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetSummaryReport_EmptyBuild() throws Exception {
		WrapperUtil.getSummaryReport(null, API_ID, API_KEY, null);
	}

	@Test(expected = ApiException.class)
	public void testGetSummaryReport_ApiException() throws Exception {
		String errorXml = XmlDocumentGenerator.getErrorXmlDocument(ERROR_STRING);
		ProxyBlock proxyBlock = PowerMockito.mock(ProxyBlock.class);
		ResultsAPIWrapper resultsAPIWrapper = PowerMockito.mock(ResultsAPIWrapper.class);
		PowerMockito.whenNew(ResultsAPIWrapper.class).withNoArguments().thenReturn(resultsAPIWrapper);
		PowerMockito.when(resultsAPIWrapper.summaryReport(any())).thenReturn(errorXml);
		WrapperUtil.getSummaryReport(BUILD_ID, API_ID, API_KEY, proxyBlock);
	}
}