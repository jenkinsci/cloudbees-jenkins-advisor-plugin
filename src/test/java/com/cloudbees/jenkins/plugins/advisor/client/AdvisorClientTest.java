package com.cloudbees.jenkins.plugins.advisor.client;

import com.cloudbees.jenkins.plugins.advisor.client.model.Recipient;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientResponse;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientUploadRequest;
import com.cloudbees.jenkins.plugins.advisor.utils.EmailUtil;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AdvisorClientTest {

  private static final String TEST_EMAIL = "test@acme.com";
  private static final String TEST_INSTANCE_ID = "12345";
  private static final String TEST_PLUGIN_VERSION = "2.9";

  private final Recipient recipient = new Recipient(TEST_EMAIL);

  private final AdvisorClient subject = new AdvisorClient(recipient);

  @Rule
  public final WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

  @Before
  public void setup() {
    // Dynamically configure the Advisor Server URL to reach WireMock server
    System.setProperty("com.cloudbees.jenkins.plugins.advisor.client.AdvisorClientConfig.advisorURL",
            wireMockRule.url("/"));
  }

  @Test
  public void testDoCheckHealth() {
    stubHealth();
    String token = subject.doCheckHealth();

    assertThat(token, is(AdvisorClient.HEALTH_SUCCESS));
  }

  @Test
  public void testDoTestEmail() {
    stubFor(get(urlEqualTo(format("/api/test/emails/%s", TEST_EMAIL)))
        .willReturn(aResponse()
            .withStatus(200)));
    String token = subject.doTestEmail();

    assertThat(token, is(AdvisorClient.EMAIL_SUCCESS));
  }

  @Test
  public void uploadFile() {
    stubHealth();
    stubUpload();

    File bundle = new File(getClass().getResource("/bundle.zip").getFile());
    ClientResponse response = subject.uploadFile(new ClientUploadRequest(TEST_INSTANCE_ID, bundle, null, TEST_PLUGIN_VERSION));

    assertThat(response.getCode(), is(200));
  }

  @Test
  public void uploadFileWithCC() {
    stubHealth();
    stubUploadCc(TEST_EMAIL);

    File bundle = new File(getClass().getResource("/bundle.zip").getFile());
    ClientResponse response = subject.uploadFile(new ClientUploadRequest(TEST_INSTANCE_ID, bundle, TEST_EMAIL, TEST_PLUGIN_VERSION));

    assertThat(response.getCode(), is(200));
  }

  @Test
  public void uploadFileWithCCMultipleRecipients() {
    String cc = TEST_EMAIL + "," + TEST_EMAIL;
    stubHealth();
    stubUploadCc(cc);

    File bundle = new File(getClass().getResource("/bundle.zip").getFile());
    ClientResponse response = subject.uploadFile(new ClientUploadRequest(TEST_INSTANCE_ID, bundle, cc, TEST_PLUGIN_VERSION));

    assertThat(response.getCode(), is(200));
  }

  private void stubHealth() {
    stubFor(get(urlEqualTo("/api/health"))
        //.withHeader("Content-Type", WireMock.equalTo("application/json"))
        .willReturn(aResponse()
            .withStatus(200)));
  }

  private void stubUpload() {
    stubFor(post(urlEqualTo(format("/api/users/%s/upload/%s", TEST_EMAIL, TEST_INSTANCE_ID)))
        .willReturn(aResponse()
            .withStatus(200)));
  }

  private void stubUploadCc(String cc) {
    stubFor(post(urlEqualTo(format("/api/users/%s/upload/%s?cc=%s", TEST_EMAIL, TEST_INSTANCE_ID, EmailUtil.urlEncode(cc))))
        .willReturn(aResponse()
            .withStatus(200)));
  }

}
