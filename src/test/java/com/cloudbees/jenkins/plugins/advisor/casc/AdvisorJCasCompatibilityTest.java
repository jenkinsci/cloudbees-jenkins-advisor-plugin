package com.cloudbees.jenkins.plugins.advisor.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.jenkins.plugins.advisor.AdvisorGlobalConfiguration;
import com.cloudbees.jenkins.plugins.advisor.client.model.Recipient;
import io.jenkins.plugins.casc.misc.junit.jupiter.AbstractRoundTripTest;
import java.util.Arrays;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class AdvisorJCasCompatibilityTest extends AbstractRoundTripTest {

    @Override
    protected void assertConfiguredAsExpected(JenkinsRule jenkinsRule, String s) {
        AdvisorGlobalConfiguration advisor = AdvisorGlobalConfiguration.getInstance();
        assertEquals("me@email.com", advisor.getEmail());
        assertThat(
                advisor.getCcs().stream().map(Recipient::getEmail).toArray(),
                arrayContainingInAnyOrder(
                        Arrays.asList("we@email.com", "they@email.com").toArray()));
        assertTrue(advisor.isAcceptToS());
        assertTrue(advisor.isNagDisabled());
        assertEquals(2, advisor.getExcludedComponents().size());
        assertThat(
                advisor.getExcludedComponents().toArray(),
                arrayContainingInAnyOrder(
                        Arrays.asList("ThreadDumps", "PipelineThreadDump").toArray()));
    }

    @Override
    protected String stringInLogExpected() {
        return "excludedComponents = [ThreadDumps, PipelineThreadDump]";
    }
}
