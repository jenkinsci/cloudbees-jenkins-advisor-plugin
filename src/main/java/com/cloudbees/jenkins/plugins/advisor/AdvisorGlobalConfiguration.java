package com.cloudbees.jenkins.plugins.advisor;

import com.cloudbees.jenkins.plugins.advisor.client.AdvisorClient;
import com.cloudbees.jenkins.plugins.advisor.client.model.AccountCredentials;
import com.cloudbees.jenkins.plugins.advisor.webhook.AdvisorWebhookCredentialsManager;
import com.cloudbees.jenkins.support.SupportAction;
import com.cloudbees.jenkins.support.SupportPlugin;
import com.cloudbees.jenkins.support.api.Component;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.*;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Descriptor.FormException;
import hudson.model.ManagementLink;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.util.io.OnMaster;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Extension
public class AdvisorGlobalConfiguration
  extends ManagementLink
  implements Describable<AdvisorGlobalConfiguration>, ExtensionPoint, Saveable, OnMaster {

  public static final String PLUGIN_NAME = "cloudbees-jenkins-advisor";

  private static final Logger LOG = Logger.getLogger(AdvisorGlobalConfiguration.class.getName());

  private String email;
  private Secret password;
  private Set<String> excludedComponents;
  private boolean isValid;
  private boolean nagDisabled;
  private String credentialsId;

  @SuppressWarnings("unused")
  public AdvisorGlobalConfiguration() {
    load();
  }

  @SuppressWarnings("unused")
  @DataBoundConstructor
  public AdvisorGlobalConfiguration(String email, Secret password, Set<String> excludedComponents, String credentialsId) {
    this.email = email;
    this.password = password;
    this.excludedComponents = excludedComponents;
    this.credentialsId = credentialsId;
  }

  @CheckForNull
  @Override
  public String getIconFileName() {
    return "/plugin/cloudbees-jenkins-advisor/icons/heart.png";
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
  public String getActionBlurb() {
    return Messages.Insights_Disclaimer();
  }

  @SuppressWarnings("unused")
  public String getDisclaimer() {
    return Messages.Insights_Disclaimer();
  }

  public boolean isNagDisabled() {
    return nagDisabled;
  }

  @SuppressWarnings("unused")
  public void setNagDisabled(boolean nagDisabled) {
    if (this.nagDisabled != nagDisabled) {
      this.nagDisabled = nagDisabled;
    }
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
  @SuppressWarnings("unused") // stapler web method binding
  public HttpResponse doConfigure(@Nonnull StaplerRequest req) throws IOException, ServletException,
    FormException {
    Jenkins jenkins = Jenkins.getInstance();
    jenkins.checkPermission(Jenkins.ADMINISTER);
    try {
      // For Pardot; only want to send on new email setup
      String oldEmail = email;
      isValid = configureDescriptor(req, req.getSubmittedForm(), getDescriptor());
      save();
      String validPath = sendToPardot(req.getSubmittedForm(), oldEmail, req.getContextPath());
      return HttpResponses.redirectTo(isValid ? validPath : req.getContextPath() + "/" + getUrlName());

    } catch (Exception e) {
      isValid = false;
      LOG.severe("Unable to save CloudBees Jenkins Advisor configuration: " + Functions.printThrowable(e));
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
  private boolean configureDescriptor(StaplerRequest req, JSONObject json, Descriptor<?> d) throws
    FormException, ServletException {
    req.bindJSON(this, json);
    return d.configure(req, json);
  }

  /**
   * Determine if we should send the information onto Pardot.  Pardot will only process
   * requests with a non-null email, which isn't a guaranteed state for the
   * GlobalConfiguration.  Only forward the request onto Pardot if the email exists.
   */
  private String sendToPardot(JSONObject json, String oldEmail, String contextPath) {
    String url = contextPath + "/manage";

    try {
      String email = json.getString("email");
      if(email != null && !email.isEmpty() && (oldEmail == null || !email.equals(oldEmail))) {
        url = URLEncoder.encode(url, "UTF-8");
        email = URLEncoder.encode(email, "UTF-8");
        url = "https://go.pardot.com/l/272242/2017-07-27/47fs4?success_location=" + url + "&email=" + email;
      }
    } catch (UnsupportedEncodingException ex) {
      //Don't bother sending information to Pardot; continue on
    }
    return url;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Descriptor<AdvisorGlobalConfiguration> getDescriptor() {
    return Jenkins.getInstance().getDescriptorOrDie(getClass());
  }

  @SuppressWarnings("unused")
  public void setEmail(@CheckForNull String email) {
    String emailAddress = Util.nullify(email);
    if(emailAddress != null && emailAddress.startsWith("\"") && emailAddress.endsWith("\"")) {
      emailAddress = emailAddress.substring(1,emailAddress.length()-1);
    }
    this.email = emailAddress;
  }

  @SuppressWarnings("WeakerAccess")
  public @Nonnull String getEmail() {
    return email;
  }

  public @Nonnull Secret getPassword() {
    return password;
  }

  @SuppressWarnings("unused")
  public void setPassword(@CheckForNull Secret password) {
    this.password = password;
  }

  public Set<String> getExcludedComponents() {
    return excludedComponents != null ? excludedComponents : Collections.emptySet();
  }

  public void setExcludedComponents(Set<String> excludedComponents) {
    this.excludedComponents = excludedComponents;
  }

  public List<Component> getIncludedComponents() {
    if (getExcludedComponents().isEmpty()) {
      return getComponents().stream()
        .filter(Component::isSelectedByDefault)
        .collect(Collectors.toList());
    }
    return getComponents().stream()
      .filter(c -> !getExcludedComponents().contains(c.getId()))
      .collect(Collectors.toList());
  }

  @SuppressWarnings("unused")
  public boolean selectedByDefault(Component c) {
    if (getExcludedComponents().isEmpty()) {
      return c.isSelectedByDefault();
    }
    return !getExcludedComponents().contains(c.getId());
  }

  @SuppressWarnings("unused")
  public List<Component> getComponents() {
    return SupportPlugin.getComponents();
  }

  public boolean isValid() {
    return isValid;
  }

  public void setValid(boolean valid) {
    isValid = valid;
  }

  public void setCredentialsId(String credentialsId) {
    this.credentialsId = credentialsId;
  }

  public String getCredentialsId() {
    return credentialsId;
  }

  public AdvisorReports getAdvisorReports() {
    return Jenkins.getInstance().getExtensionList(AdvisorReports.class).get(0);
  }

  @SuppressWarnings("unused")
  @Extension
  public static final class DescriptorImpl extends Descriptor<AdvisorGlobalConfiguration> {

    public DescriptorImpl() {
      load();
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
    public FormValidation doCheckEmail(@QueryParameter String value) throws IOException, ServletException {
      if (value == null || value.isEmpty()) {
        return FormValidation.error("Email cannot be blank");
      }

      try {
        new InternetAddress(value).validate();
      } catch (AddressException e) {
        return FormValidation.error("Invalid email: " + e.getMessage());
      }
      return FormValidation.ok();
    }

    @SuppressWarnings("WeakerAccess")
    public FormValidation doCheckPassword(@QueryParameter String value) throws IOException, ServletException {
      if (value == null || value.isEmpty()) {
        return FormValidation.error("Password cannot be blank");
      }
      return FormValidation.ok();
    }

    @SuppressWarnings("unused")
    public FormValidation doTestConnection(@QueryParameter("email") final String email,
                                           @QueryParameter("password") final Secret password)
      throws IOException, ServletException {
      try {
        AdvisorClient advisorClient = new AdvisorClient(new AccountCredentials(email.trim(), Secret.toString(password)));

        advisorClient.doAuthenticate().get();
        return FormValidation.ok("Success");
      } catch (Exception e) {
        return FormValidation.error("Client error : "+e.getMessage());
      }
    }

    @SuppressWarnings("unused")
    public String connectionTest(String credentials) {
      try {
        String[] creds = credentials.split(",");
        AdvisorClient advisorClient = new AdvisorClient(new AccountCredentials(creds[0].trim(), creds[1]));
        advisorClient.doAuthenticate().get();
        return "You are connected to CloudBees Jenkins Advisor!";
      } catch(Exception e) {
        return "" + e.getMessage();
      }
    }

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {
      LOG.log(Level.INFO, "DOES kwhetstone_advisor_webhook exist? "  + AdvisorWebhookCredentialsManager.credentialsExists("kwhetstone_advisor_webhook") );
      AdvisorWebhookCredentialsManager.listAllCredentials(LOG);
      return AdvisorWebhookCredentialsManager.populateDropdown(credentialsId) ;
    }
  
    //USE THIS TO RUN THE VALIDATION TO INSIGHTS
    public FormValidation doCheckCredentialsId(@AncestorInPath Item item, @QueryParameter String value) {
        if (item == null ) {
          return FormValidation.ok();
        }
        if(!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
          return FormValidation.ok();
        }
  
        if (StringUtils.isBlank(value)) {
          return FormValidation.ok();
        }
  
        if (AdvisorWebhookCredentialsManager.credentialsExists(value)) {
          return FormValidation.error("Cannot find currently selected credentials");
        }
        return FormValidation.ok();
      }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
      String email = json.getString("email");
      String password = json.getString("password");
      Boolean nagDisabled = json.getBoolean("nagDisabled");
      JSONObject advanced = json.getJSONObject("advanced");

      Set<String> remove = new HashSet<>();
      for (SupportAction.Selection s : req.bindJSONToList(SupportAction.Selection.class, advanced.get("components"))) {
        if (!s.isSelected()) {
          LOG.log(Level.FINER, "Excluding ''{0}'' from list of components to include", s.getName());
          remove.add(s.getName());
        }
      }
      // Note that we're not excluding anything
      if(remove.isEmpty()) { 
        remove.add("SENDALL");  
      }

      final AdvisorGlobalConfiguration insights = AdvisorGlobalConfiguration.getInstance();
      if (insights != null) {
        insights.setExcludedComponents(remove);
      }

      try {
        if(!nagDisabled) {
          if (doCheckEmail(email).kind.equals(FormValidation.Kind.ERROR)
            || doCheckPassword(password).kind.equals(FormValidation.Kind.ERROR)) {
            return false;
          }
        }
      } catch (Exception e) {
        LOG.severe("Unexpected error while validating form: " + Functions.printThrowable(e));
        return false;
      }
      return true;
    }
  }

  private volatile long nextCheck = 0;
  private volatile boolean lastEnabledState = false;

  boolean isPluginEnabled() {
    if (System.currentTimeMillis() > nextCheck) {
      try {
        PluginWrapper plugin = Jenkins.getInstance().getPluginManager().getPlugin(PLUGIN_NAME);

        if (plugin == null) {
          LOG.severe("Expected to find plugin: [" + PLUGIN_NAME + "] but none found");
          return false;
        }
        lastEnabledState = plugin.isEnabled();
        nextCheck = System.currentTimeMillis() + 5000;
      } catch (NullPointerException e) {
        return false;
      }
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
    return new XmlFile(new File(Jenkins.getInstance().getRootDir(),getClass().getName()+".xml"));
  }

  public static AdvisorGlobalConfiguration getInstance() {
    return Jenkins.getInstance().getExtensionList(AdvisorGlobalConfiguration.class).get(0);
  }
}
