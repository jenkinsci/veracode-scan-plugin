package com.veracode.jenkins.plugin.data;

/**
 * Corresponds to an "optionalBlock" jelly element that represents user
 * credentials data.
 *
 *
 */
public final class CredentialsBlock {

    private final String _vid;
    private final String _vkey;

    /**
     * Corresponds to the {@code vid} identifier referenced in a jelly file.
     *
     * @return String
     */
    public String getVid() {
        return this._vid;
    }

    /**
     * Corresponds to the {@code vkey} identifier referenced in a jelly file.
     *
     * @return String
     */
    public String getVkey() {
        return this._vkey;
    }

    /**
     * Called by Jenkins with form data.
     *
     * {@link org.kohsuke.stapler.DataBoundConstructor DataBoundContructor}
     *
     * @param vid  String
     * @param vkey String
     */
    @org.kohsuke.stapler.DataBoundConstructor
    public CredentialsBlock(String vid, String vkey) {
        this._vid = vid;
        this._vkey = vkey;
    }
}