package com.veracode.jenkins.plugin.utils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.veracode.apiwrapper.cli.VeracodeCommand;

import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.util.VersionNumber;
import jenkins.model.Jenkins;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Jenkins.class, VeracodeCommand.class })
public class UserAgentUtilTest {

	@Test
	public void testGetVersionDetails() {
		Jenkins jenkins = PowerMockito.mock(Jenkins.class);
		PowerMockito.mockStatic(Jenkins.class);
		PluginWrapper pluginWrapper = PowerMockito.mock(PluginWrapper.class);
		PluginManager pluginManager = PowerMockito.mock(PluginManager.class);
		VersionNumber versionNumber = new VersionNumber("2.277.3");
		PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
		Mockito.when(jenkins.getPluginManager()).thenReturn(pluginManager);
		Mockito.when(pluginManager.getPlugin("veracode-scan")).thenReturn(pluginWrapper);
		Mockito.when(pluginWrapper.getVersion()).thenReturn("1.0.0");
		PowerMockito.when(Jenkins.getVersion()).thenReturn(versionNumber);
		final String jreVersion = VeracodeCommand.getJreVersion();
		String versionDetails = UserAgentUtil.getVersionDetails();
		Assert.assertEquals("Version details are incorrect",
				"VeracodeScanJenkins/1.0.0 (Jenkins/2.277.3; Java/" + jreVersion + ")", versionDetails);
	}

	@Test
	public void testGetVersionDetailsWithExceptions() {
		Jenkins jenkins = PowerMockito.mock(Jenkins.class);
		PowerMockito.mockStatic(Jenkins.class);
		PowerMockito.mockStatic(VeracodeCommand.class);
		PluginManager pluginManager = PowerMockito.mock(PluginManager.class);
		PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
		PowerMockito.when(Jenkins.getVersion()).thenReturn(null);
		PowerMockito.when(VeracodeCommand.getJreVersion()).thenThrow(new RuntimeException());
		Mockito.when(jenkins.getPluginManager()).thenReturn(pluginManager);
		String versionDetails = UserAgentUtil.getVersionDetails();
		Assert.assertEquals("Version details are incorrect",
				"VeracodeScanJenkins/Unknown (Jenkins/Unknown; Java/Unknown)", versionDetails);
	}
}