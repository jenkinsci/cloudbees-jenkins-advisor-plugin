package com.cloudbees.jenkins.plugins.advisor.utils;

import hudson.Util;

public final class EmailUtil {

  private EmailUtil() {
    throw new UnsupportedOperationException("Cannot instantiate utility class");
  }

  public static String fixEmptyAndTrimAllSpaces(String value) {
    String emailAddress = Util.fixEmptyAndTrim(value);
    if (emailAddress != null) {
      emailAddress = emailAddress.replaceAll(" ", "");
    }
    return emailAddress;
  }

  public static String urlEncode(String value) {
    if (value != null) {
      value = value.replaceAll(",", "%2C");
    }
    return value;
  }
}
