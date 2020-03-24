package io.jenkins.plugins.veracode.api;

import com.veracode.http.WebRequestHandlerImpl;

import io.jenkins.plugins.veracode.utils.UserAgentUtil;

/**
 * The ACWebRequestHandler class enables the Veracode Java API wrapper to make
 * web requests to Veracode with a custom User-Agent header value.
 * <p>
 * Because the API wrapper class expects the fully qualified name of this class
 * to be "com.veracode.jenkins.plugin.api.ACWebRequestHandler", renaming or
 * moving this class to another package should be done with the understanding
 * that doing so will prevent the use of the custom User-Agent header value.
 * <p>
 * This class is not intended to be called from user code.
 *
 *
 */
public final class ACWebRequestHandler extends WebRequestHandlerImpl {

    @Override
    public String getDefaultUserAgentHeaderValue() {
        return UserAgentUtil.getVersionDetails();
    }

}