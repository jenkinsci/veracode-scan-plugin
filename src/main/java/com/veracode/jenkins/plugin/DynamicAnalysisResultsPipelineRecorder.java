package com.veracode.jenkins.plugin;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.veracode.jenkins.plugin.common.Constant;
import com.veracode.jenkins.plugin.common.DAAdapterService;
import com.veracode.jenkins.plugin.data.ProxyBlock;
import com.veracode.jenkins.plugin.utils.FormValidationUtil;
import com.veracode.jenkins.plugin.utils.StringUtil;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

/**
 * The DynamicAnalysisResultsPipelineRecorder class handles processing for
 * "veracodeDynamicAnalysisReview" Pipeline script. The UI interface of Snippet
 * Generator for "veracodeDynamicAnalysisReview : Review Veracode Dynamic
 * Analysis Results" is defined in associated config.jelly.
 * <p>
 * This class extends the {@link hudson.tasks.Recorder} class.
 * 
 */
public class DynamicAnalysisResultsPipelineRecorder extends Recorder implements SimpleBuildStep {

    @DataBoundSetter
    public final int waitForResultsDuration;
    @DataBoundSetter
    public final boolean failBuildForPolicyViolation;
    @DataBoundSetter
    public final boolean debug;
    @DataBoundSetter
    public final String vid;
    @DataBoundSetter
    public final String vkey;
    // Proxy
    @DataBoundSetter
    public final boolean useProxy;
    @DataBoundSetter
    public final String pHost;
    @DataBoundSetter
    public final String pPort;
    @DataBoundSetter
    public final String pUser;
    @DataBoundSetter
    public final String pPassword;

    /**
     * Constructor for DynamicAnalysisResultsPipelineRecorder.
     *
     * @param waitForResultsDuration      a int.
     * @param failBuildForPolicyViolation a boolean.
     * @param debug                       a boolean.
     * @param useProxy                    a boolean.
     * @param pHost                       a {@link java.lang.String} object.
     * @param pPort                       a {@link java.lang.String} object.
     * @param pUser                       a {@link java.lang.String} object.
     * @param pPassword                   a {@link java.lang.String} object.
     * @param vid                         a {@link java.lang.String} object.
     * @param vkey                        a {@link java.lang.String} object.
     */
    @DataBoundConstructor
    public DynamicAnalysisResultsPipelineRecorder(int waitForResultsDuration,
            boolean failBuildForPolicyViolation, boolean debug, boolean useProxy, String pHost,
            String pPort, String pUser, String pPassword, String vid, String vkey) {

        this.waitForResultsDuration = waitForResultsDuration;
        this.failBuildForPolicyViolation = failBuildForPolicyViolation;
        this.debug = debug;
        this.vid = vid;
        this.vkey = vkey;
        this.useProxy = useProxy;
        this.pHost = useProxy ? pHost : null;
        this.pPort = useProxy ? pPort : null;
        this.pUser = useProxy ? pUser : null;
        this.pPassword = useProxy ? pPassword : null;
    }

    /**
     * Returns an object that represents the scope of the synchronization monitor
     * expected by the plugin.
     */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return null;
    }

    /**
     * Called by Jenkins after a build for a job specified to use the plugin is
     * performed.
     */
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {

        ProxyBlock proxyBlock = null;
        if (useProxy) {
            proxyBlock = new ProxyBlock(pHost, pPort, pUser, pPassword);
        }

        DAAdapterService daAdapterService = new DAAdapterService();
        boolean buildSuccess = daAdapterService.reviewDynamicAnalysis(run, workspace, listener,
                waitForResultsDuration, failBuildForPolicyViolation, vid, vkey, debug, proxyBlock);

        run.setResult(buildSuccess ? Result.SUCCESS : Result.FAILURE);
    }

    /**
     * Returns the
     * {@link com.veracode.jenkins.plugin.DynamicAnalysisResultsPipelineRecorder.PipelineDynamicAnalysisResultsDescriptorImpl}
     * object associated with this instance.
     *
     */
    @Override
    public PipelineDynamicAnalysisResultsDescriptorImpl getDescriptor() {
        return (PipelineDynamicAnalysisResultsDescriptorImpl) super.getDescriptor();
    }

    @Symbol("veracodeDynamicAnalysisReview")
    @Extension
    public static final class PipelineDynamicAnalysisResultsDescriptorImpl
            extends BuildStepDescriptor<Publisher> {

        @SuppressWarnings("rawtypes")
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return false;
        }

        @Override
        public String getDisplayName() {
            return Constant.POST_BUILD_ACTION_DISPLAY_TEXT_REVIEW;
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            String waitForResultsDuration = formData.getString("waitForResultsDuration");
            formData.put("waitForResultsDuration",
                    StringUtil.isNullOrEmpty(waitForResultsDuration)
                            ? TimeUnit.MINUTES
                                    .toHours(Constant.DEFAULT_VALUE_DA_WAIT_FOR_RESULTS_MINUTES)
                            : waitForResultsDuration);
            return super.newInstance(req, formData);
        }

        // Validate wait for results duration
        public FormValidation doCheckWaitForResultsDuration(
                @QueryParameter String waitForResultsDuration)
                throws IOException, ServletException {
            return FormValidationUtil.checkWaitForResultsDuration(waitForResultsDuration);
        }

        // Validate proxy host
        public FormValidation doCheckPHost(@QueryParameter("pHost") String pHost,
                @QueryParameter("pPort") String pPort) throws IOException, ServletException {
            return FormValidationUtil.checkFields(pHost, pPort, "Host", "Port", null);
        }

        // Validate proxy port
        public FormValidation doCheckPPort(@QueryParameter("pHost") String pHost,
                @QueryParameter("pPort") String pPort) throws IOException, ServletException {
            return FormValidationUtil.checkFields(pPort, pHost, "Port", "Host", null);
        }

        // Validate proxy username
        public FormValidation doCheckPUser(@QueryParameter("pUser") String pUser,
                @QueryParameter("pPassword") String pPassword)
                throws IOException, ServletException {
            return FormValidationUtil.checkMutuallyInclusiveFields(pUser, pPassword, "User",
                    "Password", null);
        }

        // Validate proxy password
        public FormValidation doCheckPPassword(@QueryParameter("pUser") String pUser,
                @QueryParameter("pPassword") String pPassword)
                throws IOException, ServletException {
            return FormValidationUtil.checkMutuallyInclusiveFields(pPassword, pUser, "Password",
                    "User", null);
        }
    }
}