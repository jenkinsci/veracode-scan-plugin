package com.veracode.jenkins.plugin.utils;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.veracode.jenkins.plugin.VeracodeNotifier;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.model.Jenkins;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FileUtil.class, Jenkins.JenkinsHolder.class, Jenkins.class, VeracodeNotifier.VeracodeDescriptor.class,
		FileInputStream.class, FilePath.class })
public class FileUtilTest {

	private static final String VERACODE_PROPERTIES_FILE_NAME = "veracode.properties";
	private static final String TEMP_DIRECTORY = "temp_directory";

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testDeleteDirectory() throws IOException {
		File tempDir = tempFolder.newFolder(TEMP_DIRECTORY);
		File tempFile = new File(tempDir, "temp_file");
		tempFile.createNewFile();
		FileUtil.deleteDirectory(tempDir);
		Assert.assertFalse("File exists", tempFile.exists());
		Assert.assertFalse("Directory exists", tempDir.exists());
	}

	@Test
	public void testGetStringFilePaths() throws IOException, InterruptedException {
		FilePath[] filePaths = new FilePath[10];
		for (int i = 0; i < 10; i++) {
			filePaths[i] = new FilePath(tempFolder.newFolder(TEMP_DIRECTORY + i));
		}
		String[] stringFilePaths = FileUtil.getStringFilePaths(filePaths);
		for (int x = 0; x < filePaths.length; x++) {
			Assert.assertEquals("File path is incorrect", stringFilePaths[x], filePaths[x].getRemote());
		}
	}

	@Test(expected = IOException.class)
	public void testGetStringFilePaths_IOException() throws Exception {
		FilePath[] filePaths = new FilePath[10];
		for (int i = 0; i < 10; i++) {
			filePaths[i] = new FilePath(tempFolder.newFolder(TEMP_DIRECTORY + i));
		}
		PowerMockito.whenNew(FileUtil.FileCallableImpl.class).withNoArguments().thenThrow(new IOException());
		FileUtil.getStringFilePaths(filePaths);
	}

	@Test(expected = InterruptedException.class)
	public void testGetStringFilePaths_InterruptedException() throws Exception {
		FilePath[] filePaths = new FilePath[10];
		for (int i = 0; i < 10; i++) {
			filePaths[i] = new FilePath(tempFolder.newFolder(TEMP_DIRECTORY + i));
		}
		PowerMockito.whenNew(FileUtil.FileCallableImpl.class).withNoArguments().thenThrow(new InterruptedException());
		FileUtil.getStringFilePaths(filePaths);
	}

	@Test
	public void testGetStringFilePath() throws IOException, InterruptedException {
		FilePath filePath = new FilePath(tempFolder.newFolder(TEMP_DIRECTORY));
		String stringFilePath = FileUtil.getStringFilePath(filePath);
		Assert.assertEquals("File path is incorrect", stringFilePath, filePath.getRemote());
	}

	@Test
	public void testCopyJarFiles() throws Exception {
		AbstractBuild<?, ?> build = Mockito.mock(AbstractBuild.class);
		PrintStream ps = PowerMockito.mock(PrintStream.class);
		Node node = PowerMockito.mock(Node.class);
		VirtualChannel virtualChannel = PowerMockito.mock(VirtualChannel.class);
		when(build.getBuiltOn()).thenReturn(node);
		when(node.getChannel()).thenReturn(virtualChannel);
		File localDir = tempFolder.newFolder("local_directory");
		File remoteDir = tempFolder.newFolder("remote_directory");
		File tempFile = new File(localDir, "vosp-api-wrappers-java.jar");
		tempFile.createNewFile();
		FilePath remoteFilePath = PowerMockito.spy(new FilePath(remoteDir));
		FilePath localFilePath = PowerMockito.spy(new FilePath(localDir));
		String jarName = remoteDir.getPath() + File.separator + "vosp-api-wrappers-java.jar";
		PowerMockito.whenNew(FilePath.class).withArguments(node.getChannel(), jarName).thenReturn(localFilePath);
		doNothing().when(localFilePath).copyToWithPermission(any());
		boolean isFileCopied = FileUtil.copyJarFiles(build, localFilePath, remoteFilePath, ps);
		Assert.assertTrue("Files are not copied", isFileCopied);
		Assert.assertFalse("Files are not copied", remoteFilePath.list().isEmpty());
	}

	@Test
	public void testCopyJarFiles_RunTimeException() throws Exception {
		AbstractBuild<?, ?> build = Mockito.mock(AbstractBuild.class);
		PrintStream ps = new PrintStream(System.out);
		PowerMockito.mockStatic(VeracodeNotifier.VeracodeDescriptor.class);
		VeracodeNotifier.VeracodeDescriptor veracodeDescriptor = PowerMockito
				.mock(VeracodeNotifier.VeracodeDescriptor.class);
		Jenkins jenkins = PowerMockito.mock(Jenkins.class);
		PowerMockito.mockStatic(Jenkins.class);
		PowerMockito.when(Jenkins.get()).thenReturn(jenkins);
		PowerMockito.when(jenkins.getDescriptor(VeracodeNotifier.class)).thenReturn(veracodeDescriptor);
		PowerMockito.when(veracodeDescriptor.getFailbuild()).thenReturn(true);
		boolean isCopied = FileUtil.copyJarFiles(build, null, null, ps);
		Assert.assertFalse("Expected Files are not copied", isCopied);
	}

	@Test
	public void testCleanUpBuildProperties() throws IOException {
		Run<?, ?> run = Mockito.mock(Run.class);
		TaskListener listener = Mockito.mock(TaskListener.class);
		File tempDir = tempFolder.newFolder("temp_directory");
		File tempFile = new File(tempDir.getParent(), "veracode.properties");
		tempFile.createNewFile();
		when(run.getRootDir()).thenReturn(tempDir);
		boolean fileDeleted = FileUtil.cleanUpBuildProperties(run, listener);
		Assert.assertTrue("File is not deleted", fileDeleted);
		Assert.assertFalse("property file is not deleted", tempFile.exists());
	}

	@Test
	public void testCleanUpBuildProperties_IOException() throws IOException {
		Run<?, ?> run = Mockito.mock(Run.class);
		TaskListener listener = Mockito.mock(TaskListener.class);
		PrintStream printStream = PowerMockito.mock(PrintStream.class);
		PowerMockito.mockStatic(Files.class);
		File tempDir = tempFolder.newFolder("temp_directoryE");
		File tempFile = new File(tempDir.getParent(), "veracode.properties");
		tempFile.createNewFile();
		when(run.getRootDir()).thenReturn(tempDir);
		when(listener.getLogger()).thenReturn(printStream);
		PowerMockito.doThrow(new IOException()).when(Files.class);
		Files.delete(any());
		boolean isFileDeleted = FileUtil.cleanUpBuildProperties(run, listener);
		Assert.assertFalse("File is deleted", isFileDeleted);
		Assert.assertTrue("File is deleted",
				Arrays.asList(Objects.requireNonNull(tempDir.getParentFile().list())).contains("veracode.properties"));
	}

	@Test
	public void testCreateBuildPropertiesFile() throws IOException {
		Run<?, ?> run = mock(Run.class);
		TaskListener listener = Mockito.mock(TaskListener.class);
		Properties properties = Mockito.mock(Properties.class);
		File tempDir = tempFolder.newFolder(TEMP_DIRECTORY);
		when(run.getRootDir()).thenReturn(tempDir);
		boolean fileCreated = FileUtil.createBuildPropertiesFile(run, properties, listener);
		File property_file_path = new File(tempDir.getParent() + File.separator + VERACODE_PROPERTIES_FILE_NAME);
		Assert.assertTrue("property file is not created", fileCreated);
		Assert.assertTrue("property file is not created", property_file_path.exists());
	}

	@Test
	public void testCreateBuildPropertiesFile_FileNotFoundException() throws Exception {
		Run<?, ?> run = mock(Run.class);
		TaskListener listener = Mockito.mock(TaskListener.class);
		Properties properties = Mockito.mock(Properties.class);
		PrintStream printStream = Mockito.mock(PrintStream.class);
		File tempDir = Mockito.spy(tempFolder.newFolder(TEMP_DIRECTORY));
		when(run.getRootDir()).thenReturn(tempDir);
		when(tempDir.getParent()).thenReturn("Error_tempDir");
		when(listener.getLogger()).thenReturn(printStream);
		boolean fileCreated = FileUtil.createBuildPropertiesFile(run, properties, listener);
		Assert.assertFalse("property file is created", fileCreated);
	}

	@Test
	public void testReadBuildPropertiesFile() throws IOException {
		Run<?, ?> run = mock(Run.class);
		TaskListener listener = Mockito.mock(TaskListener.class);
		File tempDir = tempFolder.newFolder(TEMP_DIRECTORY);
		File tempFile = new File(tempDir.getParent(), "veracode.properties");
		tempFile.createNewFile();
		OutputStream fileOutputStream = new FileOutputStream(tempFile.getPath());
		Properties props = new Properties();
		props.setProperty("db.url", "veracode.com");
		props.store(fileOutputStream, null);
		fileOutputStream.close();
		when(run.getRootDir()).thenReturn(tempDir);
		Properties actualProperties = FileUtil.readBuildPropertiesFile(run, listener);
		Assert.assertEquals("Property value is not read correctly", "veracode.com",
				actualProperties.getProperty("db.url"));
	}
}
