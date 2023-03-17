package com.veracode.jenkins.plugin.args;

import com.veracode.jenkins.plugin.data.ProxyBlock;

import hudson.EnvVars;

/**
 * The GetAppListArgs class builds the command line argument passed to the
 * Veracode API wrapper that causes it to call the "getapplist.do" end point.
 *
 */
public final class GetAppListArgs extends AbstractArgs {

    /**
     * Constructor for GetAppListArgs.
     */
    private GetAppListArgs() {
        addAction("GetAppList");
    }

    /**
     * Returns a GetAppListArgs object initialized with the specified arguments.
     *
     * @param envVars  a {@link hudson.EnvVars} object.
     * @param vid      a {@link java.lang.String} object.
     * @param vkey     a {@link java.lang.String} object.
     * @param proxy    a {@link com.veracode.jenkins.plugin.data.ProxyBlock} object.
     * @param isRemote a boolean.
     * @return a {@link com.veracode.jenkins.plugin.args.GetAppListArgs} object.
     */
    public static GetAppListArgs newGetAppListArgs(boolean isRemote, EnvVars envVars, String vid, String vkey,
            ProxyBlock proxy) {
        GetAppListArgs args = new GetAppListArgs();
        args.addApiCredentials(isRemote, envVars, vid, vkey);

        if (proxy != null) {
            args.addProxyConfiguration(isRemote, envVars, proxy.getPhost(), proxy.getPport(), proxy.getPuser(),
                    proxy.getPpassword());
        }

        return args;
    }
}