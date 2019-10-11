package com.cloudbees.jenkins.plugins.advisor;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import hudson.LocalPluginManager;
import hudson.PluginWrapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.recipes.WithPluginManager;
import org.jvnet.hudson.test.recipes.WithTimeout;

import javax.annotation.CheckForNull;
import java.io.File;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class BundleUploadTest {

  private static final String TEST_EMAIL = "test@acme.com";

  @Rule
  public final JenkinsRule j = new JenkinsRule();
  @Rule
  public final WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

  @Before
  public void setup() {
    wireMockRule.resetAll();
    // Dynamically configure the Advisor Server URL to reach WireMock server
    System.setProperty("com.cloudbees.jenkins.plugins.advisor.client.AdvisorClientConfig.advisorURL",
      wireMockRule.url("/"));
  }

  @WithTimeout(30)
  @Test
  public void execute() throws Exception {
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

    // wait for the AsyncPeriodicWork in BundleUpload to kick off
    while (wireMockRule.getAllServeEvents().size() < 2) {
      Thread.sleep(1000L);
    }

    verify(getRequestedFor(urlEqualTo("/api/health")));

    verify(
      postRequestedFor(urlEqualTo(format("/api/users/%s/upload/%s", TEST_EMAIL, j.getInstance().getLegacyInstanceId())))
        .withHeader("Content-Type", WireMock.containing("multipart/form-data")));

    // Refresh the configuration?
    assertThat(config.getLastBundleResult(), containsString("Successfully uploaded a bundle"));
  }

  @Test
  @WithPluginManager(DisabledPluginManager.class)
  public void execute_pluginDisabled() {
    BundleUpload subject = j.getInstance().getExtensionList(BundleUpload.class).get(0);

    stubFor(any(anyUrl()));

    subject.run();

    verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  public void execute_isNotValid() {
    BundleUpload subject = j.getInstance().getExtensionList(BundleUpload.class).get(0);
    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    config.setValid(false);

    stubFor(any(anyUrl()));

    subject.run();

    verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  public void execute_noConnection() {
    BundleUpload subject = j.getInstance().getExtensionList(BundleUpload.class).get(0);

    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    config.setEmail(TEST_EMAIL);
    config.setValid(true);

    wireMockRule.shutdownServer();

    subject.run();
  }

  @WithoutJenkins
  @Test
  public void getRecurrencePeriod() {
    assertThat(new BundleUpload().getRecurrencePeriod(),
      is(equalTo(TimeUnit.HOURS.toMillis(BundleUpload.RECURRENCE_PERIOD_HOURS))));
  }

  @WithoutJenkins
  @Test
  public void getInitialDelay() {
    assertThat(new BundleUpload().getInitialDelay(),
      is(equalTo(TimeUnit.MINUTES.toMillis(BundleUpload.INITIAL_DELAY_MINUTES))));
  }

  /**
   * Work around issues where the PluginManager doesn't have permission to save files
   */
  public static class DisabledPluginManager extends LocalPluginManager {
    public DisabledPluginManager(File rootDir) {
      super(rootDir);
    }

    @Override
    @CheckForNull
    public PluginWrapper getPlugin(String shortName) {
      PluginWrapper actual = super.getPlugin(shortName);
      if (shortName.equals(AdvisorGlobalConfiguration.PLUGIN_NAME)) {
        PluginWrapper pw = spy(actual);

        doReturn(false).when(pw).isEnabled();
        return pw;
      }
      return actual;
    }
  }

}
