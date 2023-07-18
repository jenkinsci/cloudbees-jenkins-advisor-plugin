package com.cloudbees.jenkins.plugins.advisor;

import static com.cloudbees.jenkins.plugins.advisor.BundleUpload.BUNDLE_SUCCESSFULLY_UPLOADED;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.recipes.LocalData;
import org.jvnet.hudson.test.recipes.WithTimeout;

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
        System.setProperty(
                "com.cloudbees.jenkins.plugins.advisor.client.AdvisorClientConfig.advisorURL", wireMockRule.url("/"));
    }

    @WithTimeout(30)
    @Test
    public void execute() throws Exception {
        AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
        config.setEmail(TEST_EMAIL);
        config.setAcceptToS(true);
        assertTrue("The configuration must be valid", config.isValid());

        stubFor(get(urlEqualTo("/api/health")).willReturn(aResponse().withStatus(200)));

        stubFor(post(urlEqualTo(format(
                        "/api/users/%s/upload/%s", TEST_EMAIL, j.getInstance().getLegacyInstanceId())))
                .willReturn(aResponse().withStatus(200)));

        runBundleUpload();

        verify(getRequestedFor(urlEqualTo("/api/health")));

        verify(postRequestedFor(urlEqualTo(format(
                        "/api/users/%s/upload/%s", TEST_EMAIL, j.getInstance().getLegacyInstanceId())))
                .withHeader("Content-Type", WireMock.containing("multipart/form-data")));

        // Refresh the configuration?
        assertThat(config.getLastBundleResult(), containsString(BUNDLE_SUCCESSFULLY_UPLOADED));

        try (Stream<Path> children = Files.list(Paths.get(BundleUpload.TEMP_BUNDLE_DIRECTORY))) {
            assertThat(children.collect(Collectors.toList()), is(empty()));
        }
    }

    @Test
    @LocalData
    public void execute_pluginDisabled() {
        stubFor(any(anyUrl()));

        runBundleUpload();

        verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    public void execute_isNotValid() {
        AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
        assertFalse("The configuration must be valid", config.isValid());

        stubFor(any(anyUrl()));

        runBundleUpload();

        verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    public void execute_noConnection() {
        AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
        config.setAcceptToS(true);
        config.setEmail(TEST_EMAIL);
        assertTrue("The configuration must be valid", config.isValid());

        waitForWiremockShutdown();
        runBundleUpload();
    }

    private void waitForWiremockShutdown() {
        wireMockRule.shutdownServer();

        while (wireMockRule.isRunning()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @WithoutJenkins
    @Test
    public void getTempBundleDirectory() {
        assertThat(new BundleUpload().getTempBundleDirectory(), is(equalTo(BundleUpload.TEMP_BUNDLE_DIRECTORY)));
    }

    @WithoutJenkins
    @Test
    public void getRecurrencePeriod() {
        assertThat(
                new BundleUpload().getRecurrencePeriod(),
                is(equalTo(TimeUnit.HOURS.toMillis(BundleUpload.RECURRENCE_PERIOD_HOURS))));
    }

    @WithoutJenkins
    @Test
    public void getInitialDelay() {
        assertThat(
                new BundleUpload().getInitialDelay(),
                is(equalTo(TimeUnit.MINUTES.toMillis(BundleUpload.INITIAL_DELAY_MINUTES))));
    }

    /**
     * Runs the {@link BundleUpload} task and waits for it to finish.
     */
    private void runBundleUpload() {
        BundleUpload subject =
                j.getInstance().getExtensionList(BundleUpload.class).get(0);
        subject.execute(() -> System.out);
    }
}
