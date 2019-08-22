package com.cloudbees.jenkins.plugins.advisor;

import com.cloudbees.jenkins.plugins.advisor.client.AdvisorClient;
import com.cloudbees.jenkins.plugins.advisor.client.PluginHelper;
import com.cloudbees.jenkins.plugins.advisor.client.model.AccountCredentials;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientResponse;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientUploadRequest;
import com.cloudbees.jenkins.support.SupportPlugin;
import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.security.ACLContext;
import jenkins.model.Jenkins;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jenkinsci.Symbol;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
@Symbol("bundleUpload")
public class BundleUpload extends AsyncPeriodicWork {

  @SuppressWarnings("WeakerAccess")
  public static final int RECURRENCE_PERIOD_HOURS = Integer.getInteger(
    BundleUpload.class.getName()+".recurrencePeriodHours", 24);

  @SuppressWarnings("WeakerAccess")
  public static final int INITIAL_DELAY_MINUTES = Integer.getInteger(
          BundleUpload.class.getName()+".initialDelayMinutes", 5);

  private static final Logger LOG = Logger.getLogger(BundleUpload.class.getName());
  private TaskListener task;

  @SuppressWarnings("unused")
  public BundleUpload() {
    super("Bundle Upload");
  }

  private static final String UNABLE_TO_GENERATE_SUPPORT_BUNDLE = "ERROR: Unable to generate support bundle";

  @Override
  protected void execute(TaskListener listener) throws IOException, InterruptedException {
    task = listener;

    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    if (config == null) {
      return;
    }

    if (!config.isPluginEnabled()) {
      log(Level.FINEST, "CloudBees Jenkins Advisor plugin disabled. Skipping bundle upload.");
      return;
    }
    if (!config.isValid()) {
      log(Level.FINEST, "User not registered. Skipping bundle upload.");
      return;
    }
    if (!config.isAcceptToS()) {
      log(Level.FINEST, "Terms of conditions not accepted. Skipping bundle upload.");
      return;
    }

    File bundle = generateBundle();
    String pluginVersion = PluginHelper.getPluginVersion();
    if (bundle != null) {
      executeInternal(config.getEmail(), bundle,pluginVersion);
    } else {
      log(Level.SEVERE, UNABLE_TO_GENERATE_SUPPORT_BUNDLE);
      config.setLastBundleResult(UNABLE_TO_GENERATE_SUPPORT_BUNDLE);
    }
  }

  private static final String COULD_NOT_SAVE_SUPPORT_BUNDLE = "ERROR: Could not save support bundle";
  private static final String BUNDLE_DIR_DOES_NOT_EXIST = "Bundle root directory does not exist and could not be created";

  private File generateBundle() {
    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    try(ACLContext ignored = ACL.as(ACL.SYSTEM)) {
      File bundleDir = SupportPlugin.getRootDirectory();
      if (!bundleDir.exists() && !bundleDir.mkdirs()) {
        log(Level.SEVERE, String.format("%s %s", COULD_NOT_SAVE_SUPPORT_BUNDLE, BUNDLE_DIR_DOES_NOT_EXIST));
        config.setLastBundleResult(String.format("%s%n%s", COULD_NOT_SAVE_SUPPORT_BUNDLE, BUNDLE_DIR_DOES_NOT_EXIST));
        return null;
      }

      File file = new File(bundleDir, SupportPlugin.getBundleFileName());
      try(FileOutputStream fos = new FileOutputStream(file)) {
        SupportPlugin.writeBundle(fos, config.getIncludedComponents());
        return file;
      }
    } catch (Exception e) {
      logError(COULD_NOT_SAVE_SUPPORT_BUNDLE, e);
      config.setLastBundleResult(String.format("%s%n%s", COULD_NOT_SAVE_SUPPORT_BUNDLE, e));
    }
    return null;
  }

  private void executeInternal(String email, File file, String pluginVersion) {
    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    try {
      AdvisorClient advisorClient = new AdvisorClient(new AccountCredentials(email));

      ClientResponse response = advisorClient.uploadFile(new ClientUploadRequest(Jenkins.getInstance().getLegacyInstanceId(), file, config.getCc(), pluginVersion));
      if (response.getCode() == 200) {
        config.setLastBundleResult("Successfully uploaded a bundle at " +
          new SimpleDateFormat("yyyy MM dd HH:mm:ss").format(Calendar.getInstance().getTime()));
      } else {
        config.setLastBundleResult("Bundle upload failed. Response code was: " + response.getCode() + ". " +
            "Response message: " + response.getMessage());
      }
    } catch (Exception e) {
      log(Level.SEVERE, "Issue while uploading file to bundle upload service: " + e.getMessage());
      log(Level.FINEST, "Exception while uploading file to bundle upload service. Cause: " + ExceptionUtils.getStackTrace(e));
      config.setLastBundleResult("ERROR: Issue while uploading file to bundle upload service: " + e.getMessage());
    }
  }

  @Override
  public long getRecurrencePeriod() {
    return TimeUnit.HOURS.toMillis(RECURRENCE_PERIOD_HOURS);
  }

  /**
   * Log to both this class and the task listener's logger.
   */
  private void log(Level level, String message) {
    if(task != null) {
      if(level.equals(Level.SEVERE) || level.equals(Level.WARNING)) {
          task.error(message);
      } else {
        task.getLogger().println(message);
      }
    }

    LOG.log(level, message);
  }

  private void logError(String message, Throwable t) {
    if(task != null) {
      task.error(message, t);
    }
    LOG.log(Level.SEVERE, message, t);
  }

  /**
   * By default we wait a few minutes to allow support-core plugin time to generate a bundle first.
   *
   * @return initial delay before running work (in milliseconds).
   */
  @Override
  public long getInitialDelay() {
    return TimeUnit.MINUTES.toMillis(INITIAL_DELAY_MINUTES);
  }
}
