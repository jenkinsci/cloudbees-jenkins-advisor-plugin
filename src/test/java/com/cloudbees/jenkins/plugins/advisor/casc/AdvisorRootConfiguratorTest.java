package com.cloudbees.jenkins.plugins.advisor.casc;

import static com.cloudbees.jenkins.plugins.advisor.casc.AdvisorRootConfigurator.ACCEPT_TOS_ATTR;
import static com.cloudbees.jenkins.plugins.advisor.casc.AdvisorRootConfigurator.CCS_ATTR;
import static com.cloudbees.jenkins.plugins.advisor.casc.AdvisorRootConfigurator.EMAIL_ATTR;
import static com.cloudbees.jenkins.plugins.advisor.casc.AdvisorRootConfigurator.EXCLUDED_COMPONENTS_ATTR;
import static com.cloudbees.jenkins.plugins.advisor.casc.AdvisorRootConfigurator.NAG_DISABLED_ATTR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.jenkins.plugins.advisor.AdvisorGlobalConfiguration;
import com.cloudbees.jenkins.plugins.advisor.client.model.Recipient;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Sequence;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class AdvisorRootConfiguratorTest {

    private static final String FAKE_EMAIL = "fake@email.com";
    private static final List<Recipient> FAKE_CC =
            Arrays.asList(new Recipient("onemore@mail.com"), new Recipient("another@email.com"));
    private static final Set<String> EXCLUDED = new HashSet<>(Arrays.asList("ThreadDumps", "PipelineThreadDump"));
    private static final Boolean ACCEPT_TOS = Boolean.TRUE;
    private static final Boolean NAG_DISABLED = Boolean.TRUE;

    private static JenkinsRule rule;

    private AdvisorRootConfigurator configurator;
    private AdvisorGlobalConfiguration configuration;
    private Mapping mapping;
    private ConfigurationContext context;

    @BeforeAll
    static void setUp(JenkinsRule r) {
        rule = r;
    }

    @BeforeEach
    void setUpConfigurator() {
        context = new ConfigurationContext(ConfiguratorRegistry.get());
        configurator = new AdvisorRootConfigurator();

        configuration = new AdvisorGlobalConfiguration(FAKE_EMAIL, FAKE_CC, EXCLUDED);
        configuration.setAcceptToS(ACCEPT_TOS);
        configuration.setNagDisabled(NAG_DISABLED);

        mapping = new Mapping();
        mapping.put(ACCEPT_TOS_ATTR, ACCEPT_TOS);
        Sequence ccs = new Sequence();
        ccs.add(new Scalar("onemore@mail.com"));
        ccs.add(new Scalar("another@email.com"));
        mapping.put(CCS_ATTR, ccs);
        mapping.put(EMAIL_ATTR, FAKE_EMAIL);
        Sequence excluded = new Sequence();
        excluded.add(new Scalar("ThreadDumps"));
        excluded.add(new Scalar("PipelineThreadDump"));
        mapping.put(EXCLUDED_COMPONENTS_ATTR, excluded);
        mapping.put(NAG_DISABLED_ATTR, NAG_DISABLED);
    }

    @Test
    void testGetName() {
        assertEquals("advisor", configurator.getName());
    }

    @Test
    void testGetTarget() {
        assertEquals(AdvisorGlobalConfiguration.class, configurator.getTarget(), "Wrong target class");
    }

    @Test
    void testCanConfigure() {
        assertTrue(
                configurator.canConfigure(AdvisorGlobalConfiguration.class),
                "Can't configure AdvisorGlobalConfiguration");
        assertFalse(configurator.canConfigure(AdvisorRootConfigurator.class), "Can configure AdvisorRootConfigurator");
    }

    @Test
    void testGetImplementedAPI() {
        assertEquals(AdvisorGlobalConfiguration.class, configurator.getImplementedAPI(), "Wrong implemented API");
    }

    @Test
    void testGetConfigurators() {
        assertThat(configurator.getConfigurators(context), contains(configurator));
    }

    @Test
    void testDescribe() throws Exception {
        Mapping described = configurator.describe(configuration, context).asMapping();
        assertNotNull(described);
        assertEquals(mapping.getScalarValue(EMAIL_ATTR), described.getScalarValue(EMAIL_ATTR));
        List<String> ccMapping = toListValues(mapping.get(CCS_ATTR).asSequence());
        List<String> ccDescribed = toListValues(described.get(CCS_ATTR).asSequence());
        assertEquals(ccMapping.size(), ccDescribed.size());
        assertThat(ccDescribed.toArray(), arrayContainingInAnyOrder(ccMapping.toArray()));
        assertEquals(mapping.getScalarValue(ACCEPT_TOS_ATTR), described.getScalarValue(ACCEPT_TOS_ATTR));
        assertEquals(mapping.getScalarValue(NAG_DISABLED_ATTR), described.getScalarValue(NAG_DISABLED_ATTR));
        Set<String> excludedComponentsMapping =
                toScalarValues(mapping.get(EXCLUDED_COMPONENTS_ATTR).asSequence());
        Set<String> excludedComponentsDescribed =
                toScalarValues(described.get(EXCLUDED_COMPONENTS_ATTR).asSequence());
        assertEquals(excludedComponentsMapping.size(), excludedComponentsDescribed.size());
        assertThat(
                excludedComponentsDescribed.toArray(), arrayContainingInAnyOrder(excludedComponentsMapping.toArray()));
    }

    @Test
    void testDescribeWithEmptyEmail() throws Exception {
        final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration("", FAKE_CC, EXCLUDED);
        c.setAcceptToS(ACCEPT_TOS);
        c.setNagDisabled(NAG_DISABLED);

        Mapping described = configurator.describe(c, context).asMapping();
        assertNotNull(described);
        assertTrue(described.isEmpty());
    }

    @Test
    void testDescribeWithNullEmail() throws Exception {
        final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration(null, FAKE_CC, EXCLUDED);
        c.setAcceptToS(ACCEPT_TOS);
        c.setNagDisabled(NAG_DISABLED);

        Mapping described = configurator.describe(c, context).asMapping();
        assertNotNull(described);
        assertTrue(described.isEmpty());
    }

    @Test
    void testDescribeWithBlankEmail() throws Exception {
        final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration(" ", FAKE_CC, EXCLUDED);
        c.setAcceptToS(ACCEPT_TOS);
        c.setNagDisabled(NAG_DISABLED);

        Mapping described = configurator.describe(c, context).asMapping();
        assertNotNull(described);
        assertTrue(described.isEmpty());
    }

    @Test
    void testDescribeWithVarialbeValue() throws Exception {
        List<Recipient> cc_with_var = Arrays.asList(new Recipient("${admin_cc}"), new Recipient("${admin_cc}"));
        final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration("${admin_email}", cc_with_var, EXCLUDED);
        c.setAcceptToS(ACCEPT_TOS);
        c.setNagDisabled(NAG_DISABLED);

        Mapping described = configurator.describe(c, context).asMapping();
        assertNotNull(described);
        String email = described.getScalarValue(EMAIL_ATTR);

        assertEquals("^${admin_email}", email);
        assertTrue(
                toListValues(described.get(CCS_ATTR).asSequence()).stream().anyMatch(cc -> cc.equals("^${admin_cc}")),
                "encoded email cc not found in list");
    }

    @Test
    @SetEnvironmentVariable(key = "admin_email", value = "mike@is.cool.com")
    void testResolveWithVariableName() {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        context = new ConfigurationContext(registry);
        assertThat(context.getSecretSourceResolver().resolve("${admin_email}"), equalTo("mike@is.cool.com"));
    }

    @Test
    void testDescribeWithEmptyCC() throws Exception {
        final AdvisorGlobalConfiguration c =
                new AdvisorGlobalConfiguration(FAKE_EMAIL, Collections.emptyList(), EXCLUDED);
        c.setAcceptToS(ACCEPT_TOS);
        c.setNagDisabled(NAG_DISABLED);

        Mapping described = configurator.describe(c, context).asMapping();
        assertNotNull(described);
        assertEquals(mapping.getScalarValue(EMAIL_ATTR), described.getScalarValue(EMAIL_ATTR));
        assertFalse(described.containsKey(CCS_ATTR));
        assertEquals(mapping.getScalarValue(ACCEPT_TOS_ATTR), described.getScalarValue(ACCEPT_TOS_ATTR));
        assertEquals(mapping.getScalarValue(NAG_DISABLED_ATTR), described.getScalarValue(NAG_DISABLED_ATTR));
        Set<String> excludedComponentsMapping =
                toScalarValues(mapping.get(EXCLUDED_COMPONENTS_ATTR).asSequence());
        Set<String> excludedComponentsDescribed =
                toScalarValues(described.get(EXCLUDED_COMPONENTS_ATTR).asSequence());
        assertEquals(excludedComponentsMapping.size(), excludedComponentsDescribed.size());
        assertThat(
                excludedComponentsDescribed.toArray(), arrayContainingInAnyOrder(excludedComponentsMapping.toArray()));
    }

    @Test
    void testDescribeWithNullCC() throws Exception {
        final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration(FAKE_EMAIL, null, EXCLUDED);
        c.setAcceptToS(ACCEPT_TOS);
        c.setNagDisabled(NAG_DISABLED);

        Mapping described = configurator.describe(c, context).asMapping();
        assertNotNull(described);
        assertEquals(mapping.getScalarValue(EMAIL_ATTR), described.getScalarValue(EMAIL_ATTR));
        assertFalse(described.containsKey(CCS_ATTR));
        assertEquals(mapping.getScalarValue(ACCEPT_TOS_ATTR), described.getScalarValue(ACCEPT_TOS_ATTR));
        assertEquals(mapping.getScalarValue(NAG_DISABLED_ATTR), described.getScalarValue(NAG_DISABLED_ATTR));
        Set<String> excludedComponentsMapping =
                toScalarValues(mapping.get(EXCLUDED_COMPONENTS_ATTR).asSequence());
        Set<String> excludedComponentsDescribed =
                toScalarValues(described.get(EXCLUDED_COMPONENTS_ATTR).asSequence());
        assertEquals(excludedComponentsMapping.size(), excludedComponentsDescribed.size());
        assertThat(
                excludedComponentsDescribed.toArray(), arrayContainingInAnyOrder(excludedComponentsMapping.toArray()));
    }

    @Test
    void testDescribeWithNullExcluded() throws Exception {
        final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration(FAKE_EMAIL, FAKE_CC, null);
        c.setAcceptToS(ACCEPT_TOS);
        c.setNagDisabled(NAG_DISABLED);

        Mapping described = configurator.describe(c, context).asMapping();
        assertNotNull(described);
        assertEquals(mapping.getScalarValue(EMAIL_ATTR), described.getScalarValue(EMAIL_ATTR));
        List<String> ccMapping = toListValues(mapping.get(CCS_ATTR).asSequence());
        List<String> ccDescribed = toListValues(described.get(CCS_ATTR).asSequence());
        assertEquals(ccMapping.size(), ccDescribed.size());
        assertThat(ccDescribed.toArray(), arrayContainingInAnyOrder(ccMapping.toArray()));
        assertEquals(mapping.getScalarValue(ACCEPT_TOS_ATTR), described.getScalarValue(ACCEPT_TOS_ATTR));
        assertEquals(mapping.getScalarValue(NAG_DISABLED_ATTR), described.getScalarValue(NAG_DISABLED_ATTR));
        Set<String> excludedComponentsDescribed =
                toScalarValues(described.get(EXCLUDED_COMPONENTS_ATTR).asSequence());
        assertEquals(1, excludedComponentsDescribed.size());
        assertEquals(
                AdvisorGlobalConfiguration.SEND_ALL_COMPONENTS,
                excludedComponentsDescribed.toArray()[0]);
    }

    @Test
    void testDescribeWithEmptyExcluded() throws Exception {
        final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration(FAKE_EMAIL, FAKE_CC, new HashSet<>());
        c.setAcceptToS(ACCEPT_TOS);
        c.setNagDisabled(NAG_DISABLED);

        Mapping described = configurator.describe(c, context).asMapping();
        assertNotNull(described);
        assertEquals(mapping.getScalarValue(EMAIL_ATTR), described.getScalarValue(EMAIL_ATTR));
        List<String> ccMapping = toListValues(mapping.get(CCS_ATTR).asSequence());
        List<String> ccDescribed = toListValues(described.get(CCS_ATTR).asSequence());
        assertEquals(ccMapping.size(), ccDescribed.size());
        assertThat(ccDescribed.toArray(), arrayContainingInAnyOrder(ccMapping.toArray()));
        assertEquals(mapping.getScalarValue(ACCEPT_TOS_ATTR), described.getScalarValue(ACCEPT_TOS_ATTR));
        assertEquals(mapping.getScalarValue(NAG_DISABLED_ATTR), described.getScalarValue(NAG_DISABLED_ATTR));
        Set<String> excludedComponentsDescribed =
                toScalarValues(described.get(EXCLUDED_COMPONENTS_ATTR).asSequence());
        assertEquals(1, excludedComponentsDescribed.size());
        assertEquals(
                AdvisorGlobalConfiguration.SEND_ALL_COMPONENTS,
                excludedComponentsDescribed.toArray()[0]);
    }

    private static Set<String> toScalarValues(Sequence s) {
        Set<String> converted = new HashSet<>(s.size());
        for (CNode cNode : s) {
            converted.add(cNode.asScalar().getValue());
        }
        return converted;
    }

    private static List<String> toListValues(Sequence s) {
        List<String> converted = new ArrayList<>(s.size());
        for (CNode cNode : s) {
            converted.add(cNode.asScalar().getValue());
        }
        return converted;
    }

    @Test
    void testInstance() {
        AdvisorGlobalConfiguration instance = configurator.instance(mapping, context);
        assertNotNull(instance);
        assertEquals(configuration.getEmail(), instance.getEmail());
        assertEquals(configuration.getCcs().size(), instance.getCcs().size());
        assertThat(
                instance.getCcs().stream().map(Recipient::getEmail).toArray(),
                arrayContainingInAnyOrder(
                        configuration.getCcs().stream().map(Recipient::getEmail).toArray()));
        assertEquals(configuration.isAcceptToS(), instance.isAcceptToS());
        assertEquals(configuration.isNagDisabled(), instance.isNagDisabled());
        assertEquals(
                configuration.getExcludedComponents().size(),
                instance.getExcludedComponents().size());
        assertThat(
                instance.getExcludedComponents().toArray(),
                arrayContainingInAnyOrder(configuration.getExcludedComponents().toArray()));
        assertTrue(instance.isValid());
    }

    @Test
    void testInstanceSENDALL() {
        final Mapping mappingWithDefault = new Mapping();
        mappingWithDefault.put(ACCEPT_TOS_ATTR, ACCEPT_TOS);
        mappingWithDefault.put(EMAIL_ATTR, FAKE_EMAIL);
        Sequence cc = new Sequence();
        cc.add(new Scalar("onemore@mail.com"));
        cc.add(new Scalar("another@email.com"));
        mappingWithDefault.put(CCS_ATTR, cc);

        AdvisorGlobalConfiguration instance = configurator.instance(mappingWithDefault, context);
        assertNotNull(instance);
        assertTrue(instance.isAcceptToS());
        assertFalse(instance.isNagDisabled());
        assertEquals(1, instance.getExcludedComponents().size());
        assertEquals(
                AdvisorGlobalConfiguration.SEND_ALL_COMPONENTS,
                instance.getExcludedComponents().toArray()[0]);
        assertTrue(instance.isValid());
    }

    @Test
    void testInstanceWithOutToS() {
        assertThrows(ConfiguratorException.class, () -> {
            final Mapping mappingWithDefault = new Mapping();
            mappingWithDefault.put(EMAIL_ATTR, FAKE_EMAIL);
            Sequence cc = new Sequence();
            cc.add(new Scalar("onemore@mail.com"));
            cc.add(new Scalar("another@email.com"));
            mappingWithDefault.put(CCS_ATTR, cc);

            configurator.instance(mappingWithDefault, context);
        });
    }

    @Test
    void testInstanceNotAcceptedToS() {
        assertThrows(ConfiguratorException.class, () -> {
            final Mapping mappingNotAcceptingToS = new Mapping();
            mappingNotAcceptingToS.put(ACCEPT_TOS_ATTR, false);
            mappingNotAcceptingToS.put(EMAIL_ATTR, FAKE_EMAIL);
            Sequence cc = new Sequence();
            cc.add(new Scalar("onemore@mail.com"));
            cc.add(new Scalar("another@email.com"));
            mappingNotAcceptingToS.put(CCS_ATTR, cc);
            mappingNotAcceptingToS.put(NAG_DISABLED_ATTR, NAG_DISABLED);

            configurator.instance(mappingNotAcceptingToS, context);
        });
    }

    @Test
    void testInstanceEmptyEmail() {
        assertThrows(ConfiguratorException.class, () -> {
            final Mapping m = new Mapping();
            m.put(ACCEPT_TOS_ATTR, ACCEPT_TOS);
            m.put(EMAIL_ATTR, "");
            Sequence cc = new Sequence();
            cc.add(new Scalar("onemore@mail.com"));
            cc.add(new Scalar("another@email.com"));
            m.put(CCS_ATTR, cc);
            m.put(NAG_DISABLED_ATTR, NAG_DISABLED);

            configurator.instance(m, context);
        });
    }

    @Test
    void testInstanceBadEmail() {
        assertThrows(ConfiguratorException.class, () -> {
            final Mapping m = new Mapping();
            m.put(ACCEPT_TOS_ATTR, ACCEPT_TOS);
            Sequence cc = new Sequence();
            cc.add(new Scalar("onemore@mail.com"));
            cc.add(new Scalar("another@email.com"));
            m.put(CCS_ATTR, cc);
            m.put(EMAIL_ATTR, "");
            m.put(NAG_DISABLED_ATTR, NAG_DISABLED);

            configurator.instance(m, context);
        });
    }

    @Test
    void testInstanceEmptyCC() {
        assertThrows(ConfiguratorException.class, () -> {
            final Mapping m = new Mapping();
            m.put(ACCEPT_TOS_ATTR, false);
            m.put(EMAIL_ATTR, FAKE_EMAIL);
            m.put(NAG_DISABLED_ATTR, NAG_DISABLED);

            configurator.instance(m, context);
        });
    }

    @Test
    void testInstanceBadCC() {
        assertThrows(ConfiguratorException.class, () -> {
            final Mapping m = new Mapping();
            m.put(ACCEPT_TOS_ATTR, false);
            Sequence cc = new Sequence();
            cc.add(new Scalar("bad_cc"));
            m.put(CCS_ATTR, cc);
            m.put(EMAIL_ATTR, FAKE_EMAIL);
            m.put(NAG_DISABLED_ATTR, NAG_DISABLED);

            configurator.instance(m, context);
        });
    }
}
