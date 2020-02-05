/*******************************************************************************
 * Copyright (c) 2014 Veracode, Inc. All rights observed.
 *
 * Available for use by Veracode customers as described in the accompanying license agreement.
 *
 * Send bug reports or enhancement requests to support@veracode.com.
 *
 * See the license agreement for conditions on submitted materials.
 ******************************************************************************/

package io.jenkins.plugins.veracode.data;

/**
 * Corresponds to an "optionalBlock" jelly element that represents proxy
 * configuration data.
 *
 *
 */
public final class ProxyBlock {
	private final String _phost;
	private final String _pport;
	private final String _puser;
	private final String _ppassword;

	/**
	 * Corresponds to the {@code phost} identifier referenced in a jelly file.
	 * @return String
	 */
	public String getPhost() {
		return this._phost;
	}
	/**
	 * Corresponds to the {@code pport} identifier referenced in a jelly file.
	 * @return String
	 */
	public String getPport() {
		return this._pport;
	}
	/**
	 * Corresponds to the {@code puser} identifier referenced in a jelly file.
	 * @return String
	 */
	public String getPuser() {
		return this._puser;
	}
	/**
	 * Corresponds to the {@code ppassword} identifier referenced in a jelly file.
	 * @return String
	 */
	public String getPpassword() {
		return this._ppassword;
	}

	/**
	 *
	 * Called by Jenkins with form data.
	 *
	 * {@link org.kohsuke.stapler.DataBoundConstructor DataBoundContructor}
	 *
	 * @param phost String
	 * @param pport String
	 * @param puser String
	 * @param ppassword String
	 */
	@org.kohsuke.stapler.DataBoundConstructor
	public ProxyBlock(String phost, String pport, String puser, String ppassword) {
		this._phost = phost;
		this._pport = pport;
		this._puser = puser;
		this._ppassword = ppassword;
	}
}