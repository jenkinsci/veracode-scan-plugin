/*******************************************************************************
 * Copyright (c) 2014 Veracode, Inc. All rights observed.
 *
 * Available for use by Veracode customers as described in the accompanying license agreement.
 *
 * Send bug reports or enhancement requests to support@veracode.com.
 *
 * See the license agreement for conditions on submitted materials.
 ******************************************************************************/

package io.jenkins.plugins.veracode.args;

import io.jenkins.plugins.veracode.data.ProxyBlock;

/**
 * Builds the command line argument passed to the Veracode API wrapper that
 * causes it to call the "getapplist.do" end point.
 *
 *
 */
public final class GetAppListArgs extends AbstractArgs {
	private GetAppListArgs() {
		addAction("GetAppList");
	}

	/**
	 * Returns a GetAppListArgs object initialized with the specified arguments.
	 *
	 * @param vid String
	 * @param vkey  String
	 * @param proxy  ProxyBlock
	 * @return GetAppListArgs
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