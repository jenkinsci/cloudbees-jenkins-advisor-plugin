package com.cloudbees.jenkins.plugins.advisor;

import com.cloudbees.jenkins.plugins.advisor.client.AdvisorClient;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.jvnet.hudson.test.JenkinsRule;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.testng.PowerMockTestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.http.conn.ssl.*", "javax.net.ssl.*" , "javax.crypto.*"})
public class AdvisorGlobalConfigurationTest2 extends PowerMockTestCase {

  private AdvisorGlobalConfiguration advisor;
  private final String email = "test@cloudbees.com";
  private final String password = "test123";

  @Rule
  public JenkinsRule j = new JenkinsRule();

  @Before
  public void setup() {
    advisor = AdvisorGlobalConfiguration.getInstance();
  }

  @Test
  @PrepareOnlyThisForTest({AdvisorGlobalConfiguration.DescriptorImpl.class, AdvisorClient.class})
  public void testConnection() throws Exception {
    final AdvisorGlobalConfiguration.DescriptorImpl advisorDescriptor = (AdvisorGlobalConfiguration.DescriptorImpl) advisor.getDescriptor();
    FormValidation formValidation = advisorDescriptor.doTestConnection(email, Secret.fromString(password));
    assertEquals("Test connection fail was expected", FormValidation.Kind.ERROR, formValidation.kind);
    initMockCJAService(advisorDescriptor);
    formValidation = advisorDescriptor.doTestConnection(email, Secret.fromString(password));
    assertEquals("Test connection pass was expected", FormValidation.Kind.OK, formValidation.kind);
  }
  
  /**
   * Mock connection to the CloudBees Jenkins Advisor service.
   */
  private void initMockCJAService(AdvisorGlobalConfiguration.DescriptorImpl advisorDescriptor) throws Exception {
    AdvisorClient impl = PowerMockito.mock(AdvisorClient.class);
    doReturn(CompletableFuture.completedFuture("success"))
      .when(impl).doAuthenticate();
    PowerMockito.whenNew(AdvisorClient.class).withAnyArguments().thenReturn(impl);
  }
}