package com.cloudbees.jenkins.plugins.advisor.casc;

import com.cloudbees.jenkins.plugins.advisor.AdvisorGlobalConfiguration;
import com.cloudbees.jenkins.plugins.advisor.client.model.Recipient;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

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
    static final String CCS_ATTR = "ccs";
    static final String EXCLUDED_COMPONENTS_ATTR = "excludedComponents";
    static final String NAG_DISABLED_ATTR = "nagDisabled";

    private static final Logger LOG = Logger.getLogger(AdvisorRootConfigurator.class.getName());

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
        final boolean nagDisabled = (mapping.get(NAG_DISABLED_ATTR) != null
                && BooleanUtils.toBoolean(mapping.getScalarValue(NAG_DISABLED_ATTR)));
        final boolean acceptToS = (mapping.get(ACCEPT_TOS_ATTR) != null
                && BooleanUtils.toBoolean(mapping.getScalarValue(ACCEPT_TOS_ATTR)));

        // List values
        final List<Recipient> cc = new ArrayList<>();
        CNode ccCN = mapping.get(CCS_ATTR);
        if (ccCN != null) {
            if (ccCN instanceof Sequence) {
                Sequence s = (Sequence) ccCN;
                for (CNode cNode : s) {
                    cc.add(new Recipient(cNode.asScalar().getValue()));
                }
                // We don't want to process it anymore because the mapping in YAML
                // doesn't map the objects model (List<String> vs List<Recipient>
                mapping.remove(CCS_ATTR);
            } else {
                throw new ConfiguratorException(this, CCS_ATTR + " is expected to be a list.");
            }
        }

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
                throw new ConfiguratorException(this, EXCLUDED_COMPONENTS_ATTR + " is expected to be a list.");
            }

        } else {
            excludedComponents.add(AdvisorGlobalConfiguration.SEND_ALL_COMPONENTS);
        }

        AdvisorGlobalConfiguration advisor = getTargetComponent(configurationContext);
        if (!AdvisorGlobalConfiguration.isValid(true, acceptToS, email, cc)) {
            // In UI, if the fields are invalid, the configuration is not applied, so here we throw an exception
            throw new ConfiguratorException(
                    this,
                    "Invalid configuration for CloudBees Jenkins Advisor. Please check the logs and review the content in the yaml file.");
        }
        updateConfiguration(advisor, email, cc, true, nagDisabled, excludedComponents);

        return advisor;
    }

    private void updateConfiguration(
            AdvisorGlobalConfiguration conf,
            String email,
            List<Recipient> ccs,
            boolean acceptToS,
            boolean nagDisabled,
            Set<String> excludedComponents) {
        conf.setEmail(email);
        conf.setCcs(ccs);
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
        // If the UI config is invalid nothing should be exported
        if (instance.isValid()) {

            // Manually generate the YAML to ensure a longer term compatibility
            // by keeping the control on the mapping in both sides

            for (Attribute<AdvisorGlobalConfiguration, ?> attribute : describe()) {
                switch (attribute.getName()) {
                    case ACCEPT_TOS_ATTR:
                        mapping.put(ACCEPT_TOS_ATTR, attribute.describe(instance, context));
                        break;
                    case EMAIL_ATTR:
                        mapping.put(EMAIL_ATTR, attribute.describe(instance, context));
                        break;
                    case CCS_ATTR:
                        // We build it manually because we don't want to expose the Bean model
                        Sequence ccs = new Sequence();
                        instance.getCcs().stream()
                                .map(Recipient::getEmail)
                                .map(Scalar::new)
                                .forEach(ccs::add);
                        if (!ccs.isEmpty()) {
                            mapping.put(CCS_ATTR, ccs);
                        }
                        break;
                    case EXCLUDED_COMPONENTS_ATTR:
                        CNode excludedCNode = attribute.describe(instance, context);
                        if (excludedCNode instanceof Sequence) {
                            Sequence seq = (Sequence) excludedCNode;
                            if (seq.isEmpty()) {
                                seq.add(new Scalar(AdvisorGlobalConfiguration.SEND_ALL_COMPONENTS));
                            }
                        }
                        mapping.put(EXCLUDED_COMPONENTS_ATTR, excludedCNode);
                        break;
                    case NAG_DISABLED_ATTR:
                        mapping.put(NAG_DISABLED_ATTR, attribute.describe(instance, context));
                        break;
                    default:
                        LOG.fine("Unknown attribute:" + attribute.getName());
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
