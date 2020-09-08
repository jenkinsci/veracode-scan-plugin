package com.veracode.jenkins.plugin.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.veracode.apiwrapper.AbstractAPIWrapper;
import com.veracode.apiwrapper.cli.VeracodeCommand.VeracodeParser;
import com.veracode.apiwrapper.wrapper.cli.exceptions.ApiException;
import com.veracode.apiwrapper.wrappers.ResultsAPIWrapper;
import com.veracode.apiwrapper.wrappers.SandboxAPIWrapper;
import com.veracode.apiwrapper.wrappers.UploadAPIWrapper;
import com.veracode.http.Region;
import com.veracode.jenkins.plugin.args.GetRegionArgs;
import com.veracode.jenkins.plugin.common.Constant;
import com.veracode.jenkins.plugin.data.ProxyBlock;

/**
 * The WrapperUtil class contains the helpers for using wrappers.
 *
 */
public class WrapperUtil {

    /**
     * Setup the credential of a wrapper.
     *
     * @param wrapper a {@link com.veracode.apiwrapper.AbstractAPIWrapper} object -
     *                the target wrapper to set the credential.
     * @param id      a {@link java.lang.String} object - the Veracode API ID.
     * @param key     a {@link java.lang.String} object - the Veracode API key.
     */
    public static final void setupCredential(AbstractAPIWrapper wrapper, String id, String key) {
        if (null == wrapper) {
            return;
        }

        wrapper.setUpApiCredentials(id, key);
    }

    /**
     * Setup proxy settings of a wrapper.
     *
     * @param wrapper   a {@link com.veracode.apiwrapper.AbstractAPIWrapper} object
     *                  - the target wrapper to set the proxy settings.
     * @param proxyInfo a {@link com.veracode.jenkins.plugin.data.ProxyBlock} object
     *                  - the proxy settings.
     */
    public static final void setupProxy(AbstractAPIWrapper wrapper, ProxyBlock proxyInfo) {
        if (null == wrapper || null == proxyInfo) {
            return;
        }

        wrapper.setUpProxy(proxyInfo.getPhost(), proxyInfo.getPport(), proxyInfo.getPuser(),
                proxyInfo.getPpassword());
    }

    /**
     * Get the latest build info of an application.
     *
     * @param appName     a {@link java.lang.String} object - the target
     *                    application.
     * @param sandboxName a {@link java.lang.String} object - the name of the
     *                    sandbox being used for this build. It could be null or
     *                    empty.
     * @param id          a {@link java.lang.String} object - the Veracode API ID.
     * @param key         a {@link java.lang.String} object - the Veracode API key.
     * @param proxy       a {@link com.veracode.jenkins.plugin.data.ProxyBlock}
     *                    object - the proxy settings. Use null if no proxy is
     *                    required.
     * @return a {@link java.lang.String} object - the ID of the latest build of the
     *         given application.
     * @throws java.lang.Exception when an error is encountered during the process.
     */
    public static final String getBuildInfo(final String appName, final String sandboxName,
            final String id, final String key, final ProxyBlock proxy) throws Exception {
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
     * Get the build information for a given build id.
     *
     * @param appId   a {@link java.lang.String} object - the target application ID.
     * @param buildId a {@link java.lang.String} object - the target build ID.
     * @param id      a {@link java.lang.String} object - the Veracode API ID.
     * @param key     a {@link java.lang.String} object - the Veracode API key.
     * @param proxy   a {@link com.veracode.jenkins.plugin.data.ProxyBlock} object -
     *                the proxy settings. Use null if no proxy is required.
     * @return a {@link java.lang.String} object - build info XML of the given
     *         application ID and build ID.
     * @throws java.lang.Exception when an error is encountered during the process.
     */
    public static final String getBuildInfoByAppIdBuildId(final String appId, final String buildId,
            final String id, final String key, final ProxyBlock proxy) throws Exception {
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
     * Get the detailed report of a given build (by ID).
     *
     * @param buildId a {@link java.lang.String} object - the ID of a build.
     * @param id      a {@link java.lang.String} object - the Veracode API ID.
     * @param key     a {@link java.lang.String} object - the Veracode API key.
     * @param proxy   a {@link com.veracode.jenkins.plugin.data.ProxyBlock} object -
     *                the proxy settings. Use null if no proxy is required.
     * @return a {@link java.lang.String} object - the detailed report in XML.
     * @throws java.lang.Exception when an error is encountered during the process.
     */
    public static final String getDetailedReport(final String buildId, final String id,
            final String key, final ProxyBlock proxy) throws Exception {
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
     * Get the list of sandbox of a given application.
     *
     * @param appId a {@link java.lang.String} object - the ID of an application.
     * @param id    a {@link java.lang.String} object - the Veracode API ID.
     * @param key   a {@link java.lang.String} object - the Veracode API key.
     * @param proxy a {@link com.veracode.jenkins.plugin.data.ProxyBlock} object -
     *              the proxy settings. Use null if no proxy is required.
     * @return a {@link java.lang.String} object - the sandbox list in XML.
     * @throws java.lang.Exception when an error is encountered during the process.
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

    /**
     * Get the summary report of a given build (by ID).
     *
     * @param buildId a {@link java.lang.String} object - the ID of a build.
     * @param id      a {@link java.lang.String} object - the Veracode API ID.
     * @param key     a {@link java.lang.String} object - the Veracode API key.
     * @param proxy   a {@link com.veracode.jenkins.plugin.data.ProxyBlock} object -
     *                the proxy settings. Use null if no proxy is required.
     * @return a {@link java.lang.String} object - the summary report in XML.
     * @throws java.lang.Exception when an error is encountered during the process.
     */
    public static final String getSummaryReport(final String buildId, final String id,
            final String key, final ProxyBlock proxy) throws Exception {
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

    /**
     * Get the region of given credentials
     *
     * @param id    a {@link java.lang.String} object - the Veracode API ID.
     * @param key   a {@link java.lang.String} object - the Veracode API key.
     * @param proxy a {@link com.veracode.jenkins.plugin.data.ProxyBlock} object -
     *              the proxy settings. Use null if no proxy is required.
     * @return a {@link com.veracode.http.Region} object - the region info.
     * @throws java.lang.Exception when an error is encountered during the process.
     */
    public static final Region getRegion(final String id, final String key, final ProxyBlock proxy)
            throws Exception {

        if (StringUtil.isNullOrEmpty(id) || StringUtil.isNullOrEmpty(key)) {
            throw new IllegalArgumentException("Credentials provided are invalid.");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos, true, Constant.CHARACTER_ENCODING_UTF8)) {

            VeracodeParser parser = new VeracodeParser();
            parser.throwExceptions(true);
            parser.setOutputWriter(ps);
            parser.setErrorWriter(null);

            GetRegionArgs getRegionArgs = GetRegionArgs.newGetRegionArgs(id, key, proxy);

            int retcode = parser.parse(getRegionArgs.getArguments());

            if (retcode != 0) {
                throw new Exception("Cannot retrieve the region information.");
            }

            String output = baos.toString(Constant.CHARACTER_ENCODING_UTF8);
            return new Gson().fromJson(output, Region.class);
        } catch (JsonParseException e) {
            throw new JsonParseException("Cannot parse region information.");
        }
    }
}