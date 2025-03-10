package com.cloudbees.jenkins.plugins.advisor.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;

class EmailValidatorTest {

    private static final String EMAIL = "test@cloudbees.com";

    @Test
    void validEmails() {
        assertTrue(EmailValidator.isValidEmail("jdoe@acme.com"), "simple email");
        assertTrue(EmailValidator.isValidEmail("jdoe+advisor@acme.com"), "email with tag");
        assertTrue(EmailValidator.isValidEmail("test.test-test@acme.com"), "JENKINS-65230 email");
        // Not supported
        // assertTrue("email with name", emailValidator.isValid("John Doe <jdoe@acme.com>"));
    }

    @Test
    void invalidEmails() {
        assertFalse(EmailValidator.isValidEmail(""), "email is empty");
    }

    @Test
    void testDoCheckEmail() {
        FormValidation formValidation;

        formValidation = EmailValidator.validateEmail(null);
        assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

        formValidation = EmailValidator.validateEmail("");
        assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

        formValidation = EmailValidator.validateEmail(";");
        assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

        formValidation = EmailValidator.validateEmail(",");
        assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

        formValidation = EmailValidator.validateEmail(EMAIL + ";");
        assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

        formValidation = EmailValidator.validateEmail(EMAIL + ",");
        assertEquals(FormValidation.Kind.ERROR, formValidation.kind);

        formValidation = EmailValidator.validateEmail(StringUtils.capitalize(EMAIL));
        assertEquals(FormValidation.Kind.OK, formValidation.kind);
        formValidation = EmailValidator.validateEmail(EMAIL);
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
