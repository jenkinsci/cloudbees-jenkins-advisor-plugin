package com.cloudbees.jenkins.plugins.advisor.utils;

import hudson.util.FormValidation;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.Optional;

public final class FormValidationHelper {

  private FormValidationHelper() {
    throw new UnsupportedOperationException("Cannot instantiate utility class");
  }

  public static Optional<FormValidation> validateCC(String cc) {
    if (cc != null && !cc.isEmpty()) {
      try {
        for (String ccEmail : cc.split(",")) {
          new InternetAddress(ccEmail).validate();
        }
      } catch (AddressException ex) {
        return Optional.of(FormValidation.error("Invalid cc email: " + ex.getMessage()));
      }
    }
    return Optional.empty();
  }
}
