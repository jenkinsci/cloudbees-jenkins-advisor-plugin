package com.cloudbees.jenkins.plugins.advisor;

import com.cloudbees.jenkins.plugins.advisor.client.AdvisorClient;
import com.cloudbees.jenkins.plugins.advisor.client.model.AccountCredentials;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientUploadRequest;
import com.cloudbees.jenkins.support.SupportPlugin;
import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.util.IOUtils;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.util.SystemProperties;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jenkinsci.Symbol;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
@Symbol("bundleUpload")
public class BundleUpload extends AsyncPeriodicWork {

  public static final int RECURRENCE_PERIOD_MINUTES = SystemProperties.getInteger(
    BundleUpload.class.getName()+".recurrencePeriodMinutes", (int) TimeUnit.HOURS.toMinutes(24));

  private static final Logger LOG = Logger.getLogger(BundleUpload.class.getName());

  @SuppressWarnings("unused")
  public BundleUpload() {
    super("Bundle Upload");
  }

  @Override
  protected void execute(TaskListener listener) throws IOException, InterruptedException {
    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();

    if (config == null) {
      return;
    }

    if (!config.isPluginEnabled()) {
      LOG.finest("CloudBees Jenkins Advisor plugin disabled. Skipping bundle upload.");
      return;
    }
    if (!config.isValid()) {
      LOG.finest("User not registered. Skipping bundle upload.");
      return;
    }

    Optional<File> bundle = generateBundle();
    if (bundle.isPresent()) {
      executeInternal(config.getEmail(), Secret.toString(config.getPassword()), bundle.get());
    } else {
      LOG.warning("Unable to generate support bundle");
    }
  }

  private Optional<File> generateBundle() {
    SecurityContext old = ACL.impersonate(ACL.SYSTEM);
    try {
      File bundleDir = SupportPlugin.getRootDirectory();
      if (!bundleDir.exists()) {
        if (!bundleDir.mkdirs()) {
          return Optional.empty();
        }
      }

      File file = new File(bundleDir, SupportPlugin.getBundleFileName());
      FileOutputStream fos = null;
      try {
        fos = new FileOutputStream(file);
        AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
        SupportPlugin.writeBundle(fos, config.getIncludedComponents());
        return Optional.of(file);
      } finally {
        IOUtils.closeQuietly(fos);
      }
    } catch (Throwable t) {
      LOG.log(Level.WARNING, "Could not save support bundle", t);
    } finally {
      SecurityContextHolder.setContext(old);
    }
    return Optional.empty();
  }

  private void executeInternal(String email, String password, File file) {
    try {
      AdvisorClient advisorClient = new AdvisorClient(new AccountCredentials(email, password));

      advisorClient.uploadFile(new ClientUploadRequest(Jenkins.getInstance().getLegacyInstanceId(), file));
    } catch (Exception e) {
      LOG.severe("Issue while uploading file to bundle upload service: " + e.getMessage());
      LOG.finest("Exception while uploading file to bundle upload service. Cause: " + ExceptionUtils.getStackTrace(e));
    }
  }

  @Override
  public long getRecurrencePeriod() {
    return TimeUnit.MINUTES.toMillis(RECURRENCE_PERIOD_MINUTES);
  }
}
