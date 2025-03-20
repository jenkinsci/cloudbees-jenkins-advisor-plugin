package com.cloudbees.jenkins.plugins.advisor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Properties;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ReminderTest {

    private String blurb;

    @BeforeEach
    void setup() throws Exception {
        Properties props = new Properties();
        InputStream input = Reminder.class.getResourceAsStream("Reminder/message.properties");
        props.load(input);
        blurb = props.getProperty("Blurb");
    }

    @Test
    void visitConfigPage(JenkinsRule j) throws Exception {
        // just in case the disable test is run first
        j.getPluginManager().getPlugin(AdvisorGlobalConfiguration.PLUGIN_NAME).enable();

        // page displays the warning
        WebClient w = j.createWebClient();
        String part = AdvisorGlobalConfiguration.getInstance().getUrlName();

        HtmlPage managePage = w.goTo("manage");
        assertTrue(managePage.asNormalizedText().contains(blurb));

        // page doesn't show warning
        submitForm(j, w, part, "test@test.test", false, true);
        managePage = w.goTo("manage");
        assertFalse(managePage.asNormalizedText().contains(blurb));

        // page shows warning
        submitForm(j, w, part, "", false, true);
        managePage = w.goTo("manage");
        assertTrue(managePage.asNormalizedText().contains(blurb));

        // page doesn't show warning
        submitForm(j, w, part, "", true, true);
        managePage = w.goTo("manage");
        assertFalse(managePage.asNormalizedText().contains(blurb));

        // page shows warning
        submitForm(j, w, part, "", false, false);
        managePage = w.goTo("manage");
        assertTrue(managePage.asNormalizedText().contains(blurb));

        // page doesn't show warning
        submitForm(j, w, part, "", true, false);
        managePage = w.goTo("manage");
        assertFalse(managePage.asNormalizedText().contains(blurb));
    }

    @Test
    void testPluginDisabled(JenkinsRule j) throws Exception {
        j.getPluginManager().getPlugin(AdvisorGlobalConfiguration.PLUGIN_NAME).disable();

        WebClient w = j.createWebClient();
        HtmlPage managePage = w.goTo("manage");
        assertFalse(managePage.asNormalizedText().contains(blurb));
    }

    private static void submitForm(
            JenkinsRule j, WebClient wc, String part, String userEmail, boolean nagOff, boolean acceptTerms)
            throws Exception {
        HtmlForm form = wc.goTo(part).getFirstByXPath("//form[@action='configure']");
        form.getInputByName("_.email").setValue(userEmail);
        form.getInputByName("_.nagDisabled").setChecked(nagOff);
        form.getInputByName("_.acceptToS").setChecked(acceptTerms);
        j.submit(form);
    }
}
