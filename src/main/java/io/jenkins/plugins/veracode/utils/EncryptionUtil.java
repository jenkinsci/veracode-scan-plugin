/*******************************************************************************
 * Copyright (c) 2014 Veracode, Inc. All rights observed.
 *
 * Available for use by Veracode customers as described in the accompanying license agreement.
 *
 * Send bug reports or enhancement requests to support@veracode.com.
 *
 * See the license agreement for conditions on submitted materials.
 ******************************************************************************/

package io.jenkins.plugins.veracode.utils;

import hudson.util.Secret;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

/**
 * A utility class for encrypting and decrypting plugin-related configuration
 * data.
 *
 *
 */
public final class EncryptionUtil {
	/**
	 * Encrypts plugin-related configuration data present in a
	 * {@link org.kohsuke.stapler.StaplerRequest StaplerRequest} and a
	 * {@link net.sf.json.JSONObject JSONObject} object before it is persisted
	 * to disk.
	 *
	 * @param req StaplerRequest
	 * @param formData JSONObject
	 * @throws ServletException exception
	 */
	public static void encrypt(StaplerRequest req, JSONObject formData) throws ServletException {
		try {
			if (req != null && formData != null) {
				encrypt(req.getSubmittedForm(), formData);
			}
		} finally {
			if (formData != null) {
				encrypt(formData);
			}
		}
	}

	/**
	 * Encrypts a JSON object's JSONObject elements that match another
	 * JSONObject.
	 * <p>
	 * If this method is not called with a "json" argument that represents an
	 * object with runtime type of JSONObject or JSONArray this method does
	 * nothing.
	 *
	 * @notes JSON=JSONArray, JSONObject, JSONNull. <br>
	 *        JSONArray=ordered sequence of values. <br>
	 *        JSONObject=unordered collection of name/value pairs. <br>
	 *        JSONNull=represents java-script null. <br>
	 *        values=boolean, number, string, JSONArray, JSONObject, JSONNull.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void encrypt(JSON json, JSONObject formData) {
		if (json instanceof JSONArray) {
			Iterator it = ((JSONArray)json).listIterator();
			while(it.hasNext()) {
				Object next = it.next();
				if (next instanceof JSON) {
					encrypt((JSON)next, formData);
				}
			}
		} else if (json instanceof JSONObject) {
			Iterator<Map.Entry<String, Object>> it = ((JSONObject)json).entrySet().iterator();
			while(it.hasNext()) {
				Map.Entry<String, Object> entry = it.next();
				if (entry != null) {
					String key = entry.getKey();
					Object o = ((JSONObject)json).get(key);
					if (o instanceof JSONObject && ((JSONObject)o).equals(formData)) {
						//expected key for the plugin: "com-veracode-jenkins-plugin-VeracodeNotifier"
						encrypt((JSONObject)o);
					} else if (o instanceof JSON) {
						encrypt((JSON)o, formData);
					}
				}
			}
		}
	}

	/**
	 * Encrypts a JSON object's String elements
	 * <p>
	 *
	 * @notes JSON=JSONArray, JSONObject, JSONNull. <br>
	 *       JSONArray=ordered sequence of values.<br>
	 *       JSONObject=unordered collection of name/value pairs.<br>
	 *       JSONNull=represents java-script null.<br>
	 *       values=boolean, number, string, JSONArray, JSONObject, JSONNull.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void encrypt(JSON json) {
		if (json instanceof JSONArray) {
			Iterator it = ((JSONArray)json).listIterator();
			while(it.hasNext()) {
				Object next = it.next();
				if (next instanceof JSON) {
					encrypt((JSON)next);
				}
			}
		} else if (json instanceof JSONObject) {
			Iterator<Map.Entry<String, Object>> it = ((JSONObject)json).entrySet().iterator();
			while(it.hasNext()) {
				Map.Entry<String, Object> entry = it.next();
				if (entry != null) {
					String key = entry.getKey();
					Object o = ((JSONObject)json).get(key);
					if (o instanceof String) {
						if (!"kind".equals(key) && !"stapler-class".equals(key) && !"$class".equals(key)) {
							//expected value for the plugin: "com.veracode.jenkins.plugin.VeracodeNotifier"
							((JSONObject)json).put(key, Secret.fromString((String)o).getEncryptedValue());
						}
					} else if (o instanceof JSON) {
						encrypt((JSON)o);
					}
				}
			}
		}
	}

	private EncryptionUtil(){}
}
