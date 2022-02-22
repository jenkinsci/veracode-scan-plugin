package com.veracode.jenkins.plugin.utils;

import javax.servlet.ServletException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.StaplerRequest;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.util.Secret;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ JSONObject.class, Secret.class })
public class EncryptionUtilTest {

	private static final String API_ID = "abcd";
	private static final String API_KEY = "1234";
	private static final String ENCRYPTED_ID = "32E2!#@ND";
	private static final String ENCRYPTED_KEY = "@^#HJKHUFE";

	@Test
	public void testEncrypt() throws ServletException {
		StaplerRequest staplerRequest = PowerMockito.mock(StaplerRequest.class);
		PowerMockito.mockStatic(Secret.class);
		Secret secretId = PowerMockito.mock(Secret.class);
		Secret secretKey = PowerMockito.mock(Secret.class);

		PowerMockito.when(Secret.fromString(API_ID)).thenReturn(secretId);
		PowerMockito.when(secretId.getEncryptedValue()).thenReturn(ENCRYPTED_ID);
		PowerMockito.when(Secret.fromString(API_KEY)).thenReturn(secretKey);
		PowerMockito.when(secretKey.getEncryptedValue()).thenReturn(ENCRYPTED_KEY);

		JSONObject jsonForm = new JSONObject(false);
		jsonForm.element("id", API_ID);
		jsonForm.element("key", API_KEY);
		JSONArray jsonArray = new JSONArray();
		jsonArray.element(jsonForm);
		JSONObject json = new JSONObject(false);
		json.element("json_array", jsonArray);
		PowerMockito.when(staplerRequest.getSubmittedForm()).thenReturn(json);
		EncryptionUtil.encrypt(staplerRequest, jsonForm);

		Assert.assertTrue(jsonForm.containsKey("id"));
		Assert.assertTrue(jsonForm.containsValue(ENCRYPTED_ID));
		Assert.assertTrue(jsonForm.containsKey("key"));
		Assert.assertTrue(jsonForm.containsValue(ENCRYPTED_KEY));
	}

	@Test
	public void testEncrypt_NullRequest() throws ServletException {
		PowerMockito.mockStatic(Secret.class);
		Secret secretId = PowerMockito.mock(Secret.class);
		Secret secretKey = PowerMockito.mock(Secret.class);
		Secret secretAction = PowerMockito.mock(Secret.class);
		PowerMockito.when(Secret.fromString("getAppList")).thenReturn(secretAction);
		PowerMockito.when(secretAction.getEncryptedValue()).thenReturn("getAppList");
		PowerMockito.when(Secret.fromString(API_ID)).thenReturn(secretId);
		PowerMockito.when(secretId.getEncryptedValue()).thenReturn(ENCRYPTED_ID);
		PowerMockito.when(Secret.fromString(API_KEY)).thenReturn(secretKey);
		PowerMockito.when(secretKey.getEncryptedValue()).thenReturn(ENCRYPTED_KEY);

		JSONObject action_json = new JSONObject(false);
		action_json.element("action", "getAppList");
		JSONArray jsonArray = new JSONArray();
		jsonArray.element(action_json);
		JSONObject jsonForm = new JSONObject(false);
		jsonForm.element("id", API_ID);
		jsonForm.element("key", API_KEY);
		jsonForm.element("json_form", jsonArray);
		EncryptionUtil.encrypt(null, jsonForm);

		Assert.assertTrue(jsonForm.containsKey("id"));
		Assert.assertTrue(jsonForm.containsValue(ENCRYPTED_ID));
		Assert.assertTrue(jsonForm.containsKey("key"));
		Assert.assertTrue(jsonForm.containsValue(ENCRYPTED_KEY));
	}
}