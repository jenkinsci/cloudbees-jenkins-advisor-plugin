package com.cloudbees.jenkins.plugins.advisor.webhook;

import com.cloudbees.jenkins.plugins.advisor.AdvisorGlobalConfiguration;
import hudson.util.Secret;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

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
    Secret found = AdvisorWebhookCredentialsManager.secretFor(AdvisorGlobalConfiguration.getInstance().getCredentialsId());
    checkNotNull(found);
    return new CalculateValidSignature(fromPost.trim(), found);
  }

  public boolean matches(String digest) {
    // compute hash
    String computed = encode().trim();

    // String comparison
    return computed.equals(digest);
  }

  private String encode() {
    try {
      final SecretKeySpec keySpec = new SecretKeySpec(Secret.toString(secret).getBytes(UTF_8), HMAC_SHA1_ALGORITHM);
      final Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
      mac.init(keySpec);
      final byte[] rawHMACBytes = mac.doFinal(payload.getBytes(UTF_8));

      return Hex.encodeHexString(rawHMACBytes);
    } catch (Exception e) {
      return INVALID_SIGNATURE;
    }
  }

}