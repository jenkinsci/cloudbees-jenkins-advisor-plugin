package com.cloudbees.jenkins.plugins.advisor.utils;

import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmailValidatorTest {

  private final String email = "test@cloudbees.com";

  @Test
  public void validEmails() {
    assertTrue("simple email", EmailValidator.isValidEmail("jdoe@acme.com"));
    assertTrue("email with tag", EmailValidator.isValidEmail("jdoe+advisor@acme.com"));
    assertTrue("JENKINS-65230 email", EmailValidator.isValidEmail("test.test-test@acme.com"));
    // Not supported
    //assertTrue("email with name", emailValidator.isValid("John Doe <jdoe@acme.com>"));
  }

  @Test
  public void invalidEmails() {
    assertFalse("email is empty", EmailValidator.isValidEmail(""));
  }

  @Test
  public void testDoCheckEmail() {
    FormValidation formValidation;

    formValidation = EmailValidator.validateEmail(null);
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

    formValidation = EmailValidator.validateEmail("");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

    formValidation = EmailValidator.validateEmail(";");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

    formValidation = EmailValidator.validateEmail(",");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

    formValidation = EmailValidator.validateEmail(email + ";");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

    formValidation = EmailValidator.validateEmail(email + ",");
    assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

    formValidation = EmailValidator.validateEmail(StringUtils.capitalize(email));
    assertEquals(FormValidation.Kind.OK, formValidation.kind);
    formValidation = EmailValidator.validateEmail(email);
    assertEquals(FormValidation.Kind.OK, formValidation.kind);

    formValidation = EmailValidator.validateEmail("test.foo@cloudbees.com");
    assertEquals(FormValidation.Kind.OK, formValidation.kind);
    formValidation = EmailValidator.validateEmail("test-foo@cloudbees.com");
    assertEquals(FormValidation.Kind.OK, formValidation.kind);
    formValidation = EmailValidator.validateEmail("test_foo@cloudbees.com");
    assertEquals(FormValidation.Kind.OK, formValidation.kind);
    formValidation = EmailValidator.validateEmail("test+foo@cloudbees.com");
    assertEquals(FormValidation.Kind.OK, formValidation.kind);
  }

}
