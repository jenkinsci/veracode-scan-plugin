package com.veracode.jenkins.plugin.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Properties;

import org.jenkinsci.remoting.RoleChecker;

import com.veracode.apiwrapper.cli.VeracodeCommand;
import com.veracode.jenkins.plugin.VeracodeNotifier;
import com.veracode.jenkins.plugin.VeracodeNotifier.VeracodeDescriptor;
import com.veracode.jenkins.plugin.common.Constant;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.model.Jenkins;

/**
 * The FileUtil is a utility class for working with files and directories.
 *
 */
public final class FileUtil {

    private static final String VERACODE_PROPERTIES_FILE_NAME = "veracode.properties";

    /**
     * Deletes the file represented by the specified {@link java.io.File File}
     * object. If {@code file} represents a directory it also recursively deletes
     * its contents.
     *
     * @param file a {@link java.io.File} object.
     * @return a boolean.
     */
    public static boolean deleteDirectory(File file) {
        if (file.isDirectory()) {
            File[] list = file.listFiles();
            if (list != null) {
                for (File f : list) {
                    deleteDirectory(f);
                }
            }
        }
        return file.delete();
    }

    /**
     * Returns a String array whose elements correspond to the textual
     * representation of the file paths of the files represented by the elements of
     * the specified {@link hudson.FilePath FilePath} array.
     *
     * @param filePaths an array of {@link hudson.FilePath} objects.
     * @return an array of {@link java.lang.String} objects.
     * @throws java.io.IOException            if any.
     * @throws java.lang.InterruptedException if any.
     */
    public static String[] getStringFilePaths(FilePath[] filePaths)
            throws IOException, InterruptedException {
        String[] stringFilePaths = new String[filePaths.length];
        for (int x = 0; x < filePaths.length; x++) {
            try {
                stringFilePaths[x] = getStringFilePath(filePaths[x]);
            } catch (IOException ioe) {
                throw new IOException(
                        String.format("Could not locate the specified file: %s.", filePaths[x]),
                        ioe);
            } catch (InterruptedException ie) {
                throw new InterruptedException(
                        String.format("Could not locate the specified file: %s.", filePaths[x]));
            }
        }
        return stringFilePaths;
    }

    /**
     * Returns a String that corresponds to the textual representation of the file
     * path of the file represented by the specified {@link hudson.FilePath
     * FilePath} object.
     *
     * @param filePath a {@link hudson.FilePath} object.
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException            if any.
     * @throws java.lang.InterruptedException if any.
     */
    public static String getStringFilePath(FilePath filePath)
            throws IOException, InterruptedException {
        // because the FileCallable interface extends Serializable the
        // argument to the "act" method should not be an instance of a class
        // that contains an implicit reference to an instance of a
        // non-serializable class (don't use an anonymous inner class).
        return filePath.act(new FileCallableImpl());
    }

    /**
     * Implements {@link hudson.FilePath.FileCallable FileCallable}'s
     * {@link hudson.FilePath.FileCallable#invoke(File, VirtualChannel) invoke}
     * method, which is executed on the machine containing the file whose file path
     * is represented by the {@link hudson.FilePath FilePath} object on which the
     * {@link hudson.FilePath#act(FilePath.FileCallable) act} method is called.
     *
     */
    public static final class FileCallableImpl implements FilePath.FileCallable<String> {
        private static final long serialVersionUID = 1L;

        public String invoke(File f, VirtualChannel channel)
                throws IOException, InterruptedException {
            return f.getPath();
        }

        @Override
        public void checkRoles(RoleChecker arg0) throws SecurityException {
            // TODO Auto-generated method stub
        }
    }

    /**
     * Copies the Veracode API Wrapper to the remote location.
     *
     * @param build  a {@link hudson.model.AbstractBuild} object.
     * @param local  a {@link hudson.FilePath} object.
     * @param remote a {@link hudson.FilePath} object.
     * @param ps     a {@link java.io.PrintStream} object.
     * @return a boolean.
     * @throws java.lang.Exception if any.
     */
    public static boolean copyJarFiles(AbstractBuild<?, ?> build, FilePath local, FilePath remote,
            PrintStream ps) throws Exception {
        boolean bRet = false;
        try {
            Node node = build.getBuiltOn();
            if (node == null) {
                throw new RuntimeException("Cannot locate the build node.");
            }
            local.copyRecursiveTo(Constant.inclusive, null, remote);

            // now make a copy of the jar as 'VeracodeJavaAPI.jar' as the name
            // of the jarfile in the plugin
            // will change depending on the wrapper version it has been built
            // with

            FilePath[] files = remote.list(Constant.inclusive);
            String jarName = files[0].getRemote();
            FilePath oldJar = new FilePath(node.getChannel(), jarName);
            String newJarName = jarName.replaceAll(Constant.regex, Constant.execJarFile + "$2");
            FilePath newjarFilePath = new FilePath(node.getChannel(), newJarName);
            oldJar.copyToWithPermission(newjarFilePath);
            bRet = true;
        } catch (RuntimeException ex) {
            VeracodeDescriptor veracodeDescriptor = (VeracodeDescriptor) Jenkins.getInstance()
                    .getDescriptor(VeracodeNotifier.class);
            if (veracodeDescriptor != null && veracodeDescriptor.getFailbuild()) {
                ps.println("Failed to copy the jarfiles\n");
            }
        }
        return bRet;
    }

    /**
     * Returns the Veracode API Wrapper location situated in master.
     *
     * @return a {@link hudson.FilePath} object.
     * @throws java.net.URISyntaxException if any.
     */
    public static FilePath getLocalWorkspaceFilepath() throws URISyntaxException {
        File wrapperFile = new File(VeracodeCommand.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI().getPath());
        return new FilePath(wrapperFile.getParentFile());
    }

    /**
     * Deletes the properties file of the specified build.
     *
     * @param run      a {@link hudson.model.Run} object.
     * @param listener a {@link hudson.model.TaskListener} object.
     * @return a boolean.
     */
    public static boolean cleanUpBuildProperties(Run<?, ?> run, TaskListener listener) {
        File file = null;
        try {
            // run.getRootDir().getParent() is the builds directory
            file = new File(
                    run.getRootDir().getParent() + File.separator + VERACODE_PROPERTIES_FILE_NAME);
            if (file.exists()) {
                Files.delete(file.toPath());
            }
            return true;
        } catch (IOException e) {
            listener.getLogger().println(Constant.NEWLINE + Constant.NEWLINE + e.getMessage());
            return false;
        }
    }

    /**
     * Creates a properties file for the specified build with the given properties.
     *
     * @param run        a {@link hudson.model.Run} object.
     * @param properties a {@link java.util.Properties} object.
     * @param listener   a {@link hudson.model.TaskListener} object.
     * @return a boolean.
     * @throws java.io.IOException if any.
     */
    public static boolean createBuildPropertiesFile(Run<?, ?> run, Properties properties,
            TaskListener listener) throws IOException {
        File file = null;
        FileOutputStream fileOutputStream = null;
        try {
            file = new File(
                    run.getRootDir().getParent() + File.separator + VERACODE_PROPERTIES_FILE_NAME);
            fileOutputStream = new FileOutputStream(file);
            properties.store(fileOutputStream, "Veracode");
            return true;
        } catch (FileNotFoundException e) {
            listener.getLogger().println(Constant.NEWLINE + Constant.NEWLINE + e.getMessage());
            return false;
        } catch (IOException e) {
            listener.getLogger().println(Constant.NEWLINE + Constant.NEWLINE + e.getMessage());
            return false;
        } finally {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }
    }

    /**
     * Returns the properties of the specified build.
     *
     * @param run      a {@link hudson.model.Run} object.
     * @param listener a {@link hudson.model.TaskListener} object.
     * @return a {@link java.util.Properties} object.
     * @throws java.io.IOException if any.
     */
    public static Properties readBuildPropertiesFile(Run<?, ?> run, TaskListener listener)
            throws IOException {
        File file = null;
        FileInputStream fileInputStream = null;
        Properties properties = null;
        try {
            file = new File(
                    run.getRootDir().getParent() + File.separator + VERACODE_PROPERTIES_FILE_NAME);
            if (file.exists()) {
                fileInputStream = new FileInputStream(file);
                properties = new Properties();
                properties.load(fileInputStream);
            }
        } catch (FileNotFoundException e) {
            listener.getLogger().print(Constant.NEWLINE + Constant.NEWLINE + e.getMessage());
        } catch (IOException e) {
            listener.getLogger().print(Constant.NEWLINE + Constant.NEWLINE + e.getMessage());
        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            cleanUpBuildProperties(run, listener);
        }
        return properties;
    }

    /**
     * Constructor for FileUtil.
     */
    private FileUtil() {
    }
}