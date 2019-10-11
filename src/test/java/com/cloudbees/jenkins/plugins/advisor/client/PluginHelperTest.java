package com.cloudbees.jenkins.plugins.advisor.client;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class PluginHelperTest {

  @Rule
  public JenkinsRule j = new JenkinsRule();

  @Test
  public void getPluginVersion() {
    assertThat(PluginHelper.getPluginVersion(), is(notNullValue()));
  }
}
