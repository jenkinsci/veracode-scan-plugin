/*******************************************************************************
 * Copyright (c) 2014 Veracode, Inc. All rights observed.
 *
 * Available for use by Veracode customers as described in the accompanying license agreement.
 *
 * Send bug reports or enhancement requests to support@veracode.com.
 *
 * See the license agreement for conditions on submitted materials.
 ******************************************************************************/

package io.jenkins.plugins.veracode;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import com.veracode.apiwrapper.cli.VeracodeCommand.VeracodeParser;
import io.jenkins.plugins.veracode.VeracodeNotifier.VeracodeDescriptor;
import io.jenkins.plugins.veracode.args.UploadAndScanArgs;
import io.jenkins.plugins.veracode.common.Constant;
import io.jenkins.plugins.veracode.data.CredentialsBlock;
import io.jenkins.plugins.veracode.data.ProxyBlock;
import io.jenkins.plugins.veracode.data.ScanHistory;
import io.jenkins.plugins.veracode.utils.EncryptionUtil;
import io.jenkins.plugins.veracode.utils.FileUtil;
import io.jenkins.plugins.veracode.utils.FormValidationUtil;
import io.jenkins.plugins.veracode.utils.RemoteScanUtil;
import io.jenkins.plugins.veracode.utils.StringUtil;
import io.jenkins.plugins.veracode.utils.WrapperUtil;
import io.jenkins.plugins.veracode.utils.XmlUtil;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Contains the code that is executed after a job that is configured to use the
 * Veracode plugin is built and provides getter methods for the form fields
 * defined in config.jelly.
 * <p>
 *
 * This class extends the {@link hudson.tasks.Notifier Notifier} class.
 *
 *
 */
public class VeracodeNotifier extends Notifier {
	/**
	 * Contains the code that is executed after a user submits the
	 * "Configure System" form and provides getter methods for the form fields
	 * defined in global.jelly.
	 * This class extends the {@link hudson.tasks.BuildStepDescriptor
	 * BuildStepDescriptor} class.
	 *
     *       Converting this class to a top-level class should be done with the
     *       understanding that doing so might prevent the plugin from working
     *       properly if not at all.
     *
     *
	 */
	@hudson.Extension
	public static final class VeracodeDescriptor extends BuildStepDescriptor<Publisher> {
		private static final String PostBuildActionDisplayText = "Upload and Scan with Veracode";
		private static final String vidDisplayName = 		"API ID";
		private static final String vkeyDisplayName = 		"API Key";
		private static final String vidIHelpTextName = 		"ID";
		private static final String vkeyIHelpTextName = 	"key";
		private static final String appNameDisplayName = 	"Application Name";
		private static final String versionDisplayName = 	"Scan Name";
		private static final String filenamePatternDisplayName = 	"Filename Pattern";
		private static final String replacementPatternDisplayName =	"Replacement Pattern";
		private static final String[] criticalityDisplayNames = 	new String[] { "Very High", "High", "Medium", "Low", "Very Low" };


		//--------------------------------------------------------------------------------------
		// Backing fields for methods that correspond to identifiers referenced in global.jelly
		//--------------------------------------------------------------------------------------
		private String gvid;
		private String gvkey;
		private boolean failbuild = true;
		private boolean copyremotefiles;
		private boolean autoappname;
		private boolean autodescription;
		private boolean autoversion;
		private boolean debug;
		private boolean proxy;
		private String phost;
		private String pport;
		private String puser;
		private String ppassword;

		//-------------------------------------------------------------------
		// Methods that correspond to identifiers referenced in global.jelly
		//-------------------------------------------------------------------
		public String getGvid() {
			return EncryptionUtil.decrypt(gvid);
		}
		public String getGvkey() {
			return EncryptionUtil.decrypt(gvkey);
		}
		public boolean getFailbuild() {
			return failbuild;
		}
		public boolean getCopyremotefiles() {
			return copyremotefiles;
		}
		public boolean getAutoappname() {
			return autoappname;
		}
		public boolean getAutodescription() {
			return autodescription;
		}
		public boolean getAutoversion() {
			return autoversion;
		}
		public boolean getDebug() {
			return debug;
		}
		public boolean getProxy() {
			//needed in order to tell if the Proxy optionalblock is checked
			return proxy;
		}
		public String getPhost() {
			return EncryptionUtil.decrypt(phost);
		}
		public String getPport() {
			return EncryptionUtil.decrypt(pport);
		}
		public String getPuser() {
			return EncryptionUtil.decrypt(puser);
		}
		public String getPpassword() {
			return EncryptionUtil.decrypt(ppassword);
		}

		//------------------------------------------------------------------------------------------------------------------------
		// Methods that correspond to validation of data supplied in the "Configure System" page and the "Job Configuration" page
		//------------------------------------------------------------------------------------------------------------------------
		public FormValidation doTestConnection( @QueryParameter("gvid") final String gv_id, @QueryParameter("gvkey") final String gv_key,
												@QueryParameter("proxy") final boolean _proxy, @QueryParameter("phost") final String p_host, @QueryParameter("pport") final String p_port,
												@QueryParameter("puser") final String p_user, @QueryParameter("ppassword") final String p_password) {
			return FormValidationUtil.checkConnection(gv_id, gv_key, _proxy ? new ProxyBlock(p_host, p_port, p_user, p_password) : null);
		}
		public FormValidation doCheckGvid(@QueryParameter("gvid") String gv_id, @QueryParameter("gvkey") String gv_key) throws IOException, ServletException {
			return FormValidationUtil.checkMutuallyInclusiveFields(gv_id, gv_key, vidDisplayName, vkeyDisplayName, vkeyIHelpTextName);
		}
		public FormValidation doCheckGvkey(@QueryParameter("gvid") String gv_id, @QueryParameter("gvkey") String gv_key) throws IOException, ServletException {
			return FormValidationUtil.checkMutuallyInclusiveFields(gv_key, gv_id, vkeyDisplayName, vidDisplayName, vidIHelpTextName);
		}
		public FormValidation doCheckVid(@QueryParameter("vid") String v_id, @QueryParameter("vkey") String v_key) throws IOException, ServletException {
			boolean hasJobCredentials = !StringUtil.isNullOrEmpty(v_id) && !StringUtil.isNullOrEmpty(v_key);
			boolean hasGlobalCredentials = hasGlobalCredentials();

			if (hasGlobalCredentials) {
				if (hasJobCredentials) {
					return FormValidation.warning("These Veracode API credentials override the global Veracode API credentials.");
				} else if(!hasValidCredentials(v_id, v_key)){
					return FormValidationUtil.checkFields(v_id, v_key, vidDisplayName, vkeyDisplayName, vkeyIHelpTextName);
				} else {
					return FormValidationUtil.checkMutuallyInclusiveFields(v_id, v_key, vidDisplayName, vkeyDisplayName, vkeyIHelpTextName);
				}
			} else {
				return FormValidationUtil.checkFields(v_id, v_key, vidDisplayName, vkeyDisplayName, vkeyIHelpTextName);
			}
		}
		public FormValidation doCheckVkey(@QueryParameter("vid") String v_id, @QueryParameter("vkey") String v_key) throws IOException, ServletException {
			boolean hasJobCredentials = !StringUtil.isNullOrEmpty(v_id) && !StringUtil.isNullOrEmpty(v_key);
			boolean hasGlobalCredentials = hasGlobalCredentials();

			if (hasGlobalCredentials) {
				if (hasJobCredentials) {
					return FormValidation.warning("These Veracode API credentials override the global Veracode API credentials.");
				} else if(!hasValidCredentials(v_id, v_key)){
					return FormValidationUtil.checkFields(v_key, v_id, vkeyDisplayName, vidDisplayName, vidIHelpTextName);
				} else {
					return FormValidationUtil.checkMutuallyInclusiveFields(v_key, v_id, vkeyDisplayName, vidDisplayName, vidIHelpTextName);
				}
			} else {
				return FormValidationUtil.checkFields(v_key, v_id, vkeyDisplayName, vidDisplayName, vidIHelpTextName);
			}
		}
		public FormValidation doCheckAppname(@QueryParameter String value) throws IOException, ServletException {
			if (StringUtil.isNullOrEmpty(value) && !getAutoappname()) {
				return FormValidation.error(String.format("%s is required.", appNameDisplayName));
			} else {
				return FormValidation.ok();
			}
		}
		public FormValidation doCheckVersion(@QueryParameter String value) throws IOException, ServletException {
			if (StringUtil.isNullOrEmpty(value) && !getAutoversion()) {
				return FormValidation.error(String.format("%s is required.", versionDisplayName));
			} else {
				return FormValidation.ok();
			}
		}
		public FormValidation doCheckFilenamepattern(@QueryParameter("filenamepattern") String filename_pattern, @QueryParameter("replacementpattern") String replacement_pattern) throws IOException, ServletException {
			return FormValidationUtil.checkMutuallyInclusiveFields(filename_pattern, replacement_pattern, filenamePatternDisplayName, replacementPatternDisplayName, null);
		}
		public FormValidation doCheckReplacementpattern(@QueryParameter("filenamepattern") String filename_pattern, @QueryParameter("replacementpattern") String replacement_pattern) throws IOException, ServletException {
			return FormValidationUtil.checkMutuallyInclusiveFields(replacement_pattern, filename_pattern, replacementPatternDisplayName, filenamePatternDisplayName, null);
		}
		public FormValidation doCheckTimeout(@QueryParameter("timeout") String timeout) {
			if (!StringUtil.isNullOrEmpty(timeout)) {
					try {
						Integer.parseInt(timeout);
					} catch(NumberFormatException nfe) {
						return FormValidation.error(String.format("%s is not a valid number.", timeout));
					}
				}
			return FormValidation.ok();
		}
		public ListBoxModel doFillCriticalityItems(@QueryParameter("criticality") String criticality) {
		      ListBoxModel items = new ListBoxModel();
		      for (String s : criticalityDisplayNames)
		            items.add(new ListBoxModel.Option(s, s.replace(" ", ""), s.replace(" ", "").equals(criticality)));
		      return items;
		}
		//--------------------------------------------------------------
		// Overridden methods
		//--------------------------------------------------------------
		/**
		 * The name of the plugin displayed in the UI.
		 */
		public String getDisplayName() {
			return getPostBuildActionDisplayText();
		}
		/**
		 * Whether this task is applicable to the given project.
		 */
		@SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}
		/**
		 * Called by Jenkins when it needs to create an instance of the
		 * {@link io.jenkins.plugins.veracode.VeracodeNotifier VeracodeNotifier}
		 * class.
		 */
		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			try {
				EncryptionUtil.encrypt(req, formData);
			} catch (ServletException e) {
				throw new RuntimeException(e);
			}
			return super.newInstance(req, formData);
		}
		/**
		 * Called by Jenkins when the "Configure System" page is submitted.
		 */
		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			try {
				EncryptionUtil.encrypt(req, formData);
			} catch (ServletException e) {
				throw new RuntimeException(e);
			}

			try {
				initInstanceFields(formData);
				saveSensitiveFormFields(req);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			save();

			return super.configure(req, formData);
		}

		//--------------------------------------------------------------
		// Helper methods
		//--------------------------------------------------------------
		/**
		 * Whether Veracode credentials were supplied in the "Configure System"
		 * page.
		 * <p>
		 * This method is public because it is also called from config.jelly.
		 *
		 * @return boolean
		 */
		public boolean hasGlobalCredentials() {
			return !StringUtil.isNullOrEmpty(getGvid()) && !StringUtil.isNullOrEmpty(getGvkey());
		}
		/**
		 * Initializes this class' instance fields using the data in the
		 * specified {@link net.sf.json.JSONObject JSONObject} object.
		 * <p>
		 *
		 * This method is intended to be called after the data in the
		 * {@code formData} object has been encrypted.
		 *
		 * @param formData
		 */
		private void initInstanceFields(JSONObject formData) {
			gvid = formData.getString("gvid");
			gvkey = formData.getString("gvkey");

			failbuild = formData.getBoolean("failbuild");
			copyremotefiles = formData.getBoolean("copyremotefiles");
			autoappname = formData.getBoolean("autoappname");
			autodescription = formData.getBoolean("autodescription");
			autoversion = formData.getBoolean("autoversion");
			debug = formData.getBoolean("debug");

			//the "proxy" optionalBlock in global.jelly uses inline=true, allowing direct access to fields
			proxy = formData.getBoolean("proxy");
			phost = formData.getString("phost");
			pport = formData.getString("pport");
			puser = formData.getString("puser");
			ppassword = formData.getString("ppassword");
		}
		/**
		 * Copies the values of instance fields that have been initialized with
		 * sensitive encrypted data to the submitted form associated with the
		 * specified {@link org.kohsuke.stapler.StaplerRequest StaplerRequest}
		 * object.
		 * <p>
		 * This is done mostly as a safety measure and is probably not necessary
		 * if the form's data was already encrypted.
		 *
		 * @param req
		 * @throws ServletException
		 */
		private void saveSensitiveFormFields(StaplerRequest req) throws ServletException {
			req.getSubmittedForm().put("gvkey", gvkey);
			req.getSubmittedForm().put("gvid", gvid);

			req.getSubmittedForm().put("ppassword", ppassword);
			req.getSubmittedForm().put("puser", puser);
			req.getSubmittedForm().put("pport", pport);
			req.getSubmittedForm().put("phost", phost);
		}
		/**
		 * Checks to see if the id and key fields are empty
		 * despite having valid information in the global settings page
		 * @param id String
		 * @param key String
		 * @return boolean
		 */
		public boolean hasValidCredentials(String id, String key){
			return !StringUtil.isNullOrEmpty(id) && StringUtil.isNullOrEmpty(key);
		}

		/**
		 * This constructor makes it possible for global configuration data
		 * to be re-loaded after Jenkins is restarted.
		 */
		public VeracodeDescriptor() {
			super(VeracodeNotifier.class);

			load();
		}
		public static String getPostBuildActionDisplayText() {
			return PostBuildActionDisplayText;
		}
	}

	//--------------------------------------------------------------------------------------
	// Backing fields for methods that correspond to identifiers referenced in config.jelly
	//--------------------------------------------------------------------------------------
	private final String _appname;
	private final boolean _createprofile;
	private final String _teams;
	private final String _criticality;
	private final String _sandboxname;
	private final boolean _createsandbox;
	private final String _version;
	private final String _uploadincludespattern;
	private final String _uploadexcludespattern;
	private final String _scanincludespattern;
	private final String _scanexcludespattern;
	private final String _filenamepattern;
	private final String _replacementpattern;
	private final CredentialsBlock _credentials;
	private final boolean _waitforscan;
	private String _timeout;

	//-------------------------------------------------------------------
	// Methods that correspond to identifiers referenced in config.jelly
	//-------------------------------------------------------------------
	public String getAppname() {
		return EncryptionUtil.decrypt(this._appname);
	}
	public boolean getCreateprofile() {
		return this._createprofile;
	}
	public String getTeams() {
		return this.getCreateprofile() ? EncryptionUtil.decrypt(this._teams) : null;
	}
	public String getCriticality() {
		return EncryptionUtil.decrypt(this._criticality);
	}
	public String getSandboxname() {
		return EncryptionUtil.decrypt(this._sandboxname);
	}
	public boolean getCreatesandbox() {
		return this._createsandbox;
	}
	public String getVersion() {
		return EncryptionUtil.decrypt(this._version);
	}
	public String getUploadincludespattern() {
		return EncryptionUtil.decrypt(this._uploadincludespattern);
	}
	public String getUploadexcludespattern() {
		return EncryptionUtil.decrypt(this._uploadexcludespattern);
	}
	public String getScanincludespattern() {
		return EncryptionUtil.decrypt(this._scanincludespattern);
	}
	public String getScanexcludespattern() {
		return EncryptionUtil.decrypt(this._scanexcludespattern);
	}
	public String getFilenamepattern() {
		return EncryptionUtil.decrypt(this._filenamepattern);
	}
	public String getReplacementpattern() {
		return EncryptionUtil.decrypt(this._replacementpattern);
	}
	public CredentialsBlock getCredentials() {
		// needed in order to tell if the Credentials optionalblock is checked
		return this._credentials;
	}

	public boolean getWaitForScan() {
		return this._waitforscan;
	}

	public String getTimeout() {
		return this.getWaitForScan() ? EncryptionUtil.decrypt(this._timeout) : null;
	}

	public String getVid() {
		return EncryptionUtil.decrypt((this._credentials != null) ?  this._credentials.getVid() : null);
	}

	public String getVkey() {
		return EncryptionUtil.decrypt((this._credentials != null) ? this._credentials.getVkey() : null);
	}

	//--------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------
	/**
	 * Returns the
	 * {@link io.jenkins.plugins.veracode.VeracodeNotifier.VeracodeDescriptor
	 * VeracodeDescriptor} object associated with this instance.
	 *
	 */
	@Override
	public VeracodeDescriptor getDescriptor() {
		return (VeracodeDescriptor) super.getDescriptor();
	}
	/**
	 * Returns an object that represents the scope of the synchronization monitor expected by the plugin.
	 */
	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	/**
	In this overridden method we are taking care of copying the wrapper to remote location
	and making the build ready for scan
	**/


	@Override
	public boolean prebuild (AbstractBuild<?, ?> build, BuildListener listener)
	{
		boolean bRet = false;
		boolean debug = getDescriptor().getDebug();

		PrintStream ps = listener.getLogger();
		FilePath workspace = build.getWorkspace();
		if (workspace == null) {
        	ps.print("\r\n\r\nFailed to locate the build workspace.\r\n");
			return !getDescriptor().getFailbuild();
        }
		boolean copyRemoteFiles = getDescriptor().getCopyremotefiles();
		boolean isRemoteWorkspace = workspace.isRemote();

		//only copy if remote workspace and the checkbox to copy from remote to master is unchecked
		if(isRemoteWorkspace && !copyRemoteFiles)
		{
			try
			{
				FilePath localWorkspaceFilePath = FileUtil.getLocalWorkspaceFilepath();
				FilePath remoteVeracodeFilePath  = RemoteScanUtil.getRemoteVeracodePath(build);
				//create the directory (where we want to copy the javawrapper jar) if it does not exist

				if(!remoteVeracodeFilePath.exists())
				{

					if(debug) {
						ps.println("Making remote dir");
					}

					remoteVeracodeFilePath.mkdirs();
				}

				FilePath[] files = remoteVeracodeFilePath.list(Constant.inclusive);

				//copy the jar if it does not exist
				if(files.length == 0 )

				{
					bRet = FileUtil.copyJarFiles(build,localWorkspaceFilePath, remoteVeracodeFilePath, ps);
				}
				else // if file exits
				{
					FilePath[] newfiles = localWorkspaceFilePath.list(Constant.inclusive);
					String newjarName = newfiles[0].getRemote();
					int newVersion = RemoteScanUtil.getJarVersion (newjarName);
					String oldjarName = files[0].getRemote();
					int oldVersion = RemoteScanUtil.getJarVersion (oldjarName);

					//also copy the jar if there is a newer version in the plugin directory and delete the old one
					if(newVersion > oldVersion)
					{
						if (debug) {
							ps.println("Newer veracode library version, copying it to remote machine");
						}

						remoteVeracodeFilePath.deleteContents();
						bRet = FileUtil.copyJarFiles(build, localWorkspaceFilePath, remoteVeracodeFilePath, ps);
					}
					else //just make sure we have our jarfile (defensive coding)
					{
						String jarName = files[0].getRemote();
						String newJarName = jarName.replaceAll(Constant.regex, Constant.execJarFile+"$2");
						Node node = build.getBuiltOn();
						if (node == null) {
				        	ps.print("\r\n\r\nFailed to locate the build node.\r\n");
							return !getDescriptor().getFailbuild();
				        }
						FilePath newjarFilePath = new FilePath(node.getChannel(), newJarName);


						if(newjarFilePath.exists())
							bRet = true;
					}
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				if (getDescriptor().getFailbuild()) {
					ps.print(ex.getMessage());
				}
			}
		}
		else
			bRet = true;

		return bRet;
	}


	/**
	 * Called by Jenkins after a build for a job specified to use the plugin is performed.
	 */
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		PrintStream ps = listener.getLogger();

		ps.println("------------------------------------------------------------------------");
		ps.println(VeracodeDescriptor.getPostBuildActionDisplayText());
		ps.println("------------------------------------------------------------------------");

		boolean debug = getDescriptor().getDebug();
		EnvVars envar = build.getEnvironment(listener);
		UploadAndScanArgs.setEnvVars(envar, build.getDisplayName(), build.getProject().getDisplayName());
		String uploadincludePattern = envar.expand(this.getUploadincludespattern());
		String uploadexcludePattern = envar.expand(this.getUploadexcludespattern());

		if (debug) {
			ps.println("\r\n[Debug mode is on]\r\n");

			ps.println(String.format("Can Fail Build?%n%s%n", getDescriptor().getFailbuild()));

			try {
				Method method = com.veracode.apiwrapper.cli.VeracodeCommand.class.getDeclaredMethod("getVersionString");
				method.setAccessible(true);
				String version = (String)method.invoke(null);
				if (!StringUtil.isNullOrEmpty(version)) {
					ps.println(String.format("Version information:%n%s", version));
				}
			} catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				ps.println("Could not retrieve API wrapper's version information.");
			}
			try {
				String location = this.getClass().getProtectionDomain().getCodeSource().getLocation().toString();
				if (!StringUtil.isNullOrEmpty(location)) {
					ps.println("\r\nHPI location: ");
					location = location.replace("file:/", "");
					listener.hyperlink("file://" + location, location);
				}
			} catch (Exception e) {
				ps.println("\r\nCould not retrieve hpi file's directory.");
			}
		}

		FilePath workspace = build.getWorkspace();
		if (workspace == null) {
        	ps.print("\r\n\r\nFailed to locate the build workspace.\r\n");
			return !getDescriptor().getFailbuild();
        }
		boolean copyRemoteFiles = getDescriptor().getCopyremotefiles();
		boolean isRemoteWorkspace = workspace.isRemote();

		if (debug) {
			ps.println(String.format("%n%nProcessing files in [%s] workspace: ", isRemoteWorkspace ? "remote" : "local"));
			String workspaceDir = workspace.getRemote();
			workspaceDir = workspaceDir.replace("\\", "/");
			listener.hyperlink("file://" + workspaceDir, workspaceDir);
		}

		String[] uploadAndScanFilePaths = null;
		File localWorkspaceDir = null;

		try {
			if (isRemoteWorkspace) {
				if (debug) {
					//ps.print(String.format("\r\n\r\nCopy remote files to [local] workspace?\r\n%s", copyRemoteFiles));
					if(!copyRemoteFiles)
					{
						ps.print(String.format("%n%nPerforming scan from [remote] workspace?%n%s", !copyRemoteFiles));
						ps.print("\n");
					}
					else
					{
						ps.print(String.format("%n%nPerforming scan [local] workspace after copying remote files?%n%s", copyRemoteFiles));
						ps.print("\n");
					}
				}

				if (copyRemoteFiles) {
					localWorkspaceDir = new File(build.getParent().getRootDir(), "temp-veracode-local-workspace");

					if (debug) {
						ps.print("\r\n\r\nAttempting to copy remote files to [local] workspace:\r\n");
						String p = localWorkspaceDir.getCanonicalPath().replace("\\", "/");
						listener.hyperlink("file://" + p, p);
					}

					try {
						if(localWorkspaceDir.exists()) {
							FileUtil.deleteDirectory(localWorkspaceDir);
						}

						boolean dirCreated = localWorkspaceDir.mkdir();
						if (!dirCreated) {
							ps.print("\r\n\r\nFailed to create temporary local workspace.\r\n");
							return !getDescriptor().getFailbuild();
						}
						FilePath localWorkspaceFilePath = new FilePath(localWorkspaceDir);
						workspace.copyRecursiveTo(uploadincludePattern, uploadexcludePattern, localWorkspaceFilePath);

						//obtain the String file paths, using the includes/excludes patterns a 2nd time
						uploadAndScanFilePaths = FileUtil.getStringFilePaths(localWorkspaceFilePath.list(uploadincludePattern, uploadexcludePattern));
					} catch(Exception e) {
						String msg = e.getMessage();
						ps.print("\r\n\r\nFailed to copy remote files to the [local] workspace:\r\n" + e.getClass().getName() + (msg != null ? ": " + msg : "") + "\r\n\r\n");
						return !getDescriptor().getFailbuild();
					}
				} else {
					//let us scan from remote workspace
					return runScanFromRemote(build, listener, ps, debug);
				}
			} else {
				uploadAndScanFilePaths = FileUtil.getStringFilePaths(workspace.list(uploadincludePattern, uploadexcludePattern));
			}

			if (debug) {
				ps.print("\r\n\r\nBuilding arguments. ");
			}

			UploadAndScanArgs uploadAndScanArguments = UploadAndScanArgs.newUploadAndScanArgs(this, build, build.getEnvironment(listener), uploadAndScanFilePaths, false);

			if (debug) {

				ps.println(String.format("Calling wrapper with arguments:%n%s%n", Arrays.toString(uploadAndScanArguments.getMaskedArguments())));
			}

			try {
				VeracodeParser parser = new VeracodeParser();
				parser.setOutputWriter(ps);
				parser.setErrorWriter(ps);
				parser.throwExceptions(true);
				parser.setScanCompleteTimeout(this.getTimeout());
				int retcode = parser.parse(uploadAndScanArguments.getArguments());
				try {
                    // Starting from 17.9.4.6, the Java wrapper returns code (4) when a scan
                    // did not pass policy compliance. Therefore, we need to generate the scan
                    // result for both return code 0 and 4.
				    if (this.getWaitForScan()) {
				        if (4 == retcode || 0 == retcode) {
				            getScanResults(build, listener);
				        } else {
				            build.addAction(new VeracodeAction());
				        }
				    }
				} catch(Exception e) {
				    ps.println();
				    ps.println(String.format("Ran into problem when generating scan results in Jenkins. Error: [%s, %s]", e.getClass().getSimpleName(), e.getMessage()));
	                e.printStackTrace(ps);
				} finally { // Make sure setting the build status correctly according to the retCode
				    if (retcode != 0 && getDescriptor().getFailbuild()) {
				    	ps.println();
				        ps.println("Error- Returned code from wrapper:" + retcode);
				        return false;
				    }
                }
			} catch (Throwable e) {
				if (getDescriptor().getFailbuild()) {
					ps.println();
					return false;
				}
			}
			ps.println();
			return true;
		} finally {
			if (isRemoteWorkspace && copyRemoteFiles) {
				try {
					if(localWorkspaceDir != null && localWorkspaceDir.exists()) {
						FileUtil.deleteDirectory(localWorkspaceDir);
					}
				} catch (Throwable e){}
			}
		}
	}

	//--------------------------------------------------------------
	// Constructor
	//--------------------------------------------------------------
	/**
	 * Called by Jenkins with data supplied in the "Job Configuration" page.
	 *
     	 * @param appname String
	 * @param createprofile boolean
	 * @param teams String
	 * @param criticality String
	 * @param sandboxname  String
	 * @param createsandbox boolean
	 * @param version String
	 * @param filenamepattern String
	 * @param replacementpattern String
	 * @param uploadincludespattern String
	 * @param uploadexcludespattern String
	 * @param scanincludespattern String
	 * @param scanexcludespattern String
	 * @param waitForScan boolean
	 * @param timeout String
	 * @param credentials CredentialsBlock
	 */
	@org.kohsuke.stapler.DataBoundConstructor
	public VeracodeNotifier(String appname, boolean createprofile, String teams, String criticality, String sandboxname, boolean createsandbox, String version, String filenamepattern,
			String replacementpattern, String uploadincludespattern, String uploadexcludespattern, String scanincludespattern, String scanexcludespattern,
			boolean waitForScan, String timeout, CredentialsBlock credentials) {
		this._appname = appname;
		this._createprofile = createprofile;
		this._teams = teams;
		this._criticality = criticality;
		this._sandboxname = sandboxname;
		this._createsandbox = createsandbox;
		this._version = version;

		this._uploadincludespattern = uploadincludespattern;
		this._uploadexcludespattern = uploadexcludespattern;

		this._scanincludespattern = scanincludespattern;
		this._scanexcludespattern = scanexcludespattern;

		this._filenamepattern = filenamepattern;
		this._replacementpattern = replacementpattern;

		this._waitforscan =  waitForScan;
		this._timeout = this._waitforscan ? FormValidationUtil.formatTimeout(timeout) : null;

		this._credentials = credentials;
	}

	//invoking the CLI from remote node
	private boolean runScanFromRemote(AbstractBuild <?,?> build, BuildListener listener, PrintStream ps, boolean bDebug ) throws IOException, InterruptedException
	{
		boolean bRet = false;
		EnvVars envar = build.getEnvironment(listener);
		UploadAndScanArgs.setEnvVars(envar, build.getDisplayName(), build.getProject().getDisplayName());
		String uploadincludePattern = envar.expand(this.getUploadincludespattern());
		String uploadexcludePattern = envar.expand(this.getUploadexcludespattern());
		Node node = build.getBuiltOn();
		if (node == null) {
        	ps.print("\r\n\r\nFailed to locate the build node.\r\n");
			return !getDescriptor().getFailbuild();
        }

		FilePath remoteVeracodeFilePath  =  RemoteScanUtil.getRemoteVeracodePath(build);
		String jarFilePath = remoteVeracodeFilePath.getRemote();

		FilePath workspace = build.getWorkspace();
		if (workspace == null) {
        	ps.print("\r\n\r\nFailed to locate the build workspace.\r\n");
			return !getDescriptor().getFailbuild();
        }
		String remoteworkspace = workspace.getRemote();
		String sep = RemoteScanUtil.getPathSeparator(remoteworkspace);
		FilePath remoteworkspaceFilePath  = new FilePath(node.getChannel(),remoteworkspace);

		//obtain the String file paths, using the includes/excludes patterns a 2nd time
		try
		{
			String[] uploadAndScanFilePaths = FileUtil.getStringFilePaths(remoteworkspaceFilePath.list(uploadincludePattern, uploadexcludePattern));
			UploadAndScanArgs uploadAndScanArguments = UploadAndScanArgs.newUploadAndScanArgs(this, build, build.getEnvironment(listener), uploadAndScanFilePaths, true);

			String jarPath = jarFilePath+ sep + Constant.execJarFile +".jar";
			String cmd = "java -jar " + jarPath;
			String[] cmds = uploadAndScanArguments.getArguments();

			StringBuilder result = new StringBuilder();
			result.append(cmd);
			for (String _cmd : cmds) {
				_cmd = RemoteScanUtil.formatParameterValue(_cmd);
				result.append(" " +_cmd);
			}

			ArgumentListBuilder command = new ArgumentListBuilder();
			command.addTokenized(result.toString());

			List <String> remoteCmd = command.toList();
			int iSize = remoteCmd.size();
			Integer[] iPos = RemoteScanUtil.getMaskPosition(remoteCmd);
			int iPosKey = iPos[0];
			int iPosProxyPassword = iPos[1];

			Launcher launcher = node.createLauncher(listener);
			ProcStarter procStart = launcher.new ProcStarter();

			//masking the password related information
			boolean[] masks = new boolean[iSize];
			for( int i =0; i<iSize; i++)
			{
				if(iPosKey != -1)
				{
					if(iPosKey == i) masks[i]=true;
				}
				else if(iPosProxyPassword != -1)
				{
					if(iPosProxyPassword == i) masks[i]=true;
				}
				else
					masks[i] = false;
			}

			procStart = procStart.cmds(command).masks(masks).stdout(listener).quiet(true);

			if(bDebug)
			{
				procStart.quiet(false);
				ps.print("\nInvoking the following command in remote workspace:\n");
			}
			Proc proc = launcher.launch(procStart);

			int retcode = proc.join();

			if (retcode != 0 && getDescriptor().getFailbuild()) {
			    ps.print("\r\n\r\nError- Returned code from wrapper:"+ retcode+"\r\n\n");
			} else {
				bRet = true;
			}

            try {
                // Starting from 17.9.4.6, the Java wrapper returns code (4) when a scan
                // did not pass policy compliance. Therefore, we need to generate the scan
                // result for both return code 0 and 4.
                if (this.getWaitForScan()) {
                    if (4 == retcode || 0 == retcode ) {
                        getScanResults(build, listener);
                    } else {
                        build.addAction(new VeracodeAction());
                    }
                }
            } catch(Exception e) {
                ps.println();
                ps.println(String.format("Ran into problem when generating scan results in Jenkins. Error: [%s, %s]", e.getClass().getSimpleName(), e.getMessage()));
                e.printStackTrace(ps);
            }
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			if (getDescriptor().getFailbuild()) {
				ps.print(ex.getMessage());
			}
		}
		return bRet;
	}

    /**
     * Construct the scan result from Detailed Report
     *
     * @param run - the current Jenkins build
     * @param listener - listener of this task
     * @throws Exception when error happened during the operation
     */
	private void getScanResults(AbstractBuild<?, ?> build, BuildListener listener) throws Exception {
        VeracodeDescriptor descriptor = getDescriptor();
        String id, key;
        if (getCredentials() == null) {
            id = descriptor.getGvid();
            key = descriptor.getGvkey();
        } else {
            id = getVid();
            key = getVkey();
        }
        ProxyBlock proxy = null;
        if (descriptor.getProxy()) {
            proxy = new ProxyBlock(descriptor.getPhost(), descriptor.getPport(), descriptor.getPuser(), descriptor.getPpassword());
        }

        EnvVars envVars = build.getEnvironment(listener);
        UploadAndScanArgs.setEnvVars(envVars, build.getDisplayName(), build.getProject().getDisplayName());
        String appName = getAppname();

        //application profile name
        if (!StringUtil.isNullOrEmpty(appName)) {
            appName = envVars.expand(appName);
        } else if (descriptor.getAutoappname()) {
            appName = envVars.get(UploadAndScanArgs.CUSTOM_PROJECT_NAME_VAR);
        }

        if (!StringUtil.isNullOrEmpty(id)) {
            id = envVars.expand(id);
        }

        if (!StringUtil.isNullOrEmpty(key)) {
            key = envVars.expand(key);
        }

        String sandboxName = getSandboxname();
        if (!StringUtil.isNullOrEmpty(sandboxName)) {
            sandboxName = envVars.expand(sandboxName);
        }

        try {
            String buildInfoXML = WrapperUtil.getBuildInfo(appName, sandboxName, id, key, proxy);
            String buildId = XmlUtil.parseBuildId(buildInfoXML);
            String detailedReportXML = WrapperUtil.getDetailedReport(buildId, id, key, proxy);
            ScanHistory scanHistory = XmlUtil.newScanHistory(buildInfoXML, detailedReportXML, build);
            build.addAction(new VeracodeAction(scanHistory));
        } catch (Exception e) {
        	build.addAction(new VeracodeAction());
            throw e;
        }
	}
}
