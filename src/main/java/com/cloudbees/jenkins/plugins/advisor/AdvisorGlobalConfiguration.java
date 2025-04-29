package com.cloudbees.jenkins.plugins.advisor;

import com.cloudbees.jenkins.plugins.advisor.client.AdvisorClient;
import com.cloudbees.jenkins.plugins.advisor.client.model.Recipient;
import com.cloudbees.jenkins.plugins.advisor.utils.EmailUtil;
import com.cloudbees.jenkins.plugins.advisor.utils.EmailValidator;
import com.cloudbees.jenkins.support.SupportAction;
import com.cloudbees.jenkins.support.SupportPlugin;
import com.cloudbees.jenkins.support.api.Component;
import com.cloudbees.jenkins.support.configfiles.AgentsConfigFile;
import com.cloudbees.jenkins.support.configfiles.ConfigFileComponent;
import com.cloudbees.jenkins.support.configfiles.OtherConfigFilesComponent;
import com.cloudbees.jenkins.support.impl.AboutBrowser;
import com.cloudbees.jenkins.support.impl.AboutJenkins;
import com.cloudbees.jenkins.support.impl.AboutUser;
import com.cloudbees.jenkins.support.impl.AdministrativeMonitors;
import com.cloudbees.jenkins.support.impl.BuildQueue;
import com.cloudbees.jenkins.support.impl.EnvironmentVariables;
import com.cloudbees.jenkins.support.impl.FileDescriptorLimit;
import com.cloudbees.jenkins.support.impl.JVMProcessSystemMetricsContents;
import com.cloudbees.jenkins.support.impl.JenkinsLogs;
import com.cloudbees.jenkins.support.impl.LoggerManager;
import com.cloudbees.jenkins.support.impl.NodeMonitors;
import com.cloudbees.jenkins.support.impl.ReverseProxy;
import com.cloudbees.jenkins.support.impl.RunningBuilds;
import com.cloudbees.jenkins.support.impl.SlaveLaunchLogs;
import com.cloudbees.jenkins.support.impl.SlaveLogs;
import com.cloudbees.jenkins.support.impl.SystemConfiguration;
import com.cloudbees.jenkins.support.impl.SystemProperties;
import com.cloudbees.jenkins.support.impl.ThreadDumps;
import com.cloudbees.jenkins.support.impl.UpdateCenter;
import com.cloudbees.jenkins.support.slowrequest.SlowRequestComponent;
import com.cloudbees.jenkins.support.threaddump.HighLoadComponent;
import com.cloudbees.jenkins.support.timer.DeadlockRequestComponent;
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import jenkins.util.io.OnMaster;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.verb.POST;

@Extension
public class AdvisorGlobalConfiguration extends ManagementLink
        implements Describable<AdvisorGlobalConfiguration>, ExtensionPoint, Saveable, OnMaster {

    public static final String PLUGIN_NAME = "cloudbees-jenkins-advisor";
    public static final String SEND_ALL_COMPONENTS = "SENDALL";

    public static final String INVALID_CONFIGURATION = "invalid-configuration";
    public static final String SERVICE_OPERATIONAL = "service-operational";

    private static final Logger LOG = Logger.getLogger(AdvisorGlobalConfiguration.class.getName());

    private static final Set<Class<? extends Component>> ALLOWED_SUPPORT_CORE_COMPONENTS = Set.of(
            AboutBrowser.class,
            AboutJenkins.class,
            AboutUser.class,
            AdministrativeMonitors.class,
            SystemConfiguration.class,
            UpdateCenter.class,
            ThreadDumps.class,
            SystemProperties.class,
            SlowRequestComponent.class,
            SlaveLogs.class,
            SlaveLaunchLogs.class,
            RunningBuilds.class,
            JVMProcessSystemMetricsContents.Agents.class,
            SystemConfiguration.Agents.class,
            AgentsConfigFile.class,
            BuildQueue.class,
            ConfigFileComponent.class,
            DeadlockRequestComponent.class,
            EnvironmentVariables.class,
            FileDescriptorLimit.class,
            HighLoadComponent.class,
            JenkinsLogs.class,
            LoggerManager.class,
            JVMProcessSystemMetricsContents.Master.class,
            SystemConfiguration.Master.class,
            NodeMonitors.class,
            // it's used in some probes and is anonymized, and also removes credentials.xml... so
            // in theory that should be ok...
            OtherConfigFilesComponent.class,
            ReverseProxy.class);

    // a set of additional components that are not in the support core plugin dependencies
    // and not necessarily public but are used by some advisor probes
    private static final Set<String> ALLOWED_EXTRA_COMPONENTS = Set.of(
            "com.cloudbees.opscenter.server.model.ConnectedMasterSupportComponent",
            "org.jenkinsci.plugins.useractivity.support.UserActivityComponent",
            "com.cloudbees.opscenter.support.FIPSSupport",
            "com.cloudbees.jenkins.plugins.assurance.CloudBeesAssuranceSupport");

    private static final Set<String> ALLOWED_COMPONENTS;

    static {
        var allowedComponents = new HashSet<String>();
        allowedComponents.addAll(ALLOWED_EXTRA_COMPONENTS);
        allowedComponents.addAll(
                ALLOWED_SUPPORT_CORE_COMPONENTS.stream().map(Class::getName).collect(Collectors.toSet()));
        ALLOWED_COMPONENTS = Collections.unmodifiableSet(allowedComponents);
    }

    private String email;
    private List<Recipient> ccs;
    private Set<String> excludedComponents;
    private boolean nagDisabled;
    private boolean acceptToS;
    private String lastBundleResult;

    /**
     * @deprecated replaced by ccs since 2.12
     */
    @Deprecated
    private transient String cc;
    /**
     * @deprecated removed since 2.12
     */
    @Deprecated
    private transient boolean isValid;

    public AdvisorGlobalConfiguration() {
        load();
    }

    public AdvisorGlobalConfiguration(String email, List<Recipient> ccs, Set<String> excludedComponents) {
        this.setEmail(email);
        this.setCcs(ccs);
        this.setExcludedComponents(excludedComponents);
    }

    public static AdvisorGlobalConfiguration getInstance() {
        return Jenkins.get().getExtensionList(AdvisorGlobalConfiguration.class).get(0);
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "symbol-medkit-outline plugin-ionicons-api";
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

    @Override
    public @Nonnull Category getCategory() {
        return Category.TROUBLESHOOTING;
    }

    @CheckForNull
    @Override
    public String getDescription() {
        return Messages.Insights_Description();
    }

    public String getActionTitle() {
        return Messages.Insights_Title();
    }

    public String getActionDisclaimer() {
        return Messages.Insights_Disclaimer();
    }

    public String getDisclaimer() {
        return Messages.Insights_Disclaimer();
    }

    public boolean isNagDisabled() {
        return nagDisabled;
    }

    @DataBoundSetter
    public void setNagDisabled(boolean nagDisabled) {
        if (this.nagDisabled != nagDisabled) {
            this.nagDisabled = nagDisabled;
        }
    }

    public boolean isAcceptToS() {
        return acceptToS;
    }

    @DataBoundSetter
    public void setAcceptToS(boolean acceptToS) {
        if (this.acceptToS != acceptToS) {
            this.acceptToS = acceptToS;
        }
    }

    public String getLastBundleResult() {
        return lastBundleResult;
    }

    @DataBoundSetter
    public void setLastBundleResult(String lastBundleResult) {
        this.lastBundleResult = lastBundleResult;
    }

    protected Object readResolve() {
        // cc is depreacated in 2.12 and replaced by ccs
        if (cc != null) {
            this.setCcs(Arrays.stream(StringUtils.split(EmailUtil.fixEmptyAndTrimAllSpaces(cc), ","))
                    .map(EmailUtil::fixEmptyAndTrimAllSpaces)
                    .filter(Objects::nonNull)
                    .map(Recipient::new)
                    .collect(Collectors.toList()));
            save();
        }
        return this;
    }

    /**
     * Handles the form submission
     *
     * @param req the request.
     * @return the response.
     */
    @RequirePOST
    @Nonnull
    @Restricted(NoExternalUse.class)
    public HttpResponse doConfigure(@Nonnull StaplerRequest2 req) {
        Jenkins jenkins = Jenkins.get();
        jenkins.checkPermission(Jenkins.ADMINISTER);
        try {
            configureDescriptor(req, req.getSubmittedForm(), getDescriptor());
            save();
            // We want to refresh the page to reload the status even when we click on "Apply"
            if (!isValid() || StringUtils.isNotBlank(req.getParameter("advisor:apply"))) {
                return HttpResponses.redirectToDot();
            } else {
                return HttpResponses.redirectTo(req.getContextPath() + "/manage");
            }
        } catch (Exception e) {
            LOG.severe(
                    "Unable to save Jenkins Health Advisor by CloudBees configuration: " + Functions.printThrowable(e));
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
    private boolean configureDescriptor(StaplerRequest2 req, JSONObject json, Descriptor<?> d) throws FormException {
        req.bindJSON(this, json);
        return d.configure(req, json);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Descriptor<AdvisorGlobalConfiguration> getDescriptor() {
        return Jenkins.get().getDescriptorOrDie(getClass());
    }

    public String getEmail() {
        return email;
    }

    @DataBoundSetter
    public void setEmail(@CheckForNull String email) {
        this.email = EmailUtil.fixEmptyAndTrimAllSpaces(email);
    }

    @Nonnull
    public List<Recipient> getCcs() {
        return ccs != null ? ccs : Collections.emptyList();
    }

    @DataBoundSetter
    public void setCcs(List<Recipient> ccs) {
        this.ccs = ccs;
    }

    @Nonnull
    public Set<String> getExcludedComponents() {
        return excludedComponents != null ? excludedComponents : Collections.emptySet();
    }

    @DataBoundSetter
    public void setExcludedComponents(Set<String> excludedComponents) {
        this.excludedComponents = excludedComponents;
    }

    public List<Component> getIncludedComponents() {
        List<Component> included = new ArrayList<>();
        if (getExcludedComponents().isEmpty()) {
            for (Component c : getComponents()) {
                if (c.isSelectedByDefault()) {
                    included.add(c);
                }
            }
        } else {
            for (Component c : getComponents()) {
                if (!getExcludedComponents().contains(c.getId())) {
                    included.add(c);
                }
            }
        }
        return included;
    }

    public boolean selectedByDefault(Component c) {
        if (getExcludedComponents().isEmpty()) {
            return c.isSelectedByDefault();
        }
        return !getExcludedComponents().contains(c.getId());
    }

    public List<Component> getComponents() {
        return SupportPlugin.getComponents().stream()
                .filter(c -> ALLOWED_COMPONENTS.contains(c.getClass().getName()))
                .collect(Collectors.toUnmodifiableList());
    }

    public boolean isValid() {
        return isValid(false, isAcceptToS(), getEmail(), getCcs());
    }

    public static boolean isValid(boolean logErrors, boolean acceptToS, String email, List<Recipient> ccs) {
        if (!acceptToS) {
            if (logErrors) {
                LOG.warning("acceptToS is invalid, it must be set to true");
            }
            return false;
        }
        if (!EmailValidator.isValidEmail(email)) {
            if (logErrors) {
                LOG.warning(() -> String.format("email \"%s\" is not valid", email));
            }
            return false;
        }
        if (!ccs.isEmpty()) {
            List<String> erroneousCCEmails = ccs.stream()
                    .map(Recipient::getEmail)
                    .filter(s -> !EmailValidator.isValidEmail(s))
                    .collect(Collectors.toList());
            if (!erroneousCCEmails.isEmpty()) {
                erroneousCCEmails.forEach(s -> {
                    if (logErrors) {
                        LOG.warning(() -> String.format("cc \"%s\" is not valid", s));
                    }
                });
                return false;
            }
        }
        return true;
    }

    boolean isPluginEnabled() {
        var jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            PluginWrapper plugin = jenkins.getPluginManager().getPlugin(PLUGIN_NAME);

            if (plugin == null) {
                LOG.severe("Expected to find plugin: [" + PLUGIN_NAME + "] but none found");
                return false;
            }
            return plugin.isEnabled();
        }
        return false;
    }

    @Override
    public synchronized void save() {
        if (BulkChange.contains(this)) {
            return;
        }
        try {
            getConfigFile().write(this);
            SaveableListener.fireOnChange(this, getConfigFile());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to save " + getConfigFile(), e);
        }
    }

    public synchronized void load() {
        XmlFile file = getConfigFile();
        if (!file.exists()) {
            return;
        }

        try {
            file.unmarshal(this);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to load " + file, e);
        }
    }

    private XmlFile getConfigFile() {
        return new XmlFile(new File(Jenkins.get().getRootDir(), getClass().getName() + ".xml"));
    }

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

        // Used from AdvisorGlobalConfiguration/index.jelly
        public FormValidation doCheckAcceptToS(@QueryParameter boolean value) {
            if (!value) {
                return FormValidation.error("Accepting our Terms and Conditions is mandatory to use this service.");
            }
            return FormValidation.ok();
        }

        // Used from AdvisorGlobalConfiguration/index.jelly
        public FormValidation doCheckEmail(@QueryParameter String value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            return EmailValidator.validateEmail(value);
        }

        @POST
        public FormValidation doTestSendEmail(
                @QueryParameter("email") final String value, @QueryParameter("acceptToS") final boolean acceptToS) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            return EmailValidator.testSendEmail(value, acceptToS);
        }

        // Used from validateOnLoad.jelly
        public String validateServerConnection() {
            AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
            if (!config.isValid()) {
                return INVALID_CONFIGURATION;
            }
            try {
                AdvisorClient advisorClient = new AdvisorClient(new Recipient(null));
                advisorClient.doCheckHealth();
                return SERVICE_OPERATIONAL;
            } catch (Exception e) {
                return "" + e.getMessage();
            }
        }

        @Override
        public boolean configure(StaplerRequest2 req, JSONObject json) {
            boolean acceptToS = json.getBoolean("acceptToS");
            String email = json.getString("email");
            List<Recipient> ccs = req.bindJSONToList(Recipient.class, json.get("ccs"));
            boolean nagDisabled = json.getBoolean("nagDisabled");
            JSONObject advanced = json.getJSONObject("advanced");

            Set<String> remove = new HashSet<>();
            for (SupportAction.Selection s :
                    req.bindJSONToList(SupportAction.Selection.class, advanced.get("components"))) {
                if (!s.isSelected()) {
                    LOG.log(Level.FINER, "Excluding ''{0}'' from list of components to include", s.getName());
                    remove.add(s.getName());
                }
            }
            // Note that we're not excluding anything
            if (remove.isEmpty()) {
                remove.add(SEND_ALL_COMPONENTS);
            }

            final AdvisorGlobalConfiguration advisorGlobalConfiguration = AdvisorGlobalConfiguration.getInstance();
            if (advisorGlobalConfiguration != null) {
                advisorGlobalConfiguration.setAcceptToS(acceptToS);
                advisorGlobalConfiguration.setEmail(email);
                advisorGlobalConfiguration.setCcs(ccs);
                advisorGlobalConfiguration.setNagDisabled(nagDisabled);
                advisorGlobalConfiguration.setExcludedComponents(remove);
            }

            try {
                return advisorGlobalConfiguration != null && advisorGlobalConfiguration.isValid();
            } catch (Exception e) {
                LOG.severe("Unexpected error while validating form: " + Functions.printThrowable(e));
                return false;
            }
        }
    }
}
