package com.cloudbees.jenkins.plugins.advisor.utils;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.util.FormValidation;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmailValidator {

  private static final String EMAIL_REGEX = "^[\\w-+]+(\\.[\\w]+)*@[\\w-]+(\\.[\\w]+)*(\\.[a-z]{2,})$";
  private static final Pattern PATTERN = Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE);

  private EmailValidator() {
    throw new UnsupportedOperationException("Cannot instantiate utility class");
  }

  public static boolean isValid(@NonNull String email) {
    Matcher matcher = PATTERN.matcher(email);
    return matcher.matches();
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
