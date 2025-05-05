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
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;
import org.jvnet.hudson.test.recipes.WithTimeout;

@WithJenkins
class BundleUploadTest {

    private static final String TEST_EMAIL = "test@acme.com";

    // instance variable as it is shutdown in #execute_noConnection
    @RegisterExtension
    private WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @BeforeEach
    void setup() {
        wireMock.resetAll();
        // Dynamically configure the Advisor Server URL to reach WireMock server
        System.setProperty(
                "com.cloudbees.jenkins.plugins.advisor.client.AdvisorClientConfig.advisorURL", wireMock.url("/"));
    }

    @WithTimeout(30)
    @Test
    void execute(JenkinsRule j) throws Exception {
        AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
        config.setEmail(TEST_EMAIL);
        config.setAcceptToS(true);
        assertTrue(config.isValid(), "The configuration must be valid");

        wireMock.stubFor(get(urlEqualTo("/api/health")).willReturn(aResponse().withStatus(200)));

        wireMock.stubFor(post(urlEqualTo(format(
                        "/api/users/%s/upload/%s", TEST_EMAIL, j.getInstance().getLegacyInstanceId())))
                .willReturn(aResponse().withStatus(200)));

        runBundleUpload(j);

        wireMock.verify(getRequestedFor(urlEqualTo("/api/health")));

        wireMock.verify(postRequestedFor(urlEqualTo(format(
                        "/api/users/%s/upload/%s", TEST_EMAIL, j.getInstance().getLegacyInstanceId())))
                .withHeader("Content-Type", WireMock.containing("multipart/form-data")));

        // Refresh the configuration?
        assertThat(config.getLastBundleResult(), containsString(BUNDLE_SUCCESSFULLY_UPLOADED));

        try (Stream<Path> children = Files.list(Paths.get(BundleUpload.TEMP_BUNDLE_DIRECTORY))) {
            assertThat(children.toList(), is(empty()));
        }
    }

    @Test
    @LocalData
    void execute_pluginDisabled(JenkinsRule j) {
        wireMock.stubFor(any(anyUrl()));

        runBundleUpload(j);

        wireMock.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void execute_isNotValid(JenkinsRule j) {
        AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
        assertFalse(config.isValid(), "The configuration must be valid");

        wireMock.stubFor(any(anyUrl()));

        runBundleUpload(j);

        wireMock.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void execute_noConnection(JenkinsRule j) throws Exception {
        AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
        config.setAcceptToS(true);
        config.setEmail(TEST_EMAIL);
        assertTrue(config.isValid(), "The configuration must be valid");

        waitForWiremockShutdown();
        runBundleUpload(j);
    }

    private void waitForWiremockShutdown() throws Exception {
        wireMock.shutdownServer();

        while (true) {
            try {
                wireMock.getPort();
                return;
            } catch (IllegalStateException ex) {
                Thread.sleep(50);
            }
        }
    }

    @WithoutJenkins
    @Test
    void getTempBundleDirectory() {
        assertThat(new BundleUpload().getTempBundleDirectory(), is(equalTo(BundleUpload.TEMP_BUNDLE_DIRECTORY)));
    }

    @WithoutJenkins
    @Test
    void getRecurrencePeriod() {
        assertThat(
                new BundleUpload().getRecurrencePeriod(),
                is(equalTo(TimeUnit.HOURS.toMillis(BundleUpload.RECURRENCE_PERIOD_HOURS))));
    }

    @WithoutJenkins
    @Test
    void getInitialDelay() {
        assertThat(
                new BundleUpload().getInitialDelay(),
                is(equalTo(TimeUnit.MINUTES.toMillis(BundleUpload.INITIAL_DELAY_MINUTES))));
    }

    @WithTimeout(30)
    @Test
    void protectsAgainstRogueServer(JenkinsRule j) {
        var config = AdvisorGlobalConfiguration.getInstance();
        config.setEmail(TEST_EMAIL);
        config.setAcceptToS(true);

        wireMock.stubFor(get(urlEqualTo("/api/health")).willReturn(aResponse().withStatus(200)));
        wireMock.stubFor(post(format(
                        "/api/users/%s/upload/%s", TEST_EMAIL, j.getInstance().getLegacyInstanceId()))
                .willReturn(aResponse().withStatus(201).withBody("<img/src/onerror=alert(1)>")));

        runBundleUpload(j);

        // The stored response is properly escaped
        assertThat(config.getLastBundleResult(), containsString("<code>201 - &lt;img/src/onerror=alert(1)&gt;</code>"));
    }

    /**
     * Runs the {@link BundleUpload} task and waits for it to finish.
     */
    private static void runBundleUpload(JenkinsRule j) {
        BundleUpload subject =
                j.getInstance().getExtensionList(BundleUpload.class).get(0);
        subject.execute(() -> System.out);
    }
}
