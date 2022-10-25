package com.veracode.jenkins.plugin.args;

import java.util.ArrayList;
import java.util.List;

import com.veracode.jenkins.plugin.utils.StringUtil;

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
     * Adds the Veracode API credentials switches and arguments to the command line
     * arguments list.
     *
     * @param vid  a {@link java.lang.String} object.
     * @param vkey a {@link java.lang.String} object.
     */
    protected void addApiCredentials(String vid, String vkey) {
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
