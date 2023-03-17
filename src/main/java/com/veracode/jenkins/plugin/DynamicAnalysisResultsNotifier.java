package com.veracode.jenkins.plugin;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.veracode.jenkins.plugin.VeracodeNotifier.VeracodeDescriptor;
import com.veracode.jenkins.plugin.common.Constant;
import com.veracode.jenkins.plugin.common.DAAdapterService;
import com.veracode.jenkins.plugin.data.CredentialsBlock;
import com.veracode.jenkins.plugin.data.ProxyBlock;
import com.veracode.jenkins.plugin.utils.EncryptionUtil;
import com.veracode.jenkins.plugin.utils.FormValidationUtil;
import com.veracode.jenkins.plugin.utils.StringUtil;

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

/**
 * The DynamicAnalysisResultsNotifier class handles processing for post build
 * action "Review Veracode Dynamic Analysis Results". The UI interface is
 * defined in associated config.jelly.
 * <p>
 * User provides: 
 *  - how long to wait for analysis results (in minutes) 
 *  - whether to use global API credentials or define ID/Key specific to the job.
 *
 * This class extends the {@link hudson.tasks.Notifier Notifier} class.
 */
public class DynamicAnalysisResultsNotifier extends Notifier {

    private final int waitForResultsDuration;
    private final boolean failBuildForPolicyViolation;
    private final CredentialsBlock credentials;
    private boolean isGlobalCredentialsEnabled;

    /**
     * Constructor for DynamicAnalysisResultsNotifier.
     *
     * @param waitForResultsDuration      a int.
     * @param failBuildForPolicyViolation a boolean.
     * @param credentials                 a
     *                                    {@link com.veracode.jenkins.plugin.data.CredentialsBlock}
     *                                    object.
     */
    @DataBoundConstructor
    public DynamicAnalysisResultsNotifier(final int waitForResultsDuration,
            final boolean failBuildForPolicyViolation, final CredentialsBlock credentials) {

        this.waitForResultsDuration = waitForResultsDuration;
        this.failBuildForPolicyViolation = failBuildForPolicyViolation;
        this.credentials = credentials;

        if (credentials == null) {
            this.isGlobalCredentialsEnabled = true;
        } else {
            this.isGlobalCredentialsEnabled = false;
        }
    }

    /**
     * Returns an object that represents the scope of the synchronization monitor
     * expected by the plugin.
     */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    /**
     * Called by Jenkins after a build for a job specified to use the plugin is
     * performed.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher,
            final BuildListener listener) {

        DynamicAnalysisResultsDescriptorImpl descriptor = getDescriptor();
        descriptor.updateFromGlobalConfiguration();

        String apiId = getCredentials() != null ? getVid() : descriptor.getGvid();
        String apiKey = getCredentials() != null ? getVkey() : descriptor.getGvkey();
        ProxyBlock proxyBlock = null;
        if (descriptor.isProxyEnabled()) {
            proxyBlock = new ProxyBlock(descriptor.getPhost(), descriptor.getPport(),
                    descriptor.getPuser(), descriptor.getPpassword());
        }

        DAAdapterService daAdapterService = new DAAdapterService();
        return daAdapterService.reviewDynamicAnalysis(build, build.getWorkspace(), listener,
                waitForResultsDuration, failBuildForPolicyViolation, apiId, apiKey,
                descriptor.isDebugEnabled(), proxyBlock);
    }

    /**
     * Returns the
     * {@link com.veracode.jenkins.plugin.DynamicAnalysisResultsNotifier.DynamicAnalysisResultsDescriptorImpl}
     * object associated with this instance.
     *
     */
    @Override
    public DynamicAnalysisResultsDescriptorImpl getDescriptor() {
        return (DynamicAnalysisResultsDescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DynamicAnalysisResultsDescriptorImpl
            extends BuildStepDescriptor<Publisher> {

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
        public DynamicAnalysisResultsDescriptorImpl() {
            load();
        }

        /**
         * The name of the plugin displayed in the UI.
         */
        @Override
        public String getDisplayName() {
            return Constant.POST_BUILD_ACTION_DISPLAY_TEXT_REVIEW;
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
            String waitForResultsDuration = formData.getString("waitForResultsDuration");
            formData.put("waitForResultsDuration",
                    StringUtil.isNullOrEmpty(waitForResultsDuration)
                            ? Constant.DEFAULT_VALUE_DA_WAIT_FOR_RESULTS_MINUTES
                            : waitForResultsDuration);
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

        // Validate wait for results duration
        public FormValidation doCheckWaitForResultsDuration(
                @QueryParameter String waitForResultsDuration)
                throws IOException, ServletException {
            return FormValidationUtil.checkWaitForResultsDuration(waitForResultsDuration);
        }

        // Validate Veracode API ID
        public FormValidation doCheckVid(@QueryParameter("vid") String vid,
                @QueryParameter("vkey") String vkey) throws IOException, ServletException {
            return FormValidationUtil.checkApiId(vid, vkey, hasGlobalApiIdKeyCredentials());
        }

        // Validate Veracode API Key
        public FormValidation doCheckVkey(@QueryParameter("vid") String vid,
                @QueryParameter("vkey") String vkey) throws IOException, ServletException {
            return FormValidationUtil.checkApiKey(vid, vkey, hasGlobalApiIdKeyCredentials());
        }

        private void updateFromGlobalConfiguration() {
            VeracodeDescriptor globalVeracodeDescriptor = (VeracodeDescriptor) Jenkins.get()
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

    public int getWaitForResultsDuration() {
        return waitForResultsDuration;
    }

    public boolean isFailBuildForPolicyViolation() {
        return failBuildForPolicyViolation;
    }

    public boolean isGlobalCredentialsEnabled() {
        return isGlobalCredentialsEnabled;
    }

    public CredentialsBlock getCredentials() {
        return credentials;
    }

    public String getVid() {
        return EncryptionUtil
                .decrypt((this.credentials != null) ? this.credentials.getVid() : null);
    }

    public String getVkey() {
        return EncryptionUtil
                .decrypt((this.credentials != null) ? this.credentials.getVkey() : null);
    }
}
