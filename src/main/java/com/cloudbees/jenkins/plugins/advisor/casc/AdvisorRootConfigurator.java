package com.cloudbees.jenkins.plugins.advisor.casc;

import com.cloudbees.jenkins.plugins.advisor.AdvisorGlobalConfiguration;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Sequence;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A root element configurator used for configuring CloudBees Jenkins Advisor through {@link io.jenkins.plugins.casc.ConfigurationAsCode}
 * https://github.com/jenkinsci/configuration-as-code-plugin
 */
@Extension(optional = true)
@Restricted(NoExternalUse.class)
public class AdvisorRootConfigurator extends BaseConfigurator<AdvisorGlobalConfiguration>
  implements RootElementConfigurator<AdvisorGlobalConfiguration> {

  static final String ACCEPT_TOS_ATTR = "acceptToS";
  static final String EMAIL_ATTR = "email";
  static final String CC_ATTR = "cc";
  static final String EXCLUDED_COMPONENTS_ATTR = "excludedComponents";
  static final String NAG_DISABLED_ATTR = "nagDisabled";
  
  // Ignoring lastBundleResult since it is not configured in the plugin. It only informs about the last bundle generation
  // Ignoring isValid since it is something auto-calculated during the configuration
  private final Collection<String> excludedAttributesInConf = Arrays.asList("lastBundleResult", "valid");

  @NonNull
  @Override
  public String getName() {
    return "advisor";
  }

  @Override
  public String getDisplayName() {
    return getConfiguration().getDisplayName();
  }

  @Override
  protected AdvisorGlobalConfiguration instance(Mapping mapping, ConfigurationContext configurationContext)
    throws ConfiguratorException {
    // Scalar values
    final String email = (mapping.get(EMAIL_ATTR) != null ? mapping.getScalarValue(EMAIL_ATTR) : StringUtils.EMPTY);
    final String cc = (mapping.get(CC_ATTR) != null ? mapping.getScalarValue(CC_ATTR) : StringUtils.EMPTY);
    final boolean nagDisabled = (mapping.get(NAG_DISABLED_ATTR) != null &&
      BooleanUtils.toBoolean(mapping.getScalarValue(NAG_DISABLED_ATTR)));
    final boolean acceptToS = (mapping.get(ACCEPT_TOS_ATTR) != null &&
      BooleanUtils.toBoolean(mapping.getScalarValue(ACCEPT_TOS_ATTR)));

    // List values
    final Set<String> excludedComponents = new HashSet<>();
    CNode excludedCN = mapping.get(EXCLUDED_COMPONENTS_ATTR);
    if (excludedCN != null) {
      if (excludedCN instanceof Sequence) {
        Sequence s = (Sequence) excludedCN;
        for (CNode cNode : s) {
          excludedComponents.add(cNode.asScalar().getValue());
        }

        if (excludedComponents.isEmpty()) {
          excludedComponents.add(AdvisorGlobalConfiguration.SEND_ALL_COMPONENTS);
        }
      } else {
        throw new ConfiguratorException(this, "Excluded components are expected to be a list.");
      }

    } else {
      excludedComponents.add(AdvisorGlobalConfiguration.SEND_ALL_COMPONENTS);
    }

    AdvisorGlobalConfiguration advisor = getTargetComponent(configurationContext);
    if (!AdvisorGlobalConfiguration.isValid(true, acceptToS, email, cc)) {
      // In UI, if the fields are invalid, the configuration is not applied, so here we throw an exception
      throw new ConfiguratorException(this,
        "Invalid configuration for CloudBees Jenkins Advisor. Please check the logs and review the content in the yaml file.");
    }
    updateConfiguration(advisor, email, cc, true, nagDisabled, excludedComponents);

    return advisor;
  }

  private void updateConfiguration(AdvisorGlobalConfiguration conf, String email, String cc, boolean acceptToS,
                                   boolean nagDisabled, Set<String> excludedComponents) {
    conf.setEmail(email);
    conf.setCc(cc);
    conf.setAcceptToS(acceptToS);
    conf.setNagDisabled(nagDisabled);
    conf.setExcludedComponents(excludedComponents);
  }

  @Override
  public AdvisorGlobalConfiguration getTargetComponent(ConfigurationContext configurationContext) {
    return getConfiguration();
  }

  @Override
  public Class<AdvisorGlobalConfiguration> getTarget() {
    return AdvisorGlobalConfiguration.class;
  }

  @CheckForNull
  @Override
  public CNode describe(AdvisorGlobalConfiguration instance, ConfigurationContext context) throws Exception {
    Mapping mapping = new Mapping();
    // In UI, email is mandatory. If the email is empty, the Advisor is not configured, so nothing should be exported
    if (StringUtils.isNotBlank(instance.getEmail())) {
      for (Attribute<AdvisorGlobalConfiguration, ?> attribute : describe()) {
        final String attributeName = attribute.getName();
        if (!excludedAttributesInConf.contains(attributeName)) {
          mapping.put(attributeName, attribute.describe(instance, context));
        }
      }

      CNode excluded = mapping.get(EXCLUDED_COMPONENTS_ATTR);
      if (excluded instanceof Sequence) {
        Sequence seq = (Sequence) excluded;
        if (seq.isEmpty()) {
          seq.add(new Scalar(AdvisorGlobalConfiguration.SEND_ALL_COMPONENTS));
        }
      }
    }

    return mapping;
  }

  private AdvisorGlobalConfiguration getConfiguration() {
    AdvisorGlobalConfiguration current = AdvisorGlobalConfiguration.getInstance();
    return current != null ? current : new AdvisorGlobalConfiguration();
  }
}
