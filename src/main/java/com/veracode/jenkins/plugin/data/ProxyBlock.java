package com.veracode.jenkins.plugin.data;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * The ProxyBlock class corresponds to an "optionalBlock" jelly element that
 * represents proxy configuration data.
 *
 */
public final class ProxyBlock {

    private final String _phost;
    private final String _pport;
    private final String _puser;
    private final String _ppassword;

    /**
     * Corresponds to the {@code phost} identifier referenced in a jelly file.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPhost() {
        return this._phost;
    }

    /**
     * Corresponds to the {@code pport} identifier referenced in a jelly file.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPport() {
        return this._pport;
    }

    /**
     * Corresponds to the {@code puser} identifier referenced in a jelly file.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPuser() {
        return this._puser;
    }

    /**
     * Corresponds to the {@code ppassword} identifier referenced in a jelly file.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPpassword() {
        return this._ppassword;
    }

    
    /**
     * Called by Jenkins with form data.
     *
     * @param phost     a {@link java.lang.String} object.
     * @param pport     a {@link java.lang.String} object.
     * @param puser     a {@link java.lang.String} object.
     * @param ppassword a {@link java.lang.String} object.
     */
    @DataBoundConstructor
    public ProxyBlock(String phost, String pport, String puser, String ppassword) {
        this._phost = phost;
        this._pport = pport;
        this._puser = puser;
        this._ppassword = ppassword;
    }
}