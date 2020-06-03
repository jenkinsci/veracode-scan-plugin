package com.veracode.jenkins.plugin.api;

import com.veracode.http.WebRequestHandlerImpl;
import com.veracode.jenkins.plugin.utils.UserAgentUtil;

/**
 * The ACWebRequestHandler class enables the Veracode Java API wrapper to make
 * web requests to Veracode with a custom User-Agent header value.
 * <p>
 * This class is not intended to be called from user code.
 *
 */
public final class ACWebRequestHandler extends WebRequestHandlerImpl {

    @Override
    public String getDefaultUserAgentHeaderValue() {
        return UserAgentUtil.getVersionDetails();
    }

}