package io.jenkins.plugins.veracode.utils;

import java.util.List;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import io.jenkins.plugins.veracode.common.Constant;

public final class RemoteScanUtil {

    // getting the jar version so that we can decide to copy the latest
    public static int getJarVersion(String jarName) {
        String regex = ".*?vosp-api-wrappers-java-(.*?).jar";
        String temp = jarName.replaceAll(regex, "$1");
        String strVersion = temp.replaceAll("\\D", "");
        return Integer.parseInt(strVersion);
    }

    // need to mask the sensitive data
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

    // Getting the path separator windows or unix
    public static String getPathSeparator(String remote) {
        if (remote.length() > 3 && remote.charAt(1) == ':' && remote.charAt(2) == '\\')
            return "\\";
        else
            return "/";
    }

    // Getting the remote veracodepath where we will copy the wrapper
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

    // Getting the remote veracodepath where we will copy the wrapper
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
     * Edit parameter values to avoid splitting if they contain spaces in pipeline
     * with remote scan
     * 
     * @param parameterValue String
     * @return String formatted parameter values
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
}