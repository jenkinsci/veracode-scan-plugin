package com.veracode.jenkins.plugin.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.veracode.apiwrapper.dynamicanalysis.model.client.ScanOccurrenceInfo;

import hudson.model.Run;
import com.veracode.jenkins.plugin.DynamicAnalysisResultsAction;
import com.veracode.jenkins.plugin.VeracodeAction;
import com.veracode.jenkins.plugin.data.DAScanHistory;
import com.veracode.jenkins.plugin.data.FindingCounts;
import com.veracode.jenkins.plugin.data.SCAComponent;
import com.veracode.jenkins.plugin.data.SCAScanHistory;
import com.veracode.jenkins.plugin.data.ScanHistory;
import com.veracode.jenkins.plugin.enums.SeverityLevel;

/**
 * Helpers to work with XML
 *
 */
public class XmlUtil {

    private static final String SCA_XPATH = "/detailedreport/*[local-name()='software_composition_analysis']";
    private static final int SEVERITY_LEVEL_NUMBER = 6;
    private static final int MAX_BUILDS_TO_SEARCH = 60; // Maximum number of Jenkins builds to
                                                        // search for scan results.
    private static final String STATIC_ANALYSIS_ELEMENT_NODE = "static-analysis";
    private static final String DYNAMIC_ANALYSIS_ELEMENT_NODE = "dynamic-analysis";
    private static final String STATIC_ANALYSIS_FLAWS_ELEMENT_NODE = "staticflaws";
    private static final String DYNAMIC_ANALYSIS_FLAWS_ELEMENT_NODE = "dynamicflaws";

    /**
     * Get the scan results from the detailed report and previous Jenkins builds
     * result to compose the scan result for the current Jenkins build.
     *
     * @param buildInfoXml      - XML returned from calling GetBuildInfo API
     * @param detailedReportXml - XML returned from calling GetDetailedReport API
     * @param build             - The current Jenkins build
     * @return an ScanHistory instance containing the info to be displayed in the
     *         Veracode post build step.
     * @throws Exception when an error is encountered during the operation.
     */
    public static final ScanHistory newScanHistory(String buildInfoXml, String detailedReportXml,
            Run<?, ?> build) throws Exception {

        Element buildInfoRoot = XmlUtil.getXmlDocument(buildInfoXml).getDocumentElement();
        String accountId = buildInfoRoot.getAttribute("account_id");
        String appId = buildInfoRoot.getAttribute("app_id");
        String buildId = XmlUtil.parseBuildId(buildInfoXml);

        Document xml = getXmlDocument(detailedReportXml);
        Element root = xml.getDocumentElement();
        String policyName = root.getAttribute("policy_name");
        String policyComplianceStatus = root.getAttribute("policy_compliance_status");
        String veracodeLevel = root.getAttribute("veracode_level");
        boolean scanOverdue = Boolean.parseBoolean(root.getAttribute("scan_overdue"));

        XPathFactory xpf = XPathFactory.newInstance();
        XPath xPathObj = xpf.newXPath();

        // Get the Static Analysis score from the detailed report XML
        int score = parseAnalysisScore(xPathObj, xml, STATIC_ANALYSIS_ELEMENT_NODE);

        // Get Net Change count from detailedreport.xml
        int[] netChange = getNetChangeCount(xPathObj, xml);

        // Find out real flaws count and mitigated status for each level
        Object[] realCountObj = getRealFlawCount(xPathObj, xml, STATIC_ANALYSIS_FLAWS_ELEMENT_NODE);

        int[] realCount = (int[]) realCountObj[0];
        boolean[] mtgStatus = (boolean[]) realCountObj[1];

        // Check if the flaw affect policy compliance status
        boolean[] policyaffect = getPolicyAffectedness(xPathObj, xml,
                STATIC_ANALYSIS_FLAWS_ELEMENT_NODE);

        // Get total flaw count
        int totalFlawsCount = 0;
        for (int i = 0; i < realCount.length; i++) {
            totalFlawsCount += realCount[i];
        }

        long buildDate = build.getTimestamp().getTimeInMillis();
        Map<String, Long> thisScanStats = createStats(buildDate, Long.valueOf(totalFlawsCount));

        Run<?, ?> lastBuild = build.getPreviousBuild();
        SCAScanHistory lastSCAHistory = null;
        List<Map<String, Long>> lastFlawsCountHistory = null;
        int buildCount = 0;
        // Find the scan stats from previous build
        for (lastFlawsCountHistory = null, buildCount = 0; null == lastFlawsCountHistory
                && buildCount < MAX_BUILDS_TO_SEARCH; lastBuild = lastBuild
                        .getPreviousBuild(), buildCount++) {
            // No more previous build, done searching
            if (null == lastBuild) {
                break;
            }

            VeracodeAction lastBuildAction = lastBuild.getAction(VeracodeAction.class);
            // If there is no Veracode action in this previous build (maybe the build failed
            // before our code generates the result) or
            // the last build encountered a problem when generating the scan results, then
            // move on to the next previous build
            if (null == lastBuildAction || !lastBuildAction.isScanHistoryAvailable()) {
                continue;
            }

            lastFlawsCountHistory = lastBuildAction.getFlawsCountHistory();
            lastSCAHistory = lastBuildAction.getSCAScanHistory();
        }

        return new ScanHistory(accountId, appId, buildId, policyName, policyComplianceStatus, score,
                veracodeLevel, scanOverdue, totalFlawsCount, realCount, mtgStatus, netChange,
                createCountHistory(thisScanStats, lastFlawsCountHistory),
                newSCAHistory(detailedReportXml, buildDate, lastSCAHistory), policyaffect);
    }

    /**
     * Retrieve SCA results from the given Detailed Report XML
     *
     * @param detailedReportXml String
     * @param buildDate         long
     * @param lastSCAHistory    SCAScanHistory
     * @return an instance of SCAHistory that contains the SCA result in the
     *         detailed report
     * @throws Exception when an error is encountered during the operation.
     */
    public static final SCAScanHistory newSCAHistory(String detailedReportXml, long buildDate,
            SCAScanHistory lastSCAHistory) throws Exception {
        if (StringUtil.isNullOrEmpty(detailedReportXml)) {
            throw new IllegalArgumentException("Cannot process empty detailed report.");
        }

        XPathFactory xpf = XPathFactory.newInstance();
        XPath xPathObj = xpf.newXPath();
        Document detailedReportDoc = getXmlDocument(detailedReportXml);

        SCAScanHistory result = null;
        if (isSubscribedToSCA(xPathObj, detailedReportDoc)) {
            // the count of vulnerability found by SCA scan in this build
            Set<FindingCounts> findingCounts = parseFindingCounts(xPathObj, detailedReportDoc,
                    lastSCAHistory);
            int totalVulCount = 0;
            for (FindingCounts vc : findingCounts) {
                if (null != vc) {
                    totalVulCount += vc.getCount();
                }
            }

            result = new SCAScanHistory(parseMaxCVSSScore(xPathObj, detailedReportDoc),
                    parseBlacklistedCompsCount(xPathObj, detailedReportDoc), findingCounts,
                    parseSCAComponentInfo(xPathObj, detailedReportDoc),
                    createCountHistory(createStats(buildDate, Long.valueOf(totalVulCount)),
                            (lastSCAHistory == null ? null : lastSCAHistory.getVulCountHistory())));
        } else {
            // Even if this scan shows the user is not subscribed to SCA, we still need to
            // figure out if
            // there are any SCA vulnerability count in the past builds that need to be
            // shown in the
            // trend chart.
            result = new SCAScanHistory(createCountHistory(createStats(buildDate, null),
                    (lastSCAHistory == null ? null : lastSCAHistory.getVulCountHistory())));
        }
        return result;
    }

    /**
     * Get the scan results from the detailed report and scan occurrence result. In
     * addition, comparison of results to previous successful Jenkins build result.
     *
     * @param detailedReportXml  - XML returned from calling GetDetailedReport API
     * @param scanOccurrenceInfo - analysis occurrence info returned from DA via
     *                           REST API
     * @param build              - linked application id
     * @return DAScanHistory instance containing the info to be displayed in the
     *         Veracode post build step.
     * @throws Exception when an error is encountered during the operation.
     */
    public static final DAScanHistory newDAScanHistory(String detailedReportXml,
            ScanOccurrenceInfo scanOccurrenceInfo, Run<?, ?> build) throws Exception {

        Element detailedReportRoot = XmlUtil.getXmlDocument(detailedReportXml).getDocumentElement();
        Document xml = getXmlDocument(detailedReportXml);
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xPathObj = xpf.newXPath();

        String accountId = detailedReportRoot.getAttribute("account_id");
        String appId = scanOccurrenceInfo.getLinkedPlatformAppId();
        String buildId = scanOccurrenceInfo.getLinkedAppData().getBuildId();
        String policyName = detailedReportRoot.getAttribute("policy_name");
        String policyComplianceStatus = detailedReportRoot.getAttribute("policy_compliance_status");
        String veracodeLevel = detailedReportRoot.getAttribute("veracode_level");
        boolean scanOverdue = Boolean.parseBoolean(detailedReportRoot.getAttribute("scan_overdue"));

        // Get the Static Analysis score from the detailed report XML
        int score = parseAnalysisScore(xPathObj, xml, DYNAMIC_ANALYSIS_ELEMENT_NODE);

        // Determine actual flaw count and mitigated boolean status for each severity
        // level.
        // Note the actual flaw count does not include mitigated accepted flaws.
        // If flaws are mitigated, then an asterisk is added to the severity flaw count
        // in the graph
        Object[] actualFlawDataObj = getRealFlawCount(xPathObj, xml,
                DYNAMIC_ANALYSIS_FLAWS_ELEMENT_NODE);
        int[] actualFlawCount = (int[]) actualFlawDataObj[0];
        boolean[] isMitigated = (boolean[]) actualFlawDataObj[1];

        // Determine total actual flaw count
        int totalFlawsCount = 0;
        for (int i = 0; i < actualFlawCount.length; i++) {
            totalFlawsCount += actualFlawCount[i];
        }

        // Check if the vulnerability affects policy compliance status
        boolean[] policyaffect = getPolicyAffectedness(xPathObj, xml,
                DYNAMIC_ANALYSIS_FLAWS_ELEMENT_NODE);

        int[] netChangeList = {
                0, 0, 0, 0, 0, 0
        };

        // Store build time and total flaw count for trend graph
        long buildDate = build.getTimestamp().getTimeInMillis();
        Map<String, Long> thisScanStats = createStats(buildDate, Long.valueOf(totalFlawsCount));

        // Find the scan flaw stats from previous successful Jenkins build
        Run<?, ?> lastBuild = build.getPreviousBuild();
        List<Map<String, Long>> lastFlawsCountHistory = null;
        int buildCount = 0;
        for (lastFlawsCountHistory = null, buildCount = 0; null == lastFlawsCountHistory
                && buildCount < MAX_BUILDS_TO_SEARCH; lastBuild = lastBuild
                        .getPreviousBuild(), buildCount++) {
            // No more previous build, done searching
            if (null == lastBuild) {
                break;
            }

            DynamicAnalysisResultsAction lastBuildAction = lastBuild
                    .getAction(DynamicAnalysisResultsAction.class);

            // If there is no Veracode Dynamic Results action in this previous build (maybe
            // the build failed before our code generates the result)
            // or the last build encountered a problem when generating the scan results,
            // then move on to the next previous build
            if (null == lastBuildAction || !lastBuildAction.isScanHistoryAvailable()) {
                continue;
            }
            lastFlawsCountHistory = lastBuildAction.getFlawsCountHistory();

            // Determine net change per severity type. Note that new count is determined
            // from net change data
            for (int sevIndex = 0; sevIndex < SEVERITY_LEVEL_NUMBER; sevIndex++) {
                int netChange = 0;
                int prevCount = lastBuildAction.getFlawsCountInt(sevIndex);
                try {
                    netChange = actualFlawCount[sevIndex] - prevCount;
                } catch (IllegalArgumentException iae) {
                    netChange = 0;
                } finally {
                    netChangeList[sevIndex] = netChange;
                }
            }
        }

        return new DAScanHistory(accountId, appId, buildId, policyName, policyComplianceStatus,
                score, veracodeLevel, scanOverdue, totalFlawsCount, actualFlawCount, isMitigated,
                netChangeList, createCountHistory(thisScanStats, lastFlawsCountHistory),
                policyaffect);
    }

    /**
     * Get the scan score from the detailed report XML
     *
     * @param detailedReportXml
     * @return
     */
    private static final int parseAnalysisScore(XPath xPathObj, Document xml,
            String analysisElementNodeType) throws Exception {
        final String ANALYSIS_NODE_XPATH = "/detailedreport/*[local-name()='"
                + analysisElementNodeType + "']";
        Node node = (Node) xPathObj.evaluate(ANALYSIS_NODE_XPATH, xml.getDocumentElement(),
                XPathConstants.NODE);
        String score = "";
        if (null != node) {
            score = node.getAttributes().getNamedItem("score").getNodeValue();
        }
        return Integer.parseInt(score);
    }

    /**
     * Find the Application ID by its name within an XML document
     *
     * @param appName          - Name of an application
     * @param xmlAppListResult - XML result from a GetAppList call
     * @return The ID of the application or null if the ID is not found
     * @throws Exception when given invalid parameter(s) or an error occurred when
     *                   parsing the given XML.
     */
    public static final String parseAppId(String appName, String xmlAppListResult)
            throws Exception {
        Document xml = getXmlDocument(xmlAppListResult);
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xPathObj = xpf.newXPath();
        NodeList nodeList = (NodeList) xPathObj.evaluate(
                "/*/*[local-name()='app'][@app_id][@app_name]", xml.getDocumentElement(),
                XPathConstants.NODESET);
        String app_id = null;

        for (int x = 0; x < nodeList.getLength(); x++) {
            Node node = nodeList.item(x);

            if (StringUtil.compare(node.getAttributes().getNamedItem("app_name").getNodeValue(),
                    appName, true) == 0) {
                app_id = node.getAttributes().getNamedItem("app_id").getNodeValue();
                break;
            }
        }
        return app_id;
    }

    /**
     * Find the sandbox ID by its name in an XML document
     *
     * @param sandboxName          - Name of a sandbox
     * @param xmlSandboxListResult - XML from the getSandboxList API
     * @return The ID of the given sandbox or empty string if none found
     * @throws Exception when given invalid parameter(s) or an error occurred when
     *                   parsing the given XML.
     */
    public static final String parseSandboxId(String sandboxName, String xmlSandboxListResult)
            throws Exception {
        if (StringUtil.isNullOrEmpty(xmlSandboxListResult)) {
            throw new IllegalArgumentException("Empty XML document.");
        }

        Document xml = getXmlDocument(xmlSandboxListResult);
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xPathObj = xpf.newXPath();
        Node node = (Node) xPathObj.evaluate(
                "/sandboxlist/sandbox[@sandbox_name=\"" + sandboxName + "\"]",
                xml.getDocumentElement(), XPathConstants.NODE);
        String sandboxId = "";
        if (null != node) {
            sandboxId = node.getAttributes().getNamedItem("sandbox_id").getNodeValue();
        }
        return (!StringUtil.isNullOrEmpty(sandboxId)) ? sandboxId : "";
    }

    /**
     * Takes an XML return an creates a DOM tree.
     *
     * @param xmlString String
     * @return Document
     * @throws Exception exception
     */
    public static final Document getXmlDocument(String xmlString) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl",
                true); /* Solves security vulnerability */
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);

        InputStream inputStream = new ByteArrayInputStream(xmlString.getBytes("UTF-8"));
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document xml = db.parse(new InputSource(new InputStreamReader(inputStream, "UTF-8")));
        return xml;
    }

    /**
     * Find the build ID within a XML document.
     *
     * @param xmlBuildInfoResult A String that represents the buildinfo XML
     *                           document, which references the buildinfo.xsd
     *                           schema.
     * @return A build ID
     * @throws Exception when given invalid XML document or an error occurred when
     *                   parsing the given XML.
     */
    public static final String parseBuildId(String xmlBuildInfoResult) throws Exception {
        if (StringUtil.isNullOrEmpty(xmlBuildInfoResult)) {
            throw new IllegalArgumentException("Empty XML document.");
        }

        Document xml = getXmlDocument(xmlBuildInfoResult);
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xPathObj = xpf.newXPath();
        Node node = (Node) xPathObj.evaluate("/*/*[local-name()='build'][@build_id]",
                xml.getDocumentElement(), XPathConstants.NODE);
        String buildId = "";
        if (null != node) {
            buildId = node.getAttributes().getNamedItem("build_id").getNodeValue();
        }
        return (!StringUtil.isNullOrEmpty(buildId)) ? buildId : "";
    }

    /**
     * Get the error string, if any, from a XML document.
     *
     * @param xmlString A XML document
     * @return The error message in the XML document if found, empty string
     *         otherwise. The return value will never be null.
     */
    public static final String getErrorString(String xmlString) {
        if (StringUtil.isNullOrEmpty(xmlString)) {
            return "";
        }

        String errorString = StringUtil.EMPTY;
        StringBuilder builder = new StringBuilder();
        // named groups (ie: "<error>(?<text>.*?)</error>") not compatible with
        // all versions of java
        Pattern pattern = Pattern.compile("<error>(.*?)</error>");
        Matcher matcher = pattern.matcher(xmlString);
        while (matcher.find()) {
            builder.append(matcher.group(1) + StringUtil.NEWLINE);
        }
        errorString = builder.toString();
        if (errorString.contains(StringUtil.NEWLINE)) {
            errorString = errorString.substring(0, builder.lastIndexOf(StringUtil.NEWLINE));
        }
        return errorString;
    }

    /**
     * Check each severity level for any flaws which affect policy compliance
     *
     * @param xPathObj, xml
     * @return Array of flaw count for each severity level, Array of policy
     *         affection for each severity level
     */
    private static boolean[] getPolicyAffectedness(XPath xPathObj, Document xml, String flawType) {
        boolean[] policyAffectStatus = new boolean[SEVERITY_LEVEL_NUMBER];

        try {
            for (int i = 0; i < SEVERITY_LEVEL_NUMBER; i++) {
                String flaws_path = "/detailedreport/severity[@level = '" + i + "']/category/cwe/"
                        + flawType
                        + "/*[local-name()='flaw' and @affects_policy_compliance='true']";
                NodeList flawsNodes = (NodeList) xPathObj.evaluate(flaws_path,
                        xml.getDocumentElement(), XPathConstants.NODESET);
                if (flawsNodes != null && flawsNodes.getLength() > 0) {
                    policyAffectStatus[i] = true;
                }
            }

        } catch (XPathExpressionException ex) {
            throw new RuntimeException(ex);
        }
        return policyAffectStatus;
    }

    /**
     * Get the flaws count (not including mitigated flaws) from a XML document.
     *
     * @param xPathObj, xml, flawType
     * @return Object of two Array: Array of flaw count for each severity level,
     *         Array of mitigation status flag for each severity level
     */
    private static Object[] getRealFlawCount(XPath xPathObj, Document xml, String flawType) {

        final String FLAW_NODE_XPATH = "/detailedreport/severity/category/cwe/" + flawType
                + "/*[local-name()='flaw']";
        final String MITIGATION_STATUS = "accepted";
        final String REMEDIATION_STATUS = "Fixed";
        final String SEVERITY_ATTR = "severity";
        final String MITIGATION_ATTR = "mitigation_status";
        final String REMEDIATION_ATTR = "remediation_status";

        String flawSeverity = "";
        String mitigateStatus = "";
        String remediationStatus = "";

        // Initialize realCount array and mitigate status
        int[] realCount = new int[SEVERITY_LEVEL_NUMBER];
        boolean[] mtgStatus = new boolean[SEVERITY_LEVEL_NUMBER];
        for (int x = 0; x < SEVERITY_LEVEL_NUMBER; x++) {
            mtgStatus[x] = false;
        }

        try {
            // parsing detailedreport.xml file flaw tag
            NodeList flaws = (NodeList) xPathObj.evaluate(FLAW_NODE_XPATH, xml.getDocumentElement(),
                    XPathConstants.NODESET);

            for (int i = 0; i < flaws.getLength(); i++) {
                Node thisFlaw = flaws.item(i);
                flawSeverity = thisFlaw.getAttributes().getNamedItem(SEVERITY_ATTR).getNodeValue();
                mitigateStatus = thisFlaw.getAttributes().getNamedItem(MITIGATION_ATTR)
                        .getNodeValue();
                remediationStatus = thisFlaw.getAttributes().getNamedItem(REMEDIATION_ATTR)
                        .getNodeValue();

                for (int j = 0; j < SEVERITY_LEVEL_NUMBER; j++) {
                    // Adding flaw count to each severity level
                    if (flawSeverity.equals(Integer.toString(j))
                            && !remediationStatus.equals(REMEDIATION_STATUS)) {
                        // this flaw is mitigated, don't count it, add "*" later
                        if (mitigateStatus.equals(MITIGATION_STATUS)) {
                            mtgStatus[j] = true;
                        } else {
                            realCount[j]++;
                        }
                    }
                }
            }
        } catch (XPathExpressionException xpee) {
            throw new RuntimeException(xpee);
        }

        // Return an object include two array lists
        Object[] arrayObjects = new Object[2];
        arrayObjects[0] = realCount;
        arrayObjects[1] = mtgStatus;
        return arrayObjects;
    }

    /**
     * Get the net change flaws count from a XML document.
     *
     * @param xPathObj, xml
     * @return Array of net change flaw count for each severity level
     */
    private static int[] getNetChangeCount(XPath xPathObj, Document xml) {

        final String FLAW_STATUS_NODE_XPATH = "/detailedreport/*[local-name()='flaw-status']";
        // NOTE: Currently, the 'flaw-status' element in the Detailed Report does not
        // contain the "sev-0-change" attribute. Therefore,
        // the net change and new flaws count will always be zero for sev 0. However, we
        // still try to retrieve it (and check for valid result)
        // in case the Detailed Report contain that info in the future and we don't have
        // to release the Jenkins plugin again.
        final String[] SEV_ATTRS = {
                "sev-0-change", "sev-1-change", "sev-2-change", "sev-3-change", "sev-4-change",
                "sev-5-change"
        };
        int[] netChange = new int[SEV_ATTRS.length];

        try {
            // parsing detailedreport.xml file flaw-status tag
            Node node = (Node) xPathObj.evaluate(FLAW_STATUS_NODE_XPATH, xml.getDocumentElement(),
                    XPathConstants.NODE);

            if (null != node) {
                for (int i = 0; i < SEV_ATTRS.length; i++) {
                    String count = null;
                    try {
                        Node sevNode = node.getAttributes().getNamedItem(SEV_ATTRS[i]);
                        if (null != sevNode) {
                            count = sevNode.getNodeValue();
                        }
                    } catch (DOMException de) {
                        // if ran into DOM Exception, skip this attribute and continue
                        netChange[i] = 0;
                        continue;
                    }

                    if (!StringUtil.isNullOrEmpty(count)) {
                        try {
                            netChange[i] = Integer.parseInt(count);
                        } catch (NumberFormatException nfe) {
                            // If the attribute somehow contains a non-numeric value, default to 0
                            netChange[i] = 0;
                        }
                    } else {
                        netChange[i] = 0;
                    }
                }
            }
        } catch (XPathExpressionException xpee) {
            throw new RuntimeException(xpee);
        }

        return netChange;
    }

    /**
     * Find the Max CVSS Score among all the SCA components in the detailed report
     * XML
     *
     * @param xPathObj - XPath evaluation environment
     * @param xml      - The detailed report XML
     * @return the max CVSS score, or -1 to indicate none of the SCA components has
     *         a CVSS score
     * @throws Exception thrown if error is encountered when finding the score
     */
    private static final double parseMaxCVSSScore(XPath xPathObj, Document xml) throws Exception {
        final String SCA_COMPONENTS_NODE_XPATH = "/detailedreport/software_composition_analysis/vulnerable_components/*[local-name()='component']";
        final String MAX_CVSS_SCORE_ATTR = "max_cvss_score";

        double maxOverallScore = -1.0;
        try {
            // parsing detailedreport.xml file flaw tag
            NodeList comps = (NodeList) xPathObj.evaluate(SCA_COMPONENTS_NODE_XPATH,
                    xml.getDocumentElement(), XPathConstants.NODESET);
            String maxCompScoreStr = "";

            for (int i = 0; i < comps.getLength(); i++) {
                Node thisComp = comps.item(i);
                maxCompScoreStr = thisComp.getAttributes().getNamedItem(MAX_CVSS_SCORE_ATTR)
                        .getNodeValue();
                double maxCompScore = -1.0;
                if (!StringUtil.isNullOrEmpty(maxCompScoreStr)) {
                    try {
                        maxCompScore = Double.parseDouble(maxCompScoreStr);
                    } catch (NumberFormatException nfe) {
                        // Ignoring the score that cannot be parsed as a double.
                        maxCompScore = -1;
                    }
                    maxOverallScore = (maxCompScore > maxOverallScore) ? maxCompScore
                            : maxOverallScore;
                }
            }
        } catch (XPathExpressionException xpee) {
            throw new RuntimeException(xpee);
        }
        return maxOverallScore;
    }

    /**
     * Determine if the organization is subscribed to SCA by the existence of the
     * "software_composition_analysis" XML element
     *
     * @param xPathObj - XPath evaluation environment
     * @param xml      - The detailed report XML
     * @return true if the "software_composition_analysis" XML element exists, false
     *         otherwise.
     * @throws Exception thrown if error is encountered during the operation
     */
    private static final boolean isSubscribedToSCA(XPath xPathObj, Document xml) throws Exception {
        return (null != xPathObj.evaluate(SCA_XPATH, xml.getDocumentElement(),
                XPathConstants.NODE));
    }

    /**
     * Find the number of blacklisted components in the detailed report XML
     *
     * @param xPathObj - XPath evaluation environment
     * @param xml      - The detailed report XML
     * @return the number of blacklisted component, or -1 to indicate the count is
     *         not available
     * @throws Exception thrown if error is encountered during the operation
     */
    private static final int parseBlacklistedCompsCount(XPath xPathObj, Document xml)
            throws Exception {
        final String BLACKLISTED_COMPS_ATTR = "blacklisted_components";

        int count = -1;
        Node node = (Node) xPathObj.evaluate(SCA_XPATH, xml.getDocumentElement(),
                XPathConstants.NODE);
        if (null != node) {
            if (null != node.getAttributes().getNamedItem(BLACKLISTED_COMPS_ATTR)) {
                String countStr = node.getAttributes().getNamedItem(BLACKLISTED_COMPS_ATTR)
                        .getNodeValue();
                if (!StringUtil.isNullOrEmpty(countStr)) {
                    count = Integer.parseInt(countStr);
                }
            }
        }
        return count;
    }

    /**
     * Find the number of SCA vulnerabilities in this build and if any severity
     * level contains mitigated vulnerabilities.
     *
     * Calculate the new/net vulnerabilities counts against the last build (which
     * could be empty/null).
     *
     * @param xPathObj          - XPath evaluation environment
     * @param xml               - The detailed report XML
     * @param lastFindingCounts - Finding Counts from the last build
     * @return an instance of FindingCounts
     */
    private static final Set<FindingCounts> parseFindingCounts(XPath xPathObj, Document xml,
            SCAScanHistory lastSCAHistory) {
        final String SCA_VUL_NODE_XPATH = "/detailedreport/software_composition_analysis/vulnerable_components/component/vulnerabilities/*[local-name()='vulnerability']";
        final String MITIGATED_ATTR = "mitigation";
        final String SEVERITY_ATTR = "severity";

        int maxSevLevel = SeverityLevel.values().length;
        int[] counts = new int[maxSevLevel];
        boolean[] mitigated = new boolean[maxSevLevel];
        try {
            NodeList vuls = (NodeList) xPathObj.evaluate(SCA_VUL_NODE_XPATH,
                    xml.getDocumentElement(), XPathConstants.NODESET);

            // Get the severity level and mitigation status of each "vulnerability" element
            // If there was an error parsing an element, the code will skip to the next one.
            for (int i = 0; i < vuls.getLength(); i++) {
                Node thisVul = vuls.item(i);
                try {
                    int sev = Integer.parseInt(
                            thisVul.getAttributes().getNamedItem(SEVERITY_ATTR).getNodeValue());
                    if (sev < maxSevLevel) {
                        boolean thisVulIsMitigated = Boolean.parseBoolean(thisVul.getAttributes()
                                .getNamedItem(MITIGATED_ATTR).getNodeValue());
                        if (!thisVulIsMitigated) {
                            counts[sev]++;
                        }
                        if (false == mitigated[sev] && thisVulIsMitigated) {
                            mitigated[sev] = true;
                        }
                    }
                } catch (NumberFormatException nfe) {
                    continue;
                } catch (DOMException domex) {
                    continue;
                }
            }
        } catch (XPathExpressionException xpee) {
            throw new RuntimeException(xpee);
        }

        Set<FindingCounts> results = new LinkedHashSet<FindingCounts>();
        for (int i = 0; i < maxSevLevel; i++) {
            int newCount = 0, netCount = 0;
            if (null != lastSCAHistory && lastSCAHistory.isSubscribed()) {
                try {
                    netCount = counts[i] - lastSCAHistory
                            .getCountBySeverity(SeverityLevel.findSevLevel(i)).getCount();
                    newCount = Math.max(netCount, 0);
                } catch (IllegalArgumentException iae) {
                    netCount = 0;
                    newCount = 0;
                }
                // for initial scan set to current vulnerability count for particular severity
            } else {
                netCount = counts[i];
                newCount = counts[i];
            }
            results.add(new FindingCounts(SeverityLevel.findSevLevel(i), counts[i], newCount,
                    netCount, mitigated[i]));
        }

        return results;
    }

    /**
     * Parse and provide SCA Component information extracted from the detailed
     * report XML
     *
     * @param xPathObj - XPath evaluation environment
     * @param xml      - The detailed report XML
     * @return an instance of SCAComponent data
     * @throws Exception thrown if error is encountered when finding the particular
     *                   component values
     */
    private static final Set<SCAComponent> parseSCAComponentInfo(XPath xPathObj, Document xml)
            throws Exception {

        final String SCA_COMPONENTS_NODE_XPATH = "/detailedreport/software_composition_analysis/vulnerable_components/*[local-name()='component']";
        final String COMPONENT_NAME_ATTR = "file_name";
        final String IS_BLACKLISTED_ATTR = "blacklisted";
        final String IS_NEW_ATTR = "new";
        final String IS_VIOLATED_POLICY_ATTR = "component_affects_policy_compliance";

        Set<SCAComponent> newSCAComponentInfo = new HashSet<SCAComponent>();

        try {
            // parsing detailedreport.xml sca component tag
            NodeList comps = (NodeList) xPathObj.evaluate(SCA_COMPONENTS_NODE_XPATH,
                    xml.getDocumentElement(), XPathConstants.NODESET);

            String componentName = "";
            boolean isBlacklisted = false;
            boolean isNew = false;
            boolean isViolatedPolicy = false;

            // Loop through all the SCA components. Extract component name and indicators
            // for blacklisted,
            // new component since previous build, and violated policy.
            for (int i = 0; i < comps.getLength(); i++) {
                Node thisComp = comps.item(i);

                try {
                    if (thisComp.getAttributes().getNamedItem(COMPONENT_NAME_ATTR) != null) {
                        componentName = thisComp.getAttributes().getNamedItem(COMPONENT_NAME_ATTR)
                                .getNodeValue();
                    }
                    if (thisComp.getAttributes().getNamedItem(IS_BLACKLISTED_ATTR) != null) {
                        isBlacklisted = Boolean.parseBoolean(thisComp.getAttributes()
                                .getNamedItem(IS_BLACKLISTED_ATTR).getNodeValue());
                    }
                    if (thisComp.getAttributes().getNamedItem(IS_NEW_ATTR) != null) {
                        isNew = Boolean.parseBoolean(
                                thisComp.getAttributes().getNamedItem(IS_NEW_ATTR).getNodeValue());
                    }
                    if (thisComp.getAttributes().getNamedItem(IS_VIOLATED_POLICY_ATTR) != null) {
                        isViolatedPolicy = Boolean.parseBoolean(thisComp.getAttributes()
                                .getNamedItem(IS_VIOLATED_POLICY_ATTR).getNodeValue());
                    }

                    newSCAComponentInfo.add(new SCAComponent(componentName, isBlacklisted, isNew,
                            isViolatedPolicy));

                } catch (DOMException domex) {
                    continue;
                }
            }
        } catch (XPathExpressionException xpee) {
            throw new RuntimeException(xpee);
        }
        return newSCAComponentInfo;
    }

    /**
     * Create a stats with the given build date and count
     *
     * @param buildDate - Build date
     * @param count     - Flaw or vulnerability count
     * @return A map containing the key/value mapping of the 2 parameters
     */
    private static final Map<String, Long> createStats(long buildDate, Long count) {
        Map<String, Long> thisScanStats = new HashMap<String, Long>();
        thisScanStats.put(ScanHistory.BUILD_DATE, buildDate);
        thisScanStats.put(ScanHistory.FLAWS_COUNT, count);

        return thisScanStats;
    }

    /**
     * Create count history based off the given count history from last build
     *
     * @param countInThisBuild - The count in the current build. This could be null
     *                         if no data is available in this build
     * @param lastCountHistory - Count history from last build
     * @return count history after combining the count history from last build and
     *         current build
     */
    private static final List<Map<String, Long>> createCountHistory(
            Map<String, Long> countInThisBuild, List<Map<String, Long>> lastCountHistory) {
        // Construct the scan history of the past (at most) 8 builds
        final int MAX_PREV_BUILDS = 8;
        List<Map<String, Long>> countHistory = null;
        // If we cannot find the last build (ex. the first build of the project) or the
        // history is empty somehow,
        // then just insert the stats of this build.
        if (null == lastCountHistory || lastCountHistory.size() == 0) {
            countHistory = new ArrayList<Map<String, Long>>();
            countHistory.add(countInThisBuild);
        } else {
            countHistory = new ArrayList<Map<String, Long>>(lastCountHistory);
            // Reached the max number of build stats, remove the oldest (at index 0) and
            // append the stats from this build.
            // BTW, the list should never exceed the MAX_PREV_BUILDS, so the loop should
            // never executed more than once. But,
            // let's be safe and trim down the list to MAX_PREV_BUILDS, no matter how many
            // over the size is.
            while (countHistory.size() >= MAX_PREV_BUILDS) {
                countHistory.remove(0);
            }
            countHistory.add(countInThisBuild);
        }

        return countHistory;
    }
}