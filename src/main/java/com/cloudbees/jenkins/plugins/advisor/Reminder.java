package com.cloudbees.jenkins.plugins.advisor;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
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
  @SuppressWarnings("unused")
  public HttpResponse doAct(@QueryParameter(fixEmpty = true) String yes, @QueryParameter(fixEmpty = true) String no) {
    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    if (yes != null) {
      return HttpResponses.redirectViaContextPath(config.getUrlName());
    } else if (no != null) {
      // should never return null if we get here
      return HttpResponses.redirectViaContextPath(Jenkins.get().getPluginManager().getSearchUrl() + "/installed");
    } else { //remind later
      return HttpResponses.forwardToPreviousPage();
    }
  }
}
