package com.cloudbees.jenkins.plugins.advisor.webhook;

import com.cloudbees.jenkins.plugins.advisor.client.model.AdvisorWebhookCredentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import hudson.model.Item;
import jenkins.model.Jenkins;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import java.util.logging.Logger;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.always;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.filter;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;


public class AdvisorWebhookCredentialsManager {

    public static void listAllCredentials(Logger logger) {
        List<AdvisorWebhookCredentials> creds = filter(
            lookupCredentials(AdvisorWebhookCredentials.class, Jenkins.getInstance(), 
                ACL.SYSTEM, Collections.<DomainRequirement>emptyList()),
                always());
        for(AdvisorWebhookCredentials awc : creds) {
            logger.info("listAllCredentials getId() " + awc.getId());
        }
    }

  public static ListBoxModel populateDropdown(String selectedId) {
    StandardListBoxModel result = new StandardListBoxModel();

    return result
      .includeEmptyValue()
      .includeAs(ACL.SYSTEM, Jenkins.getInstance(), AdvisorWebhookCredentials.class, Collections.<DomainRequirement>emptyList())
      .includeCurrentValue(selectedId);
  }

  public static boolean credentialsExists(String idLookup) {
    List<AdvisorWebhookCredentials> creds = filter(
        lookupCredentials(AdvisorWebhookCredentials.class, Jenkins.getInstance(), 
            ACL.SYSTEM, Collections.<DomainRequirement>emptyList()),
        withId(trimToEmpty(idLookup))
      );
  
      return !creds.isEmpty();
  }

  /**
   * Tries to find the credentials by id and returns secret from it.
   *
   * @param credentialsId id to find creds
   * @return secret from creds or empty optional
   */
  @Nonnull
  public static Secret secretFor(String credentialsId) {
    List<AdvisorWebhookCredentials> creds = filter(
      lookupCredentials(AdvisorWebhookCredentials.class, Jenkins.getInstance(),
          ACL.SYSTEM, Collections.<DomainRequirement>emptyList()),
      withId(trimToEmpty(credentialsId))
    );

    try {
        if(!creds.isEmpty()) {
            return creds.get(0).getAdvisorServiceSecret();
        } else {
            return null;
        }
    } catch (Exception ex) {
        return null;
    }
    
  }

}