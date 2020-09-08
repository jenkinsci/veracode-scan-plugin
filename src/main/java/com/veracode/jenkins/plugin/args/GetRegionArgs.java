package com.veracode.jenkins.plugin.args;

import com.veracode.jenkins.plugin.data.ProxyBlock;

/**
 * The GetRegionArgs class builds the command line argument passed to the
 * Veracode API wrapper that causes it to call the "getregion" action.
 *
 */
public final class GetRegionArgs extends AbstractArgs {

    /**
     * Constructor for GetRegionArgs.
     */
    private GetRegionArgs() {
        addAction("GetRegion");
    }

    /**
     * Returns a GetRegionArgs object initialized with the specified arguments.
     *
     * @param vid   a {@link java.lang.String} object.
     * @param vkey  a {@link java.lang.String} object.
     * @param proxy a {@link com.veracode.jenkins.plugin.data.ProxyBlock} object.
     * @return a {@link com.veracode.jenkins.plugin.args.GetRegionArgs} object.
     */
    public static GetRegionArgs newGetRegionArgs(String vid, String vkey, ProxyBlock proxy) {
        GetRegionArgs args = new GetRegionArgs();
        args.addApiCredentials(vid, vkey);

        if (proxy != null) {
            args.addProxyCredentials(proxy.getPuser(), proxy.getPpassword());
            args.addProxyConfiguration(proxy.getPhost(), proxy.getPport());
        }

        return args;
    }
}