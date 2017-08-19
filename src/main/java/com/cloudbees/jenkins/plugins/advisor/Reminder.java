package com.cloudbees.jenkins.plugins.advisor;

import hudson.Extension;
import hudson.model.PageDecorator;
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
public class Reminder extends PageDecorator {

  private transient volatile long lastNagTime;

  public Reminder() {
    load();
  }

  public boolean isNagDue() {
    if (!(Jenkins.getInstance().servletContext.getAttribute("app") instanceof Jenkins)) {
      return false;   // no point in nagging the user during licensing screens
    }

    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    if (config.isValid()) {
      return false; // no nag when registered
    }
    if (!config.isPluginEnabled()) {
      return false; // no nag when disabled
    }
    if (config.isNagDisabled()) {
      return false; // no nag when explicitly avoided
    }
    try {
      HttpSession session = Stapler.getCurrentRequest().getSession(false);
      if (session != null) {
        Long nextNagTime = (Long) session.getAttribute(Reminder.class.getName() + ".nextNagTime");
        if (nextNagTime != null) {
          return System.currentTimeMillis() > nextNagTime;
        }
      }
      return System.currentTimeMillis() >= lastNagTime + TimeUnit.SECONDS.toMillis(600);
    } catch (Exception ex) {
      // If there's an issue trying to figure out if we need to nag, let's nag
    }
    return true;
  }

  public static Reminder getInstance() {
    for (PageDecorator d : PageDecorator.all()) {
      if (d instanceof Reminder) {
        return (Reminder) d;
      }
    }
    throw new AssertionError(Reminder.class + " is missing its descriptor");
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
      lastNagTime = System.currentTimeMillis();
      return HttpResponses.forwardToPreviousPage();
    }
  }
}
