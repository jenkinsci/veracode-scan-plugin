package com.veracode.jenkins.plugin.utils;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.veracode.jenkins.plugin.args.GetAppListArgs;
import com.veracode.jenkins.plugin.data.ProxyBlock;

import hudson.util.FormValidation;
import hudson.util.Secret;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Secret.class, EncryptionUtil.class, GetAppListArgs.class })
public class FormValidationUtilTest {

	private static final String VID_DISPLAY_NAME = "API ID";
	private static final String VKEY_DISPLAY_NAME = "API Key";
	private static final String VID_I_HELP_TEXT_NAME = "id";
	private static final String VKEY_I_HELP_TEXT_NAME = "key";
	private static final String VID = "abcd";
	private static final String VKEY = "1234";

	@Test
	public void testCheckMutuallyInclusiveFields() {
		FormValidation formValidation = FormValidationUtil.checkMutuallyInclusiveFields("1234", "abcd",
				VKEY_DISPLAY_NAME, VID_DISPLAY_NAME, VID_I_HELP_TEXT_NAME);
		Assert.assertNull("Error in field validation", formValidation.getMessage());

		FormValidation emptyAPIKey = FormValidationUtil.checkMutuallyInclusiveFields("", VID, VKEY_DISPLAY_NAME,
				VID_DISPLAY_NAME, VID_I_HELP_TEXT_NAME);
		Assert.assertEquals("Error message for empty API key is invalid", emptyAPIKey.getMessage(),
				"API Key is required.");

		FormValidation emptyId = FormValidationUtil.checkMutuallyInclusiveFields(VKEY, "", VKEY_DISPLAY_NAME,
				VID_DISPLAY_NAME, VID_I_HELP_TEXT_NAME);
		Assert.assertEquals("Error message for empty API id is invalid", emptyId.getMessage(),
				"After entering your API Key, you must also enter your id in the API ID field.");

		FormValidation emptyIdWithWAPIId = FormValidationUtil.checkMutuallyInclusiveFields(VKEY, "", VKEY_DISPLAY_NAME,
				"vidDisplayName", VID_I_HELP_TEXT_NAME);
		Assert.assertEquals("Error message for empty vid display name is invalid", emptyIdWithWAPIId.getMessage(),
				"If API Key is provided, vidDisplayName must also be provided.");
	}

	@Test
	public void testCheckFields() {
		FormValidation formValidation = FormValidationUtil.checkFields(VID, VKEY, VID_DISPLAY_NAME, VKEY_DISPLAY_NAME,
				VKEY_I_HELP_TEXT_NAME);
		Assert.assertNull("", formValidation.getMessage());
		FormValidation emptyAPIKey = FormValidationUtil.checkFields("", VID, VKEY_DISPLAY_NAME, VID_DISPLAY_NAME,
				VID_I_HELP_TEXT_NAME);
		Assert.assertEquals("Error message for empty API key is invalid", emptyAPIKey.getMessage(),
				"API Key is required.");

		FormValidation emptyId = FormValidationUtil.checkFields(VKEY, "", VKEY_DISPLAY_NAME, VID_DISPLAY_NAME,
				VID_I_HELP_TEXT_NAME);
		Assert.assertEquals("Error message for empty API id is invalid", emptyId.getMessage(),
				"After entering your API Key, you must also enter your id in the API ID field.");

		FormValidation emptyIdWithWAPIId = FormValidationUtil.checkFields(VKEY, "", VKEY_DISPLAY_NAME, "vidDisplayName",
				VID_I_HELP_TEXT_NAME);
		Assert.assertEquals("Invalid Error Message", emptyIdWithWAPIId.getMessage(),
				"If API Key is provided, vidDisplayName must also be provided.");
	}

	@Test
	public void testCheckConnection_IDAndKeyFormat() {
		ProxyBlock proxyBlock = new ProxyBlock("testhost", "123", "user", "abc");
		FormValidation errorCredFormat = FormValidationUtil.checkConnection("asdcd", "key", proxyBlock);
		Assert.assertTrue("Error message for credentials format is invalid",
				errorCredFormat.getMessage().contains("The credentials are not in the correct format"));
	}

	@Test
	public void testCheckConnection_Exception() {
		PowerMockito.mockStatic(GetAppListArgs.class);
		PowerMockito.when(GetAppListArgs.newGetAppListArgs(anyBoolean(), any(), any(), any(), any())).thenThrow(new RuntimeException());
		FormValidation errorCredFormat = FormValidationUtil.checkConnection(VID, VKEY, null);
		Assert.assertTrue("Error message for connection exception is invalid",
				errorCredFormat.getMessage().contains("java.lang.RuntimeException"));
	}

	@Test
	public void testCheckAnalysisName() {
		FormValidation formValidation = FormValidationUtil.checkAnalysisName("analysis_name");
		Assert.assertNull("", formValidation.getMessage());
		FormValidation emptyAnalysisName = FormValidationUtil.checkAnalysisName("");
		Assert.assertEquals("Error message for empty Analysis name is invalid", "Analysis Name is required.",
				emptyAnalysisName.getMessage());
		String longAnalysisName = "longAnalysisNameeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
				+ "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
				+ "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";
		FormValidation nameMaxLenExceed = FormValidationUtil.checkAnalysisName(longAnalysisName);
		Assert.assertEquals("Error message for analysis name length exceed is invalid",
				"Enter an analysis name of 6-190 characters.", nameMaxLenExceed.getMessage());
		FormValidation nameTooShortError = FormValidationUtil.checkAnalysisName("a1");
		Assert.assertEquals("Error message for analysis name length too short is invalid",
				"Enter an analysis name of 6-190 characters.", nameTooShortError.getMessage());
	}

	@Test
	public void testCheckMaximumDuration() {
		FormValidation formValidation = FormValidationUtil.checkMaximumDuration("60");
		Assert.assertNull("Error in given Max Duration", formValidation.getMessage());

		FormValidation nullMaxDuration = FormValidationUtil.checkMaximumDuration("");
		Assert.assertNull("Error in given Max Duration", nullMaxDuration.getMessage());

		FormValidation invalidDuration = FormValidationUtil.checkMaximumDuration("error");
		Assert.assertEquals("error is not a valid number.", invalidDuration.getMessage());

		FormValidation durationExceedError = FormValidationUtil.checkMaximumDuration("0.5");
		Assert.assertEquals("Error message for invalid max duration is invalid", "0.5 is not a valid number.",
				durationExceedError.getMessage());

		FormValidation durationTooShortError = FormValidationUtil.checkMaximumDuration("601");
		Assert.assertEquals("Error message for invalid max duration is invalid",
				"Enter a maximum duration of 1-600 hours.", durationTooShortError.getMessage());
	}

	@Test
	public void testCheckWaitForResultsDuration_String() {
		FormValidation formValidation = FormValidationUtil.checkWaitForResultsDuration("60");
		Assert.assertNull(formValidation.getMessage());

		FormValidation formValidationWithNullInput = FormValidationUtil.checkWaitForResultsDuration("");
		Assert.assertNull(formValidationWithNullInput.getMessage());

		FormValidation invalidDuration = FormValidationUtil.checkWaitForResultsDuration("error");
		Assert.assertEquals("error is not a valid number.", invalidDuration.getMessage());

		FormValidation durationExceedError = FormValidationUtil.checkWaitForResultsDuration("-1");
		Assert.assertEquals("Enter a wait for results duration of up to 600 hours (25 days).",
				durationExceedError.getMessage());

		FormValidation durationTooShortError = FormValidationUtil.checkWaitForResultsDuration("601");
		Assert.assertEquals("Enter a wait for results duration of up to 600 hours (25 days).",
				durationTooShortError.getMessage());
	}

	@Test
	public void testCheckApiId() {
		FormValidation warning = FormValidationUtil.checkApiId(VID, VKEY, true);
		Assert.assertEquals("Invalid Message", warning.getMessage(),
				"These Veracode API credentials override the global Veracode API credentials.");

		FormValidation emptyAPIKey = FormValidationUtil.checkApiId(VID, "", true);
		Assert.assertEquals("Invalid Error Message", emptyAPIKey.getMessage(),
				"After entering your API ID, you must also enter your API Key in the API Key field.");

		FormValidation emptyId = FormValidationUtil.checkApiId("", VKEY, true);
		Assert.assertEquals("Invalid Error Message", emptyId.getMessage(), "API ID is required.");

		FormValidation useGlobalCred = FormValidationUtil.checkApiId("", "", true);
		Assert.assertNull("Error Validation", useGlobalCred.getMessage());

		FormValidation useLocalCred = FormValidationUtil.checkApiId(VID, VKEY, false);
		Assert.assertNull("Error Validation", useLocalCred.getMessage());
	}

	@Test
	public void testCheckApiKey() {
		FormValidation warning = FormValidationUtil.checkApiKey(VID, VKEY, true);
		Assert.assertEquals(warning.getMessage(),
				"These Veracode API credentials override the global Veracode API credentials.");

		FormValidation emptyAPIKey = FormValidationUtil.checkApiKey("", VKEY, true);
		Assert.assertEquals(emptyAPIKey.getMessage(),
				"After entering your API Key, you must also enter your API ID in the API ID field.");

		FormValidation useGlobalCred = FormValidationUtil.checkApiKey("", "", true);
		Assert.assertNull(useGlobalCred.getMessage());

		FormValidation useLocalCred = FormValidationUtil.checkApiKey(VID, VKEY, false);
		Assert.assertNull(useLocalCred.getMessage());
	}

	@Test
	public void testFormatTimeout() {
		String timeout = "20";
		String defaultTimeoutValue = "60";
		Secret secret = PowerMockito.mock(Secret.class);
		PowerMockito.mockStatic(Secret.class);
		PowerMockito.mockStatic(EncryptionUtil.class);
		String defaultTimeout = FormValidationUtil.formatTimeout(timeout);
		Assert.assertEquals("Issue in timeout value", defaultTimeoutValue, defaultTimeout);
		PowerMockito.when(Secret.fromString(Matchers.any())).thenReturn(secret);
		PowerMockito.when(EncryptionUtil.decrypt(timeout)).thenReturn(timeout);
		String actualTimeout = FormValidationUtil.formatTimeout(timeout);
		Assert.assertEquals("Issue in timeout value", timeout, actualTimeout);
	}
}