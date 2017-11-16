package com.cloudbees.jenkins.plugins.advisor.webhook;

import hudson.util.Secret;

import static com.google.common.base.Preconditions.checkNotNull;

//https://github.com/jenkinsci/github-plugin/blob/master/src/main/java/org/jenkinsci/plugins/github/webhook/GHWebhookSignature.java
//https://github.com/jenkinsci/github-plugin/blob/master/src/main/java/org/jenkinsci/plugins/github/webhook/GHEventPayload.java

/**
 * Determines if the given Advisor payload is valid.
 */

public class CalculateValidSignature {

  private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
  public static final String INVALID_SIGNATURE = "COMPUTED_INVALID_SIGNATURE";

  private Secret secret;
  private String payload;

  private CalculateValidSignature(String payload, Secret secret) {
    this.payload = payload;
    this.secret = secret;
  }

  public static CalculateValidSignature setUp(String fromPost) {
    checkNotNull(fromPost);
    Secret found = AdvisorWebhookCredentialsManager.secretFor("To get from Webhook");
    checkNotNull(found);
    return new CalculateValidSignature(fromPost, found);
  }

  public boolean matches() {
    // compute hash
    String computed = decrypt();

    // String comparison
    return computed.equals(payload);
  }

  private String decrypt() {
      return INVALID_SIGNATURE;
  }

}