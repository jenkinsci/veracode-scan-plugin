package io.jenkins.plugins.veracode.common;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.w3c.dom.Node;

import com.veracode.apiwrapper.dynamicanalysis.model.client.AnalysisInfo;
import com.veracode.apiwrapper.dynamicanalysis.model.client.AnalysisOccurrenceInfo;
import com.veracode.apiwrapper.dynamicanalysis.model.client.AnalysisOccurrenceStatusInfo.StatusTypeEnum;
import com.veracode.apiwrapper.dynamicanalysis.model.client.ScanOccurrenceInfo;
import com.veracode.apiwrapper.exceptions.ApiException;
import com.veracode.apiwrapper.services.APIServiceManager;
import com.veracode.apiwrapper.services.DynamicAnalysisAPIService;
import com.veracode.parser.enums.CredentialTypes;
import com.veracode.parser.util.XmlUtils;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.veracode.DynamicAnalysisResultsAction;
import io.jenkins.plugins.veracode.data.DAScanHistory;
import io.jenkins.plugins.veracode.data.ProxyBlock;
import io.jenkins.plugins.veracode.utils.FileUtil;
import io.jenkins.plugins.veracode.utils.FormValidationUtil;
import io.jenkins.plugins.veracode.utils.StringUtil;
import io.jenkins.plugins.veracode.utils.WrapperUtil;
import io.jenkins.plugins.veracode.utils.XmlUtil;

public class DAAdapterService {

    private static final String PARAM_DA_ANALYSIS_NAME = "DA_ANALYSIS_NAME";
    private static final String PARAM_DA_PREVIOUS_OCCURRENCE_ID = "DA_PREVIOUS_OCCURRENCE_ID";
    private static final short GET_DA_SLEEP_TIME_MINUTES = 5;
    private static final short MAX_ALLOWED_CONSECUTIVE_API_EXCEPTIONS = 5;

    /**
     * Resubmit Veracode Dynamic Analysis - A common method for both Freestyle and
     * Pipeline
     * 
     * @param run                   Run
     * @param workspace             FilePath
     * @param listener              TaskListener
     * @param analysisName          String
     * @param maximumDuration       int
     * @param failBuildAsScanFailed boolean
     * @param apiID                 String
     * @param apiKey                String
     * @param debugEnabled          boolean
     * @param proxyBlock            ProxyBlock
     * @return boolean
     */
    public boolean resubmitDynamicAnalysis(Run<?, ?> run, FilePath workspace, TaskListener listener,
            final String analysisName, final int maximumDuration,
            final boolean failBuildAsScanFailed, final String apiID, final String apiKey,
            final boolean debugEnabled, final ProxyBlock proxyBlock) {

        try {
            log(listener, Constant.STARTING_POST_BUILD_ACTION_LOG,
                    Constant.POST_BUILD_ACTION_DISPLAY_TEXT_RESUBMIT);

            // Display HPI location if debug enabled
            if (debugEnabled) {
                log(listener, "[Debug mode is on]");
                showHPILocation(listener);
            }

            // Clean up previous build properties
            if (!FileUtil.cleanUpBuildProperties(run, listener)) {
                log(listener, "Failed to clean up previous build properties.");
                return !failBuildAsScanFailed;
            }

            // Display user inputs
            log(listener,
                    "Project: %s" + Constant.NEWLINE + "Dynamic Analysis name: %s"
                            + Constant.NEWLINE + "Maximum duration (in hours): %s"
                            + Constant.NEWLINE + "Fail the build if the analysis fails: %s"
                            + Constant.NEWLINE + "Use proxy: %s",
                    workspace, analysisName, maximumDuration, failBuildAsScanFailed,
                    String.valueOf(proxyBlock != null));

            // Validate user inputs
            if (!validateUserInputsForResubmit(apiID, apiKey, analysisName, maximumDuration,
                    listener)) {
                return !failBuildAsScanFailed;
            }

            // Setup proxy
            Proxy proxy = setupProxy(proxyBlock, listener);
            if (proxy == null) {
                return !failBuildAsScanFailed;
            }

            DynamicAnalysisAPIService daApiService = APIServiceManager
                    .createInstance(CredentialTypes.API, apiID, apiKey, proxy)
                    .getDynamicAnalysisAPIService();

            // Get analysis information for the specified analysis name
            AnalysisInfo analysisInfo = getAnalysisInfo(daApiService, analysisName, listener);
            if (analysisInfo == null || StringUtil.isNullOrEmpty(analysisInfo.getAnalysisId())) {
                logWithTimeStamp(listener,
                        "Resubmit failed. Preconfigured dynamic analysis not found.");
                return !failBuildAsScanFailed;
            }

            // Resubmit the scan
            if (!resubmitAnalysis(daApiService, analysisInfo, maximumDuration, listener)) {
                return !failBuildAsScanFailed;
            }

            // Store the job properties to be used in the review results step
            Properties properties = new Properties();
            properties.setProperty(PARAM_DA_ANALYSIS_NAME, analysisInfo.getAnalysisName());
            if (!StringUtil.isNullOrEmpty(analysisInfo.getAnalysisOccurrenceId())) {
                // This is needed in order to not to check the analysis status of the previous
                // occurrence in DynamicAnalysisResultsNotifier.
                // So the previous occurrence id should be stored.
                properties.setProperty(PARAM_DA_PREVIOUS_OCCURRENCE_ID,
                        analysisInfo.getAnalysisOccurrenceId());
            }
            FileUtil.createBuildPropertiesFile(run, properties, listener);

            log(listener, Constant.FINISHED_POST_BUILD_ACTION_LOG,
                    Constant.POST_BUILD_ACTION_DISPLAY_TEXT_RESUBMIT);

            return true;

        } catch (Exception e) {
            logWithTimeStamp(listener,
                    "Resubmit failed. Unexpected error occurred: %s" + Constant.NEWLINE,
                    e.getMessage());
            return !failBuildAsScanFailed;
        }
    }

    /**
     * Validate user inputs for Resubmit post build action
     *
     * @param apiID
     * @param apiKey
     * @param analysisName
     * @param maximumDuration
     * @param listener
     * @return
     */
    private boolean validateUserInputsForResubmit(String apiID, String apiKey, String analysisName,
            int maximumDuration, TaskListener listener) {
        // Validate API credentials
        if (StringUtil.isNullOrEmpty(apiID) || (StringUtil.isNullOrEmpty(apiKey))) {
            log(listener,
                    "No dynamic analysis submitted. Required API ID and key credentials not provided."
                            + Constant.NEWLINE);
            return false;
        }

        // Validate Dynamic Analysis Name
        if (StringUtil.isNullOrEmpty(analysisName)) {
            log(listener, "Dynamic analysis name is empty");
            return false;
        }

        // Validate Maximum duration
        if (FormValidationUtil.checkMaximumDuration(maximumDuration) != null) {
            log(listener,
                    FormValidationUtil.checkMaximumDuration(maximumDuration) + Constant.NEWLINE);
            return false;
        }

        return true;
    }

    /**
     * Retrieve analysis information
     *
     * @param analysisName
     * @param daApiService
     * @param listener
     * @return
     */
    private AnalysisInfo getAnalysisInfo(DynamicAnalysisAPIService daApiService,
            String analysisName, TaskListener listener) {
        try {
            return daApiService.getAnalysisByName(analysisName);
        } catch (ApiException e) {
            logWithTimeStamp(listener,
                    "Resubmit failed. Error retrieving analysis information, server returned HTTP response code: "
                            + e.getResponseCode());
            logErrorResponse(e.getResponseCode(), listener);
            return null;
        } catch (Exception e) {
            logWithTimeStamp(listener, "Resubmit failed. Error retrieving analysis information: %s",
                    e.getMessage());
            return null;
        }
    }

    /**
     * Resubmit Dynamic Analysis
     *
     * @param analysisInfo
     * @param maximumDuration
     * @param daApiService
     * @param listener
     * @return
     */
    private boolean resubmitAnalysis(DynamicAnalysisAPIService daApiService,
            AnalysisInfo analysisInfo, int maximumDuration, TaskListener listener) {
        try {
            daApiService.resubmitAnalysisById(analysisInfo.getAnalysisId(), maximumDuration);
            logWithTimeStamp(listener,
                    "Resubmitting dynamic analysis for '%s' with duration %s hour(s).",
                    analysisInfo.getAnalysisName(), maximumDuration);
            logWithTimeStamp(listener, "Resubmit succeeded.");
            return true;
        } catch (ApiException e) {
            logWithTimeStamp(listener,
                    "Resubmit failed. Error submitting dynamic analysis, server returned HTTP response code: "
                            + e.getResponseCode());
            logErrorResponse(e.getResponseCode(), listener);
            logWithTimeStamp(listener, "Analysis status for '%s' is %s",
                    analysisInfo.getAnalysisName(),
                    analysisInfo.getStatusInfo().getStatus() + Constant.NEWLINE);
            return false;
        } catch (Exception e) {
            logWithTimeStamp(listener,
                    "Resubmit failed. Unexpected error submitting dynamic analysis: %s",
                    e.getMessage());
            logWithTimeStamp(listener, "Analysis Status: %s",
                    analysisInfo.getStatusInfo().getStatus() + Constant.NEWLINE);
            return false;
        }
    }

    /**
     * Review Veracode Dynamic Analysis Results - A common method for both Freestyle
     * and Pipeline
     *
     * @param build                       Run
     * @param workspace                   FilePath
     * @param listener                    TaskListener
     * @param waitForResultsDuration      int
     * @param failBuildForPolicyViolation boolean
     * @param apiID                       String
     * @param apiKey                      String
     * @param debugEnabled                boolean
     * @param proxyBlock                  ProxyBlock
     * @return boolean
     */
    public boolean reviewDynamicAnalysis(Run<?, ?> build, FilePath workspace, TaskListener listener,
            final int waitForResultsDuration, final boolean failBuildForPolicyViolation,
            final String apiID, final String apiKey, final boolean debugEnabled,
            final ProxyBlock proxyBlock) {

        log(listener, Constant.STARTING_POST_BUILD_ACTION_LOG,
                Constant.POST_BUILD_ACTION_DISPLAY_TEXT_REVIEW);

        // Total time to wait for results to be available
        long expirationResultsWaitTime = System.currentTimeMillis()
                + TimeUnit.HOURS.toMillis(waitForResultsDuration);

        try {
            // Display HPI location if debug enabled
            if (debugEnabled) {
                log(listener, "[Debug mode is on]");
                showHPILocation(listener);
            }

            // Read build properties
            Properties veracodeProps = FileUtil.readBuildPropertiesFile(build, listener);
            if (veracodeProps == null || veracodeProps.isEmpty()) {
                log(listener,
                        "Failed to retrieve dynamic analysis info from resubmit dynamic analysis step.");
                return false;
            }

            // Display user inputs
            log(listener,
                    "Project: %s" + Constant.NEWLINE + "Dynamic Analysis name: %s"
                            + Constant.NEWLINE + "Results wait time (in hours): %s"
                            + Constant.NEWLINE + "Fail the build for policy violation: %s"
                            + Constant.NEWLINE + "Use proxy: %s",
                    workspace, veracodeProps.getProperty(PARAM_DA_ANALYSIS_NAME),
                    waitForResultsDuration, failBuildForPolicyViolation,
                    String.valueOf(proxyBlock != null));

            // Validate user inputs
            if (!validateUserInputsForReview(apiID, apiKey,
                    veracodeProps.getProperty(PARAM_DA_ANALYSIS_NAME), waitForResultsDuration,
                    listener)) {
                build.addAction(new DynamicAnalysisResultsAction());
                return false;
            }

            // Setup proxy
            Proxy proxy = setupProxy(proxyBlock, listener);
            if (proxy == null) {
                build.addAction(new DynamicAnalysisResultsAction());
                return false;
            }

            DynamicAnalysisAPIService daApiService = APIServiceManager
                    .createInstance(CredentialTypes.API, apiID, apiKey, proxy)
                    .getDynamicAnalysisAPIService();

            log(listener,
                    "Requesting dynamic analysis results for '%s' with results wait time duration of %s hour(s).",
                    veracodeProps.getProperty(PARAM_DA_ANALYSIS_NAME), waitForResultsDuration);

            /**
             * The following phases use Dynamic Analysis REST APIs.
             *
             * Phase 1: Verify analysis initiated and determine analysis occurrence id
             * daApiService.getAnalysisByName(analysis name);
             *
             * Phase 2: Wait for analysis to complete and report RESULTS_AVAILABLE
             * daApiService.getLatestAnalysisOccurrence(analysis occurrence id);
             *
             * Phase 3: DA is linked to an application for policy evaluation. Get linked
             * data including appid, appname, buildId
             * daApiService.getScanOccurrences(analysis occurrence id);
             *
             * The following phases use java wrapper util APIs.
             *
             * Phase 4: Wait for linking to complete which includes policy evaluation and
             * flaw data Get link (scan) status via getBuildInfo API and repeat until status
             * of RESULTS_READY WrapperUtil.getBuildInfoByAppIdBuildId(appId, buildId, true,
             * apiID, apiKey, proxyBlock)
             *
             * Phase 5: Get detailed report and parse for flaw data, score, policy status
             * WrapperUtil.getDetailedReport(buildId, true, apiID, apiKey, proxyBlock)
             *
             * API exception handling: If HTTP 500 Internal Server Error or HTTP 504 Gateway
             * Timeout response is returned from the platform, then retry after poll
             * interval for a maximum of MAX_ALLOWED_CONSECUTIVE_API_EXCEPTIONS consecutive
             * failures. If other HTTP error received, then fail the job.
             */

            ScanOccurrenceInfo scanOccurrenceInfo = null;

            try {
                // Phase 1: Wait for analysis to initiate and determine analysis occurrence id
                // If wait for results duration expired, then fail job.
                String currentOccurrenceId = determineAnalysisOccurrenceId(daApiService,
                        veracodeProps.getProperty(PARAM_DA_ANALYSIS_NAME),
                        veracodeProps.getProperty(PARAM_DA_PREVIOUS_OCCURRENCE_ID),
                        expirationResultsWaitTime, listener);
                if (StringUtil.isNullOrEmpty(currentOccurrenceId)) {
                    logWithTimeStamp(listener, "Timeout waiting for dynamic analysis to initiate.");
                    build.addAction(new DynamicAnalysisResultsAction());
                    return false;
                }

                // Phase 2: Wait for analysis scan to complete and results available.
                // If wait for results duration expired, then fail job.
                boolean isAnalysisFinished = false;
                isAnalysisFinished = waitForAnalysisToComplete(daApiService, currentOccurrenceId,
                        expirationResultsWaitTime, listener);
                if (!isAnalysisFinished) {
                    logWithTimeStamp(listener,
                            "Timeout waiting for dynamic analysis to complete and publish results.");
                    build.addAction(new DynamicAnalysisResultsAction());
                    return false;
                }

                // Phase 3: Wait for linked results of app id, app name, build id to be
                // available
                // If wait for results duration expired, then fail job.
                scanOccurrenceInfo = getLinkedAnalysisResults(daApiService, currentOccurrenceId,
                        expirationResultsWaitTime, listener);
                if (null == scanOccurrenceInfo) {
                    logWithTimeStamp(listener,
                            "Timeout waiting for dynamic analysis link results.");
                    build.addAction(new DynamicAnalysisResultsAction());
                    return false;
                }
            } catch (InterruptedException e) {
                logWithTimeStamp(listener,
                        "Interrupted exception handling dynamic analysis action.");
                build.addAction(new DynamicAnalysisResultsAction());
                e.printStackTrace();
                return false;
            } catch (RuntimeException e) {
                logWithTimeStamp(listener,
                        "Runtime exception error handling dynamic analyis action: %s",
                        e.getMessage());
                e.printStackTrace();
                build.addAction(new DynamicAnalysisResultsAction());
                return false;
            } catch (Exception e) {
                logWithTimeStamp(listener, "Exception handling dynamic analysis action: %s",
                        e.getMessage());
                e.printStackTrace();
                build.addAction(new DynamicAnalysisResultsAction());
                return false;
            }

            // set policy default to passed
            String policyRulesStatus = Constant.PASSED;

            try {

                // Phase 4: Wait for application build to complete. Analysis is linked to
                // Veracode application for policy evaluation.
                boolean isBuildReady = waitForBuildReady(scanOccurrenceInfo, apiID, apiKey,
                        proxyBlock, expirationResultsWaitTime, listener);
                if (!isBuildReady) {
                    log(listener, "Timeout waiting for dynamic analysis link results.");
                    build.addAction(new DynamicAnalysisResultsAction());
                    return false;
                }

                // Phase 5: Get the detailed report. Need flaw counts by severity, score, policy
                // status, mitigated findings.
                String detailedReportXML = WrapperUtil.getDetailedReport(
                        scanOccurrenceInfo.getLinkedAppData().getBuildId(), apiID, apiKey,
                        proxyBlock);

                // retrieve analysis results from the detailed report and occurrence info
                DAScanHistory daScanHistory = XmlUtil.newDAScanHistory(detailedReportXML,
                        scanOccurrenceInfo, build);

                policyRulesStatus = daScanHistory.getPolicyComplianceStatus();

                log(listener,
                        Constant.NEWLINE
                                + "The Dynamic Analysis finished with policy rule status: %s",
                        policyRulesStatus + Constant.NEWLINE);

                // Create action for the results graph
                build.addAction(new DynamicAnalysisResultsAction(daScanHistory));

            } catch (ApiException e) {
                logWithTimeStamp(listener,
                        "API exception while waiting for Dynamic Analysis linking results status.");
                build.addAction(new DynamicAnalysisResultsAction());
                return false;
            } catch (InterruptedException e) {
                logWithTimeStamp(listener,
                        "Interrupted exception handling dynamic analysis linking results action.");
                build.addAction(new DynamicAnalysisResultsAction());
                e.printStackTrace();
                return false;
            } catch (RuntimeException e) {
                logWithTimeStamp(listener,
                        "Runtime exception error handling dynamic analyis linking results action: %s",
                        e.getMessage());
                e.printStackTrace();
                build.addAction(new DynamicAnalysisResultsAction());
                return false;
            }

            log(listener, Constant.FINISHED_POST_BUILD_ACTION_LOG,
                    Constant.POST_BUILD_ACTION_DISPLAY_TEXT_REVIEW);

            return policyRulesStatus.equalsIgnoreCase(Constant.PASSED) ? true
                    : !failBuildForPolicyViolation;

        } catch (Exception e) {
            log(listener, "Unexpected error occurred: %s" + Constant.NEWLINE, e.getMessage());
            build.addAction(new DynamicAnalysisResultsAction());
            return false;
        }
    }

    /**
     * Validate user inputs for Review post build action
     *
     * @param apiID
     * @param apiKey
     * @param analysisName
     * @param waitForResultsDuration
     * @param listener
     * @return
     */
    private boolean validateUserInputsForReview(String apiID, String apiKey, String analysisName,
            int waitForResultsDuration, TaskListener listener) {
        // Validate API credentials
        if (StringUtil.isNullOrEmpty(apiID) || (StringUtil.isNullOrEmpty(apiKey))) {
            log(listener,
                    "Error requesting Dynamic Analysis results - required API ID and key credentials not provided");
            return false;
        }

        // Validate Wait for results duration
        if (FormValidationUtil.checkWaitForResultsDuration(waitForResultsDuration) != null) {
            log(listener, "Error requesting Dynamic Analysis results.");
            log(listener, FormValidationUtil.checkWaitForResultsDuration(waitForResultsDuration)
                    + Constant.NEWLINE);
            return false;
        }

        // Validate Dynamic Analysis Name
        if (StringUtil.isNullOrEmpty(analysisName)) {
            log(listener,
                    "Error requesting Dynamic Analysis results - Dynamic Analysis scan name is unknown.");
            log(listener,
                    "Verify Resubmit Veracode Dynamic Analysis post build action ran successfully in this Jenkins build prior to requesting results."
                            + Constant.NEWLINE);
            return false;
        }

        return true;
    }

    private String determineAnalysisOccurrenceId(DynamicAnalysisAPIService daApiService,
            String analysisName, String previousOccurrenceId, long expirationResultsWaitTime,
            TaskListener listener) throws Exception {

        // Verify a new analysis has been created and submission process initiated
        String currentOccurrenceId = "";
        boolean isAnalysisCreated = false;
        boolean isTimeToQuit = false;
        int exceptionCount = 0;

        while (!isAnalysisCreated && !isTimeToQuit) {
            try {
                // determine latest analysis occurrence to query analysis status
                AnalysisInfo analysisInfo = daApiService.getAnalysisByName(analysisName);

                currentOccurrenceId = analysisInfo != null ? analysisInfo.getAnalysisOccurrenceId()
                        : null;

                // If analysis not yet initiated, then retry until new occurrence found
                if (StringUtil.isNullOrEmpty(currentOccurrenceId)
                        || currentOccurrenceId.equals(previousOccurrenceId)) {
                    logWithTimeStamp(listener,
                            "Dynamic analysis not yet initiated. Check in %s minutes.",
                            GET_DA_SLEEP_TIME_MINUTES);
                    Thread.sleep(TimeUnit.MINUTES.toMillis(GET_DA_SLEEP_TIME_MINUTES));
                } else {
                    isAnalysisCreated = true;
                }
                isTimeToQuit = isWaitTimeDurationExpired(expirationResultsWaitTime);
                exceptionCount = 0;
            } catch (ApiException e) {
                log(listener,
                        "API exception error waiting for dynamic analysis to initiate. Server returned HTTP response code: "
                                + e.getResponseCode());
                logErrorResponse(e.getResponseCode(), listener);
                // Retry if internal server or gateway timeout error
                if ((500 == e.getResponseCode()) || (504 == e.getResponseCode())) {
                    exceptionCount++;
                    if ((exceptionCount == MAX_ALLOWED_CONSECUTIVE_API_EXCEPTIONS)
                            || isWaitTimeDurationExpired(expirationResultsWaitTime)) {
                        isTimeToQuit = true;
                    } else {
                        logWithTimeStamp(listener,
                                "Retry checking if dynamic analysis initiated in %s minutes.",
                                GET_DA_SLEEP_TIME_MINUTES);
                        Thread.sleep(TimeUnit.MINUTES.toMillis(GET_DA_SLEEP_TIME_MINUTES));
                        isTimeToQuit = isWaitTimeDurationExpired(expirationResultsWaitTime);
                    }
                } else {
                    isTimeToQuit = true;
                }
            }
        }
        return currentOccurrenceId;
    }

    private boolean waitForAnalysisToComplete(DynamicAnalysisAPIService daApiService,
            String currentOccurrenceId, long expirationResultsWaitTime, TaskListener listener)
            throws Exception {

        // Wait for analysis scan to complete and results available
        AnalysisOccurrenceInfo analysisOccurrenceInfo = null;
        boolean isTimeToQuit = false;
        boolean isAnalysisFinished = false;
        int exceptionCount = 0;

        while (!isAnalysisFinished && !isTimeToQuit) {

            try {
                analysisOccurrenceInfo = daApiService
                        .getLatestAnalysisOccurrence(currentOccurrenceId);
                exceptionCount = 0;
                if (analysisOccurrenceInfo == null) {
                    logWithTimeStamp(listener,
                            "Dynamic analysis occurrence not found. Check in %s minutes.",
                            GET_DA_SLEEP_TIME_MINUTES);
                    Thread.sleep(TimeUnit.MINUTES.toMillis(GET_DA_SLEEP_TIME_MINUTES));
                    isTimeToQuit = isWaitTimeDurationExpired(expirationResultsWaitTime);
                    continue;
                }

                StatusTypeEnum analysisStatus = analysisOccurrenceInfo.getAnalysisStatus()
                        .getStatus();

                // Dynamic analysis complete and results published. Next step is linking the
                // analysis results to an application.
                if (analysisStatus != null
                        && analysisStatus.equals(StatusTypeEnum.FINISHED_RESULTS_AVAILABLE)) {
                    isAnalysisFinished = true;
                    logWithTimeStamp(listener, "The status of the dynamic analysis is: %s",
                            analysisStatus);
                    logWithTimeStamp(listener,
                            "The dynamic analysis finished with occurrence id: %s",
                            currentOccurrenceId);
                    logWithTimeStamp(listener,
                            "The next step is linking the analysis to the application for policy evaluation.");
                } else if (analysisStatus != null && (analysisStatus
                        .equals(StatusTypeEnum.VERIFICATION_FAILED)
                        || analysisStatus.equals(StatusTypeEnum.STOPPED)
                        || analysisStatus.equals(StatusTypeEnum.STOPPED_TIME)
                        || analysisStatus.equals(StatusTypeEnum.STOPPED_TIME_VERIFYING_RESULTS)
                        || analysisStatus.equals(StatusTypeEnum.STOPPED_TECHNICAL_ISSUE)
                        || analysisStatus.equals(StatusTypeEnum.STOPPED_VERIFYING_RESULTS_BY_USER)
                        || analysisStatus.equals(StatusTypeEnum.STOPPED_VERIFYING_RESULTS))) {
                    logWithTimeStamp(listener,
                            "The dynamic analysis failed to complete with status: %s",
                            analysisStatus);
                    isTimeToQuit = true;
                } else {
                    logWithTimeStamp(listener,
                            "The status of the dynamic analysis is: %s" + Constant.NEWLINE
                                    + timestamp() + "Requesting status in %s minutes",
                            analysisStatus, GET_DA_SLEEP_TIME_MINUTES);
                    Thread.sleep(TimeUnit.MINUTES.toMillis(GET_DA_SLEEP_TIME_MINUTES));
                    isTimeToQuit = isWaitTimeDurationExpired(expirationResultsWaitTime);
                }

            } catch (ApiException e) {
                log(listener,
                        "API exception error retrieving scan analysis status. Server returned HTTP response code: "
                                + e.getResponseCode());
                logErrorResponse(e.getResponseCode(), listener);

                // Retry if internal server or gateway timeout error
                if ((500 == e.getResponseCode()) || (504 == e.getResponseCode())) {
                    exceptionCount++;
                    if ((exceptionCount == MAX_ALLOWED_CONSECUTIVE_API_EXCEPTIONS)
                            || isWaitTimeDurationExpired(expirationResultsWaitTime)) {
                        isTimeToQuit = true;
                    } else {
                        logWithTimeStamp(listener,
                                "Retry requesting scan occurrence info in %s minutes.",
                                GET_DA_SLEEP_TIME_MINUTES);
                        Thread.sleep(TimeUnit.MINUTES.toMillis(GET_DA_SLEEP_TIME_MINUTES));
                        isTimeToQuit = isWaitTimeDurationExpired(expirationResultsWaitTime);
                    }
                } else {
                    isTimeToQuit = true;
                }
            }
        }

        return isAnalysisFinished;
    }

    private ScanOccurrenceInfo getLinkedAnalysisResults(DynamicAnalysisAPIService daApiService,
            String occurrenceId, long expirationResultsWaitTime, TaskListener listener)
            throws Exception {

        boolean isLinkedAppInfoReady = false;
        boolean isTimeToQuit = false;
        int exceptionCount = 0;

        ScanOccurrenceInfo scanOccurrenceResultsInfo = null;

        while (!isLinkedAppInfoReady && !isTimeToQuit) {

            try {
                Set<ScanOccurrenceInfo> scanOccurrenceInfoSet = daApiService
                        .getScanOccurrences(occurrenceId);

                if (scanOccurrenceInfoSet != null && !scanOccurrenceInfoSet.isEmpty()) {
                    if (scanOccurrenceInfoSet.size() > 1) {
                        throw new RuntimeException("Multiple scan occurrences found.");
                    }
                    scanOccurrenceResultsInfo = (ScanOccurrenceInfo) scanOccurrenceInfoSet
                            .toArray()[0];
                }
                exceptionCount = 0;
                if (scanOccurrenceResultsInfo != null) {

                    // if dynamic analysis is not manually linked to an application then abort.
                    // The appid value is populated at any stage of scanning.
                    if (StringUtil
                            .isNullOrEmpty(scanOccurrenceResultsInfo.getLinkedPlatformAppId())) {
                        logWithTimeStamp(listener,
                                "Review results failed. Linked application is unknown.");
                        logWithTimeStamp(listener,
                                "Verify dynamic analysis is linked to an application.");
                        return null;
                    }
                    // the linking phase may be delayed depending on platform load, so need to wait
                    // for
                    // build id to be populated when linking initiated
                    if ((null != scanOccurrenceResultsInfo.getLinkedAppData())
                            && (!StringUtil.isNullOrEmpty(
                                    scanOccurrenceResultsInfo.getLinkedAppData().getBuildId()))) {
                        isLinkedAppInfoReady = true;
                        logWithTimeStamp(listener, "The linked application is: %s (appid=%s)",
                                scanOccurrenceResultsInfo.getLinkedPlatformAppName(),
                                scanOccurrenceResultsInfo.getLinkedPlatformAppId());
                        logWithTimeStamp(listener, "The linked application build ID is: %s",
                                scanOccurrenceResultsInfo.getLinkedAppData().getBuildId());
                    } else {
                        logWithTimeStamp(listener,
                                "Build id is not available." + Constant.NEWLINE + timestamp()
                                        + "Requesting build id again in %s minute(s).",
                                GET_DA_SLEEP_TIME_MINUTES);
                        Thread.sleep(TimeUnit.MINUTES.toMillis(GET_DA_SLEEP_TIME_MINUTES));
                    }
                    if (isTimeToQuit = isWaitTimeDurationExpired(expirationResultsWaitTime)) {
                        return null;
                    }
                } else {
                    logWithTimeStamp(listener, "Error getting linked application data");
                    return null;
                }
            } catch (ApiException e) {
                log(listener,
                        "API exception error retrieving scan occurrence info. Server returned HTTP response code: "
                                + e.getResponseCode());
                logErrorResponse(e.getResponseCode(), listener);

                // Retry if internal server or gateway timeout error
                if ((500 == e.getResponseCode()) || (504 == e.getResponseCode())) {
                    exceptionCount++;
                    if ((exceptionCount == MAX_ALLOWED_CONSECUTIVE_API_EXCEPTIONS)
                            || isWaitTimeDurationExpired(expirationResultsWaitTime)) {
                        isTimeToQuit = true;
                    } else {
                        logWithTimeStamp(listener,
                                "Retry requesting scan occurrence info in %s minutes.",
                                GET_DA_SLEEP_TIME_MINUTES);
                        Thread.sleep(TimeUnit.MINUTES.toMillis(GET_DA_SLEEP_TIME_MINUTES));
                        isTimeToQuit = isWaitTimeDurationExpired(expirationResultsWaitTime);
                    }
                } else {
                    isTimeToQuit = true;
                }
            }
        } // end while

        return scanOccurrenceResultsInfo;
    }

    private boolean waitForBuildReady(ScanOccurrenceInfo scanOccurrenceInfo, String apiID,
            String apiKey, ProxyBlock proxyBlock, long expirationResultsWaitTime,
            TaskListener listener) throws Exception {

        boolean isBuildReady = false;
        boolean isTimeToQuit = false;
        int exceptionCount = 0;

        logWithTimeStamp(listener, "Requesting dynamic analysis linked results");

        while (!isBuildReady && !isTimeToQuit) {
            try {
                String buildInfo = WrapperUtil.getBuildInfoByAppIdBuildId(
                        scanOccurrenceInfo.getLinkedPlatformAppId(),
                        scanOccurrenceInfo.getLinkedAppData().getBuildId(), apiID, apiKey,
                        proxyBlock);

                if (!StringUtil.isNullOrEmpty(buildInfo)) {

                    Node nodeAnalysisUnit = XmlUtils.getXmlNode(buildInfo,
                            "/*/*/*[local-name()='analysis_unit']");
                    String buildStatus = nodeAnalysisUnit.getAttributes().getNamedItem("status")
                            .getNodeValue();

                    if (buildStatus != null
                            && buildStatus.equalsIgnoreCase(Constant.RESULTS_READY)) {
                        logWithTimeStamp(listener,
                                "Dynamic analysis linking is complete with status: %s",
                                buildStatus);
                        isBuildReady = true;
                        // linked results are not yet ready so retry
                    } else {
                        logWithTimeStamp(listener,
                                "The linking status of the dynamic analysis is: %s"
                                        + Constant.NEWLINE + timestamp()
                                        + "Requesting linking status in %s minutes",
                                buildStatus, GET_DA_SLEEP_TIME_MINUTES);
                        Thread.sleep(TimeUnit.MINUTES.toMillis(GET_DA_SLEEP_TIME_MINUTES));
                        isTimeToQuit = isWaitTimeDurationExpired(expirationResultsWaitTime);
                    }
                } else {
                    logWithTimeStamp(listener, "Error getting build info after analysis linked");
                    isTimeToQuit = true;
                }
                exceptionCount = 0;
            } catch (ApiException e) {
                log(listener,
                        "API exception error retrieving linked results status. Server returned HTTP response code: "
                                + e.getResponseCode());
                logErrorResponse(e.getResponseCode(), listener);

                // Retry if internal server or gateway timeout error
                if ((500 == e.getResponseCode()) || (504 == e.getResponseCode())) {
                    exceptionCount++;
                    if ((exceptionCount == MAX_ALLOWED_CONSECUTIVE_API_EXCEPTIONS)
                            || isWaitTimeDurationExpired(expirationResultsWaitTime)) {
                        isTimeToQuit = true;
                    } else {
                        logWithTimeStamp(listener, "Retry requesting linked status in %s minutes.",
                                GET_DA_SLEEP_TIME_MINUTES);
                        Thread.sleep(TimeUnit.MINUTES.toMillis(GET_DA_SLEEP_TIME_MINUTES));
                        isTimeToQuit = isWaitTimeDurationExpired(expirationResultsWaitTime);
                    }
                } else {
                    isTimeToQuit = true;
                }
            }
        } // end while

        return isBuildReady;
    }

    private boolean isWaitTimeDurationExpired(long expirationResultsWaitTime) {
        return (System.currentTimeMillis() > expirationResultsWaitTime);
    }

    /**
     * Find and display the location of the HPI file
     * 
     * @param listener
     */
    private void showHPILocation(TaskListener listener) {
        try {
            String location = this.getClass().getProtectionDomain().getCodeSource().getLocation()
                    .toString();
            if (!StringUtil.isNullOrEmpty(location)) {
                log(listener, "HPI location: ");
                location = location.replace("file:/", "");
                listener.hyperlink("file://" + location, location);
            }
        } catch (Exception e) {
            log(listener, "Could not retrieve hpi file directory.");
        }
    }

    /**
     * Setup proxy
     *
     * @param proxyBlock
     * @param listener
     * @return
     */
    private Proxy setupProxy(final ProxyBlock proxyBlock, TaskListener listener) {
        try {
            if (proxyBlock == null) {
                return Proxy.NO_PROXY;
            }

            if (StringUtil.isNullOrEmpty(proxyBlock.getPhost())
                    || StringUtil.isNullOrEmpty(proxyBlock.getPport())) {
                log(listener, "Proxy is enabled, but the host or port is empty");
                return null;
            }

            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyBlock.getPhost(),
                    Integer.parseInt(proxyBlock.getPport())));
            if (!StringUtil.isNullOrEmpty(proxyBlock.getPuser())
                    && !StringUtil.isNullOrEmpty(proxyBlock.getPpassword())) {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxyBlock.getPuser(),
                                proxyBlock.getPpassword().toCharArray());
                    }
                });
            }
            return proxy;
        } catch (NumberFormatException e) {
            log(listener, "Invalid port number for proxy");
            return null;
        } catch (Exception e) {
            log(listener, "Failed to setup proxy");
            return null;
        }
    }

    /**
     * Log information
     * 
     * @param listener
     * @param format
     * @param args
     */
    private final void log(TaskListener listener, String format, Object... args) {
        listener.getLogger().printf(Constant.NEWLINE + format + Constant.NEWLINE, args);
    }

    /**
     * Log information with timestamp
     * 
     * @param listener
     * @param format
     * @param args
     */
    private final void logWithTimeStamp(TaskListener listener, String format, Object... args) {
        listener.getLogger().printf(Constant.NEWLINE + timestamp() + format, args);
        listener.getLogger().printf(Constant.NEWLINE + timestamp());
    }

    /**
     * Get the timestamp
     * 
     * @return
     */
    private static String timestamp() {
        return String.format("[%s] ", new SimpleDateFormat("yy.MM.dd HH:mm:ss").format(new Date()));
    }

    /**
     * Log error response
     * 
     * @param responseCode
     * @param listener
     */
    private final void logErrorResponse(int responseCode, TaskListener listener) {
        if (responseCode == 401) {
            log(listener, Constant.AUTHENTICATION_ERROR);
        } else if (responseCode == 403) {
            log(listener, Constant.PRIVILEGE_ERROR);
        } else if (responseCode == 500) {
            log(listener, Constant.INTERNAL_SERVER_ERROR);
        } else if (responseCode == 504) {
            log(listener, Constant.GATEWAY_TIMEOUT_ERROR);
        }
    }
}