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

import java.io.IOException;

/**
 * Displays the reminder that the user needs to register.
 */
@Extension
public class Reminder extends AdministrativeMonitor {

  @Override
  public boolean isActivated() {
    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    if (config.isValid()) {
      return false; // no nag when registered
    } else if (!config.isPluginEnabled()) {
      return false; // no nag when disabled
    } else if (config.isNagDisabled()) {
      return false; // no nag when explicitly avoided
    } else {
      return true;
    }
  }

  @Restricted(NoExternalUse.class)
  @RequirePOST
  public HttpResponse doAct(@QueryParameter(fixEmpty = true) String yes,
                            @QueryParameter(fixEmpty = true) String no) throws IOException {
    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    if (yes != null) {
      return HttpResponses.redirectViaContextPath(config.getUrlName());
    } else if (no != null) {
      // should never return null if we get here
      return HttpResponses.redirectViaContextPath(Jenkins.getInstance().getPluginManager().getSearchUrl() + "/installed");
    } else { //remind later
      return HttpResponses.forwardToPreviousPage();
    }
  }
}
