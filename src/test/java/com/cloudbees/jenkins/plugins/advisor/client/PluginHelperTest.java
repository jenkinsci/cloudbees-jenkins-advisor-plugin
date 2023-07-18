package com.cloudbees.jenkins.plugins.advisor.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class PluginHelperTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void getPluginVersion() {
        assertThat(PluginHelper.getPluginVersion(), is(notNullValue()));
    }
}
