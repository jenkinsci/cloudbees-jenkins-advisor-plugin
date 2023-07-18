package com.cloudbees.jenkins.plugins.advisor;

import com.cloudbees.jenkins.plugins.advisor.client.AdvisorClient;
import com.cloudbees.jenkins.plugins.advisor.client.PluginHelper;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientResponse;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientUploadRequest;
import com.cloudbees.jenkins.plugins.advisor.client.model.Recipient;
import com.cloudbees.jenkins.support.SupportPlugin;
import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.security.ACLContext;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jenkinsci.Symbol;

@Extension
@Symbol("bundleUpload")
public class BundleUpload extends AsyncPeriodicWork {

    public static final int RECURRENCE_PERIOD_HOURS =
            Integer.getInteger(BundleUpload.class.getName() + ".recurrencePeriodHours", 24);

    public static final int INITIAL_DELAY_MINUTES =
            Integer.getInteger(BundleUpload.class.getName() + ".initialDelayMinutes", 30);

    public static final String TEMP_BUNDLE_DIRECTORY = System.getProperty(
            BundleUpload.class.getName() + ".tempBundleDirectory",
            Paths.get(SupportPlugin.getRootDirectory().toString(), "advisor").toString());

    private static final Logger LOG = Logger.getLogger(BundleUpload.class.getName());
    private static final String UNABLE_TO_GENERATE_SUPPORT_BUNDLE = "Unable to generate support bundle";
    private static final String COULD_NOT_SAVE_SUPPORT_BUNDLE = "Unable to save support bundle";
    private static final String BUNDLE_DIR_DOES_NOT_EXIST =
            "Bundle root directory does not exist and could not be created";
    protected static final String BUNDLE_SUCCESSFULLY_UPLOADED = "Bundle uploaded";
    private TaskListener task;

    public BundleUpload() {
        super("Bundle Upload");
    }

    @Override
    protected void execute(TaskListener listener) {
        task = listener;

        AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
        if (config == null) {
            return;
        }

        if (!config.isPluginEnabled()) {
            // How could it be possible ?
            log(Level.INFO, "Jenkins Health Advisor by CloudBees plugin disabled. Skipping bundle upload.");
            updateLastBundleResult(
                    config, createTimestampedWarnMessage("<strong>Plugin disabled</strong>, the upload was skipped"));
            return;
        }
        if (!config.isValid()) {
            log(Level.INFO, "Invalid configuration. Skipping bundle upload.");
            updateLastBundleResult(
                    config,
                    createTimestampedWarnMessage("<strong>Invalid configuration</strong>, the upload was skipped"));
            return;
        }

        File bundle = generateBundle();
        String pluginVersion = PluginHelper.getPluginVersion();
        if (bundle != null) {
            executeInternal(config.getEmail(), bundle, pluginVersion);
        } else {
            log(Level.SEVERE, UNABLE_TO_GENERATE_SUPPORT_BUNDLE);
        }
    }

    private File generateBundle() {
        AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
        File file = null;
        try (ACLContext ignored = ACL.as(ACL.SYSTEM)) {
            File bundleDir = new File(TEMP_BUNDLE_DIRECTORY);
            if (!bundleDir.exists() && !bundleDir.mkdirs()) {
                log(Level.SEVERE, String.format("%s: %s", COULD_NOT_SAVE_SUPPORT_BUNDLE, BUNDLE_DIR_DOES_NOT_EXIST));
                updateLastBundleResult(
                        config,
                        createTimestampedErrorMessage(
                                "<strong>%s</strong>: %s", COULD_NOT_SAVE_SUPPORT_BUNDLE, BUNDLE_DIR_DOES_NOT_EXIST));
                return null;
            }

            file = new File(bundleDir, SupportPlugin.getBundleFileName());
            try (FileOutputStream fos = new FileOutputStream(file)) {
                SupportPlugin.writeBundle(fos, config.getIncludedComponents());
                return file;
            }
        } catch (Exception e) {
            logError(COULD_NOT_SAVE_SUPPORT_BUNDLE, e);
            updateLastBundleResult(
                    config,
                    createTimestampedErrorMessage(
                            "<strong>%s</strong><br/><pre><code>%s</code></pre>",
                            COULD_NOT_SAVE_SUPPORT_BUNDLE, e.getMessage()));
            if (file != null && file.exists() && !file.delete()) {
                log(Level.WARNING, "Could not delete bundle {0}" + file);
            }
        }
        return null;
    }

    private void executeInternal(String email, File file, String pluginVersion) {
        AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
        try {
            AdvisorClient advisorClient = new AdvisorClient(new Recipient(email));

            ClientResponse response = advisorClient.uploadFile(
                    new ClientUploadRequest(Jenkins.get().getLegacyInstanceId(), file, config.getCcs(), pluginVersion));
            if (response.getCode() == 200) {
                updateLastBundleResult(config, createTimestampedInfoMessage(BUNDLE_SUCCESSFULLY_UPLOADED));
            } else {
                updateLastBundleResult(
                        config,
                        createTimestampedErrorMessage(
                                "<strong>Bundle upload failed</strong><br/>Server response is: <code>%d - %s</code>",
                                response.getCode(), response.getMessage()));
            }
        } catch (Exception e) {
            log(Level.SEVERE, "Issue while uploading file to bundle upload service: " + e.getMessage());
            log(
                    Level.FINEST,
                    "Exception while uploading file to bundle upload service. Cause: "
                            + ExceptionUtils.getStackTrace(e));
            updateLastBundleResult(
                    config,
                    createTimestampedErrorMessage(
                            "<strong>Bundle upload failed</strong><br/><pre><code>%s</code></pre>", e.getMessage()));
        } finally {
            if (!file.delete()) {
                log(Level.WARNING, "Could not delete bundle {0}" + file);
            }
            cleanup(new File(TEMP_BUNDLE_DIRECTORY));
        }
    }

    /**
     * Cleanup the bundle directory. Removing all bundles older than the recurrence period.
     *
     * @param bundleDir the bundle directory as {@link File}
     */
    private void cleanup(File bundleDir) {
        File[] files = bundleDir.listFiles((dir, name) -> name.endsWith(".zip"));
        if (files == null) {
            log(Level.WARNING, "Could not list files under" + bundleDir.getAbsolutePath());
            return;
        }
        long maxAge = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(RECURRENCE_PERIOD_HOURS);
        for (File f : files) {
            if (f.lastModified() < maxAge) {
                log(Level.INFO, "Deleting bundle" + f);
                if (!f.delete()) {
                    log(Level.WARNING, "Could not delete bundle {0}" + f);
                }
            }
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
        if (task != null) {
            if (level.equals(Level.SEVERE) || level.equals(Level.WARNING)) {
                task.error(message);
            } else {
                task.getLogger().println(message);
            }
        }

        LOG.log(level, message);
    }

    private void logError(String message, Throwable t) {
        if (task != null) {
            task.error(message, t);
        }
        LOG.log(Level.SEVERE, message, t);
    }

    private String createTimestampedInfoMessage(String message) {
        return createTimestampedMessage(null, message);
    }

    private String createTimestampedWarnMessage(String message) {
        return createTimestampedMessage("WARNING", message);
    }

    private String createTimestampedErrorMessage(String format, Object... args) {
        return createTimestampedMessage("ERROR", format, args);
    }

    private String createTimestampedMessage(String level, String format, Object... args) {
        return createTimestampedMessage(level, String.format(format, args));
    }

    private String createTimestampedMessage(String level, String message) {
        if (level != null) {
            return String.format(
                    "%1$s - %2$tF %2$tT - %3$s", level, Calendar.getInstance().getTime(), message);
        } else {
            return String.format("%1$tF %1$tT - %2$s", Calendar.getInstance().getTime(), message);
        }
    }

    private void updateLastBundleResult(AdvisorGlobalConfiguration config, String message) {
        config.setLastBundleResult(message);
        config.save();
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

    /**
     * Get the directory where Advisor bundle are temporarily generated before being sent to the remote server.
     * @return the path as {@link String}
     */
    public String getTempBundleDirectory() {
        return TEMP_BUNDLE_DIRECTORY;
    }
}
