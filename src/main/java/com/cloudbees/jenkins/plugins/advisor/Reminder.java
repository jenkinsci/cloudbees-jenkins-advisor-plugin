package com.cloudbees.jenkins.plugins.advisor;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import java.io.IOException;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Displays the reminder that the configuration must be done.
 */
@Extension
public class Reminder extends AdministrativeMonitor {

    @Override
    public boolean isActivated() {
        AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
        /*
        no nag when configured
        no nag when disabled
        */
        return !config.isValid() && config.isPluginEnabled() && !config.isNagDisabled();
    }

    @Override
    public String getDisplayName() {
        return Messages.Reminder_DisplayName();
    }

    @Restricted(NoExternalUse.class)
    @RequirePOST
    public void doAct(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
        if (req.hasParameter("yes")) {
            rsp.sendRedirect(req.getContextPath() + "/manage/" + config.getUrlName());
        } else if (req.hasParameter("no")) {
            // should never return null if we get here
            rsp.sendRedirect(req.getContextPath() + "/"
                    + Jenkins.get().getPluginManager().getSearchUrl() + "/installed");
        } else {
            rsp.forwardToPreviousPage(req);
        }
    }
}
