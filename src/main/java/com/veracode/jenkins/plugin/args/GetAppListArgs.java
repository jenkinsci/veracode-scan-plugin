package com.veracode.jenkins.plugin.args;

import com.veracode.jenkins.plugin.data.ProxyBlock;

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
     * @param vid   a {@link java.lang.String} object.
     * @param vkey  a {@link java.lang.String} object.
     * @param proxy a {@link com.veracode.jenkins.plugin.data.ProxyBlock} object.
     * @return a {@link com.veracode.jenkins.plugin.args.GetAppListArgs} object.
     */
    public static GetAppListArgs newGetAppListArgs(String vid, String vkey, ProxyBlock proxy) {
        GetAppListArgs args = new GetAppListArgs();
        args.addApiCredentials(vid, vkey);

        if (proxy != null) {
            args.addProxyCredentials(proxy.getPuser(), proxy.getPpassword());
            args.addProxyConfiguration(proxy.getPhost(), proxy.getPport());
        }

        return args;
    }
}