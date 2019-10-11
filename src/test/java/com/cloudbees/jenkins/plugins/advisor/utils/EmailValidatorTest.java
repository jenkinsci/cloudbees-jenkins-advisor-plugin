package com.cloudbees.jenkins.plugins.advisor.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmailValidatorTest {

  private final EmailValidator emailValidator = EmailValidator.getInstance();

  @Test
  public void validEmails() {
    assertTrue("simple email", emailValidator.isValid("jdoe@acme.com"));
    assertTrue("email with tag", emailValidator.isValid("jdoe+advisor@acme.com"));
    // Not supported
    //assertTrue("email with name", emailValidator.isValid("John Doe <jdoe@acme.com>"));
  }

  @Test
  public void invalidEmails() {
    assertFalse("email is empty", emailValidator.isValid(""));
    assertFalse("not an email", emailValidator.isValid("Not valid"));
    assertFalse("email with not root DN", emailValidator.isValid("jdoe@acme"));
  }

}
