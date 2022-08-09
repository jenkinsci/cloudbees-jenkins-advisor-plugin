package com.cloudbees.jenkins.plugins.advisor.casc;

import com.cloudbees.jenkins.plugins.advisor.AdvisorGlobalConfiguration;
import com.cloudbees.jenkins.plugins.advisor.client.model.Recipient;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.cloudbees.jenkins.plugins.advisor.casc.AdvisorRootConfigurator.ACCEPT_TOS_ATTR;
import static com.cloudbees.jenkins.plugins.advisor.casc.AdvisorRootConfigurator.CCS_ATTR;
import static com.cloudbees.jenkins.plugins.advisor.casc.AdvisorRootConfigurator.EMAIL_ATTR;
import static com.cloudbees.jenkins.plugins.advisor.casc.AdvisorRootConfigurator.EXCLUDED_COMPONENTS_ATTR;
import static com.cloudbees.jenkins.plugins.advisor.casc.AdvisorRootConfigurator.NAG_DISABLED_ATTR;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class AdvisorRootConfiguratorTest {

  private final static String FAKE_EMAIL = "fake@email.com";
  private final static List<Recipient> FAKE_CC =
    Arrays.asList(new Recipient("onemore@mail.com"), new Recipient("another@email.com"));
  private final static Set<String> EXCLUDED = new HashSet<>(Arrays.asList("ThreadDumps", "PipelineThreadDump"));
  private final static Boolean ACCEPT_TOS = Boolean.TRUE;
  private final static Boolean NAG_DISABLED = Boolean.TRUE;
  @ClassRule
  public static JenkinsRule rule = new JenkinsRule();
  @Rule
  public final ExpectedException thrown = ExpectedException.none();
  private AdvisorRootConfigurator configurator;
  private AdvisorGlobalConfiguration configuration;
  private Mapping mapping;
  private ConfigurationContext context;

  @Before
  public void setUpConfigurator() {
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
  public void testGetName() {
    assertEquals("advisor", configurator.getName());
  }

  @Test
  public void testGetTarget() {
    assertEquals("Wrong target class", configurator.getTarget(), AdvisorGlobalConfiguration.class);
  }

  @Test
  public void testCanConfigure() {
    assertTrue("Can't configure AdvisorGlobalConfiguration",
      configurator.canConfigure(AdvisorGlobalConfiguration.class));
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
    assertNotNull(described);
    assertEquals(mapping.getScalarValue(EMAIL_ATTR), described.getScalarValue(EMAIL_ATTR));
    List<String> ccMapping = toListValues(mapping.get(CCS_ATTR).asSequence());
    List<String> ccDescribed = toListValues(described.get(CCS_ATTR).asSequence());
    assertEquals(ccMapping.size(), ccDescribed.size());
    assertThat(ccDescribed.toArray(), arrayContainingInAnyOrder(ccMapping.toArray()));
    assertEquals(mapping.getScalarValue(ACCEPT_TOS_ATTR), described.getScalarValue(ACCEPT_TOS_ATTR));
    assertEquals(mapping.getScalarValue(NAG_DISABLED_ATTR), described.getScalarValue(NAG_DISABLED_ATTR));
    Set<String> excludedComponentsMapping = toScalarValues(mapping.get(EXCLUDED_COMPONENTS_ATTR).asSequence());
    Set<String> excludedComponentsDescribed = toScalarValues(described.get(EXCLUDED_COMPONENTS_ATTR).asSequence());
    assertEquals(excludedComponentsMapping.size(), excludedComponentsDescribed.size());
    assertThat(excludedComponentsDescribed.toArray(), arrayContainingInAnyOrder(excludedComponentsMapping.toArray()));
  }

  @Test
  public void testDescribeWithEmptyEmail() throws Exception {
    final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration("", FAKE_CC, EXCLUDED);
    c.setAcceptToS(ACCEPT_TOS);
    c.setNagDisabled(NAG_DISABLED);

    Mapping described = configurator.describe(c, context).asMapping();
    assertNotNull(described);
    assertTrue(described.isEmpty());
  }

  @Test
  public void testDescribeWithNullEmail() throws Exception {
    final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration(null, FAKE_CC, EXCLUDED);
    c.setAcceptToS(ACCEPT_TOS);
    c.setNagDisabled(NAG_DISABLED);

    Mapping described = configurator.describe(c, context).asMapping();
    assertNotNull(described);
    assertTrue(described.isEmpty());
  }

  @Test
  public void testDescribeWithBlankEmail() throws Exception {
    final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration(" ", FAKE_CC, EXCLUDED);
    c.setAcceptToS(ACCEPT_TOS);
    c.setNagDisabled(NAG_DISABLED);

    Mapping described = configurator.describe(c, context).asMapping();
    assertNotNull(described);
    assertTrue(described.isEmpty());
  }

  @Test
  public void testDescribeWithEmptyCC() throws Exception {
    final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration(FAKE_EMAIL, Collections.emptyList(), EXCLUDED);
    c.setAcceptToS(ACCEPT_TOS);
    c.setNagDisabled(NAG_DISABLED);

    Mapping described = configurator.describe(c, context).asMapping();
    assertNotNull(described);
    assertEquals(mapping.getScalarValue(EMAIL_ATTR), described.getScalarValue(EMAIL_ATTR));
    assertFalse(described.containsKey(CCS_ATTR));
    assertEquals(mapping.getScalarValue(ACCEPT_TOS_ATTR), described.getScalarValue(ACCEPT_TOS_ATTR));
    assertEquals(mapping.getScalarValue(NAG_DISABLED_ATTR), described.getScalarValue(NAG_DISABLED_ATTR));
    Set<String> excludedComponentsMapping = toScalarValues(mapping.get(EXCLUDED_COMPONENTS_ATTR).asSequence());
    Set<String> excludedComponentsDescribed = toScalarValues(described.get(EXCLUDED_COMPONENTS_ATTR).asSequence());
    assertEquals(excludedComponentsMapping.size(), excludedComponentsDescribed.size());
    assertThat(excludedComponentsDescribed.toArray(), arrayContainingInAnyOrder(excludedComponentsMapping.toArray()));
  }

  @Test
  public void testDescribeWithNullCC() throws Exception {
    final AdvisorGlobalConfiguration c = new AdvisorGlobalConfiguration(FAKE_EMAIL, null, EXCLUDED);
    c.setAcceptToS(ACCEPT_TOS);
    c.setNagDisabled(NAG_DISABLED);

    Mapping described = configurator.describe(c, context).asMapping();
    assertNotNull(described);
    assertEquals(mapping.getScalarValue(EMAIL_ATTR), described.getScalarValue(EMAIL_ATTR));
    assertFalse(described.containsKey(CCS_ATTR));
    assertEquals(mapping.getScalarValue(ACCEPT_TOS_ATTR), described.getScalarValue(ACCEPT_TOS_ATTR));
    assertEquals(mapping.getScalarValue(NAG_DISABLED_ATTR), described.getScalarValue(NAG_DISABLED_ATTR));
    Set<String> excludedComponentsMapping = toScalarValues(mapping.get(EXCLUDED_COMPONENTS_ATTR).asSequence());
    Set<String> excludedComponentsDescribed = toScalarValues(described.get(EXCLUDED_COMPONENTS_ATTR).asSequence());
    assertEquals(excludedComponentsMapping.size(), excludedComponentsDescribed.size());
    assertThat(excludedComponentsDescribed.toArray(), arrayContainingInAnyOrder(excludedComponentsMapping.toArray()));
  }

  @Test
  public void testDescribeWithNullExcluded() throws Exception {
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
    Set<String> excludedComponentsDescribed = toScalarValues(described.get(EXCLUDED_COMPONENTS_ATTR).asSequence());
    assertEquals(1, excludedComponentsDescribed.size());
    assertEquals(AdvisorGlobalConfiguration.SEND_ALL_COMPONENTS, excludedComponentsDescribed.toArray()[0]);
  }

  @Test
  public void testDescribeWithEmptyExcluded() throws Exception {
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
    Set<String> excludedComponentsDescribed = toScalarValues(described.get(EXCLUDED_COMPONENTS_ATTR).asSequence());
    assertEquals(1, excludedComponentsDescribed.size());
    assertEquals(AdvisorGlobalConfiguration.SEND_ALL_COMPONENTS, excludedComponentsDescribed.toArray()[0]);
  }

  private Set<String> toScalarValues(Sequence s) throws Exception {
    Set<String> converted = new HashSet<>(s.size());
    for (CNode cNode : s) {
      converted.add(cNode.asScalar().getValue());
    }
    return converted;
  }

  private List<String> toListValues(Sequence s) throws Exception {
    List<String> converted = new ArrayList<>(s.size());
    for (CNode cNode : s) {
      converted.add(cNode.asScalar().getValue());
    }
    return converted;
  }
  
  @Test
  public void testInstance() throws Exception {
    AdvisorGlobalConfiguration instance = configurator.instance(mapping, context);
    assertNotNull(instance);
    assertEquals(configuration.getEmail(), instance.getEmail());
    assertEquals(configuration.getCcs().size(), instance.getCcs().size());
    assertThat(instance.getCcs().stream().map(Recipient::getEmail).toArray(),
      arrayContainingInAnyOrder(configuration.getCcs().stream().map(Recipient::getEmail).toArray()));
    assertEquals(configuration.isAcceptToS(), instance.isAcceptToS());
    assertEquals(configuration.isNagDisabled(), instance.isNagDisabled());
    assertEquals(configuration.getExcludedComponents().size(), instance.getExcludedComponents().size());
    assertThat(instance.getExcludedComponents().toArray(),
      arrayContainingInAnyOrder(configuration.getExcludedComponents().toArray()));
    assertTrue(instance.isValid());
  }

  @Test
  public void testInstanceSENDALL() throws Exception {
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
    assertEquals(AdvisorGlobalConfiguration.SEND_ALL_COMPONENTS, instance.getExcludedComponents().toArray()[0]);
    assertTrue(instance.isValid());
  }

  @Test
  public void testInstanceWithOutToS() throws Exception {
    thrown.expect(ConfiguratorException.class);
    final Mapping mappingWithDefault = new Mapping();
    mappingWithDefault.put(EMAIL_ATTR, FAKE_EMAIL);
    Sequence cc = new Sequence();
    cc.add(new Scalar("onemore@mail.com"));
    cc.add(new Scalar("another@email.com"));
    mappingWithDefault.put(CCS_ATTR, cc);

    configurator.instance(mappingWithDefault, context);
  }

  @Test
  public void testInstanceNotAcceptedToS() throws Exception {
    thrown.expect(ConfiguratorException.class);
    final Mapping mappingNotAcceptingToS = new Mapping();
    mappingNotAcceptingToS.put(ACCEPT_TOS_ATTR, false);
    mappingNotAcceptingToS.put(EMAIL_ATTR, FAKE_EMAIL);
    Sequence cc = new Sequence();
    cc.add(new Scalar("onemore@mail.com"));
    cc.add(new Scalar("another@email.com"));
    mappingNotAcceptingToS.put(CCS_ATTR, cc);
    mappingNotAcceptingToS.put(NAG_DISABLED_ATTR, NAG_DISABLED);

    configurator.instance(mappingNotAcceptingToS, context);
  }

  @Test
  public void testInstanceEmptyEmail() throws Exception {
    thrown.expect(ConfiguratorException.class);
    final Mapping m = new Mapping();
    m.put(ACCEPT_TOS_ATTR, ACCEPT_TOS);
    m.put(EMAIL_ATTR, "");
    Sequence cc = new Sequence();
    cc.add(new Scalar("onemore@mail.com"));
    cc.add(new Scalar("another@email.com"));
    m.put(CCS_ATTR, cc);
    m.put(NAG_DISABLED_ATTR, NAG_DISABLED);

    configurator.instance(m, context);
  }

  @Test
  public void testInstanceBadEmail() throws Exception {
    thrown.expect(ConfiguratorException.class);
    final Mapping m = new Mapping();
    m.put(ACCEPT_TOS_ATTR, ACCEPT_TOS);
    Sequence cc = new Sequence();
    cc.add(new Scalar("onemore@mail.com"));
    cc.add(new Scalar("another@email.com"));
    m.put(CCS_ATTR, cc);
    m.put(EMAIL_ATTR, "");
    m.put(NAG_DISABLED_ATTR, NAG_DISABLED);

    configurator.instance(m, context);
  }

  @Test
  public void testInstanceEmptyCC() throws Exception {
    thrown.expect(ConfiguratorException.class);
    final Mapping m = new Mapping();
    m.put(ACCEPT_TOS_ATTR, false);
    m.put(EMAIL_ATTR, FAKE_EMAIL);
    m.put(NAG_DISABLED_ATTR, NAG_DISABLED);

    configurator.instance(m, context);
  }

  @Test
  public void testInstanceBadCC() throws Exception {
    thrown.expect(ConfiguratorException.class);
    final Mapping m = new Mapping();
    m.put(ACCEPT_TOS_ATTR, false);
    Sequence cc = new Sequence();
    cc.add(new Scalar("bad_cc"));
    m.put(CCS_ATTR, cc);
    m.put(EMAIL_ATTR, FAKE_EMAIL);
    m.put(NAG_DISABLED_ATTR, NAG_DISABLED);

    configurator.instance(m, context);
  }
}
