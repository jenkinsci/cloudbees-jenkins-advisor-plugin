package com.cloudbees.jenkins.plugins.advisor;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class BundleUploadTest {

  private static final String TEST_EMAIL = "test";

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
    wireMockRule.resetAll();
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
    config.setValid(true);

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

    verify(getRequestedFor(urlEqualTo("/api/health")));

    verify(postRequestedFor(urlEqualTo(format("/api/users/%s/upload/%s", TEST_EMAIL, j.getInstance().getLegacyInstanceId())))
        .withHeader("Content-Type", WireMock.containing("multipart/form-data")));
  }

  @Test
  public void execute_pluginDisabled() throws Exception {
    BundleUpload subject = j.getInstance().getExtensionList(BundleUpload.class).get(0);
    j.getPluginManager().getPlugin(AdvisorGlobalConfiguration.PLUGIN_NAME).disable();

    stubFor(any(anyUrl()));

    subject.run();

    verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  public void execute_isNotValid() throws Exception {
    BundleUpload subject = j.getInstance().getExtensionList(BundleUpload.class).get(0);
    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    config.setValid(false);

    stubFor(any(anyUrl()));

    subject.run();

    verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  public void execute_noConnection() throws Exception {
    BundleUpload subject = j.getInstance().getExtensionList(BundleUpload.class).get(0);

    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    config.setEmail(TEST_EMAIL);
    config.setValid(true);

    wireMockRule.shutdownServer();

    subject.run();
  }

  @WithoutJenkins
  @Test
  public void getRecurrencePeriod() throws Exception {
    assertThat(new BundleUpload().getRecurrencePeriod(), is(equalTo(TimeUnit.HOURS.toMillis(BundleUpload.RECURRENCE_PERIOD_HOURS))));
  }

}