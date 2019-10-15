package com.cloudbees.jenkins.plugins.advisor;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReminderTest {

  @Rule
  public final JenkinsRule j = new JenkinsRule();

  private String blurb;


  @Before
  public void setup() throws Exception {
    Properties props = new Properties();
    InputStream input = Reminder.class.getResourceAsStream("Reminder/message.properties");
    props.load(input);
    blurb = props.getProperty("Blurb");
  }

  @Test
  public void visitConfigPage() throws Exception {
    // just in case the disable test is run first
    j.getPluginManager().getPlugin(AdvisorGlobalConfiguration.PLUGIN_NAME).enable();

    // page displays the warning
    WebClient w = j.createWebClient();
    String part = AdvisorGlobalConfiguration.getInstance().getUrlName();

    HtmlPage managePage = w.goTo("manage");
    assertTrue(managePage.asText().contains(blurb));

    // page doesn't show warning
    submitForm(w, part, "test@test.test", false, true);
    managePage = w.goTo("manage");
    assertFalse(managePage.asText().contains(blurb));

    // page shows warning
    submitForm(w, part, "", false, true);
    managePage = w.goTo("manage");
    assertTrue(managePage.asText().contains(blurb));

    // page doesn't show warning
    submitForm(w, part, "", true, true);
    managePage = w.goTo("manage");
    assertFalse(managePage.asText().contains(blurb));

    // page shows warning
    submitForm(w, part, "", false, false);
    managePage = w.goTo("manage");
    assertTrue(managePage.asText().contains(blurb));

    // page doesn't show warning
    submitForm(w, part, "", true, false);
    managePage = w.goTo("manage");
    assertFalse(managePage.asText().contains(blurb));
  }

  @Test
  public void testPluginDisabled() throws Exception {
    j.getPluginManager().getPlugin(AdvisorGlobalConfiguration.PLUGIN_NAME).disable();

    WebClient w = j.createWebClient();
    HtmlPage managePage = w.goTo("manage");
    assertFalse(managePage.asText().contains(blurb));
  }

  private void submitForm(WebClient wc, String part, String userEmail, boolean nagOff, boolean acceptTerms) throws Exception {
    HtmlForm form = wc.goTo(part).getFirstByXPath("//form[@action='configure']");
    form.getInputByName("_.email").setValueAttribute(userEmail);
    form.getInputByName("_.nagDisabled").setChecked(nagOff);
    form.getInputByName("_.acceptToS").setChecked(acceptTerms);
    j.submit(form);
  }

}
