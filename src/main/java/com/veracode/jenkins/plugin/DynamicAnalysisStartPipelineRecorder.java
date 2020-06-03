package com.veracode.jenkins.plugin;

import java.io.IOException;

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
 * The DynamicAnalysisStartPipelineRecorder class handles processing for
 * "veracodeDynamicAnalysisResubmit" Pipeline script. The UI interface of
 * Snippet Generator for "veracodeDynamicAnalysisResubmit: Resubmit Veracode
 * Dynamic Analysis" is defined in associated config.jelly.
 * <p>
 * This class extends the {@link hudson.tasks.Recorder} class.
 * 
 */
public class DynamicAnalysisStartPipelineRecorder extends Recorder implements SimpleBuildStep {

    @DataBoundSetter
    public final String analysisName;
    @DataBoundSetter
    public final int maximumDuration;
    @DataBoundSetter
    public final boolean failBuildAsScanFailed;
    @DataBoundSetter
    public final String vid;
    @DataBoundSetter
    public final String vkey;
    @DataBoundSetter
    public final boolean debug;
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
     * Constructor for DynamicAnalysisStartPipelineRecorder.
     *
     * @param analysisName          a {@link java.lang.String} object.
     * @param maximumDuration       a int.
     * @param failBuildAsScanFailed a boolean.
     * @param vid                   a {@link java.lang.String} object.
     * @param vkey                  a {@link java.lang.String} object.
     * @param debug                 a boolean.
     * @param useProxy              a boolean.
     * @param pHost                 a {@link java.lang.String} object.
     * @param pPort                 a {@link java.lang.String} object.
     * @param pUser                 a {@link java.lang.String} object.
     * @param pPassword             a {@link java.lang.String} object.
     */
    @DataBoundConstructor
    public DynamicAnalysisStartPipelineRecorder(final String analysisName,
            final int maximumDuration, final boolean failBuildAsScanFailed, final String vid,
            final String vkey, final boolean debug, final boolean useProxy, final String pHost,
            final String pPort, final String pUser, final String pPassword) {

        this.analysisName = analysisName;
        this.maximumDuration = maximumDuration;
        this.failBuildAsScanFailed = failBuildAsScanFailed;
        this.vid = vid;
        this.vkey = vkey;
        this.debug = debug;
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
        boolean buildSuccess = daAdapterService.resubmitDynamicAnalysis(run, workspace, listener,
                analysisName, maximumDuration, failBuildAsScanFailed, vid, vkey, debug, proxyBlock);

        run.setResult(buildSuccess ? Result.SUCCESS : Result.FAILURE);
    }

    /**
     * Returns the
     * {@link com.veracode.jenkins.plugin.DynamicAnalysisStartPipelineRecorder.DynamicAnalysisStartPipelineDescriptor}
     * object associated with this instance.
     *
     */
    @Override
    public DynamicAnalysisStartPipelineDescriptor getDescriptor() {
        return (DynamicAnalysisStartPipelineDescriptor) super.getDescriptor();
    }

    @Extension
    @Symbol("veracodeDynamicAnalysisResubmit")
    public static class DynamicAnalysisStartPipelineDescriptor
            extends BuildStepDescriptor<Publisher> {

        @SuppressWarnings("rawtypes")
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return false;
        }

        @Override
        public String getDisplayName() {
            return Constant.POST_BUILD_ACTION_DISPLAY_TEXT_RESUBMIT;
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            String maximumDuration = formData.getString("maximumDuration");
            formData.put("maximumDuration",
                    StringUtil.isNullOrEmpty(maximumDuration)
                            ? Constant.DEFAULT_VALUE_DA_MAX_DURATION_HOURS
                            : maximumDuration);
            return super.newInstance(req, formData);
        }

        // Validate Analysis name
        public FormValidation doCheckAnalysisName(@QueryParameter String analysisName)
                throws IOException, ServletException {
            return FormValidationUtil.checkAnalysisName(analysisName);
        }

        // Validate maximum duration
        public FormValidation doCheckMaximumDuration(@QueryParameter String maximumDuration)
                throws IOException, ServletException {
            return FormValidationUtil.checkMaximumDuration(maximumDuration);
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
