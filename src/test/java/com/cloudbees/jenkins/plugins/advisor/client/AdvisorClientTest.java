package com.cloudbees.jenkins.plugins.advisor.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.cloudbees.jenkins.plugins.advisor.client.model.ClientResponse;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientUploadRequest;
import com.cloudbees.jenkins.plugins.advisor.client.model.Recipient;
import com.cloudbees.jenkins.plugins.advisor.utils.EmailUtil;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class AdvisorClientTest {

    private static final String TEST_EMAIL = "test@acme.com";
    private static final List<Recipient> TEST_CC = Collections.singletonList(new Recipient(TEST_EMAIL));
    private static final String TEST_INSTANCE_ID = "12345";
    private static final String TEST_PLUGIN_VERSION = "2.9";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final Recipient recipient = new Recipient(TEST_EMAIL);
    private final AdvisorClient subject = new AdvisorClient(recipient);

    @BeforeEach
    void setup() {
        // Dynamically configure the Advisor Server URL to reach WireMock server
        System.setProperty(
                "com.cloudbees.jenkins.plugins.advisor.client.AdvisorClientConfig.advisorURL", wireMock.url("/"));
    }

    @Test
    void testDoCheckHealth() {
        stubHealth();
        String token = subject.doCheckHealth();

        assertThat(token, is(AdvisorClient.HEALTH_SUCCESS));
    }

    @Test
    void testDoTestEmail() {
        wireMock.stubFor(post(urlEqualTo("/api/test/emails"))
                .withRequestBody(equalTo("{\"email\":\"test@acme.com\"}"))
                .willReturn(aResponse().withStatus(200)));
        String token = subject.doTestEmail();

        assertThat(token, is(AdvisorClient.EMAIL_SUCCESS));
    }

    @Test
    void uploadFile() {
        stubHealth();
        stubUpload();

        File bundle = new File(getClass().getResource("/bundle.zip").getFile());
        ClientResponse response =
                subject.uploadFile(new ClientUploadRequest(TEST_INSTANCE_ID, bundle, null, TEST_PLUGIN_VERSION));

        assertThat(response.getCode(), is(200));
    }

    @Test
    void uploadFileWithCC() {
        stubHealth();
        stubUploadCc(TEST_CC);

        File bundle = new File(getClass().getResource("/bundle.zip").getFile());
        ClientResponse response =
                subject.uploadFile(new ClientUploadRequest(TEST_INSTANCE_ID, bundle, TEST_CC, TEST_PLUGIN_VERSION));

        assertThat(response.getCode(), is(200));
    }

    @Test
    void uploadFileWithCCMultipleRecipients() {
        List<Recipient> cc = Arrays.asList(new Recipient(TEST_EMAIL), new Recipient(TEST_EMAIL));
        stubHealth();
        stubUploadCc(cc);

        File bundle = new File(getClass().getResource("/bundle.zip").getFile());
        ClientResponse response =
                subject.uploadFile(new ClientUploadRequest(TEST_INSTANCE_ID, bundle, cc, TEST_PLUGIN_VERSION));

        assertThat(response.getCode(), is(200));
    }

    private static void stubHealth() {
        wireMock.stubFor(get(urlEqualTo("/api/health"))
                // .withHeader("Content-Type", WireMock.equalTo("application/json"))
                .willReturn(aResponse().withStatus(200)));
    }

    private static void stubUpload() {
        wireMock.stubFor(post(urlEqualTo(format("/api/users/%s/upload/%s", TEST_EMAIL, TEST_INSTANCE_ID)))
                .willReturn(aResponse().withStatus(200)));
    }

    private static void stubUploadCc(List<Recipient> cc) {
        wireMock.stubFor(post(urlEqualTo(format(
                        "/api/users/%s/upload/%s?cc=%s",
                        TEST_EMAIL,
                        TEST_INSTANCE_ID,
                        EmailUtil.urlEncode(cc.stream().map(Recipient::getEmail).collect(Collectors.joining(","))))))
                .willReturn(aResponse().withStatus(200)));
    }
}
