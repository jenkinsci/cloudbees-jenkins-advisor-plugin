package com.cloudbees.jenkins.plugins.advisor;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BundleUploadMonitorTest {

  @Rule
  public JenkinsRule j = new JenkinsRule();
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig());

  private static final String TEST_EMAIL = "test";
  private final String textPrefix = "The CloudBees Jenkins Advisor upload failed";

  @Test
  public void testBundleUploadSuccess() throws Exception {
    // just in case the disable test is run first
    j.getPluginManager().getPlugin(AdvisorGlobalConfiguration.PLUGIN_NAME).enable();
    wireMockRule.resetAll();

    BundleUpload subject = j.getInstance().getExtensionList(BundleUpload.class).get(0);

    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    config.setEmail(TEST_EMAIL);
    config.setValid(true);
    config.setAcceptToS(true);

    stubFor(get(urlEqualTo("/api/health"))
        .willReturn(aResponse()
            .withStatus(200)));

    stubFor(post(urlEqualTo(format("/api/users/%s/upload/%s", TEST_EMAIL, j.getInstance().getLegacyInstanceId())))
        .willReturn(aResponse()
            .withStatus(200)));

    subject.run();

    // hack as wiremock doesn't seem to handle async requests
    while(wireMockRule.getAllServeEvents().size() < 2) {
      Thread.sleep(1000L);
    }

    WebClient w = j.createWebClient();
    HtmlPage managePage = w.goTo("manage");
    assertFalse(managePage.asText().contains(textPrefix));
  }

  @Test
  public void testErrorUploadingBundle() throws Exception {
    // just in case the disable test is run first
    j.getPluginManager().getPlugin(AdvisorGlobalConfiguration.PLUGIN_NAME).enable();
    wireMockRule.resetAll();

    BundleUpload subject = j.getInstance().getExtensionList(BundleUpload.class).get(0);

    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    config.setEmail(TEST_EMAIL);
    config.setValid(true);
    config.setAcceptToS(true);

    stubFor(get(urlEqualTo("/api/health"))
        .willReturn(aResponse()
            .withStatus(200)));

    stubFor(post(urlEqualTo(format("/api/users/%s/upload/%s", TEST_EMAIL, j.getInstance().getLegacyInstanceId())))
        .willReturn(aResponse()
            .withStatus(500)));

    subject.run();

    // hack as wiremock doesn't seem to handle async requests
    while(wireMockRule.getAllServeEvents().size() < 2) {
      Thread.sleep(1000L);
    }

    WebClient w = j.createWebClient();
    HtmlPage managePage = w.goTo("manage");
    String text = managePage.asText();
    assertTrue(text.contains(textPrefix));
  }

  @Test
  public void testPluginDisabled() throws Exception {
    j.getPluginManager().getPlugin(AdvisorGlobalConfiguration.PLUGIN_NAME).disable();

    WebClient w = j.createWebClient();
    HtmlPage managePage = w.goTo("manage");
    assertFalse(managePage.asText().contains("Successfully uploaded a bundle"));
  }

}