package io.jenkins.plugins.veracode.utils;

import com.veracode.apiwrapper.AbstractAPIWrapper;
import com.veracode.apiwrapper.wrapper.cli.exceptions.ApiException;
import com.veracode.apiwrapper.wrappers.ResultsAPIWrapper;
import com.veracode.apiwrapper.wrappers.SandboxAPIWrapper;
import com.veracode.apiwrapper.wrappers.UploadAPIWrapper;
import io.jenkins.plugins.veracode.data.ProxyBlock;

/**
 * Helpers for using wrappers
 *
 */
public class WrapperUtil {
    /**
     * Setup the credential of a wrapper.
     *
     * @param wrapper - the target wrapper to set the credential
     * @param id - Veracode API ID
     * @param key - Veracode API key
     */
	public static final void setupCredential(AbstractAPIWrapper wrapper,
			String id, String key) {
		if (null == wrapper) {
			return;
		}

		wrapper.setUpApiCredentials(id, key);
	}

	/**
	 * Setup proxy settings of a wrapper
	 *
	 * @param wrapper - the target wrapper to set the proxy settings
	 * @param proxyInfo - the proxy settings
	 */
	public static final void setupProxy(AbstractAPIWrapper wrapper,
			ProxyBlock proxyInfo) {
		if (null == wrapper || null == proxyInfo) {
			return;
		}

		wrapper.setUpProxy(proxyInfo.getPhost(), proxyInfo.getPport(),
				proxyInfo.getPuser(), proxyInfo.getPpassword());
	}

	/**
	 * Get the latest build info of an application
	 *
	 * @param appName - The target application
	 * @param sandboxName - The name of the sandbox being used for this build. It could be null or empty.
	 * @param id - Veracode API ID
     * @param key - Veracode API key
	 * @param proxy - the proxy settings. Use null if no proxy is required
	 * @return The ID of the latest build of the given application
	 * @throws Exception when an error is encountered during the process
	 */
    public static final String getBuildInfo(final String appName, final String sandboxName, final String id, final String key,
            final ProxyBlock proxy) throws Exception {
        if (StringUtil.isNullOrEmpty(appName)) {
            throw new IllegalArgumentException("Application name is invalid.");
        }

        UploadAPIWrapper uploadApiWrapper = new UploadAPIWrapper();
        WrapperUtil.setupCredential(uploadApiWrapper, id, key);
        if (null != proxy) {
            WrapperUtil.setupProxy(uploadApiWrapper, proxy);
        }

        String appListXml = uploadApiWrapper.getAppList();
        String error = XmlUtil.getErrorString(appListXml);
        if (!StringUtil.isNullOrEmpty(error)) {
            throw new ApiException(error);
        }

        String appId = XmlUtil.parseAppId(appName, appListXml);
        if (StringUtil.isNullOrEmpty(appId)) {
            throw new ApiException(String.format("Cannot find the ID for application %s", appName));
        }

        String sandboxId = null;
        if (!StringUtil.isNullOrEmpty(sandboxName)) {
            String sandboxListXml = WrapperUtil.getSandboxList(appId, id, key, proxy);
            error = XmlUtil.getErrorString(sandboxListXml);
            if (!StringUtil.isNullOrEmpty(error)) {
                throw new ApiException(error);
            }
            sandboxId = XmlUtil.parseSandboxId(sandboxName, sandboxListXml);
            if (StringUtil.isNullOrEmpty(sandboxId)) {
                sandboxId = null;
            }
        }
        String buildInfoXml = uploadApiWrapper.getBuildInfo(appId, null, sandboxId);
        error = XmlUtil.getErrorString(buildInfoXml);
        if (!StringUtil.isNullOrEmpty(error)) {
            throw new ApiException(error);
        }

        return buildInfoXml;
    }

    /**
     * Get the build information for a given build id
     *
     * @param appId - The target application ID
     * @param buildId - The target build ID
	 * @param id - Veracode API ID
     * @param key - Veracode API key
	 * @param proxy - the proxy settings. Use null if no proxy is required
     * @return build info XML of the given application ID and build ID
     * @throws Exception when an error is encountered during the process
     */
    public static final String getBuildInfoByAppIdBuildId(final String appId, final String buildId, final String id, final String key,
			final ProxyBlock proxy) throws Exception {
		if (StringUtil.isNullOrEmpty(appId)) {
			throw new IllegalArgumentException("Application ID is invalid.");
		}

		if (StringUtil.isNullOrEmpty(buildId)) {
			throw new IllegalArgumentException("Build ID is invalid.");
		}

		UploadAPIWrapper uploadApiWrapper = new UploadAPIWrapper();
		WrapperUtil.setupCredential(uploadApiWrapper, id, key);
		if (null != proxy) {
			WrapperUtil.setupProxy(uploadApiWrapper, proxy);
		}

		String buildInfoXml = uploadApiWrapper.getBuildInfo(appId, buildId);
		String error = XmlUtil.getErrorString(buildInfoXml);
		if (!StringUtil.isNullOrEmpty(error)) {
			throw new ApiException(error);
		}

		return buildInfoXml;
	}

    /**
     * Get the detailed report of a given build (by ID)
     *
     * @param buildId - ID of a build
     * @param id - Veracode API ID
     * @param key - Veracode API key
     * @param proxy - the proxy settings. Use null if no proxy is required
     * @return The detailed report in XML
     * @throws Exception when an error is encountered during the process
     */
    public static final String getDetailedReport(final String buildId, final String id, final String key,
            final ProxyBlock proxy) throws Exception {
        if (StringUtil.isNullOrEmpty(buildId)) {
            throw new IllegalArgumentException("Build ID is invalid.");
        }

        ResultsAPIWrapper resultsApiWrapper = new ResultsAPIWrapper();
        WrapperUtil.setupCredential(resultsApiWrapper, id, key);
        if (null != proxy) {
            WrapperUtil.setupProxy(resultsApiWrapper, proxy);
        }

        String detailedReportXml = resultsApiWrapper.detailedReport(buildId);
        String error = XmlUtil.getErrorString(detailedReportXml);
        if (!StringUtil.isNullOrEmpty(error)) {
            throw new ApiException(error);
        }
        return detailedReportXml;
    }

    /**
     * Get the list of sandbox of a given application
     *
     * @param appId - ID of an application
     * @param id String
     * @param key String
     * @param proxy ProxyBlock
     * @return The sandbox list in XML
     * @throws Exception when an error is encountered during the process
     */
    public static final String getSandboxList(final String appId, final String id, final String key,
            final ProxyBlock proxy) throws Exception {
        if (StringUtil.isNullOrEmpty(appId)) {
            throw new IllegalArgumentException("Application ID is invalid.");
        }

        SandboxAPIWrapper sandboxApiWrapper = new SandboxAPIWrapper();
        WrapperUtil.setupCredential(sandboxApiWrapper, id, key);
        if (null != proxy) {
            WrapperUtil.setupProxy(sandboxApiWrapper, proxy);
        }

        return sandboxApiWrapper.getSandboxList(appId);
    }

	public static final String getSummaryReport(final String buildId, final String id, final String key,
			final ProxyBlock proxy) throws Exception {
		if (StringUtil.isNullOrEmpty(buildId)) {
			throw new IllegalArgumentException("Build ID is invalid.");
		}

		ResultsAPIWrapper resultsApiWrapper = new ResultsAPIWrapper();
		WrapperUtil.setupCredential(resultsApiWrapper, id, key);
		if (null != proxy) {
			WrapperUtil.setupProxy(resultsApiWrapper, proxy);
		}

		String summaryReport = resultsApiWrapper.summaryReport(buildId);
		String error = XmlUtil.getErrorString(summaryReport);
		if (!StringUtil.isNullOrEmpty(error)) {
			throw new ApiException(error);
		}
		return summaryReport;
	}
}