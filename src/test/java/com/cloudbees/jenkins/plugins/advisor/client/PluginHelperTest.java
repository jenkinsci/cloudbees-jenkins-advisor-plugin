package com.cloudbees.jenkins.plugins.advisor.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class PluginHelperTest {

    @Test
    void getPluginVersion(JenkinsRule j) {
        assertThat(PluginHelper.getPluginVersion(), is(notNullValue()));
    }
}
