package com.veracode.jenkins.plugin.args;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.veracode.jenkins.plugin.utils.StringUtil;

import hudson.EnvVars;

/**
 * The AbstractArgs class contributes API-agnostic switches to Veracode API
 * wrapper command line arguments created by derived types.
 *
 */
public abstract class AbstractArgs {

    /**
     * The number of characters to use for masked arguments.
     */
    private static final byte MASKED_ARG_LENGTH = 8;
    private static final String API_ID_ENV_VAR = "VERACODE_API_KEY_ID";
    private static final String API_KEY_ENV_VAR = "VERACODE_API_KEY_SECRET";
    private static final String DELIMITER_COLON = ":";
    private static final String DELIMITER_AT = "@";
    private static final String PROTOCOL = "https://";
    private static final String HTTPS_PROXY_ENV_VAR = "https_proxy";

    protected static final String SWITCH = "-";
    protected static final String ACTION = SWITCH + "action";

    protected static final String PHOST = SWITCH + "phost";
    protected static final String PPASSWORD = SWITCH + "ppassword";
    protected static final String PPORT = SWITCH + "pport";
    protected static final String PUSER = SWITCH + "puser";

    protected static final String VID = SWITCH + "vid";
    protected static final String VKEY = SWITCH + "vkey";

    protected static final String USERAGENT = SWITCH + "useragent";

    /**
     * A list of command line arguments.
     */
    protected final List<String> list = new ArrayList<>();

    /**
     * Constructor for AbstractArgs.
     */
    protected AbstractArgs() {
    }

    /**
     * Adds the proxy configuration switches and arguments to the command line
     * arguments list.
     *
     * @param phost a {@link java.lang.String} object.
     * @param pport a {@link java.lang.String} object.
     */
    protected void addProxyConfiguration(String phost, String pport) {
        if (!StringUtil.isNullOrEmpty(phost)) {
            list.add(PHOST);
            list.add(phost);
        }
        if (!StringUtil.isNullOrEmpty(pport)) {
            list.add(PPORT);
            list.add(pport);
        }
    }

    /**
     * Adds the proxy credentials switches and arguments to the command line
     * arguments list.
     *
     * @param puser     a {@link java.lang.String} object.
     * @param ppassword a {@link java.lang.String} object.
     */
    protected void addProxyCredentials(String puser, String ppassword) {
        if (!StringUtil.isNullOrEmpty(puser)) {
            list.add(PUSER);
            list.add(puser);
        }

        if (!StringUtil.isNullOrEmpty(ppassword)) {
            list.add(PPASSWORD);
            list.add(ppassword);
        }
    }

    /**
     * Constructs proxy URL and adds it to the EnvVars if the build is happening in
     * a remote workspace. If the build is happening in the local workspace, then
     * adds the proxy detail switches and arguments to the command line arguments
     * list.
     * 
     * @param isRemote  a boolean.
     * @param envVars   a {@link hudson.EnvVars} object.
     * @param phost     a {@link java.lang.String} object.
     * @param pport     a {@link java.lang.String} object.
     * @param puser     a {@link java.lang.String} object.
     * @param ppassword a {@link java.lang.String} object.
     */
    protected void addProxyConfiguration(boolean isRemote, EnvVars envVars, String phost, String pport, String puser,
            String ppassword) {

        if (isRemote && envVars != null) {
            StringBuilder httpsProxy = new StringBuilder();
            httpsProxy.append(PROTOCOL);
            if (!StringUtil.isNullOrEmpty(puser)) {
                httpsProxy.append(encodeURLPart(puser));
                if (ppassword != null) {
                    httpsProxy.append(DELIMITER_COLON).append(encodeURLPart(ppassword));
                }
                httpsProxy.append(DELIMITER_AT);
            }
            httpsProxy.append(phost).append(DELIMITER_COLON).append(pport);
            envVars.put(HTTPS_PROXY_ENV_VAR, httpsProxy.toString());
            return;
        }

        addProxyCredentials(puser, ppassword);
        addProxyConfiguration(phost, pport);
    }

    /**
     * Given a String, return a URL encoded String using UTF_8.
     * 
     * Example: For String 'user@veracode.com', return 'user%40veracode.com'
     * 
     * @param str
     * @return
     */
    private static String encodeURLPart(String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Adds the Veracode API credentials to the EnvVars if the build is happening in
     * a remote workspace. If the build is happening in the local workspace, then
     * adds the Veracode API credentials switches and arguments to the command line
     * arguments list.
     * 
     * @param isRemote a boolean.
     * @param envVars  a {@link hudson.EnvVars} object.
     * @param vid      a {@link java.lang.String} object.
     * @param vkey     a {@link java.lang.String} object.
     */
    protected void addApiCredentials(boolean isRemote, EnvVars envVars, String vid, String vkey) {

        if (isRemote && envVars != null) {
            if (!StringUtil.isNullOrEmpty(vid)) {
                envVars.put(API_ID_ENV_VAR, vid);
            }
            if (!StringUtil.isNullOrEmpty(vkey)) {
                envVars.put(API_KEY_ENV_VAR, vkey);
            }
            return;
        }

        if (!StringUtil.isNullOrEmpty(vid)) {
            list.add(VID);
            list.add(vid);
        }

        if (!StringUtil.isNullOrEmpty(vkey)) {
            list.add(VKEY);
            list.add(vkey);
        }
    }

    /**
     * Adds the action switch and argument to the command line arguments list.
     *
     * @param action a {@link java.lang.String} object.
     */
    protected void addAction(String action) {
        if (!StringUtil.isNullOrEmpty(action)) {
            list.add(ACTION);
            list.add(action);
        }
    }

    /**
     * Returns a String that represents the plain text command line arguments added
     * to the command line arguments list.
     *
     * @return an array of {@link java.lang.String} objects.
     */
    public String[] getArguments() {
        return list.toArray(new String[0]);
    }

    /**
     * Returns a String that represents the plain text command line arguments added
     * to the command line arguments list, but replaces sensitive arguments with a
     * fixed-length sequence of '*' characters.
     * <p>
     * The length of the sequence of characters is defined by the
     * {@code MASKED_ARG_LENGTH} constant.
     *
     * @return an array of {@link java.lang.String} objects.
     */
    public String[] getMaskedArguments() {
        String[] args = getArguments();
        for (int x = 0; x < args.length; x++) {
            if (VKEY.equals(args[x]) || PPASSWORD.equals(args[x])) {
                if (x % 2 == 0 && x < args.length - 1 && args[x + 1] != null) {
                    args[x + 1] = StringUtil.repeatChar('*',
                            args[x + 1].length() > 0 ? MASKED_ARG_LENGTH : 0);
                }
            }
        }
        return args;
    }
}
