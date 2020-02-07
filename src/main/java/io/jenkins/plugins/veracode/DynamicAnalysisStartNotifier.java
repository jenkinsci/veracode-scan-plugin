package io.jenkins.plugins.veracode;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import io.jenkins.plugins.veracode.VeracodeNotifier.VeracodeDescriptor;
import io.jenkins.plugins.veracode.common.Constant;
import io.jenkins.plugins.veracode.common.DAAdapterService;
import io.jenkins.plugins.veracode.data.CredentialsBlock;
import io.jenkins.plugins.veracode.data.ProxyBlock;
import io.jenkins.plugins.veracode.utils.EncryptionUtil;
import io.jenkins.plugins.veracode.utils.FormValidationUtil;
import io.jenkins.plugins.veracode.utils.StringUtil;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/*
 * DynamicAnalysisStartNotifier handles processing for post build action "Resubmit Veracode Dynamic Analysis".
 * The UI interface is defined in associated config.jelly.
 *
 * User provides:
 *    - Dynamic Analysis name
 *    - max duration in hours for analysis scan
 *    - whether to fail Jenkins build if the analysis fails to run
 *    - whether to use global API credentials or define ID/Key specific to the job.
 *
 * This class extends the {@link hudson.tasks.Notifier Notifier} class.
 */

public class DynamicAnalysisStartNotifier extends Notifier {

	private final String analysisName;
	private final int maximumDuration;
	private final boolean failBuildAsScanFailed;
	private final CredentialsBlock credentials;
	private boolean isGlobalCredentialsEnabled;

	@DataBoundConstructor
	public DynamicAnalysisStartNotifier(final String analysisName, final int maximumDuration,
			final boolean failBuildAsScanFailed, final CredentialsBlock credentials) {

		this.analysisName = analysisName;
		this.maximumDuration = maximumDuration;
		this.failBuildAsScanFailed = failBuildAsScanFailed;
		this.credentials = credentials;

        if (credentials == null) {
            this.isGlobalCredentialsEnabled = true;
        } else {
            this.isGlobalCredentialsEnabled = false;
        }
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	/**
	 * Called by Jenkins after a build for a job specified to use the plugin is performed.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {

		DynamicAnalysisStartDescriptor descriptor = getDescriptor();

		String apiID = getCredentials() != null ? getVid() : descriptor.getGvid();
		String apiKey = getCredentials() != null ? getVkey() : descriptor.getGvkey();
		ProxyBlock proxyBlock = null;
		if (descriptor.isProxyEnabled()) {
			proxyBlock = new ProxyBlock(descriptor.getPhost(), descriptor.getPport(), descriptor.getPuser(),
				descriptor.getPpassword());
		}

		DAAdapterService daAdapterService = new DAAdapterService();
		return daAdapterService.resubmitDynamicAnalysis(build, build.getWorkspace(), listener, getAnalysisName(),
				getMaximumDuration(), isFailBuildAsScanFailed(), apiID, apiKey, descriptor.isDebugEnabled(),
				proxyBlock);
	}

	@Override
	public DynamicAnalysisStartDescriptor getDescriptor() {
		return (DynamicAnalysisStartDescriptor) super.getDescriptor();
	}

	@Extension
	public static class DynamicAnalysisStartDescriptor extends BuildStepDescriptor<Publisher> {

		private String gvid;
		private String gvkey;
		private boolean debugEnabled;
		private boolean proxyEnabled;
		private String phost;
		private String pport;
		private String puser;
		private String ppassword;

		/**
		 * Load the persisted global configuration.
		 */
		public DynamicAnalysisStartDescriptor() {
			load();
		}

		/**
		 * The name of the plugin displayed in the UI.
		 */
		@Override
		public String getDisplayName() {
			return Constant.POST_BUILD_ACTION_DISPLAY_TEXT_RESUBMIT;
		}

		/**
		 * Whether this task is applicable to the given project.
		 */
		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		/**
		 * Called when the user saves the project configuration.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			formData.put("analysisName", EncryptionUtil.encrypt(formData.getString("analysisName")));
			String maximumDuration = formData.getString("maximumDuration");
			formData.put("maximumDuration",
					StringUtil.isNullOrEmpty(maximumDuration) ? Constant.DEFAULT_VALUE_DA_MAX_DURATION_HOURS
							: maximumDuration);
			Map<String, Object> credMap = (Map<String, Object>) formData.get("credentials");
			if (credMap != null) {
				credMap.put("vid", EncryptionUtil.encrypt((String) credMap.get("vid")));
				credMap.put("vkey", EncryptionUtil.encrypt((String) credMap.get("vkey")));
				formData.put("credentials", credMap);
			}

			return super.newInstance(req, formData);
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			updateFromGlobalConfiguration();
			req.bindJSON(this, formData);
			save();
			return super.configure(req, formData);
		}

		// Validate analysis name
		public FormValidation doCheckAnalysisName(@QueryParameter String analysisName) throws IOException, ServletException {
			return FormValidationUtil.checkAnalysisName(analysisName);
		}

		// Validate maximum duration
		public FormValidation doCheckMaximumDuration(@QueryParameter String maximumDuration) throws IOException, ServletException {
			return FormValidationUtil.checkMaximumDuration(maximumDuration);
		}

		// Validate Veracode API ID
		public FormValidation doCheckVid(@QueryParameter("vid") String vid, @QueryParameter("vkey") String vkey)
				throws IOException, ServletException {
			return FormValidationUtil.checkApiId(vid, vkey, hasGlobalApiIdKeyCredentials());
		}

		// Validate Veracode API Key
		public FormValidation doCheckVkey(@QueryParameter("vid") String vid, @QueryParameter("vkey") String vkey)
				throws IOException, ServletException {
			return FormValidationUtil.checkApiKey(vid, vkey, hasGlobalApiIdKeyCredentials());
		}

		private void updateFromGlobalConfiguration() {
			VeracodeDescriptor globalVeracodeDescriptor = (VeracodeDescriptor) Jenkins.getInstance()
					.getDescriptor(VeracodeNotifier.class);
			if (globalVeracodeDescriptor != null) {
				gvid = globalVeracodeDescriptor.getGvid();
				gvkey = globalVeracodeDescriptor.getGvkey();
				debugEnabled = globalVeracodeDescriptor.getDebug();
				proxyEnabled = globalVeracodeDescriptor.getProxy();
				phost = globalVeracodeDescriptor.getPhost();
				pport = globalVeracodeDescriptor.getPport();
				puser = globalVeracodeDescriptor.getPuser();
				ppassword = globalVeracodeDescriptor.getPpassword();
			}
		}

		// Methods to get global configuration
		public String getGvid() {
			return gvid;
		}

		public String getGvkey() {
			return gvkey;
		}

		public boolean isDebugEnabled() {
			return debugEnabled;
		}

		public boolean isProxyEnabled() {
			return proxyEnabled;
		}

		public String getPhost() {
			return phost;
		}

		public String getPport() {
			return pport;
		}

		public String getPuser() {
			return puser;
		}

		public String getPpassword() {
			return ppassword;
		}

		// Check if Veracode API ID and key are saved in global configuration
		public boolean hasGlobalApiIdKeyCredentials() {
			return !StringUtil.isNullOrEmpty(getGvid()) && !StringUtil.isNullOrEmpty(getGvkey());
		}
	}

	// Getter methods

	public String getAnalysisName() {
		return EncryptionUtil.decrypt(analysisName);
	}

	public int getMaximumDuration() {
		return maximumDuration;
	}

	public boolean isFailBuildAsScanFailed() {
		return failBuildAsScanFailed;
	}

	public boolean isGlobalCredentialsEnabled() {
		return isGlobalCredentialsEnabled;
	}

	public CredentialsBlock getCredentials() {
		return credentials;
	}

	public String getVid() {
		return EncryptionUtil.decrypt((this.credentials != null) ? this.credentials.getVid() : null);
	}

	public String getVkey() {
		return EncryptionUtil.decrypt((this.credentials != null) ? this.credentials.getVkey() : null);
	}
}