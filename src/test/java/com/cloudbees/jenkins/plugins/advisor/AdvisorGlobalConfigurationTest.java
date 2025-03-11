package com.cloudbees.jenkins.plugins.advisor;

import static com.cloudbees.jenkins.plugins.advisor.AdvisorGlobalConfiguration.INVALID_CONFIGURATION;
import static com.cloudbees.jenkins.plugins.advisor.AdvisorGlobalConfiguration.SERVICE_OPERATIONAL;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.cloudbees.jenkins.plugins.advisor.client.model.Recipient;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.google.gson.Gson;
import hudson.util.FormValidation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import net.sf.json.JSONObject;
import org.htmlunit.HttpMethod;
import org.htmlunit.Page;
import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * Test the AdvisorGlobalConfiguration page; essentially the core of
 * the Jenkins Health Advisor by CloudBees connection.
 */
@WithJenkins
class AdvisorGlobalConfigurationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final String EMAIL = "test@cloudbees.com";
    private AdvisorGlobalConfiguration advisor;

    private JenkinsRule j;

    @BeforeEach
    void setup(JenkinsRule j) {
        this.j = j;
        advisor = AdvisorGlobalConfiguration.getInstance();
        // Dynamically configure the Advisor Server URL to reach WireMock server
        System.setProperty(
                "com.cloudbees.jenkins.plugins.advisor.client.AdvisorClientConfig.advisorURL", wireMock.url("/"));
    }

    @Test
    void testBaseConfigurationPage() throws Exception {
        WebClient wc = j.createWebClient();
        wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
        WebRequest req = new WebRequest(new URL(j.jenkins.getRootUrl() + advisor.getUrlName()), HttpMethod.GET);

        Page page = wc.getPage(wc.addCrumb(req));
        j.assertGoodStatus(page);
    }

    @Test
    void testHelpOnPage() throws Exception {
        j.assertHelpExists(AdvisorGlobalConfiguration.class, "-nagDisabled");
    }

    @Test
    void testServerConnectionFailure() throws Exception {
        wireMock.resetAll();
        wireMock.stubFor(get(urlEqualTo("/api/health")).willReturn(aResponse().withStatus(404)));

        WebClient wc = j.createWebClient();
        HtmlPage managePage = wc.goTo("cloudbees-jenkins-advisor");
        assertFalse(managePage.asNormalizedText().contains("There was a connection failure"));

        final AdvisorGlobalConfiguration.DescriptorImpl advisorDescriptor =
                (AdvisorGlobalConfiguration.DescriptorImpl) advisor.getDescriptor();

        // We don't do any remote test if the configuration isn't valid
        String result = advisorDescriptor.validateServerConnection();
        assertThat("Test connection fail was expected", result, containsString(INVALID_CONFIGURATION));

        DoConfigureInfo doConfigure = new DoConfigureInfo();
        doConfigure.setTerms(true);
        doConfigure.setUp(EMAIL);
        j.executeOnServer(doConfigure);

        result = advisorDescriptor.validateServerConnection();
        assertThat("Test connection fail was expected", result, containsString("404"));
        managePage = wc.goTo("cloudbees-jenkins-advisor");
        String t = managePage.asNormalizedText();
        assertTrue(managePage.asNormalizedText().contains("Connection failure"));
    }

    @Test
    void testServerConnectionPass() throws Exception {
        wireMock.resetAll();
        wireMock.stubFor(get(urlEqualTo("/api/health")).willReturn(aResponse().withStatus(200)));

        WebClient wc = j.createWebClient();
        HtmlPage managePage = wc.goTo("cloudbees-jenkins-advisor");
        assertFalse(managePage.asNormalizedText().contains("successfully connected"));

        final AdvisorGlobalConfiguration.DescriptorImpl advisorDescriptor =
                (AdvisorGlobalConfiguration.DescriptorImpl) advisor.getDescriptor();

        // We don't do any remote test if the configuration isn't valid
        String result = advisorDescriptor.validateServerConnection();
        assertThat("Test connection fail was expected", result, containsString(INVALID_CONFIGURATION));

        DoConfigureInfo doConfigure = new DoConfigureInfo();
        doConfigure.setTerms(true);
        doConfigure.setUp(EMAIL);
        j.executeOnServer(doConfigure);

        result = advisorDescriptor.validateServerConnection();
        assertThat("Test connection is ok", result, containsString(SERVICE_OPERATIONAL));

        managePage = wc.goTo("cloudbees-jenkins-advisor");
        assertTrue(managePage.asNormalizedText().contains("successfully connected"));
    }

    @Test
    void testSendEmail() {
        wireMock.resetAll();
        wireMock.stubFor(post(urlEqualTo("/api/test/emails"))
                .withRequestBody(equalTo("{\"email\":\"test@cloudbees.com\"}"))
                .willReturn(aResponse().withStatus(200)));

        final AdvisorGlobalConfiguration.DescriptorImpl advisorDescriptor =
                (AdvisorGlobalConfiguration.DescriptorImpl) advisor.getDescriptor();
        FormValidation formValidation = advisorDescriptor.doTestSendEmail(EMAIL, true);
        assertEquals(FormValidation.Kind.OK, formValidation.kind, "Test connection pass was expected");
    }

    @Test
    void testConfigure() throws Exception {

        DoConfigureInfo doConfigure = new DoConfigureInfo();
        // Invalid email - send back to main page
        doConfigure.setUp("", Collections.emptyList());
        HttpRedirect hr1 = (HttpRedirect) j.executeOnServer(doConfigure);
        String url1 = getInternalState(hr1, "url");
        assertEquals(".", url1, "Rerouted back to configuration");

        // Didn't accept Terms of Service - send back to main page
        doConfigure.setUp(EMAIL, Collections.singletonList(new Recipient(EMAIL)));
        doConfigure.setTerms(false);
        HttpRedirect hr2 = (HttpRedirect) j.executeOnServer(doConfigure);
        String url2 = getInternalState(hr2, "url");
        assertEquals(".", url2, "Rerouted back to configuration");

        // Redirect to main page
        doConfigure.setTerms(true);
        HttpRedirect hr4 = (HttpRedirect) j.executeOnServer(doConfigure);
        String url4 = getInternalState(hr4, "url");
        assertEquals("/jenkins/manage", url4, "Rerouted back to main mpage");
    }

    @Test
    void testPersistence() throws Exception {
        assertNull(advisor.getEmail(), "Email before configuration save - ");

        DoConfigureInfo doConfigure = new DoConfigureInfo();
        doConfigure.setUp(EMAIL);
        j.executeOnServer(doConfigure);
        advisor.load();
        assertEquals(EMAIL, advisor.getEmail(), "Email after configuration save - ");
        assertThat(advisor.getExcludedComponents().size(), is(5));

        DoConfigureInfo doConfigure2 = new DoConfigureInfo();
        doConfigure2.setUp(EMAIL, Collections.singletonList(new Recipient(EMAIL)));
        j.executeOnServer(doConfigure2);
        advisor.load();
        assertEquals(EMAIL, advisor.getEmail(), "Email after configuration save - ");
        assertEquals(1, advisor.getCcs().size(), "CC size after configuration save - ");
        assertEquals(EMAIL, advisor.getCcs().get(0).getEmail(), "CC content after configuration save - ");
        assertThat(advisor.getExcludedComponents().size(), is(5));
    }

    @Test
    void testSaveExcludedComponents() throws Exception {
        wireMock.resetAll();
        wireMock.stubFor(get(urlEqualTo("/api/health")).willReturn(aResponse().withStatus(200)));
        WebClient wc = j.createWebClient();

        String noComponentsSelected =
                "{\"components\": [{\"selected\": false, \"name\": \"JenkinsLogs\"}, {\"selected\": false, \"name\"\r\n: \"SlaveLogs\"}, {\"selected\": false, \"name\": \"GCLogs\"}, {\"selected\": false, \"name\": \"AgentsConfigFile\"\r\n}, {\"selected\": false, \"name\": \"ConfigFileComponent\"}, {\"selected\": false, \"name\": \"OtherConfigFilesComponent\"\r\n}, {\"selected\": false, \"name\": \"AboutBrowser\"}, {\"selected\": false, \"name\": \"AboutJenkins\"}, {\"selected\"\r\n: true, \"name\": \"AboutUser\"}, {\"selected\": false, \"name\": \"AdministrativeMonitors\"}, {\"selected\": false\r\n, \"name\": \"BuildQueue\"}, {\"selected\": false, \"name\": \"DumpExportTable\"}, {\"selected\": false, \"name\": \"EnvironmentVariables\"\r\n}, {\"selected\": false, \"name\": \"FileDescriptorLimit\"}, {\"selected\": false, \"name\": \"JVMProcessSystemMetricsContents\"\r\n}, {\"selected\": false, \"name\": \"LoadStats\"}, {\"selected\": false, \"name\": \"LoggerManager\"}, {\"selected\"\r\n: true, \"name\": \"Metrics\"}, {\"selected\": false, \"name\": \"NetworkInterfaces\"}, {\"selected\": false, \"name\"\r\n: \"NodeMonitors\"}, {\"selected\": false, \"name\": \"RootCAs\"}, {\"selected\": false, \"name\": \"SystemConfiguration\"\r\n}, {\"selected\": false, \"name\": \"SystemProperties\"}, {\"selected\": false, \"name\": \"UpdateCenter\"}, {\"selected\"\r\n: true, \"name\": \"SlowRequestComponent\"}, {\"selected\": false, \"name\": \"DeadlockRequestComponent\"}, {\"selected\"\r\n: true, \"name\": \"PipelineTimings\"}, {\"selected\": false, \"name\": \"PipelineThreadDump\"}, {\"selected\": false\r\n, \"name\": \"ThreadDumps\"}]}";
        DoConfigureInfo doConfigure = new DoConfigureInfo();
        doConfigure.setUpComponents(noComponentsSelected);
        j.executeOnServer(doConfigure);
        assertThat(advisor.getExcludedComponents().size(), is(25));
        HtmlPage managePage = wc.goTo("cloudbees-jenkins-advisor");
        assertThat(managePage.asNormalizedText(), containsString("unchecked JenkinsLogs"));
        assertTrue(managePage.asNormalizedText().contains("unchecked SlaveLogs"));

        String allComponentsSelected =
                "{\"components\": [{\"selected\": true, \"name\": \"JenkinsLogs\"}, {\"selected\": true, \"name\"\r\n: \"SlaveLogs\"}, {\"selected\": true, \"name\": \"GCLogs\"}, {\"selected\": true, \"name\": \"AgentsConfigFile\"\r\n}, {\"selected\": true, \"name\": \"ConfigFileComponent\"}, {\"selected\": true, \"name\": \"OtherConfigFilesComponent\"\r\n}, {\"selected\": true, \"name\": \"AboutBrowser\"}, {\"selected\": true, \"name\": \"AboutJenkins\"}, {\"selected\"\r\n: true, \"name\": \"AboutUser\"}, {\"selected\": true, \"name\": \"AdministrativeMonitors\"}, {\"selected\": true\r\n, \"name\": \"BuildQueue\"}, {\"selected\": true, \"name\": \"DumpExportTable\"}, {\"selected\": true, \"name\": \"EnvironmentVariables\"\r\n}, {\"selected\": true, \"name\": \"FileDescriptorLimit\"}, {\"selected\": true, \"name\": \"JVMProcessSystemMetricsContents\"\r\n}, {\"selected\": true, \"name\": \"LoadStats\"}, {\"selected\": true, \"name\": \"LoggerManager\"}, {\"selected\"\r\n: true, \"name\": \"Metrics\"}, {\"selected\": true, \"name\": \"NetworkInterfaces\"}, {\"selected\": true, \"name\"\r\n: \"NodeMonitors\"}, {\"selected\": true, \"name\": \"RootCAs\"}, {\"selected\": true, \"name\": \"SystemConfiguration\"\r\n}, {\"selected\": true, \"name\": \"SystemProperties\"}, {\"selected\": true, \"name\": \"UpdateCenter\"}, {\"selected\"\r\n: true, \"name\": \"SlowRequestComponent\"}, {\"selected\": true, \"name\": \"DeadlockRequestComponent\"}, {\"selected\"\r\n: true, \"name\": \"PipelineTimings\"}, {\"selected\": true, \"name\": \"PipelineThreadDump\"}, {\"selected\": true\r\n, \"name\": \"ThreadDumps\"}]}";
        DoConfigureInfo doConfigure2 = new DoConfigureInfo();
        doConfigure2.setUpComponents(allComponentsSelected);
        j.executeOnServer(doConfigure2);
        assertThat(advisor.getExcludedComponents().size(), is(1));
        assertTrue(advisor.getExcludedComponents().contains("SENDALL"));
        managePage = wc.goTo("cloudbees-jenkins-advisor");
        assertTrue(managePage.asNormalizedText().contains("checked JenkinsLogs"));
        assertTrue(managePage.asNormalizedText().contains("checked SlaveLogs"));

        String mixCompoentsSelected =
                "{\"components\": [{\"selected\": false, \"name\": \"JenkinsLogs\"}, {\"selected\": true, \"name\"\r\n: \"SlaveLogs\"}, {\"selected\": false, \"name\": \"GCLogs\"}, {\"selected\": true, \"name\": \"AgentsConfigFile\"\r\n}, {\"selected\": true, \"name\": \"ConfigFileComponent\"}, {\"selected\": true, \"name\": \"OtherConfigFilesComponent\"\r\n}, {\"selected\": false, \"name\": \"AboutBrowser\"}, {\"selected\": true, \"name\": \"AboutJenkins\"}, {\"selected\"\r\n: true, \"name\": \"AboutUser\"}, {\"selected\": false, \"name\": \"AdministrativeMonitors\"}, {\"selected\": true\r\n, \"name\": \"BuildQueue\"}, {\"selected\": true, \"name\": \"DumpExportTable\"}, {\"selected\": false, \"name\": \"EnvironmentVariables\"\r\n}, {\"selected\": true, \"name\": \"FileDescriptorLimit\"}, {\"selected\": false, \"name\": \"JVMProcessSystemMetricsContents\"\r\n}, {\"selected\": true, \"name\": \"LoadStats\"}, {\"selected\": true, \"name\": \"LoggerManager\"}, {\"selected\"\r\n: true, \"name\": \"Metrics\"}, {\"selected\": true, \"name\": \"NetworkInterfaces\"}, {\"selected\": true, \"name\"\r\n: \"NodeMonitors\"}, {\"selected\": false, \"name\": \"RootCAs\"}, {\"selected\": false, \"name\": \"SystemConfiguration\"\r\n}, {\"selected\": false, \"name\": \"SystemProperties\"}, {\"selected\": false, \"name\": \"UpdateCenter\"}, {\"selected\"\r\n: true, \"name\": \"SlowRequestComponent\"}, {\"selected\": true, \"name\": \"DeadlockRequestComponent\"}, {\"selected\"\r\n: true, \"name\": \"PipelineTimings\"}, {\"selected\": false, \"name\": \"PipelineThreadDump\"}, {\"selected\": true\r\n, \"name\": \"ThreadDumps\"}]}";
        DoConfigureInfo doConfigure3 = new DoConfigureInfo();
        doConfigure3.setUpComponents(mixCompoentsSelected);
        j.executeOnServer(doConfigure3);
        assertThat(advisor.getExcludedComponents().size(), is(11));
        managePage = wc.goTo("cloudbees-jenkins-advisor");
        assertTrue(managePage.asNormalizedText().contains("unchecked JenkinsLogs"));
        assertTrue(managePage.asNormalizedText().contains("checked SlaveLogs"));
    }

    @Test
    void testSetEmail() {
        advisor.setEmail(null);
        assertNull(advisor.getEmail());

        advisor.setEmail("");
        assertNull(advisor.getEmail());

        advisor.setEmail(" ");
        assertNull(advisor.getEmail());

        advisor.setEmail(EMAIL + " ");
        assertThat(advisor.getEmail(), is(EMAIL));
    }

    private class DoConfigureInfo implements Callable<HttpResponse> {
        private String testEmail = "";
        private List<Recipient> testCc = Collections.emptyList();
        private Boolean testAcceptToS = true;
        private String allToEnable =
                "{\"components\": [{\"selected\": true, \"name\": \"JenkinsLogs\"}, {\"selected\": false, \"name\"\r\n: \"SlaveLogs\"}, {\"selected\": true, \"name\": \"GCLogs\"}, {\"selected\": false, \"name\": \"AgentsConfigFile\"\r\n}, {\"selected\": false, \"name\": \"ConfigFileComponent\"}, {\"selected\": false, \"name\": \"OtherConfigFilesComponent\"\r\n}, {\"selected\": true, \"name\": \"AboutBrowser\"}, {\"selected\": true, \"name\": \"AboutJenkins\"}, {\"selected\"\r\n: true, \"name\": \"AboutUser\"}, {\"selected\": true, \"name\": \"AdministrativeMonitors\"}, {\"selected\": true\r\n, \"name\": \"BuildQueue\"}, {\"selected\": true, \"name\": \"DumpExportTable\"}, {\"selected\": true, \"name\": \"EnvironmentVariables\"\r\n}, {\"selected\": true, \"name\": \"FileDescriptorLimit\"}, {\"selected\": true, \"name\": \"JVMProcessSystemMetricsContents\"\r\n}, {\"selected\": true, \"name\": \"LoadStats\"}, {\"selected\": true, \"name\": \"LoggerManager\"}, {\"selected\"\r\n: true, \"name\": \"Metrics\"}, {\"selected\": true, \"name\": \"NetworkInterfaces\"}, {\"selected\": true, \"name\"\r\n: \"NodeMonitors\"}, {\"selected\": false, \"name\": \"RootCAs\"}, {\"selected\": true, \"name\": \"SystemConfiguration\"\r\n}, {\"selected\": true, \"name\": \"SystemProperties\"}, {\"selected\": true, \"name\": \"UpdateCenter\"}, {\"selected\"\r\n: true, \"name\": \"SlowRequestComponent\"}, {\"selected\": true, \"name\": \"DeadlockRequestComponent\"}, {\"selected\"\r\n: true, \"name\": \"PipelineTimings\"}, {\"selected\": true, \"name\": \"PipelineThreadDump\"}, {\"selected\": true\r\n, \"name\": \"ThreadDumps\"}]}";

        public void setUp(String testEmail) {
            this.testEmail = testEmail;
        }

        public void setUp(String testEmail, List<Recipient> testCc) {
            this.testEmail = testEmail;
            this.testCc = testCc;
        }

        /**
         * Because this is more painful than it should be, just accept a string
         * that should already be formatted.  It's the burden of the test case
         * to make sure the components are formatted correctly.
         * <p>
         * This call is designed to run successfully.
         */
        public void setUpComponents(String allToEnable) {
            testEmail = EMAIL;
            this.allToEnable = allToEnable;
        }

        public void setTerms(boolean terms) {
            testAcceptToS = terms;
        }

        @Override
        public HttpResponse call() throws Exception {
            StaplerRequest2 spyRequest = spy(Stapler.getCurrentRequest2());

            JSONObject json1 = new JSONObject();
            json1.element("email", testEmail);
            json1.element(
                    "ccs",
                    testCc.stream()
                            .map(recipient -> new JSONObject().element("email", recipient.getEmail()))
                            .toArray());
            json1.element("nagDisabled", false);
            json1.element("acceptToS", testAcceptToS);
            json1.element("advanced", new Gson().fromJson(allToEnable, JSONObject.class));
            doReturn(json1).when(spyRequest).getSubmittedForm();
            return advisor.doConfigure(spyRequest);
        }
    }

    private static String getInternalState(Object object, String fieldName) throws ReflectiveOperationException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(object);
    }
}
