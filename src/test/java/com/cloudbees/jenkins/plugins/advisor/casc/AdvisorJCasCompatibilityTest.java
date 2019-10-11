package com.cloudbees.jenkins.plugins.advisor.casc;

import com.cloudbees.jenkins.plugins.advisor.AdvisorGlobalConfiguration;
import io.jenkins.plugins.casc.misc.RoundTripAbstractTest;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.util.Arrays;

import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AdvisorJCasCompatibilityTest extends RoundTripAbstractTest {
  @Override
  protected void assertConfiguredAsExpected(RestartableJenkinsRule restartableJenkinsRule, String s) {
    AdvisorGlobalConfiguration advisor = AdvisorGlobalConfiguration.getInstance();
    assertEquals("me@email.com", advisor.getEmail());
    assertEquals("we@email.com,they@email.com", advisor.getCc());
    assertTrue(advisor.isAcceptToS());
    assertTrue(advisor.isNagDisabled());
    assertEquals(2, advisor.getExcludedComponents().size());
    assertThat(advisor.getExcludedComponents().toArray(),
      arrayContainingInAnyOrder(Arrays.asList("ThreadDumps", "PipelineThreadDump").toArray()));
  }

  @Override
  protected String stringInLogExpected() {
    return "excludedComponents = [ThreadDumps, PipelineThreadDump]";
  }
}
