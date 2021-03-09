package com.cloudbees.jenkins.plugins.advisor.utils;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class EmailUtilTest {

  private static final String EMAIL = "test@acme.com";

  @Test
  public void fixEmptyAndTrimAllSpaces() {
    assertThat(EmailUtil.fixEmptyAndTrimAllSpaces(null), is(nullValue()));
    assertThat(EmailUtil.fixEmptyAndTrimAllSpaces(""), is(nullValue()));
    assertThat(EmailUtil.fixEmptyAndTrimAllSpaces(" "), is(nullValue()));
    assertThat(EmailUtil.fixEmptyAndTrimAllSpaces(EMAIL + " "), is(EMAIL));
    assertThat(EmailUtil.fixEmptyAndTrimAllSpaces(" " + EMAIL), is(EMAIL));
    assertThat(EmailUtil.fixEmptyAndTrimAllSpaces(EMAIL.substring(0, 4) + " " + EMAIL.substring(4)), is(EMAIL));
  }

  @Test
  public void urlEncode() {
    assertThat(EmailUtil.urlEncode(EMAIL), is("test%40acme.com"));
    assertThat(EmailUtil.urlEncode("test+foo@acme.com"), is("test%2Bfoo%40acme.com"));
    assertThat(EmailUtil.urlEncode(EMAIL + "," + EMAIL), is("test%40acme.com%2Ctest%40acme.com"));
    assertThat(EmailUtil.urlEncode(null), is(nullValue()));
  }

}
