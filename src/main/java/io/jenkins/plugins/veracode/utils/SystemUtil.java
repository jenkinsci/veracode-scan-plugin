package io.jenkins.plugins.veracode.utils;

import io.jenkins.plugins.veracode.api.ACWebRequestHandler;

import com.veracode.http.WebClient;

import hudson.init.InitMilestone;
import hudson.init.Initializer;

public class SystemUtil {

	@Initializer(after=InitMilestone.PLUGINS_STARTED)
	public static void init() {
		WebClient.setWebRequestHandlerType(ACWebRequestHandler.class);
	}
}
