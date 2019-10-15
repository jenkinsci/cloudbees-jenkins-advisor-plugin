package com.cloudbees.jenkins.plugins.advisor.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmailValidatorTest {

  @Test
  public void validEmails() {
    assertTrue("simple email", EmailValidator.isValid("jdoe@acme.com"));
    assertTrue("email with tag", EmailValidator.isValid("jdoe+advisor@acme.com"));
    // Not supported
    //assertTrue("email with name", emailValidator.isValid("John Doe <jdoe@acme.com>"));
  }

  @Test
  public void invalidEmails() {
    assertFalse("email is empty", EmailValidator.isValid(""));
    assertFalse("not an email", EmailValidator.isValid("Not valid"));
    assertFalse("email with not root DN", EmailValidator.isValid("jdoe@acme"));
  }

}
