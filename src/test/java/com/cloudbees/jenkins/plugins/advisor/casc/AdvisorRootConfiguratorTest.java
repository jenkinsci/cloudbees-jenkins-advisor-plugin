package com.cloudbees.jenkins.plugins.advisor.casc;

import com.cloudbees.jenkins.plugins.advisor.AdvisorGlobalConfiguration;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Sequence;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class AdvisorRootConfiguratorTest {

    private final static String FAKE_EMAIL = "fake@email.com";
    private final static String FAKE_CC = "onemore@mail.com,another@email.com";
    private final static Set<String> EXCLUDED = new HashSet<>(Arrays.asList("ThreadDumps", "PipelineThreadDump"));
    private final static Boolean ACCEPT_TOS = Boolean.TRUE;
    private final static Boolean NAG_DISABLED = Boolean.TRUE;

    private AdvisorRootConfigurator configurator;
    private AdvisorGlobalConfiguration configuration;
    private Mapping mapping;
    private ConfigurationContext context;

    @ClassRule
    public static JenkinsRule rule = new JenkinsRule();
    @Rule
    public ExpectedException thrown= ExpectedException.none();

    @Before
    public void setUpConfigurator() {
        context = new ConfigurationContext(ConfiguratorRegistry.get());
        configurator = new AdvisorRootConfigurator();

        configuration = new AdvisorGlobalConfiguration(FAKE_EMAIL, FAKE_CC, EXCLUDED);
        configuration.setAcceptToS(ACCEPT_TOS);
        configuration.setNagDisabled(NAG_DISABLED);

        mapping = new Mapping();
        mapping.put("acceptToS", ACCEPT_TOS);
        mapping.put("cc", FAKE_CC);
        mapping.put("email", FAKE_EMAIL);
        mapping.put("nagDisabled", NAG_DISABLED);
        Sequence s = new Sequence();
        s.add(new Scalar("ThreadDumps"));
        s.add(new Scalar("PipelineThreadDump"));
        mapping.putIfNotEmpry("excludedComponents", s);
    }

    @Test
    public void testGetName() {
        assertEquals("advisor", configurator.getName());
    }

    @Test
    public void testGetTarget() {
        assertEquals("Wrong target class", configurator.getTarget(), AdvisorGlobalConfiguration.class);
    }

    @Test
    public void testCanConfigure() {
        assertTrue("Can't configure AdvisorGlobalConfiguration", configurator.canConfigure(AdvisorGlobalConfiguration.class));
        assertFalse("Can configure AdvisorRootConfigurator", configurator.canConfigure(AdvisorRootConfigurator.class));
    }

    @Test
    public void testGetImplementedAPI() {
        assertEquals("Wrong implemented API", configurator.getImplementedAPI(), AdvisorGlobalConfiguration.class);
    }

    @Test
    public void testGetConfigurators() {
        assertThat(configurator.getConfigurators(context), contains(configurator));
    }

    @Test
    public void testDescribe() throws Exception {
        Mapping described = configurator.describe(configuration, context).asMapping();
        assertEquals(mapping.getScalarValue("email"), described.getScalarValue("email"));
        assertEquals(mapping.getScalarValue("cc"), described.getScalarValue("cc"));
        assertEquals(mapping.getScalarValue("acceptToS"), described.getScalarValue("acceptToS"));
        assertEquals(mapping.getScalarValue("nagDisabled"), described.getScalarValue("nagDisabled"));
        Set<String> mappingSeq = toScalarValues(mapping.get("excludedComponents").asSequence());
        Set<String> describedSeq = toScalarValues(described.get("excludedComponents").asSequence());
        assertEquals(mappingSeq.size(), describedSeq.size());
        assertThat(describedSeq.toArray(), arrayContainingInAnyOrder(mappingSeq.toArray()));
    }

    @Test
    public void testDescribeWithEmptyEmail() throws Exception {
        final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration("", FAKE_CC, EXCLUDED);
        c.setAcceptToS(ACCEPT_TOS);
        c.setNagDisabled(NAG_DISABLED);

        Mapping described = configurator.describe(c, context).asMapping();
        assertTrue(described.isEmpty());
    }

    @Test
    public void testDescribeWithNullEmail() throws Exception {
        final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration(null, FAKE_CC, EXCLUDED);
        c.setAcceptToS(ACCEPT_TOS);
        c.setNagDisabled(NAG_DISABLED);

        Mapping described = configurator.describe(c, context).asMapping();
        assertTrue(described.isEmpty());
    }

    @Test
    public void testDescribeWithBlankEmail() throws Exception {
        final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration(" ", FAKE_CC, EXCLUDED);
        c.setAcceptToS(ACCEPT_TOS);
        c.setNagDisabled(NAG_DISABLED);

        Mapping described = configurator.describe(c, context).asMapping();
        assertTrue(described.isEmpty());
    }

    @Test
    public void testDescribeWithEmptyCC() throws Exception {
        final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration(FAKE_EMAIL, "", EXCLUDED);
        c.setAcceptToS(ACCEPT_TOS);
        c.setNagDisabled(NAG_DISABLED);

        Mapping described = configurator.describe(c, context).asMapping();
        assertEquals(mapping.getScalarValue("email"), described.getScalarValue("email"));
        assertNull(described.get("cc"));
        assertEquals(mapping.getScalarValue("acceptToS"), described.getScalarValue("acceptToS"));
        assertEquals(mapping.getScalarValue("nagDisabled"), described.getScalarValue("nagDisabled"));
        Set<String> mappingSeq = toScalarValues(mapping.get("excludedComponents").asSequence());
        Set<String> describedSeq = toScalarValues(described.get("excludedComponents").asSequence());
        assertEquals(mappingSeq.size(), describedSeq.size());
        assertThat(describedSeq.toArray(), arrayContainingInAnyOrder(mappingSeq.toArray()));
    }

    @Test
    public void testDescribeWithNullCC() throws Exception {
        final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration(FAKE_EMAIL, null, EXCLUDED);
        c.setAcceptToS(ACCEPT_TOS);
        c.setNagDisabled(NAG_DISABLED);

        Mapping described = configurator.describe(c, context).asMapping();
        assertEquals(mapping.getScalarValue("email"), described.getScalarValue("email"));
        assertNull(described.get("cc"));
        assertEquals(mapping.getScalarValue("acceptToS"), described.getScalarValue("acceptToS"));
        assertEquals(mapping.getScalarValue("nagDisabled"), described.getScalarValue("nagDisabled"));
        Set<String> mappingSeq = toScalarValues(mapping.get("excludedComponents").asSequence());
        Set<String> describedSeq = toScalarValues(described.get("excludedComponents").asSequence());
        assertEquals(mappingSeq.size(), describedSeq.size());
        assertThat(describedSeq.toArray(), arrayContainingInAnyOrder(mappingSeq.toArray()));
    }

    @Test
    public void testDescribeWithBlankCC() throws Exception {
        final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration(FAKE_EMAIL, " ", EXCLUDED);
        c.setAcceptToS(ACCEPT_TOS);
        c.setNagDisabled(NAG_DISABLED);

        Mapping described = configurator.describe(c, context).asMapping();
        assertEquals(mapping.getScalarValue("email"), described.getScalarValue("email"));
        assertNull(described.get("cc"));
        assertEquals(mapping.getScalarValue("acceptToS"), described.getScalarValue("acceptToS"));
        assertEquals(mapping.getScalarValue("nagDisabled"), described.getScalarValue("nagDisabled"));
        Set<String> mappingSeq = toScalarValues(mapping.get("excludedComponents").asSequence());
        Set<String> describedSeq = toScalarValues(described.get("excludedComponents").asSequence());
        assertEquals(mappingSeq.size(), describedSeq.size());
        assertThat(describedSeq.toArray(), arrayContainingInAnyOrder(mappingSeq.toArray()));
    }

    @Test
    public void testDescribeWithNullExcluded() throws Exception {
        final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration(FAKE_EMAIL, FAKE_CC, null);
        c.setAcceptToS(ACCEPT_TOS);
        c.setNagDisabled(NAG_DISABLED);

        Mapping described = configurator.describe(c, context).asMapping();
        assertEquals(mapping.getScalarValue("email"), described.getScalarValue("email"));
        assertEquals(mapping.getScalarValue("cc"), described.getScalarValue("cc"));
        assertEquals(mapping.getScalarValue("acceptToS"), described.getScalarValue("acceptToS"));
        assertEquals(mapping.getScalarValue("nagDisabled"), described.getScalarValue("nagDisabled"));
        Set<String> describedSeq = toScalarValues(described.get("excludedComponents").asSequence());
        assertEquals(1, describedSeq.size());
        assertEquals(AdvisorGlobalConfiguration.SEND_ALL_COMPONENTS, describedSeq.toArray()[0]);
    }

    @Test
    public void testDescribeWithEmptyExcluded() throws Exception {
        final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration(FAKE_EMAIL, FAKE_CC, new HashSet<>());
        c.setAcceptToS(ACCEPT_TOS);
        c.setNagDisabled(NAG_DISABLED);

        Mapping described = configurator.describe(c, context).asMapping();
        assertEquals(mapping.getScalarValue("email"), described.getScalarValue("email"));
        assertEquals(mapping.getScalarValue("cc"), described.getScalarValue("cc"));
        assertEquals(mapping.getScalarValue("acceptToS"), described.getScalarValue("acceptToS"));
        assertEquals(mapping.getScalarValue("nagDisabled"), described.getScalarValue("nagDisabled"));
        Set<String> describedSeq = toScalarValues(described.get("excludedComponents").asSequence());
        assertEquals(1, describedSeq.size());
        assertEquals(AdvisorGlobalConfiguration.SEND_ALL_COMPONENTS, describedSeq.toArray()[0]);
    }

    private Set<String> toScalarValues(Sequence s) throws Exception {
        Set<String> converted = new HashSet<>(s.size());
        for (CNode cNode : s) {
            converted.add(cNode.asScalar().getValue());
        }
        return converted;
    }

    @Test
    public void testInstance() throws Exception {
        AdvisorGlobalConfiguration instance = configurator.instance(mapping, context);
        assertEquals(configuration.getEmail(), instance.getEmail());
        assertEquals(configuration.getCc(), instance.getCc());
        assertEquals(configuration.isAcceptToS(), instance.isAcceptToS());
        assertEquals(configuration.isNagDisabled(), instance.isNagDisabled());
        assertEquals(configuration.getExcludedComponents().size(), instance.getExcludedComponents().size());
        assertThat(instance.getExcludedComponents().toArray(), arrayContainingInAnyOrder(configuration.getExcludedComponents().toArray()));
        assertTrue(instance.isValid());
    }

    @Test
    public void testInstanceSENDALL() throws Exception {
        final Mapping mappingWithDefault = new Mapping();
        mappingWithDefault.put("cc", FAKE_CC);
        mappingWithDefault.put("email", FAKE_EMAIL);
        mappingWithDefault.put("acceptToS", ACCEPT_TOS);

        AdvisorGlobalConfiguration instance = configurator.instance(mappingWithDefault, context);
        assertTrue(instance.isAcceptToS());
        assertFalse(instance.isNagDisabled());
        assertEquals(1, instance.getExcludedComponents().size());
        assertEquals(AdvisorGlobalConfiguration.SEND_ALL_COMPONENTS, instance.getExcludedComponents().toArray()[0]);
        assertTrue(instance.isValid());
    }

    @Test
    public void testInstanceWithOutToS() throws Exception {
        thrown.expect(ConfiguratorException.class);
        final Mapping mappingWithDefault = new Mapping();
        mappingWithDefault.put("cc", FAKE_CC);
        mappingWithDefault.put("email", FAKE_EMAIL);

        configurator.instance(mappingWithDefault, context);
    }

    @Test
    public void testInstanceNotAcceptedToS() throws Exception {
        thrown.expect(ConfiguratorException.class);
        final Mapping mappingNotAcceptingToS = new Mapping();
        mappingNotAcceptingToS.put("acceptToS", false);
        mappingNotAcceptingToS.put("cc", FAKE_CC);
        mappingNotAcceptingToS.put("email", FAKE_EMAIL);
        mappingNotAcceptingToS.put("nagDisabled", NAG_DISABLED);

        configurator.instance(mappingNotAcceptingToS, context);
    }

    @Test
    public void testInstanceEmptyEmail() throws Exception {
        thrown.expect(ConfiguratorException.class);
        final Mapping m = new Mapping();
        m.put("acceptToS", ACCEPT_TOS);
        m.put("cc", FAKE_CC);
        m.put("email", "");
        m.put("nagDisabled", NAG_DISABLED);

        configurator.instance(m, context);
    }

    @Test
    public void testInstanceBadEmail() throws Exception {
        thrown.expect(ConfiguratorException.class);
        final Mapping m = new Mapping();
        m.put("acceptToS", ACCEPT_TOS);
        m.put("cc", FAKE_CC);
        m.put("email", "bad_email");
        m.put("nagDisabled", NAG_DISABLED);

        configurator.instance(m, context);
    }

    @Test
    public void testInstanceEmptyCC() throws Exception {
        thrown.expect(ConfiguratorException.class);
        final Mapping m = new Mapping();
        m.put("acceptToS", false);
        m.put("cc", "");
        m.put("email", FAKE_EMAIL);
        m.put("nagDisabled", NAG_DISABLED);

        configurator.instance(m, context);
    }

    @Test
    public void testInstanceBadCC() throws Exception {
        thrown.expect(ConfiguratorException.class);
        final Mapping m = new Mapping();
        m.put("acceptToS", false);
        m.put("cc", "bad_cc");
        m.put("email", FAKE_EMAIL);
        m.put("nagDisabled", NAG_DISABLED);

        configurator.instance(m, context);
    }
}
