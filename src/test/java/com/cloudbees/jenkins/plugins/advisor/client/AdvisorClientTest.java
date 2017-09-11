package com.cloudbees.jenkins.plugins.advisor.client;

import com.cloudbees.jenkins.plugins.advisor.client.model.AccountCredentials;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientUploadRequest;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import org.asynchttpclient.Response;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class AdvisorClientTest {

  private static final String TEST_EMAIL = "test";
  private static final String TEST_PASSWORD = "password";
  private static final String TEST_TOKEN = "token";
  private static final String TEST_INSTANCE_ID = "12345";

  private final AccountCredentials accountCredentials = new AccountCredentials(TEST_EMAIL, TEST_PASSWORD,null, -1, null, null, null);

  private final AdvisorClient subject = new AdvisorClient(accountCredentials);

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(9999));

  @Test
  public void doAuthenticate() throws Exception {
    stubAuth();

    String token = subject.doAuthenticate().get();

    assertThat(token, is(TEST_TOKEN));
  }

  @Test
  public void uploadFile() throws Exception {
    stubAuth();
    stubUpload();

    File bundle = new File(getClass().getResource("/bundle.zip").getFile());
    Response response = subject.uploadFile(new ClientUploadRequest(TEST_INSTANCE_ID, bundle)).get();

    assertThat(response.getStatusCode(), is(200));
  }

  private void stubAuth() {
    Gson gson = new Gson();
    stubFor(post(urlEqualTo("/login"))
        .withHeader("Content-Type", WireMock.equalTo("application/json"))
        .withRequestBody(equalToJson(gson.toJson(accountCredentials)))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Authorization", "Bearer " + TEST_TOKEN)));
  }

  private void stubUpload() {
    stubFor(post(urlEqualTo(format("/api/users/%s/upload/%s", TEST_EMAIL, TEST_INSTANCE_ID)))
        .withHeader("Authorization", WireMock.equalTo(TEST_TOKEN))
        .willReturn(aResponse()
            .withStatus(200)));
  }

}