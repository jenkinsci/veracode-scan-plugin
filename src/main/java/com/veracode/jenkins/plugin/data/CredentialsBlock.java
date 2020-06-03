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

    /**
     * Corresponds to the {@code vid} identifier referenced in a jelly file.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getVid() {
        return this._vid;
    }

    /**
     * Corresponds to the {@code vkey} identifier referenced in a jelly file.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getVkey() {
        return this._vkey;
    }

    /**
     * Constructor for CredentialsBlock.
     * <p>
     * Called by Jenkins with form data.
     *
     * @param vid  a {@link java.lang.String} object.
     * @param vkey a {@link java.lang.String} object.
     */
    @DataBoundConstructor
    public CredentialsBlock(String vid, String vkey) {
        this._vid = vid;
        this._vkey = vkey;
    }
}