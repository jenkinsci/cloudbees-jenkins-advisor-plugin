package com.cloudbees.jenkins.plugins.advisor;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Displays the reminder that the user needs to register.
 */
@Extension
public class Reminder extends AdministrativeMonitor {

  @Override
  public boolean isActivated() {
    if (!(Jenkins.getInstance().servletContext.getAttribute("app") instanceof Jenkins)) {
      return false;   // no point in nagging the user during licensing screens
    }

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

  public HttpResponse doAct(StaplerRequest request, @QueryParameter(fixEmpty = true) String yes,
                            @QueryParameter(fixEmpty = true) String no) throws IOException, ServletException {
    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    if (yes != null) {
      return HttpResponses.redirectViaContextPath(config.getUrlName());
    } else if (no != null) {
      // should never return null if we get here
      Jenkins.getInstance().getPluginManager().getPlugin(AdvisorGlobalConfiguration.PLUGIN_NAME).disable();
      return HttpResponses
        .redirectViaContextPath(Jenkins.getInstance().getPluginManager().getSearchUrl() + "/installed");
    } else { //remind later
      return HttpResponses.forwardToPreviousPage();
    }
  }
}
