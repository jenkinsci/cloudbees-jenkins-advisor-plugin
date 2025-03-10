package com.cloudbees.jenkins.plugins.advisor;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class BundleUploadMonitorTest {

    private static final String TEST_EMAIL = "test@cloudbees.com";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final String textPrefix = "Jenkins Health Advisor by CloudBees failed!";

    @BeforeEach
    void setup() {
        // Dynamically configure the Advisor Server URL to reach WireMock server
        System.setProperty(
                "com.cloudbees.jenkins.plugins.advisor.client.AdvisorClientConfig.advisorURL", wireMock.url("/"));
    }

    @Test
    void testBundleUploadSuccess(JenkinsRule j) throws Exception {
        // just in case the disable test is run first
        j.getPluginManager().getPlugin(AdvisorGlobalConfiguration.PLUGIN_NAME).enable();
        wireMock.resetAll();

        BundleUpload subject =
                j.getInstance().getExtensionList(BundleUpload.class).get(0);

        AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
        config.setAcceptToS(true);
        config.setEmail(TEST_EMAIL);
        assertTrue(config.isValid(), "The configuration must be valid");

        wireMock.stubFor(get(urlEqualTo("/api/health")).willReturn(aResponse().withStatus(200)));

        wireMock.stubFor(post(urlEqualTo(format(
                        "/api/users/%s/upload/%s", TEST_EMAIL, j.getInstance().getLegacyInstanceId())))
                .willReturn(aResponse().withStatus(200)));

        subject.run();

        // hack as wiremock doesn't seem to handle async requests
        while (wireMock.getAllServeEvents().size() < 2) {
            Thread.sleep(1000L);
        }

        WebClient w = j.createWebClient();
        HtmlPage managePage = w.goTo("manage");
        assertFalse(managePage.asNormalizedText().contains(textPrefix));
    }

    @Test
    void testErrorUploadingBundle(JenkinsRule j) throws Exception {
        // just in case the disable test is run first
        j.getPluginManager().getPlugin(AdvisorGlobalConfiguration.PLUGIN_NAME).enable();
        wireMock.resetAll();

        BundleUpload subject =
                j.getInstance().getExtensionList(BundleUpload.class).get(0);

        AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
        config.setAcceptToS(true);
        config.setEmail(TEST_EMAIL);
        assertTrue(config.isValid(), "The configuration must be valid");

        wireMock.stubFor(get(urlEqualTo("/api/health")).willReturn(aResponse().withStatus(200)));

        wireMock.stubFor(post(urlEqualTo(format(
                        "/api/users/%s/upload/%s", TEST_EMAIL, j.getInstance().getLegacyInstanceId())))
                .willReturn(aResponse().withStatus(500)));

        subject.run();

        // hack as wiremock doesn't seem to handle async requests
        while (wireMock.getAllServeEvents().size() < 2) {
            Thread.sleep(1000L);
        }

        WebClient w = j.createWebClient();
        HtmlPage managePage = w.goTo("manage");
        String text = managePage.asNormalizedText();
        assertTrue(text.contains(textPrefix));
    }

    @Test
    void testPluginDisabled(JenkinsRule j) throws Exception {
        j.getPluginManager().getPlugin(AdvisorGlobalConfiguration.PLUGIN_NAME).disable();

        WebClient w = j.createWebClient();
        HtmlPage managePage = w.goTo("manage");
        assertFalse(managePage.asNormalizedText().contains("Successfully uploaded a bundle"));
    }
}
