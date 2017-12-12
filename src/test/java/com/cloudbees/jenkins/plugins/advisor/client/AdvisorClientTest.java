package com.cloudbees.jenkins.plugins.advisor.client;

import com.cloudbees.jenkins.plugins.advisor.client.model.AccountCredentials;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientUploadRequest;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.ning.http.client.Response;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AdvisorClientTest {

  private static final String TEST_EMAIL = "test";
  private static final String TEST_INSTANCE_ID = "12345";

  private final AccountCredentials accountCredentials = new AccountCredentials(TEST_EMAIL);

  private final AdvisorClient subject = new AdvisorClient(accountCredentials);

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(9999));

  @Test
  public void testDoCheckHealth() throws Exception {
    stubHealth();
    String token = subject.doCheckHealth().get();

    assertThat(token, is(AdvisorClient.HEALTH_SUCCESS));
  }

  @Test
  public void uploadFile() throws Exception {
    stubHealth();
    stubUpload();

    File bundle = new File(getClass().getResource("/bundle.zip").getFile());
    Response response = subject.uploadFile(new ClientUploadRequest(TEST_INSTANCE_ID, bundle)).get();

    assertThat(response.getStatusCode(), is(200));
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

}