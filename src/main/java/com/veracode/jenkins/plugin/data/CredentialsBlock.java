package com.veracode.jenkins.plugin.data;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * The CredentialsBlock class corresponds to an "optionalBlock" jelly element
 * that represents user credentials data.
 *
 */
public final class CredentialsBlock {

    private final String _vid;
    private final String _vkey;
    private final String _vuser;
    private final String _vpassword;

    /**
     * Corresponds to the {@code vid} identifier referenced in a jelly file.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getVid() {
        if (_vid != null) {
            return this._vid;
        }
        return this._vuser;
    }

    /**
     * Corresponds to the {@code vkey} identifier referenced in a jelly file.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getVkey() {
        if (_vkey != null) {
            return this._vkey;
        }
        return this._vpassword;
    }

    /**
     * Constructor for CredentialsBlock.
     * <p>
     * Called by Jenkins with form data.
     *
     * @param vid  a {@link java.lang.String} object.
     * @param vkey a {@link java.lang.String} object.
     * @param vuser  a {@link java.lang.String} object.
     * @param vpassword a {@link java.lang.String} object.
     */
    @DataBoundConstructor
    public CredentialsBlock(String vid, String vkey, String vuser, String vpassword) {
        this._vid = vid;
        this._vkey = vkey;
        this._vuser = vuser;
        this._vpassword = vpassword;
    }
}