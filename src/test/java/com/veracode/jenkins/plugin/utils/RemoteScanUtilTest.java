package com.veracode.jenkins.plugin.utils;

import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Jenkins.class })
public class RemoteScanUtilTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testGetJarVersion() {
		int remoteScanUtil = RemoteScanUtil.getJarVersion("vosp-api-wrappers-java-1.jar");
		Assert.assertEquals("jarVersion mismatch", 1, remoteScanUtil);
	}

	@Test
	public void testGetMaskPosition() {
		List<String> remoteCmd = new ArrayList<String>();
		remoteCmd.add("-action");
		remoteCmd.add("getAppList");
		remoteCmd.add("-vkey");
		remoteCmd.add("1234");
		remoteCmd.add("-ppassword");
		remoteCmd.add("abcd");
		Integer[] markPositions = RemoteScanUtil.getMaskPosition(remoteCmd);

		Assert.assertEquals(new Integer(-1), markPositions[0]);
		Assert.assertEquals(new Integer(3), markPositions[1]);
		Assert.assertEquals(new Integer(5), markPositions[2]);
	}

	@Test
	public void testGetPathSeparator() throws URISyntaxException {
		String seperator1 = RemoteScanUtil.getPathSeparator("C:\\test");
		Assert.assertEquals("Did not pick the separator correctly", "\\", seperator1);

		String seperator2 = RemoteScanUtil.getPathSeparator("test");
		Assert.assertEquals("Did not pick the separator correctly", "/", seperator2);
	}

	@Test
	public void testGetRemoteVeracodePath_Build() throws Exception {
		AbstractBuild<?, ?> build = Mockito.mock(AbstractBuild.class);
		Node node = PowerMockito.mock(Node.class);
		VirtualChannel virtualChannel = PowerMockito.mock(VirtualChannel.class);
		File tempDir = tempFolder.newFolder("temp_directory");
		FilePath remotePath = new FilePath(tempDir);
		when(build.getBuiltOn()).thenReturn(node);
		when(node.getRootPath()).thenReturn(remotePath);
		when(node.getChannel()).thenReturn(virtualChannel);
		PowerMockito.whenNew(FilePath.class).withArguments(VirtualChannel.class, String.class).thenReturn(remotePath);
		FilePath filePath = RemoteScanUtil.getRemoteVeracodePath(build);
		Assert.assertEquals("Remote veracode path does not match", tempDir.getPath() + File.separator + "veracode-scan",
				filePath.getRemote());
	}

	@Test
	public void testGetRemoteVeracodePath_Node() throws IOException {
		Node node = PowerMockito.mock(Node.class);
		VirtualChannel virtualChannel = PowerMockito.mock(VirtualChannel.class);
		File tempDir = tempFolder.newFolder("temp_directory");
		FilePath remotePath = new FilePath(tempDir);
		when(node.getRootPath()).thenReturn(remotePath);
		when(node.getChannel()).thenReturn(virtualChannel);
		FilePath remoteVPath = RemoteScanUtil.getRemoteVeracodePath(node);
		Assert.assertEquals("Remote veracode path is not match", tempDir.getPath() + File.separator + "veracode-scan",
				remoteVPath.getRemote());
	}

	@Test(expected = RuntimeException.class)
	public void testGetRemoteVeracodePath_NullRootPath() {
		AbstractBuild<?, ?> build = Mockito.mock(AbstractBuild.class);
		Node node = PowerMockito.mock(Node.class);
		when(build.getBuiltOn()).thenReturn(node);
		RemoteScanUtil.getRemoteVeracodePath(build);
	}

	@Test(expected = RuntimeException.class)
	public void testGetRemoteVeracodePath_NullBuild() {
		AbstractBuild<?, ?> build = Mockito.mock(AbstractBuild.class);
		RemoteScanUtil.getRemoteVeracodePath(build);
	}

	@Test(expected = RuntimeException.class)
	public void testGetRemoteVeracodePath_NullNode() {
		Node node = PowerMockito.mock(Node.class);
		RemoteScanUtil.getRemoteVeracodePath(node);
	}

	@Test
	public void testFormatParameterValue() {
		String stringValue1 = RemoteScanUtil.formatParameterValue("\"temp 'directory\"");
		Assert.assertEquals("Remote veracode path is not match", "\"temp 'directory\"", stringValue1);

		String stringValue2 = RemoteScanUtil.formatParameterValue("temp directory");
		Assert.assertEquals("Remote veracode path is not match", "\"temp directory\"", stringValue2);

		String stringValue3 = RemoteScanUtil.formatParameterValue("temp'directory");
		Assert.assertEquals("Remote veracode path is not match", "\"temp'directory\"", stringValue3);
	}

	@Test
	public void testAddArgumentsToCommand_ArgsWithQuotes() {
		ArgumentListBuilder command = new ArgumentListBuilder();
		command.add("java").add("-jar").add("/devops/jenkins/agent/veracode-scan/VeracodeJavaAPI.jar");

		String[] args = { "-action", "UploadAndScan", "-vid", "Test \"API' ID", "-vkey", "Test \"API' Key", "-phost",
				"210.123\".23'.12", "-pport", "12\"3'4", "-puser", "Test \"Proxy' Username", "-ppassword",
				"Test \"Proxy' Password", "-appname", "Test \"Application' Name", "-createprofile", "true", "-teams",
				"Test \"Team' Name", "-criticality", "Very\"Hi'gh", "-sandboxname", "Test \"Sandbox' Name",
				"-createsandbox", "true", "-version", "Test \"Scan' Name", "-include",
				"Test \"Scan' Include Filenames Pattern", "-exclude", "Test \"Scan' Exclude Filenames Pattern",
				"-pattern", "Test \"Save As' Filename Pattern", "-replacement", "Test \"Save As' Replacement Pattern",
				"-deleteincompletescan", "2\"", "-maxretrycount", "5", "-debug", "-useragent", "Test User Agent" };

		RemoteScanUtil.addArgumentsToCommand(command, args);

		Assert.assertTrue(command.toList().contains("\"Test \\\"API' ID\""));
		Assert.assertTrue(command.toList().contains("\"Test \\\"API' Key\""));
		Assert.assertTrue(command.toList().contains("\"210.123\\\".23'.12\""));
		Assert.assertTrue(command.toList().contains("\"12\\\"3'4\""));
		Assert.assertTrue(command.toList().contains("\"Test \\\"Proxy' Username\""));
		Assert.assertTrue(command.toList().contains("\"Test \\\"Proxy' Password\""));
		Assert.assertTrue(command.toList().contains("\"Test \\\"Application' Name\""));
		Assert.assertTrue(command.toList().contains("\"Test \\\"Team' Name\""));
		Assert.assertTrue(command.toList().contains("\"Very\\\"Hi'gh\""));
		Assert.assertTrue(command.toList().contains("\"Test \\\"Sandbox' Name\""));
		Assert.assertTrue(command.toList().contains("\"Test \\\"Scan' Name\""));
		Assert.assertTrue(command.toList().contains("\"Test \\\"Scan' Include Filenames Pattern\""));
		Assert.assertTrue(command.toList().contains("\"Test \\\"Scan' Exclude Filenames Pattern\""));
		Assert.assertTrue(command.toList().contains("\"Test \\\"Save As' Filename Pattern\""));
		Assert.assertTrue(command.toList().contains("\"Test \\\"Save As' Replacement Pattern\""));
		Assert.assertTrue(command.toList().contains("\"2\\\"\""));
	}
}