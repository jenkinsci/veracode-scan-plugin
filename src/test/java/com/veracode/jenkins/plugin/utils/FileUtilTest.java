package com.veracode.jenkins.plugin.utils;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileUtilTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testDeleteDirectory() throws IOException {
        File tempDir = tempFolder.newFolder("tempDir");
        File tempFile = new File(tempDir, "tempFile");
        tempFile.createNewFile();
        FileUtil.deleteDirectory(tempDir);
        Assert.assertFalse("File exists", tempFile.exists());
        Assert.assertFalse("Directory exists", tempDir.exists());
    }
}