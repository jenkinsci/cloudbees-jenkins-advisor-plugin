package com.cloudbees.jenkins.plugins.advisor;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Displays a message whenever there is an issue during the bundle upload process.
 * This message will match any of the error messages in the log file.
 */
@Extension
public class BundleUploadMonitor extends AdministrativeMonitor {

    @Override
    public boolean isActivated() {
        AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
        /*
        no nag when plugin is disabled
        no nag when no error messages logged by BundleUpload
        */
        return config.isPluginEnabled()
                && config.getLastBundleResult() != null
                && (config.getLastBundleResult().contains("ERROR")
                        || config.getLastBundleResult().contains("Bundle upload failed"));
    }

    @Override
    public String getDisplayName() {
        return Messages.BundleUploadMonitor_DisplayName();
    }

    @Restricted(NoExternalUse.class)
    @RequirePOST
    public void doAct(StaplerRequest2 req, StaplerResponse2 rsp) throws ServletException, IOException {
        AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
        if (req.hasParameter("yes")) {
            rsp.sendRedirect(req.getContextPath() + "/manage/" + config.getUrlName());
        } else {
            rsp.forwardToPreviousPage(req);
        }
    }

    /**
     * Get the failure message from the last bundle upload.
     *
     * @return String     the error message that was saved as part of the last BundleUpload
     */
    public String getFailureMessage() {
        return AdvisorGlobalConfiguration.getInstance().getLastBundleResult();
    }
}
