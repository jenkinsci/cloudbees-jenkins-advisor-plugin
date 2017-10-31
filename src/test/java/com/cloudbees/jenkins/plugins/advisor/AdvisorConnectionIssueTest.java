package com.cloudbees.jenkins.plugins.advisor;

import com.cloudbees.jenkins.plugins.advisor.client.model.AccountCredentials;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import java.io.InputStream;
import java.util.Properties;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;

public class AdvisorConnectionIssueTest {

  @Rule
  public JenkinsRule j = new JenkinsRule();
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(9999));

  private String blurb;
  private String email = "test@test.test";
  private String password = "testPassword";
  private String wrongPassword = "sosowrong"; 

  @Before
  public void setup() throws Exception {
      Properties props = new Properties();
      InputStream input = AdvisorConnectionIssue.class.getResourceAsStream("AdvisorConnectionIssue/message.properties");
      props.load(input);
      blurb = props.getProperty("ConnectionIssue");
  }

  @Test
  public void visitConfigPage() throws Exception {
    stubFor(post(urlEqualTo("/login"))
      .withHeader("Content-Type", WireMock.equalTo("application/json"))
      .withRequestBody(equalToJson(new Gson().toJson(new AccountCredentials(email, password))))
      .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Authorization", "Bearer 327hfeaw7ewa9")));

    stubFor(post(urlEqualTo("/login"))
      .withHeader("Content-Type", WireMock.equalTo("application/json"))
      .withRequestBody(equalToJson(new Gson().toJson(new AccountCredentials(email, wrongPassword))))
      .willReturn(aResponse()
          .withStatus(404)));

    // just in case the disable test is run first
    j.getPluginManager().getPlugin(AdvisorGlobalConfiguration.PLUGIN_NAME).enable();

    // page doesn't display the warning
    WebClient w = j.createWebClient();
    String part = AdvisorGlobalConfiguration.getInstance().getUrlName();
    
    HtmlPage managePage = w.goTo("manage");
    assertFalse(managePage.asText().contains(blurb));

    // page shows warning
    submitForm(w, part, email, wrongPassword, false);
    managePage = w.goTo("manage");
    assertTrue(managePage.asText().contains(blurb));

    // page shows warning
    submitForm(w, part, email, password, false);
    managePage = w.goTo("manage");
    assertFalse(managePage.asText().contains(blurb));
  }

  private void submitForm(WebClient wc, String part, String userEmail, String userPassword, boolean nagOff) throws Exception {
      HtmlForm form = (HtmlForm) (wc.goTo(part).getFirstByXPath("//form[@action='configure']"));
      form.getInputByName("_.email").setValueAttribute(userEmail);
      form.getInputByName("_.password").setValueAttribute(userPassword);
      form.getInputByName("_.nagDisabled").setChecked(nagOff);
      j.submit(form);
  }

}