package com.veracode.jenkins.plugin.utils;

import static com.veracode.jenkins.plugin.args.AbstractArgs.STRING_TYPE_ARGS_COMMON;
import static com.veracode.jenkins.plugin.args.UploadAndScanArgs.STRING_TYPE_ARGS_UPLOADANDSCAN;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import com.veracode.jenkins.plugin.common.Constant;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.util.ArgumentListBuilder;

/**
 * The RemoteScanUtil is a utility class related to perfoming the scans in
 * remote location.
 *
 */
public final class RemoteScanUtil {

    /**
     * Returns the Veracode API Wrapper jar version in order to decide whether to
     * copy the latest.
     *
     * @param jarName a {@link java.lang.String} object.
     * @return a int.
     */
    public static int getJarVersion(String jarName) {
        String regex = ".*?vosp-api-wrappers-java-(.*?).jar";
        String temp = jarName.replaceAll(regex, "$1");
        String strVersion = temp.replaceAll("\\D", "");
        return Integer.parseInt(strVersion);
    }

    /**
     * Masks the sensitive data.
     *
     * @param remoteCmd a {@link java.util.List} object.
     * @return an array of {@link java.lang.Integer} objects.
     */
    public static Integer[] getMaskPosition(List<String> remoteCmd) {

        Integer[] maskPos;
        maskPos = new Integer[] {
                -1, -1, -1
        };

        int i = 0;
        for (String _cmd : remoteCmd) {
            if (_cmd.equals("-vkey"))
                maskPos[1] = i + 1;
            else if (_cmd.equals("-ppassword"))
                maskPos[2] = i + 1;
            i++;
        }

        return maskPos;
    }

    /**
     * Returns the path separator for Windows or Linux.
     *
     * @param remote a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getPathSeparator(String remote) {
        if (remote.length() > 3 && remote.charAt(1) == ':' && remote.charAt(2) == '\\')
            return "\\";
        else
            return "/";
    }

    /**
     * Returns the remote file path where Veracode API Wrapper is copied.
     *
     * @param build a {@link hudson.model.AbstractBuild} object.
     * @return a {@link hudson.FilePath} object.
     */
    public static FilePath getRemoteVeracodePath(AbstractBuild<?, ?> build) {
        Node node = build.getBuiltOn();
        if (node == null) {
            throw new RuntimeException("Cannot locate the build node.");
        }
        FilePath rootpath = node.getRootPath();
        if (rootpath == null) {
            throw new RuntimeException("Cannot retrieve the build node's root path.");
        }
        String remoteRootDir = rootpath.getRemote();
        String sep = getPathSeparator(remoteRootDir);
        String remoteDir = rootpath.getRemote() + sep + Constant.nodeJarFileDir;

        FilePath remoteVeracodeFilePath = new FilePath(node.getChannel(), remoteDir);
        return remoteVeracodeFilePath;
    }

    /**
     * Returns the remote file path where Veracode API Wrapper is copied.
     *
     * @param node a {@link hudson.model.Node} object.
     * @return a {@link hudson.FilePath} object.
     */
    public static FilePath getRemoteVeracodePath(Node node) {
        FilePath rootpath = node.getRootPath();
        if (rootpath == null) {
            throw new RuntimeException("Cannot retieve the node's root path.");
        }
        String remoteRootDir = rootpath.getRemote();
        String sep = getPathSeparator(remoteRootDir);
        String remoteDir = rootpath.getRemote() + sep + Constant.nodeJarFileDir;

        FilePath remoteVeracodeFilePath = new FilePath(node.getChannel(), remoteDir);
        return remoteVeracodeFilePath;
    }

    /**
     * Returns edited parameter values to avoid splitting if they contain spaces in
     * pipeline with remote scan.
     *
     * @param parameterValue a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String formatParameterValue(final String parameterValue) {
        if (parameterValue.startsWith("\"") && parameterValue.endsWith("\"")) {
            return parameterValue;
        } else {
            return (parameterValue.contains(" ") || parameterValue.contains("'"))
                    ? "\"" + parameterValue + "\""
                    : parameterValue;
        }
    }

    /**
     * Adds data passed from pipeline Groovy syntax to the command by escaping and
     * adding quotes for any strings.
     * 
     * @param command   a {@link hudson.util.ArgumentListBuilder} object.
     * @param arguments a {@link java.lang.String} object array.
     */
    public static void addArgumentsToCommand(ArgumentListBuilder command, String[] arguments) {
        List<Integer> stringDataIndexes = new ArrayList<>();
        int i = 0;
        for (String _cmd : arguments) {

            if (STRING_TYPE_ARGS_COMMON.contains(_cmd) || STRING_TYPE_ARGS_UPLOADANDSCAN.contains(_cmd)) {
                stringDataIndexes.add(i + 1);
            }

            if (stringDataIndexes.contains(i)) {
                command.addQuoted(StringEscapeUtils.escapeJava(_cmd));
            } else {
                command.add(_cmd);
            }

            i++;
        }
    }
}