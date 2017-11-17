package com.cloudbees.jenkins.plugins.advisor;

import com.cloudbees.jenkins.plugins.advisor.client.AdvisorClient;
import com.cloudbees.jenkins.plugins.advisor.client.model.AccountCredentials;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientUploadRequest;
import com.cloudbees.jenkins.support.SupportPlugin;
import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jenkinsci.Symbol;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
@Symbol("bundleUpload")
public class BundleUpload extends AsyncPeriodicWork {

  public static final int RECURRENCE_PERIOD_MINUTES = Integer.getInteger(
    BundleUpload.class.getName()+".recurrencePeriodMinutes", (int) TimeUnit.HOURS.toMinutes(24));

  private static final Logger LOG = Logger.getLogger(BundleUpload.class.getName());
  TaskListener task;

  @SuppressWarnings("unused")
  public BundleUpload() {
    super("Bundle Upload");
  }

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

    File bundle = generateBundle();
    if (bundle != null) {
      executeInternal(config.getEmail(), Secret.toString(config.getPassword()), bundle);
    } else {
      log(Level.WARNING, "Unable to generate support bundle");
    }
  }

  private File generateBundle() {
    try(ACLContext context = ACL.as(ACL.SYSTEM)) {
      File bundleDir = SupportPlugin.getRootDirectory();
      if (!bundleDir.exists()) {
        if (!bundleDir.mkdirs()) {
          return null;
        }
      }

      File file = new File(bundleDir, SupportPlugin.getBundleFileName());
      try(FileOutputStream fos = new FileOutputStream(file)) {
        AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
        SupportPlugin.writeBundle(fos, config.getIncludedComponents());
        return file;
      }
    } catch (Throwable t) {
      log(Level.WARNING, "Could not save support bundle\n" + t);
    }
    return null;
  }

  private void executeInternal(String email, String password, File file) {
    try {
      AdvisorClient advisorClient = new AdvisorClient(new AccountCredentials(email, password));

      advisorClient.uploadFile(new ClientUploadRequest(Jenkins.getInstance().getLegacyInstanceId(), file));
    } catch (Exception e) {
      log(Level.SEVERE, "Issue while uploading file to bundle upload service: " + e.getMessage());
      log(Level.FINEST, "Exception while uploading file to bundle upload service. Cause: " + ExceptionUtils.getStackTrace(e));
    }
  }

  @Override
  public long getRecurrencePeriod() {
    return TimeUnit.MINUTES.toMillis(RECURRENCE_PERIOD_MINUTES);
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
}
