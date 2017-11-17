package com.cloudbees.jenkins.plugins.advisor.webhook;

import com.cloudbees.jenkins.plugins.advisor.AdvisorReport;
import com.cloudbees.jenkins.plugins.advisor.AdvisorReports;
import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.UnprotectedRootAction;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.Validate;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;


/**
 * Receives and saves the reports provided by the 
 * CloudBees Jenkins Advisor hook.
 *
 */
@Extension
public class AdvisorWebhook implements UnprotectedRootAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(AdvisorWebhook.class);
  public static final String URLNAME = "advisor-webhook";

  @Override
  public String getIconFileName() {
    return null;
  }

  @Override
  public String getDisplayName() {
    return null;
  }

  @Override
  public String getUrlName() {
    return URLNAME;
  }



  /**
   * Receives the webhook call
   *
   * @param req  The request
   */
  @SuppressWarnings("unused")
  @RequirePostWithAdvisorPayload
  @Nonnull
  @Restricted(NoExternalUse.class)
  public void doIndex(@Nonnull StaplerRequest req) {
    try {
        JSONObject json = req.getSubmittedForm();
        AdvisorReport newestReport = req.bindJSON(AdvisorReport.class, json);
        AdvisorReports reports = AdvisorReports.getInstance();
        reports.addNewReport(newestReport);
        reports.save();
    } catch (Exception ex) {
        LOGGER.error("Exception when processing the current webhooks.", ex);
    }
  }

  public static AdvisorWebhook get() {
    return Jenkins.getInstance().getExtensionList(RootAction.class).get(AdvisorWebhook.class);
  }

  @Nonnull
  public static Jenkins getJenkinsInstance() throws IllegalStateException {
    Jenkins instance = Jenkins.getInstance();
    Validate.validState(instance != null, "Jenkins has not been started, or was already shut down");
    return instance;
  }

}