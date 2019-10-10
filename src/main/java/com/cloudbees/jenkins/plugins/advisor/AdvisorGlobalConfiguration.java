package com.cloudbees.jenkins.plugins.advisor;

import com.cloudbees.jenkins.plugins.advisor.client.AdvisorClient;
import com.cloudbees.jenkins.plugins.advisor.client.model.AccountCredentials;
import com.cloudbees.jenkins.plugins.advisor.utils.EmailUtil;
import com.cloudbees.jenkins.plugins.advisor.utils.EmailValidator;
import com.cloudbees.jenkins.plugins.advisor.utils.FormValidationHelper;
import com.cloudbees.jenkins.support.SupportAction;
import com.cloudbees.jenkins.support.SupportPlugin;
import com.cloudbees.jenkins.support.api.Component;
import hudson.BulkChange;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.Functions;
import hudson.PluginWrapper;
import hudson.XmlFile;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.ManagementLink;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.util.io.OnMaster;
import net.sf.json.JSONObject;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class AdvisorGlobalConfiguration
  extends ManagementLink
  implements Describable<AdvisorGlobalConfiguration>, ExtensionPoint, Saveable, OnMaster {

  public static final String PLUGIN_NAME = "cloudbees-jenkins-advisor";
  public static final String SEND_ALL_COMPONENTS = "SENDALL";

  private static final Logger LOG = Logger.getLogger(AdvisorGlobalConfiguration.class.getName());

  private String email;
  private String cc;
  private Set<String> excludedComponents;
  private boolean isValid;
  private boolean nagDisabled;
  private boolean acceptToS;
  private String lastBundleResult;

  @SuppressWarnings("unused")
  public AdvisorGlobalConfiguration() {
    load();
  }

  @SuppressWarnings("unused")
  @DataBoundConstructor
  public AdvisorGlobalConfiguration(String email, String cc, Set<String> excludedComponents) {
    this.setEmail(email);
    this.setCc(cc);
    this.excludedComponents = excludedComponents;
    this.lastBundleResult = "";
  }

  @CheckForNull
  @Override
  public String getIconFileName() {
    return "/plugin/cloudbees-jenkins-advisor/icons/advisor.svg";
  }

  @CheckForNull
  @Override
  public String getUrlName() {
    return PLUGIN_NAME;
  }

  @CheckForNull
  @Override
  public String getDisplayName() {
    return Messages.Insights_DisplayName();
  }

  @CheckForNull
  @Override
  public String getDescription() {
    return Messages.Insights_Description();
  }

  @SuppressWarnings("unused")
  public String getActionTitle() {
    return Messages.Insights_Title();
  }

  @SuppressWarnings("unused")
  public String getActionDisclaimer() {
    return Messages.Insights_Disclaimer();
  }

  @SuppressWarnings("unused")
  public String getDisclaimer() {
    return Messages.Insights_Disclaimer();
  }

  @SuppressWarnings("WeakerAccess")
  public boolean isNagDisabled() {
    return nagDisabled;
  }

  @SuppressWarnings("unused")
  public void setNagDisabled(boolean nagDisabled) {
    if (this.nagDisabled != nagDisabled) {
      this.nagDisabled = nagDisabled;
    }
  }

  @SuppressWarnings("WeakerAccess")
  public boolean isAcceptToS() {
    return acceptToS;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setAcceptToS(boolean acceptToS) {
    if (this.acceptToS != acceptToS) {
      this.acceptToS = acceptToS;
    }
  }

  @SuppressWarnings("WeakerAccess")
  public String getLastBundleResult() {
    return lastBundleResult;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setLastBundleResult(String lastBundleResult) {
    this.lastBundleResult = lastBundleResult;
  }

  /**
   * Handles the form submission
   *
   * @param req the request.
   * @return the response.
   * @throws IOException if something goes wrong.
   * @throws ServletException if something goes wrong.
   * @throws FormException if something goes wrong.
   */
  @RequirePOST
  @Nonnull
  @Restricted(NoExternalUse.class)
  @SuppressWarnings({"unused", "WeakerAccess"}) // stapler web method binding
  public HttpResponse doConfigure(@Nonnull StaplerRequest req) throws IOException, ServletException,
    FormException {
    Jenkins jenkins = Jenkins.get();
    jenkins.checkPermission(Jenkins.ADMINISTER);
    try {
      isValid = configureDescriptor(req, req.getSubmittedForm(), getDescriptor());
      save();

      return HttpResponses.redirectTo(isValid
          ? req.getContextPath() + "/manage"
          : req.getContextPath() + "/" + getUrlName());
    } catch (Exception e) {
      isValid = false;
      LOG.severe("Unable to save Jenkins Health Advisor by CloudBees configuration: " + Functions.printThrowable(e));
      return FormValidation.error("Unable to save configuration: " + e.getMessage());
    }
  }

  /**
   * Performs the configuration of a specific {@link Descriptor}.
   *
   * @param req  the request.
   * @param json the JSON object.
   * @param d    the {@link Descriptor}.
   * @return {@code false} to keep the client in the same config page.
   * @throws FormException if something goes wrong.
   */
  private boolean configureDescriptor(StaplerRequest req, JSONObject json, Descriptor<?> d) throws FormException {
    req.bindJSON(this, json);
    return d.configure(req, json);
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Descriptor<AdvisorGlobalConfiguration> getDescriptor() {
    return Jenkins.get().getDescriptorOrDie(getClass());
  }

  @SuppressWarnings("unused")
  public void setEmail(@CheckForNull String email) {
    this.email = EmailUtil.fixEmptyAndTrimAllSpaces(email);
  }

  @SuppressWarnings("WeakerAccess")
  public String getEmail() {
    return email;
  }

  @SuppressWarnings("unused")
  public void setCc(@CheckForNull String cc) {
    this.cc = EmailUtil.fixEmptyAndTrimAllSpaces(cc);
  }

  @SuppressWarnings("WeakerAccess")
  public String getCc() {
    return cc;
  }

  @SuppressWarnings("WeakerAccess")
  public Set<String> getExcludedComponents() {
    return excludedComponents != null ? excludedComponents : Collections.emptySet();
  }

  @SuppressWarnings("WeakerAccess")
  public void setExcludedComponents(Set<String> excludedComponents) {
    this.excludedComponents = excludedComponents;
  }

  @SuppressWarnings("WeakerAccess")
  public List<Component> getIncludedComponents() {
    List<Component> included = new ArrayList<>();
    if (getExcludedComponents().isEmpty()) {
      for(Component c : getComponents()) {
        if(c.isSelectedByDefault()) {
          included.add(c);
        }
      }
    } else {
      for(Component c : getComponents()) {
        if(!getExcludedComponents().contains(c.getId())) {
          included.add(c);
        }
      }
    }
    return included;
  }

  @SuppressWarnings("unused")
  public boolean selectedByDefault(Component c) {
    if (getExcludedComponents().isEmpty()) {
      return c.isSelectedByDefault();
    }
    return !getExcludedComponents().contains(c.getId());
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public List<Component> getComponents() {
    return SupportPlugin.getComponents();
  }

  @SuppressWarnings("WeakerAccess")
  public boolean isValid() {
    return isValid;
  }

  @SuppressWarnings("WeakerAccess")
  public void setValid(boolean valid) {
    isValid = valid;
  }

  @SuppressWarnings("unused")
  @Extension
  public static final class DescriptorImpl extends Descriptor<AdvisorGlobalConfiguration> {

    public DescriptorImpl() {
      super.load();
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public String getDisplayName() {
      return Messages.Insights_DisplayName();
    }



    @SuppressWarnings("WeakerAccess")
    public FormValidation doCheckEmail(@QueryParameter String value) {
      String emailAddress = EmailUtil.fixEmptyAndTrimAllSpaces(value);

      if (emailAddress == null || emailAddress.isEmpty()) {
        return FormValidation.error("Email cannot be blank");
      }

      if (emailAddress.contains(";") || emailAddress.contains(",")) {
        return FormValidation.error("Email cannot contain illegal character ';' or ','. Consider using the CC field if multiple recipients are required");
      }

      EmailValidator validator = EmailValidator.getInstance();
      if (!validator.isValid(emailAddress)) {
        return FormValidation.error("Invalid email");
      }

      return FormValidation.ok();
    }

    @SuppressWarnings("WeakerAccess")
    public FormValidation doCheckCc(@QueryParameter String value) {
      String emailAddress = EmailUtil.fixEmptyAndTrimAllSpaces(value);

      if (emailAddress == null || emailAddress.isEmpty()) {
        return FormValidation.ok();
      }

      if (emailAddress.contains(";")) {
        return FormValidation.error("Email cannot contain illegal character ';'. Use ',' if multiple recipients are required.");
      }

      for (String cc : emailAddress.split(",")) {
        EmailValidator validator = EmailValidator.getInstance();
        if (!validator.isValid(cc)) {
          return FormValidation.error(String.format("Invalid email [%s]", cc));
        }
      }

      return FormValidation.ok();
    }


    @SuppressWarnings({"unused", "WeakerAccess"})
    public FormValidation doTestConnection(@QueryParameter("email") final String email, @QueryParameter("cc") final String cc) {
      try {
        if(email.isEmpty()){
          return FormValidation.error("Missing email");
        }
        Optional<FormValidation> ccErrors = FormValidationHelper.validateCC(cc);
        if (ccErrors.isPresent()) {
          return ccErrors.get();
        }
        AdvisorClient advisorClient = new AdvisorClient(new AccountCredentials(email.trim()));

        advisorClient.doCheckHealth();
        return FormValidation.ok("Success");
      } catch (Exception e) {
        return FormValidation.error("Client error : "+e.getMessage());
      }
    }

    @SuppressWarnings("unused")
    public String connectionTest(String credentials) {
      AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
      if (!config.isAcceptToS()) {
        return "tos-not-accepted";
      }

      try {
        AdvisorClient advisorClient = new AdvisorClient(new AccountCredentials(credentials));
        advisorClient.doCheckHealth();
        return "service-operational";
      } catch(Exception e) {
        return "" + e.getMessage();
      }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public FormValidation doTestSendEmail(@QueryParameter("email") final String email, @QueryParameter("cc") final String cc) {
      try {
        if(email.isEmpty()){
          return FormValidation.error("Missing email");
        }
        Optional<FormValidation> ccErrors = FormValidationHelper.validateCC(cc);
        if (ccErrors.isPresent()) {
          return ccErrors.get();
        }

        AdvisorClient advisorClient = new AdvisorClient(new AccountCredentials(email.trim()));

        advisorClient.doTestEmail();
        return FormValidation.ok("Sending email.  Please check your inbox and filters.");
      } catch (Exception e) {
        return FormValidation.error("Client error : "+e.getMessage());
      }
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
      String email = json.getString("email");
      String cc = json.getString("cc");
      JSONObject advanced = json.getJSONObject("advanced");
      Boolean acceptToS = json.getBoolean("acceptToS");

      // Have to accept the Terms of Service to have a valid configuration
      if(!acceptToS) {
        return false;
      }

      Set<String> remove = new HashSet<>();
      for (SupportAction.Selection s : req.bindJSONToList(SupportAction.Selection.class, advanced.get("components"))) {
        if (!s.isSelected()) {
          LOG.log(Level.FINER, "Excluding ''{0}'' from list of components to include", s.getName());
          remove.add(s.getName());
        }
      }
      // Note that we're not excluding anything
      if(remove.isEmpty()) {
        remove.add(SEND_ALL_COMPONENTS);
      }

      final AdvisorGlobalConfiguration insights = AdvisorGlobalConfiguration.getInstance();
      if (insights != null) {
        insights.setExcludedComponents(remove);
      }

      try {
        if (doCheckEmail(email).kind.equals(FormValidation.Kind.ERROR) || doCheckCc(cc).kind.equals(FormValidation.Kind.ERROR)) {
          return false;
        }
      } catch (Exception e) {
        LOG.severe("Unexpected error while validating form: " + Functions.printThrowable(e));
        return false;
      }
      return true;
    }
  }

  boolean isPluginEnabled() {
    boolean lastEnabledState;
    try {
      PluginWrapper plugin = Jenkins.get().getPluginManager().getPlugin(PLUGIN_NAME);

      if (plugin == null) {
        LOG.severe("Expected to find plugin: [" + PLUGIN_NAME + "] but none found");
        return false;
      }
      lastEnabledState = plugin.isEnabled();
    } catch (NullPointerException e) {
      return false;
    }
    return lastEnabledState;
  }

  @Override
  public synchronized void save() {
    if(BulkChange.contains(this))   return;
    try {
      getConfigFile().write(this);
      SaveableListener.fireOnChange(this, getConfigFile());
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Failed to save "+getConfigFile(),e);
    }
  }

  @SuppressWarnings("WeakerAccess")
  public synchronized void load() {
    XmlFile file = getConfigFile();
    if(!file.exists())
      return;

    try {
      file.unmarshal(this);
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Failed to load "+file, e);
    }
  }

  private XmlFile getConfigFile() {
    return new XmlFile(new File(Jenkins.get().getRootDir(),getClass().getName()+".xml"));
  }

  public static AdvisorGlobalConfiguration getInstance() {
    return Jenkins.get().getExtensionList(AdvisorGlobalConfiguration.class).get(0);
  }
}
