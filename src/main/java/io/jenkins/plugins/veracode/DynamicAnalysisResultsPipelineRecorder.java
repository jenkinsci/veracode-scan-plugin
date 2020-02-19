/*******************************************************************************
 * Copyright (c) 2019 Veracode, Inc. All rights observed.
 *
 * Available for use by Veracode customers as described in the accompanying license agreement.
 *
 * Send bug reports or enhancement requests to support@veracode.com.
 *
 * See the license agreement for conditions on submitted materials.
 ******************************************************************************/
package io.jenkins.plugins.veracode;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

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
import io.jenkins.plugins.veracode.common.Constant;
import io.jenkins.plugins.veracode.common.DAAdapterService;
import io.jenkins.plugins.veracode.data.ProxyBlock;
import io.jenkins.plugins.veracode.utils.FormValidationUtil;
import io.jenkins.plugins.veracode.utils.StringUtil;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

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
     * {@link org.kohsuke.stapler.DataBoundConstructor DataBoundContructor}
     *
     * @param waitForResultsDuration      int
     * @param failBuildForPolicyViolation boolean
     * @param debug                       boolean
     * @param useProxy                    boolean
     * @param pHost                       String
     * @param pPort                       String
     * @param pUser                       String
     * @param pPassword                   String
     * @param vid                         String
     * @param vkey                        String
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

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return null;
    }

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