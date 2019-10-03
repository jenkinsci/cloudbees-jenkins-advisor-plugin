package com.cloudbees.jenkins.plugins.advisor;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.reflect.Whitebox;

import java.net.URL;
import java.util.concurrent.Callable;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

/**
 * Test the AdvisorGlobalConfiguration page; essentially the core of
 * the Jenkins Health Advisor by CloudBees connection.
 */
@PowerMockIgnore({"org.apache.http.conn.ssl.*", "javax.net.ssl.*" , "javax.crypto.*"})
public class AdvisorGlobalConfigurationTest {

  @Rule
  public JenkinsRule j = new JenkinsRule();

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig());

  private AdvisorGlobalConfiguration advisor;
  private final String email = "test@cloudbees.com";
  private final String cc = "";


  @Before
  public void setup() {
    advisor = AdvisorGlobalConfiguration.getInstance();
  }


  @Test
  public void testBaseConfigurationPage() throws Exception {
    WebClient wc = j.createWebClient();
    wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
    WebRequest req = new WebRequest(
        new URL(j.jenkins.getRootUrl() + advisor.getUrlName()),
        HttpMethod.GET
    );

    Page page = wc.getPage(wc.addCrumb(req));
    j.assertGoodStatus(page);
  }


  @Test
  public void testHelpOnPage() throws Exception {
      j.assertHelpExists(AdvisorGlobalConfiguration.class, "-nagDisabled");
  }

  @Test
  public void testConnectionFailure() throws Exception {
    wireMockRule.resetAll();
    stubFor(get(urlEqualTo("/api/health"))
      .willReturn(aResponse()
          .withStatus(404)));

    WebClient wc = j.createWebClient();
    HtmlPage managePage = wc.goTo("cloudbees-jenkins-advisor");
    assertFalse(managePage.asText().contains("There was a connection failure"));

    final AdvisorGlobalConfiguration.DescriptorImpl advisorDescriptor = (AdvisorGlobalConfiguration.DescriptorImpl) advisor.getDescriptor();
    FormValidation formValidation = advisorDescriptor.doTestConnection(email, cc);
    assertEquals("Test connection fail was expected", FormValidation.Kind.ERROR, formValidation.kind);

    DoConfigureInfo doConfigure = new DoConfigureInfo();
    doConfigure.setUp(email);
    j.executeOnServer(doConfigure);
    managePage = wc.goTo("cloudbees-jenkins-advisor");
    String t = managePage.asText();
    System.out.println("\n\n\n\n\nT: " + t + "\n\n\n\n");
    assertTrue(managePage.asText().contains("Connection failure"));
  }

  @Test
  public void testConnectionPass() throws Exception {
    wireMockRule.resetAll();
    stubFor(get(urlEqualTo("/api/health"))
    .willReturn(aResponse()
        .withStatus(200)));

    WebClient wc = j.createWebClient();
    HtmlPage managePage = wc.goTo("cloudbees-jenkins-advisor");
    assertFalse(managePage.asText().contains("successfully connected"));

    final AdvisorGlobalConfiguration.DescriptorImpl advisorDescriptor = (AdvisorGlobalConfiguration.DescriptorImpl) advisor.getDescriptor();
    FormValidation formValidation = advisorDescriptor.doTestConnection(email, cc);
    assertEquals("Test connection pass was expected", FormValidation.Kind.OK, formValidation.kind);

    DoConfigureInfo doConfigure = new DoConfigureInfo();
    doConfigure.setUp(email);
    doConfigure.setTerms(true);
    j.executeOnServer(doConfigure);
    managePage = wc.goTo("cloudbees-jenkins-advisor");
    assertTrue(managePage.asText().contains("successfully connected"));
  }

  @Test
  public void testSendEmail() throws Exception {
    wireMockRule.resetAll();
    stubFor(get(urlEqualTo("/api/test/emails/test@cloudbees.com"))
    .willReturn(aResponse()
        .withStatus(200)));

    final AdvisorGlobalConfiguration.DescriptorImpl advisorDescriptor = (AdvisorGlobalConfiguration.DescriptorImpl) advisor.getDescriptor();
    FormValidation formValidation = advisorDescriptor.doTestSendEmail(email, cc);
    assertEquals("Test connection pass was expected", FormValidation.Kind.OK, formValidation.kind);
  }

  @Test
  public void testConfigure() throws Exception {

    DoConfigureInfo doConfigure = new DoConfigureInfo();
    // Invalid email - send back to main page
    doConfigure.setUp("", "");
    HttpRedirect hr1 = (HttpRedirect)j.executeOnServer(doConfigure);
    String url1 = Whitebox.getInternalState(hr1, "url");
    assertEquals("Rerouted back to configuration", "/jenkins/cloudbees-jenkins-advisor", url1);

    // Didn't accept Terms of Service - send back to main page
    doConfigure.setUp(email, email);
    doConfigure.setTerms(false);
    HttpRedirect hr2 = (HttpRedirect)j.executeOnServer(doConfigure);
    String url2 = Whitebox.getInternalState(hr2, "url");
    assertEquals("Rerouted back to configuration", "/jenkins/cloudbees-jenkins-advisor", url2);

    // Redirect to main page
    doConfigure.setTerms(true);
    HttpRedirect hr4 = (HttpRedirect)j.executeOnServer(doConfigure);
    String url4 = Whitebox.getInternalState(hr4, "url");
    assertEquals("Rerouted back to main mpage", "/jenkins/manage", url4);
  }


  @Test
  public void testPersistence() throws Exception {
    assertNull("Email before configuration save - ", advisor.getEmail());

    DoConfigureInfo doConfigure = new DoConfigureInfo();
    doConfigure.setUp(email);
    j.executeOnServer(doConfigure);
    advisor.load();
    assertEquals("Email after configuration save - ", email, advisor.getEmail());
    assertThat(advisor.getExcludedComponents().size(), is(5));

    DoConfigureInfo doConfigure2 = new DoConfigureInfo();
    doConfigure2.setUp(email, email);
    j.executeOnServer(doConfigure2);
    advisor.load();
    assertEquals("Email after configuration save - ", email, advisor.getEmail());
    assertEquals("CC after configuration save - ", email, advisor.getCc());
    assertThat(advisor.getExcludedComponents().size(), is(5));
  }

  @Test
  public void testSaveExcludedComponents() throws Exception {
    wireMockRule.resetAll();
    stubFor(get(urlEqualTo("/api/health"))
    .willReturn(aResponse()
        .withStatus(200)));
    WebClient wc = j.createWebClient();

    String noComponentsSelected = "{\"components\": [{\"selected\": false, \"name\": \"JenkinsLogs\"}, {\"selected\": false, \"name\"\r\n: \"SlaveLogs\"}, {\"selected\": false, \"name\": \"GCLogs\"}, {\"selected\": false, \"name\": \"AgentsConfigFile\"\r\n}, {\"selected\": false, \"name\": \"ConfigFileComponent\"}, {\"selected\": false, \"name\": \"OtherConfigFilesComponent\"\r\n}, {\"selected\": false, \"name\": \"AboutBrowser\"}, {\"selected\": false, \"name\": \"AboutJenkins\"}, {\"selected\"\r\n: true, \"name\": \"AboutUser\"}, {\"selected\": false, \"name\": \"AdministrativeMonitors\"}, {\"selected\": false\r\n, \"name\": \"BuildQueue\"}, {\"selected\": false, \"name\": \"DumpExportTable\"}, {\"selected\": false, \"name\": \"EnvironmentVariables\"\r\n}, {\"selected\": false, \"name\": \"FileDescriptorLimit\"}, {\"selected\": false, \"name\": \"JVMProcessSystemMetricsContents\"\r\n}, {\"selected\": false, \"name\": \"LoadStats\"}, {\"selected\": false, \"name\": \"LoggerManager\"}, {\"selected\"\r\n: true, \"name\": \"Metrics\"}, {\"selected\": false, \"name\": \"NetworkInterfaces\"}, {\"selected\": false, \"name\"\r\n: \"NodeMonitors\"}, {\"selected\": false, \"name\": \"RootCAs\"}, {\"selected\": false, \"name\": \"SystemConfiguration\"\r\n}, {\"selected\": false, \"name\": \"SystemProperties\"}, {\"selected\": false, \"name\": \"UpdateCenter\"}, {\"selected\"\r\n: true, \"name\": \"SlowRequestComponent\"}, {\"selected\": false, \"name\": \"DeadlockRequestComponent\"}, {\"selected\"\r\n: true, \"name\": \"PipelineTimings\"}, {\"selected\": false, \"name\": \"PipelineThreadDump\"}, {\"selected\": false\r\n, \"name\": \"ThreadDumps\"}]}";
    DoConfigureInfo doConfigure = new DoConfigureInfo();
    doConfigure.setUpComponents(noComponentsSelected);
    j.executeOnServer(doConfigure);
    assertThat(advisor.getExcludedComponents().size(), is(25));
    HtmlPage managePage = wc.goTo("cloudbees-jenkins-advisor");
    assertTrue(managePage.asText().contains("uncheckedJenkinsLogs"));
    assertTrue(managePage.asText().contains("uncheckedSlaveLogs"));

    String allComponentsSelected = "{\"components\": [{\"selected\": true, \"name\": \"JenkinsLogs\"}, {\"selected\": true, \"name\"\r\n: \"SlaveLogs\"}, {\"selected\": true, \"name\": \"GCLogs\"}, {\"selected\": true, \"name\": \"AgentsConfigFile\"\r\n}, {\"selected\": true, \"name\": \"ConfigFileComponent\"}, {\"selected\": true, \"name\": \"OtherConfigFilesComponent\"\r\n}, {\"selected\": true, \"name\": \"AboutBrowser\"}, {\"selected\": true, \"name\": \"AboutJenkins\"}, {\"selected\"\r\n: true, \"name\": \"AboutUser\"}, {\"selected\": true, \"name\": \"AdministrativeMonitors\"}, {\"selected\": true\r\n, \"name\": \"BuildQueue\"}, {\"selected\": true, \"name\": \"DumpExportTable\"}, {\"selected\": true, \"name\": \"EnvironmentVariables\"\r\n}, {\"selected\": true, \"name\": \"FileDescriptorLimit\"}, {\"selected\": true, \"name\": \"JVMProcessSystemMetricsContents\"\r\n}, {\"selected\": true, \"name\": \"LoadStats\"}, {\"selected\": true, \"name\": \"LoggerManager\"}, {\"selected\"\r\n: true, \"name\": \"Metrics\"}, {\"selected\": true, \"name\": \"NetworkInterfaces\"}, {\"selected\": true, \"name\"\r\n: \"NodeMonitors\"}, {\"selected\": true, \"name\": \"RootCAs\"}, {\"selected\": true, \"name\": \"SystemConfiguration\"\r\n}, {\"selected\": true, \"name\": \"SystemProperties\"}, {\"selected\": true, \"name\": \"UpdateCenter\"}, {\"selected\"\r\n: true, \"name\": \"SlowRequestComponent\"}, {\"selected\": true, \"name\": \"DeadlockRequestComponent\"}, {\"selected\"\r\n: true, \"name\": \"PipelineTimings\"}, {\"selected\": true, \"name\": \"PipelineThreadDump\"}, {\"selected\": true\r\n, \"name\": \"ThreadDumps\"}]}";
    DoConfigureInfo doConfigure2 = new DoConfigureInfo();
    doConfigure2.setUpComponents(allComponentsSelected);
    j.executeOnServer(doConfigure2);
    assertThat(advisor.getExcludedComponents().size(), is(1));
    assertTrue(advisor.getExcludedComponents().contains("SENDALL"));
    managePage = wc.goTo("cloudbees-jenkins-advisor");
    assertTrue(managePage.asText().contains("checkedJenkinsLogs"));
    assertTrue(managePage.asText().contains("checkedSlaveLogs"));

    String mixCompoentsSelected = "{\"components\": [{\"selected\": false, \"name\": \"JenkinsLogs\"}, {\"selected\": true, \"name\"\r\n: \"SlaveLogs\"}, {\"selected\": false, \"name\": \"GCLogs\"}, {\"selected\": true, \"name\": \"AgentsConfigFile\"\r\n}, {\"selected\": true, \"name\": \"ConfigFileComponent\"}, {\"selected\": true, \"name\": \"OtherConfigFilesComponent\"\r\n}, {\"selected\": false, \"name\": \"AboutBrowser\"}, {\"selected\": true, \"name\": \"AboutJenkins\"}, {\"selected\"\r\n: true, \"name\": \"AboutUser\"}, {\"selected\": false, \"name\": \"AdministrativeMonitors\"}, {\"selected\": true\r\n, \"name\": \"BuildQueue\"}, {\"selected\": true, \"name\": \"DumpExportTable\"}, {\"selected\": false, \"name\": \"EnvironmentVariables\"\r\n}, {\"selected\": true, \"name\": \"FileDescriptorLimit\"}, {\"selected\": false, \"name\": \"JVMProcessSystemMetricsContents\"\r\n}, {\"selected\": true, \"name\": \"LoadStats\"}, {\"selected\": true, \"name\": \"LoggerManager\"}, {\"selected\"\r\n: true, \"name\": \"Metrics\"}, {\"selected\": true, \"name\": \"NetworkInterfaces\"}, {\"selected\": true, \"name\"\r\n: \"NodeMonitors\"}, {\"selected\": false, \"name\": \"RootCAs\"}, {\"selected\": false, \"name\": \"SystemConfiguration\"\r\n}, {\"selected\": false, \"name\": \"SystemProperties\"}, {\"selected\": false, \"name\": \"UpdateCenter\"}, {\"selected\"\r\n: true, \"name\": \"SlowRequestComponent\"}, {\"selected\": true, \"name\": \"DeadlockRequestComponent\"}, {\"selected\"\r\n: true, \"name\": \"PipelineTimings\"}, {\"selected\": false, \"name\": \"PipelineThreadDump\"}, {\"selected\": true\r\n, \"name\": \"ThreadDumps\"}]}";
    DoConfigureInfo doConfigure3 = new DoConfigureInfo();
    doConfigure3.setUpComponents(mixCompoentsSelected);
    j.executeOnServer(doConfigure3);
    assertThat(advisor.getExcludedComponents().size(), is(11));
    managePage = wc.goTo("cloudbees-jenkins-advisor");
    assertTrue(managePage.asText().contains("uncheckedJenkinsLogs"));
    assertTrue(managePage.asText().contains("checkedSlaveLogs"));
  }

  @Test
  public void testSetEmail() throws Exception {
    advisor.setEmail(null);
    assertNull(advisor.getEmail());

    advisor.setEmail("");
    assertNull(advisor.getEmail());

    advisor.setEmail(" ");
    assertNull(advisor.getEmail());

    advisor.setEmail(email + " ");
    assertThat(advisor.getEmail(), is(email));
  }

  @Test
  public void testSetCc() throws Exception {
    advisor.setCc(null);
    assertNull(advisor.getCc());

    advisor.setCc("");
    assertNull(advisor.getCc());

    advisor.setCc(" ");
    assertNull(advisor.getCc());

    advisor.setCc(email + " ");
    assertThat(advisor.getCc(), is(email));

    advisor.setCc(email + " , " + email);
    assertThat(advisor.getCc(), is(email + "," + email));
  }

  @Test
  public void testDoCheckEmail() throws Exception {
    final AdvisorGlobalConfiguration.DescriptorImpl advisorDescriptor = (AdvisorGlobalConfiguration.DescriptorImpl) advisor.getDescriptor();
    FormValidation formValidation;

    formValidation = advisorDescriptor.doCheckEmail(null);
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

    formValidation = advisorDescriptor.doCheckEmail("");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

    formValidation = advisorDescriptor.doCheckEmail(";");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

    formValidation = advisorDescriptor.doCheckEmail(",");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

    formValidation = advisorDescriptor.doCheckEmail(email + ";");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

    formValidation = advisorDescriptor.doCheckEmail(email + ",");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

    formValidation = advisorDescriptor.doCheckEmail("<Mr Foo> foo@acme.com");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    formValidation = advisorDescriptor.doCheckEmail("Foo<foo@acme.com>");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    formValidation = advisorDescriptor.doCheckEmail("Foo <foo@acme.com>");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    formValidation = advisorDescriptor.doCheckEmail("\"foo\"@acme.com");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    formValidation = advisorDescriptor.doCheckEmail("\"foo bar\"@acme.com");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    formValidation = advisorDescriptor.doCheckEmail("\"foo\\ \"@acme.com");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    formValidation = advisorDescriptor.doCheckEmail("\"foo\\ /\"@acme.com");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);


    formValidation = advisorDescriptor.doCheckEmail(StringUtils.capitalize(email));
    assertEquals(FormValidation.Kind.OK, formValidation.kind);
    formValidation = advisorDescriptor.doCheckEmail(email);
    assertEquals(FormValidation.Kind.OK, formValidation.kind);

    formValidation = advisorDescriptor.doCheckEmail("test.foo@cloudbees.com");
    assertEquals(FormValidation.Kind.OK, formValidation.kind);
    formValidation = advisorDescriptor.doCheckEmail("test-foo@cloudbees.com");
    assertEquals(FormValidation.Kind.OK, formValidation.kind);
    formValidation = advisorDescriptor.doCheckEmail("test_foo@cloudbees.com");
    assertEquals(FormValidation.Kind.OK, formValidation.kind);
    formValidation = advisorDescriptor.doCheckEmail("test+foo@cloudbees.com");
    assertEquals(FormValidation.Kind.OK, formValidation.kind);
  }

  @Test
  public void testDoCheckCc() throws Exception {
    final AdvisorGlobalConfiguration.DescriptorImpl advisorDescriptor = (AdvisorGlobalConfiguration.DescriptorImpl) advisor.getDescriptor();
    FormValidation formValidation;

    formValidation = advisorDescriptor.doCheckCc(null);
    assertEquals(FormValidation.Kind.OK, formValidation.kind);

    formValidation = advisorDescriptor.doCheckCc("");
    assertEquals(FormValidation.Kind.OK, formValidation.kind);

    formValidation = advisorDescriptor.doCheckCc(";");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

    formValidation = advisorDescriptor.doCheckCc(email + ";");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

    formValidation = advisorDescriptor.doCheckCc("<Mr Foo> foo@acme.com");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    formValidation = advisorDescriptor.doCheckCc("Foo<foo@acme.com>");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    formValidation = advisorDescriptor.doCheckCc("Foo <foo@acme.com>");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    formValidation = advisorDescriptor.doCheckCc("\"foo\"@acme.com");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    formValidation = advisorDescriptor.doCheckCc("\"foo bar\"@acme.com");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    formValidation = advisorDescriptor.doCheckCc("\"foo\\ \"@acme.com");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    formValidation = advisorDescriptor.doCheckCc("\"foo\\ /\"@acme.com");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);


    formValidation = advisorDescriptor.doCheckCc(StringUtils.capitalize(email));
    assertEquals(FormValidation.Kind.OK, formValidation.kind);
    formValidation = advisorDescriptor.doCheckCc(email);
    assertEquals(FormValidation.Kind.OK, formValidation.kind);

    formValidation = advisorDescriptor.doCheckCc("test.foo@cloudbees.com");
    assertEquals(FormValidation.Kind.OK, formValidation.kind);
    formValidation = advisorDescriptor.doCheckCc("test-foo@cloudbees.com");
    assertEquals(FormValidation.Kind.OK, formValidation.kind);
    formValidation = advisorDescriptor.doCheckCc("test_foo@cloudbees.com");
    assertEquals(FormValidation.Kind.OK, formValidation.kind);
    formValidation = advisorDescriptor.doCheckCc("test+foo@cloudbees.com");
    assertEquals(FormValidation.Kind.OK, formValidation.kind);

    formValidation = advisorDescriptor.doCheckCc(email + " , " + email + ", " + email + "," + email);
    assertEquals(FormValidation.Kind.OK, formValidation.kind);
  }

  private class DoConfigureInfo implements Callable<HttpResponse> {
    private String testEmail = "";
    private String testCc = "";
    private Boolean testAcceptToS = true;
    private String allToEnable = "{\"components\": [{\"selected\": true, \"name\": \"JenkinsLogs\"}, {\"selected\": false, \"name\"\r\n: \"SlaveLogs\"}, {\"selected\": true, \"name\": \"GCLogs\"}, {\"selected\": false, \"name\": \"AgentsConfigFile\"\r\n}, {\"selected\": false, \"name\": \"ConfigFileComponent\"}, {\"selected\": false, \"name\": \"OtherConfigFilesComponent\"\r\n}, {\"selected\": true, \"name\": \"AboutBrowser\"}, {\"selected\": true, \"name\": \"AboutJenkins\"}, {\"selected\"\r\n: true, \"name\": \"AboutUser\"}, {\"selected\": true, \"name\": \"AdministrativeMonitors\"}, {\"selected\": true\r\n, \"name\": \"BuildQueue\"}, {\"selected\": true, \"name\": \"DumpExportTable\"}, {\"selected\": true, \"name\": \"EnvironmentVariables\"\r\n}, {\"selected\": true, \"name\": \"FileDescriptorLimit\"}, {\"selected\": true, \"name\": \"JVMProcessSystemMetricsContents\"\r\n}, {\"selected\": true, \"name\": \"LoadStats\"}, {\"selected\": true, \"name\": \"LoggerManager\"}, {\"selected\"\r\n: true, \"name\": \"Metrics\"}, {\"selected\": true, \"name\": \"NetworkInterfaces\"}, {\"selected\": true, \"name\"\r\n: \"NodeMonitors\"}, {\"selected\": false, \"name\": \"RootCAs\"}, {\"selected\": true, \"name\": \"SystemConfiguration\"\r\n}, {\"selected\": true, \"name\": \"SystemProperties\"}, {\"selected\": true, \"name\": \"UpdateCenter\"}, {\"selected\"\r\n: true, \"name\": \"SlowRequestComponent\"}, {\"selected\": true, \"name\": \"DeadlockRequestComponent\"}, {\"selected\"\r\n: true, \"name\": \"PipelineTimings\"}, {\"selected\": true, \"name\": \"PipelineThreadDump\"}, {\"selected\": true\r\n, \"name\": \"ThreadDumps\"}]}";

    public void setUp(String testEmail) {
        this.testEmail = testEmail;
    }

    public void setUp(String testEmail, String testCc) {
      this.testEmail = testEmail;
      this.testCc = testCc;
  }

    /**
     * Because this is more painful than it should be, just accept a string
     * that should already be formatted.  It's the burden of the test case
     * to make sure the components are formatted correctly.
     *
     * This call is designed to run successfully.
     */
    public void setUpComponents(String allToEnable) {
      testEmail = email;
      this.allToEnable = allToEnable;
    }

    public void setTerms(boolean terms) {
      testAcceptToS = terms;
    }

    @Override public HttpResponse call() throws Exception {
        StaplerRequest spyRequest = PowerMockito.spy(Stapler.getCurrentRequest());

        JSONObject json1 = new JSONObject();
        json1.element("email", testEmail);
        json1.element("cc", testCc);
        json1.element("nagDisabled", false);
        json1.element("acceptToS", testAcceptToS);
        json1.element("advanced", new Gson().fromJson(allToEnable, JSONObject.class));
        doReturn(json1)
            .when(spyRequest).getSubmittedForm();
        return advisor.doConfigure(spyRequest);
    }
  }

}
