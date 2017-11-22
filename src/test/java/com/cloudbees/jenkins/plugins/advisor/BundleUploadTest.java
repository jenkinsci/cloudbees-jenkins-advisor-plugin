package com.cloudbees.jenkins.plugins.advisor;

import com.cloudbees.jenkins.plugins.advisor.client.model.AccountCredentials;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import hudson.model.TaskListener;
import hudson.util.LogTaskListener;
import hudson.util.Secret;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.recipes.WithTimeout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class BundleUploadTest {

  private static final String TEST_EMAIL = "test";
  private static final String TEST_PASSWORD = "password";
  private static final String TEST_TOKEN = "token";
  private static final TaskListener NOOP_LISTENER = new LogTaskListener(Logger.getLogger(BundleUploadTest.class.getName()), Level.INFO);

  private static final Logger LOG = Logger.getLogger(BundleUpload.class.getName());
  private static OutputStream logCapturingStream;
  private static StreamHandler customLogHandler;

  @Rule
  public JenkinsRule j = new JenkinsRule();
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(9999));


  @Before
  public void attachLogCapture() {
    logCapturingStream = new ByteArrayOutputStream();
    Handler[] handlers = LOG.getParent().getHandlers();
    customLogHandler = new StreamHandler(logCapturingStream, handlers[0].getFormatter());
    LOG.addHandler(customLogHandler);
    LOG.setLevel(Level.FINEST);
  }

  private static String getTestCapturedLog() throws IOException {
    customLogHandler.flush();
    return logCapturingStream.toString();
  }

  @WithTimeout(30)
  @Test
  public void execute() throws Exception {
    BundleUpload subject = j.getInstance().getExtensionList(BundleUpload.class).get(0);

    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    config.setEmail(TEST_EMAIL);
    config.setPassword(Secret.fromString(TEST_PASSWORD));
    config.setValid(true);

    Gson gson = new Gson();
    stubFor(post(urlEqualTo("/login"))
        .withHeader("Content-Type", WireMock.equalTo("application/json"))
        .withRequestBody(equalToJson(gson.toJson(new AccountCredentials(TEST_EMAIL, TEST_PASSWORD))))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Authorization", "Bearer " + TEST_TOKEN)));

    stubFor(post(urlEqualTo(format("/api/users/%s/upload/%s", TEST_EMAIL, j.getInstance().getLegacyInstanceId())))
        .withHeader("Authorization", WireMock.equalTo(TEST_TOKEN))
        .willReturn(aResponse()
            .withStatus(200)));

    subject.execute(NOOP_LISTENER);

    // hack as wiremock doesn't seem to handle async requests
    while(wireMockRule.getAllServeEvents().size() < 2) {
      Thread.sleep(1000L);
    }

    verify(postRequestedFor(urlEqualTo("/login"))
        .withHeader("Content-Type", WireMock.equalTo("application/json")));

    verify(postRequestedFor(urlEqualTo(format("/api/users/%s/upload/%s", TEST_EMAIL, j.getInstance().getLegacyInstanceId())))
        .withHeader("Content-Type", WireMock.containing("multipart/form-data")));
  }

  @Test
  public void execute_pluginDisabled() throws Exception {
    BundleUpload subject = j.getInstance().getExtensionList(BundleUpload.class).get(0);
    j.getPluginManager().getPlugin(AdvisorGlobalConfiguration.PLUGIN_NAME).disable();

    stubFor(any(anyUrl()));

    subject.execute(NOOP_LISTENER);

    verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  public void execute_isNotValid() throws Exception {
    BundleUpload subject = j.getInstance().getExtensionList(BundleUpload.class).get(0);
    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    config.setValid(false);

    stubFor(any(anyUrl()));

    subject.execute(NOOP_LISTENER);

    verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  public void execute_noConnection() throws Exception {
    BundleUpload subject = j.getInstance().getExtensionList(BundleUpload.class).get(0);

    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    config.setEmail(TEST_EMAIL);
    config.setPassword(Secret.fromString(TEST_PASSWORD));
    config.setValid(true);

    wireMockRule.shutdownServer();

    subject.execute(NOOP_LISTENER);

    assertThat(getTestCapturedLog(), containsString("SEVERE: Issue while uploading file to bundle upload service: " +
        "Execution exception trying to get bearer token from authentication request. " +
        "Message: java.util.concurrent.ExecutionException: java.net.ConnectException: Connection refused"));
  }

  @WithoutJenkins
  @Test
  public void getRecurrencePeriod() throws Exception {
    assertThat(new BundleUpload().getRecurrencePeriod(), is(equalTo(TimeUnit.HOURS.toMillis(BundleUpload.RECURRENCE_PERIOD_HOURS))));
  }

}