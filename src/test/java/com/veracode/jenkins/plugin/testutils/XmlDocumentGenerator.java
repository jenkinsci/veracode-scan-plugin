package com.veracode.jenkins.plugin.testutils;

public class XmlDocumentGenerator {

	// getapplist
	private static final String GETAPPLIST_XML_appId_appName =
	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
	"<applist xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"https://analysiscenter.veracode.com/schema/2.0/applist\" xsi:schemaLocation=\"https://analysiscenter.veracode.com/schema/2.0/applist https://analysiscenter.veracode.com/resource/2.0/applist.xsd\" applist_version=\"1.2\" account_id=\"10001\">\r\n" +
	"	<app app_id=\"%s\" app_name=\"%s\" policy_updated_date=\"\"/>\r\n" +
	"</applist>";

	// getbuildinfo
	private static final String GETBUILDINFO_XML_appId_sandboxId_buildId_buildName_buildId_status =
	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
	"<buildinfo xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"https://analysiscenter.veracode.com/schema/4.0/buildinfo\" xsi:schemaLocation=\"https://analysiscenter.veracode.com/schema/4.0/buildinfo https://analysiscenter.veracode.com/resource/4.0/buildinfo.xsd\" buildinfo_version=\"1.4\" account_id=\"%s\" app_id=\"%s\" sandbox_id=\"%s\" build_id=\"%s\">\r\n" +
	"	<build version=\"%s\" build_id=\"%s\" submitter=\"\" platform=\"\" lifecycle_stage=\"\" results_ready=\"false\" policy_name=\"Veracode Recommended Very High\" policy_version=\"1\" policy_compliance_status=\"Calculating...\" rules_status=\"Calculating...\" grace_period_expired=\"false\" scan_overdue=\"false\" legacy_scan_engine=\"false\">\r\n" +
	"		<analysis_unit analysis_type=\"Static\" status=\"%s\"/>\r\n" +
	"	</build>\r\n" +
	"</buildinfo>";

	// getsandboxlist
	private static final String GETSANDBOXLIST_XML_appId_sandboxId_sandboxName =
	"<sandboxlist xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"https://analysiscenter.veracode.com/schema/4.0/sandboxlist\" xsi:schemaLocation=\"https://analysiscenter.veracode.com/schema/4.0/sandboxlist https://analysiscenter.veracode.com/resource/4.0/sandboxlist.xsd\" sandboxlist_version=\"1.0\" account_id=\"10001\" app_id=\"%s\">\r\n" +
	"	<sandbox sandbox_id=\"%s\" sandbox_name=\"%s\" owner=\"OWNER\" last_modified=\"\"/>\r\n" +
	"</sandboxlist>";

	// summaryreport
	public static final String SUMMARYREPORT_XML_appId_buildId_buildName_policyStatus_policyRulesStatus =
	"<?xml version='1.0' encoding='UTF-8'?>" +
	"<summaryreport xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"https://www.veracode.com/schema/reports/export/1.0\" xsi:schemaLocation=\"https://www.veracode.com/schema/reports/export/1.0 https://vospqaweb.veracode.local/resource/summaryreport.xsd\" report_format_version=\"1.3\" app_name=\"app_name\" app_id=\"%s\" first_build_submitted_date=\"\" build_id=\"%s\" version=\"%s\" submitter=\"Auto\" platform=\"\" assurance_level=\"5\" business_criticality=\"\" generation_date=\"\" veracode_level=\"\" total_flaws=\"27\" flaws_not_mitigated=\"27\" teams=\"\" life_cycle_stage=\"\" planned_deployment_date=\"\" last_update_time=\"\" is_latest_build=\"true\" policy_name=\"Veracode Transitional Very High\" policy_version=\"1\" policy_compliance_status=\"%s\" policy_rules_status=\"%s\" grace_period_expired=\"true\" scan_overdue=\"false\" business_owner=\"\" business_unit=\"\" tags=\"\" legacy_scan_engine=\"false\">\r\n" +
	"	<static-analysis rating=\"D\" score=\"86\" submitted_date=\"\" published_date=\"\" analysis_size_bytes=\"\" engine_version=\"\">\r\n" +
	"		<modules>" +
	"			<module name=\"abc-1.2.13.jar\" compiler=\"JAVAC_1_4\" os=\"Java J2SE 6\" architecture=\"JVM\" loc=\"26229\" score=\"86\" numflawssev0=\"0\" numflawssev1=\"0\" numflawssev2=\"3\" numflawssev3=\"22\" numflawssev4=\"1\" numflawssev5=\"1\" />" + 
	"		</modules>" +
	"	</static-analysis>" +
	"	<severity level=\"5\"><category categoryname=\"Untrusted Search Path\" severity=\"Very High\" count=\"1\" /></severity>" +
	"	<severity level=\"4\"><category categoryname=\"SQL Injection\" severity=\"High\" count=\"1\" /></severity>" +
	"	<severity level=\"3\"><category categoryname=\"Directory Traversal\" severity=\"Medium\" count=\"9\" /><category categoryname=\"Information Leakage\" severity=\"Medium\" count=\"6\" /><category categoryname=\"Encapsulation\" severity=\"Medium\" count=\"3\" /><category categoryname=\"Insufficient Input Validation\" severity=\"Medium\" count=\"3\" /><category categoryname=\"Credentials Management\" severity=\"Medium\" count=\"1\" /></severity>" +
	"	<severity level=\"2\"><category categoryname=\"Code Quality\" severity=\"Low\" count=\"3\" /></severity>" +
	"	<severity level=\"1\" />" +
	"	<severity level=\"0\" />" +
	"	<flaw-status new=\"27\" reopen=\"0\" open=\"0\" fixed=\"0\" total=\"27\" not_mitigated=\"27\" sev-1-change=\"0\" sev-2-change=\"3\" sev-3-change=\"22\" sev-4-change=\"1\" sev-5-change=\"1\" />" +
	"</summaryreport>";
	
	// detailedreport
	private static final String DETAILEDREPORT_XML = 
	"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\r\n" + 
	"<detailedreport xmlns=\"https://www.veracode.com/schema/reports/export/1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" account_id=\"%s\" analysis_id=\"4705951\"  app_id=\"%s\" app_name=\"Apache\" assurance_level=\"5\" build_id=\"%s\" business_criticality=\"5\" business_owner=\"\" business_unit=\"\" first_build_submitted_date=\"\" flaws_not_mitigated=\"22\" generation_date=\"\" grace_period_expired=\"true\" is_latest_build=\"true\" last_update_time=\"\" legacy_scan_engine=\"false\" life_cycle_stage=\"Not Specified\" planned_deployment_date=\"\" platform=\"Not Specified\" policy_compliance_status=\"Did Not Pass\" policy_name=\"Veracode Transitional Very High\" policy_rules_status=\"Did Not Pass\" policy_version=\"1\" report_format_version=\"1.5\" sandbox_id=\"\" scan_overdue=\"\" static_analysis_unit_id=\"\" submitter=\"\" tags=\"\" teams=\"\" total_flaws=\"22\" veracode_level=\"VL1\" version=\"13 Aug 2019 Static\" xsi:schemaLocation=\"https://www.veracode.com/schema/reports/export/1.0 https://analysiscenter.veracode.com/resource/detailedreport.xsd\">\r\n" + 
	"	<static-analysis analysis_size_bytes=\"\" engine_version=\"\" published_date=\"\" rating=\"D\" score=\"82\" submitted_date=\"\" version=\"13 Aug 2019 Static\">\r\n" + 
	"		<modules>\r\n" + 
	"			<module architecture=\"\" compiler=\"\" loc=\"\" name=\"\" numflawssev0=\"0\" numflawssev1=\"0\" numflawssev2=\"6\" numflawssev3=\"13\" numflawssev4=\"0\" numflawssev5=\"3\" os=\"Red Hat Enterprise Linux v4 (IA32)\" score=\"82\"/>\r\n" + 
	"		</modules>\r\n" + 
	"	</static-analysis>\r\n" + 
	"	<severity level=\"5\">\r\n" + 
	"		<category categoryid=\"3\" categoryname=\"\" pcirelated=\"false\">\r\n" + 
	"			<desc>\r\n" + 
	"				<para text=\"\"/>\r\n" + 
	"			</desc>\r\n" + 
	"			<recommendations>\r\n" + 
	"				<para text=\"\">\r\n" + 
	"					<bulletitem text=\"\"/>\r\n" + 
	"				</para>\r\n" + 
	"			</recommendations> \r\n" + 
	"		</category>\r\n" + 
	"	</severity>\r\n" + 
	"</detailedreport>";

	// detailedreport DA
	private static final String DA_DETAILEDREPORT_XML =
	"<detailedreport xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"https://www.veracode.com/schema/reports/export/1.0\" xsi:schemaLocation=\"https://www.veracode.com/schema/reports/export/1.0 https://analysiscenter-stage-103.stage.veracode.io/resource/detailedreport.xsd\" report_format_version=\"1.5\" account_id=\"%s\" app_name=\"dyn_da_app1\" app_id=\"%s\" analysis_id=\"122131\" static_analysis_unit_id=\"0\" sandbox_id=\"181851\" version=\"DA_Scans3\" build_id=\"123064\" submitter=\"\" platform=\"\" assurance_level=\"4\" business_criticality=\"4\" generation_date=\"\" veracode_level=\"VL2\" total_flaws=\"106\" flaws_not_mitigated=\"106\" teams=\"\" life_cycle_stage=\"\" planned_deployment_date=\"\" last_update_time=\"\" is_latest_build=\"true\" policy_name=\"Veracode Recommended High\" policy_version=\"1\" policy_compliance_status=\"Did Not Pass\" policy_rules_status=\"Did Not Pass\" grace_period_expired=\"true\" scan_overdue=\"true\" business_owner=\"\" business_unit=\"Not Specified\" tags=\"\">\r\n" + 
	"	<dynamic-analysis rating=\"A\" score=\"87\" submitted_date=\"\" published_date=\"\" version=\"DA_Scans3\" dynamic_scan_type=\"da\" scan_exit_status_id=\"13\" scan_exit_status_desc=\"min_priority\">\r\n" + 
	"		<modules>\r\n" + 
	"			<module name=\"dynamic_analysis\" compiler=\"Unknown\" os=\"\" architecture=\"\" loc=\"0\" score=\"87\" numflawssev0=\"1\" numflawssev1=\"0\" numflawssev2=\"87\" numflawssev3=\"18\" numflawssev4=\"0\" numflawssev5=\"0\" target_url=\"\" domain=\"\"/>\r\n" + 
	"		</modules>\r\n" + 
	"	</dynamic-analysis>\r\n" + 
	"	<severity level=\"5\"/>\r\n" + 
	"	<severity level=\"4\"/>\r\n" + 
	"	<severity level=\"3\">\r\n" + 
	"		<category categoryid=\"11\" categoryname=\"Authentication Issues\" pcirelated=\"false\">\r\n" + 
	"			<desc>\r\n" + 
	"				<para text=\"\"/>\r\n" + 
	"			</desc>\r\n" + 
	"			<recommendations>\r\n" + 
	"				<para text=\"\"/>\r\n" +  
	"			</recommendations>\r\n" + 
	"			<cwe cweid=\"352\" cwename=\"Cross-Site Request Forgery (CSRF)\" pcirelated=\"false\" sans=\"864\">\r\n" + 
	"				<description>\r\n" + 
	"					<text text=\"\"/>\r\n" + 
	"				</description>\r\n" + 
	"				<dynamicflaws>\r\n" + 
	"					<flaw severity=\"3\" categoryname=\"Cross-Site Request Forgery (CSRF)\" count=\"1\" issueid=\"86\" module=\"dynamic_analysis\" type=\"Cross-Site Request Forgery (CSRF)\" description=\"\" note=\"\" cweid=\"352\" remediationeffort=\"4\" categoryid=\"11\" pcirelated=\"false\" date_first_occurrence=\"\" remediation_status=\"Open\" cia_impact=\"ppn\" grace_period_expires=\"\" affects_policy_compliance=\"true\" mitigation_status=\"none\" mitigation_status_desc=\"Not Mitigated\" url=\"http://xyz.com/logout.php\" vuln_parameter=\"logged_in\"/>\r\n" + 
	"				</dynamicflaws>\r\n" + 
	"			</cwe>\r\n" + 
	"		</category>\r\n" +  
	"	</severity>\r\n" + 
	"	<severity level=\"1\"/>\r\n" + 
	"	<severity level=\"0\"/>\r\n" + 
	"	<flaw-status new=\"1\" reopen=\"0\" open=\"105\" cannot-reproduce=\"0\" fixed=\"1\" total=\"106\" not_mitigated=\"106\" sev-1-change=\"0\" sev-2-change=\"0\" sev-3-change=\"0\" sev-4-change=\"0\" sev-5-change=\"0\"/>\r\n" + 
	"</detailedreport>";

	// detailedreport SCA
	private static final String SCA_DETAILEDREPORT_XML =
	"<detailedreport xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"https://www.veracode.com/schema/reports/export/1.0\" xsi:schemaLocation=\"https://www.veracode.com/schema/reports/export/1.0 https://analysiscenter-stage-103.stage.veracode.io/resource/detailedreport.xsd\" report_format_version=\"1.5\" account_id=\"12463\" app_name=\"Sample_SCA_Test_01\" app_id=\"76015\" analysis_id=\"68976\" static_analysis_unit_id=\"68976\" sandbox_id=\"86214\" first_build_submitted_date=\"\" version=\"Scan#001\" build_id=\"69198\" submitter=\"\" platform=\"Not Specified\" assurance_level=\"5\" business_criticality=\"5\" generation_date=\"\" veracode_level=\"VL3\" total_flaws=\"3\" flaws_not_mitigated=\"3\" teams=\"\" life_cycle_stage=\"\" planned_deployment_date=\"\" last_update_time=\"\" is_latest_build=\"true\" policy_name=\"\" policy_version=\"2\" policy_compliance_status=\"Did Not Pass\" policy_rules_status=\"Did Not Pass\" grace_period_expired=\"true\" scan_overdue=\"false\" business_owner=\"\" business_unit=\"\" tags=\"\" legacy_scan_engine=\"\">\r\n" + 
	"	<static-analysis rating=\"B\" score=\"98\" submitted_date=\"\" published_date=\"\" version=\"Scan#001\" analysis_size_bytes=\"1632\" engine_version=\"\">\r\n" + 
	"		<modules>\r\n" + 
	"			<module name=\"Flaws_3.jar\" compiler=\"JAVAC_8\" os=\"Java J2SE 8\" architecture=\"JVM\" loc=\"45\" score=\"98\" numflawssev0=\"0\" numflawssev1=\"0\" numflawssev2=\"0\" numflawssev3=\"3\" numflawssev4=\"0\" numflawssev5=\"0\"/>\r\n" + 
	"		</modules>\r\n" + 
	"	</static-analysis>\r\n" + 
	"	<severity level=\"5\"/>\r\n" + 
	"	<severity level=\"4\"/>\r\n" + 
	"	<severity level=\"3\">\r\n" + 
	"		<category categoryid=\"10\" categoryname=\"\" pcirelated=\"\">\r\n" + 
	"			<desc>\r\n" + 
	"				<para text=\"\"/>\r\n" + 
	"			</desc>\r\n" + 
	"			<recommendations>\r\n" + 
	"				<para text=\"\"/>\r\n" + 
	"			</recommendations>\r\n" + 
	"			<cwe cweid=\"259\" cwename=\"\" pcirelated=\"\" owasp=\"\" sans=\"\" certjava=\"\">\r\n" + 
	"				<description>\r\n" + 
	"					<text text=\"\"/>\r\n" + 
	"				</description>\r\n" + 
	"				<staticflaws>\r\n" + 
	"					<flaw severity=\"3\" categoryname=\"\" count=\"1\" issueid=\"3\" module=\"Flaws_3.jar\" type=\"hardcodepasswdset\" description=\"\" note=\"\" cweid=\"259\" remediationeffort=\"4\" exploitLevel=\"1\" categoryid=\"10\" pcirelated=\"true\" date_first_occurrence=\"\" remediation_status=\"New\" cia_impact=\"ppn\" grace_period_expires=\"\" affects_policy_compliance=\"false\" mitigation_status=\"none\" mitigation_status_desc=\"Not Mitigated\" sourcefile=\"Main.java\" line=\"22\" sourcefilepath=\"kin/scratch/\" scope=\"\" functionprototype=\"\" functionrelativelocation=\"\">\r\n" + 
	"						<annotations>\r\n" + 
	"							<annotation action=\"\" description=\"\"/>\r\n" + 
	"						</annotations>\r\n" + 
	"					</flaw>\r\n" + 
	"				</staticflaws>\r\n" + 
	"			</cwe>\r\n" + 
	"		</category>\r\n" +
	"	</severity>\r\n" + 
	"	<severity level=\"2\"/>\r\n" + 
	"	<severity level=\"1\"/>\r\n" + 
	"	<severity level=\"0\"/>\r\n" + 
	"	<flaw-status new=\"3\" reopen=\"0\" open=\"0\" fixed=\"0\" total=\"3\" not_mitigated=\"3\" sev-1-change=\"0\" sev-2-change=\"0\" sev-3-change=\"3\" sev-4-change=\"0\" sev-5-change=\"0\"/>\r\n" + 
	"	<customfields>\r\n" + 
	"		<customfield name=\"Custom 1\" value=\"\"/>\r\n" + 
	"		<customfield name=\"Custom 2\" value=\"\"/>\r\n" +
	"	</customfields>\r\n" + 
	"	<software_composition_analysis third_party_components=\"3\" violate_policy=\"true\" components_violated_policy=\"2\" blocklisted_components=\"2\">\r\n" + 
	"		<vulnerable_components>\r\n" + 
	"			<component component_id=\"232ab-343c\" file_name=\"test-10.0.0-alpha0.jar\" sha1=\"\" vulnerabilities=\"0\" max_cvss_score=\"\" version=\"10.0.0-alpha0\" library=\"test-server\" vendor=\"org.abc.test\" description=\"\" added_date=\"\" component_affects_policy_compliance=\"false\">\r\n" + 
	"				<file_paths>\r\n" + 
	"					<file_path value=\".zip#zip:binaries/test-server-10.0.0-alpha0.jar\"/>\r\n" + 
	"				</file_paths>\r\n" + 
	"				<licenses/>\r\n" + 
	"				<vulnerabilities/>\r\n" + 
	"				<violated_policy_rules/>\r\n" + 
	"			</component>\r\n" + 
	"		</vulnerable_components>\r\n" + 
	"	</software_composition_analysis>\r\n" + 
	"</detailedreport>";

	// error
	private static final String ERROR_XML = 
	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
	"<error>%s</error>";

	public static final String getGetAppListXmlDocument(String appId, String appNam) {
		return String.format(GETAPPLIST_XML_appId_appName, appId, appNam);
	}

	public static final String getGetBuildInfoXmlDocument(String accId, String appId, String sandboxId, String buildId,
			String buildName, String status) {
		return String.format(GETBUILDINFO_XML_appId_sandboxId_buildId_buildName_buildId_status, accId, appId, sandboxId,
				buildId, buildName, buildId, status);
	}

	public static final String getGetSandboxListXmlDocument(String appId, String sandboxId, String sandboxName) {
		return String.format(GETSANDBOXLIST_XML_appId_sandboxId_sandboxName, appId, sandboxId, sandboxName);
	}

	public static final String getSummaryReportXmlDocument(String appId, String buildId, String buildName,
			String policyStatus) {
		return String.format(SUMMARYREPORT_XML_appId_buildId_buildName_policyStatus_policyRulesStatus, appId, buildId,
				buildName, policyStatus, policyStatus);
	}

	public static final String getDetailedReportXmlDocument(Object... args) {
		return String.format(DETAILEDREPORT_XML, args);
	}

	public static final String getDADetailedReportXmlDocument(Object... args) {
		return String.format(DA_DETAILEDREPORT_XML, args);
	}

	public static final String getSCADetailedReportXmlDocument() {
		return SCA_DETAILEDREPORT_XML;
	}

	public static final String getErrorXmlDocument(String errorString) {
		return String.format(ERROR_XML, errorString);
	}
}