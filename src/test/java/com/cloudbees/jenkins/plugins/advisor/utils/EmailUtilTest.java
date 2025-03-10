package com.cloudbees.jenkins.plugins.advisor.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class EmailUtilTest {

    private static final String EMAIL = "test@acme.com";

    @Test
    void fixEmptyAndTrimAllSpaces() {
        assertThat(EmailUtil.fixEmptyAndTrimAllSpaces(null), is(nullValue()));
        assertThat(EmailUtil.fixEmptyAndTrimAllSpaces(""), is(nullValue()));
        assertThat(EmailUtil.fixEmptyAndTrimAllSpaces(" "), is(nullValue()));
        assertThat(EmailUtil.fixEmptyAndTrimAllSpaces(EMAIL + " "), is(EMAIL));
        assertThat(EmailUtil.fixEmptyAndTrimAllSpaces(" " + EMAIL), is(EMAIL));
        assertThat(EmailUtil.fixEmptyAndTrimAllSpaces(EMAIL.substring(0, 4) + " " + EMAIL.substring(4)), is(EMAIL));
    }

    @Test
    void urlEncode() {
        assertThat(EmailUtil.urlEncode(EMAIL), is("test%40acme.com"));
        assertThat(EmailUtil.urlEncode("test+foo@acme.com"), is("test%2Bfoo%40acme.com"));
        assertThat(EmailUtil.urlEncode(EMAIL + "," + EMAIL), is("test%40acme.com%2Ctest%40acme.com"));
        assertThat(EmailUtil.urlEncode(null), is(nullValue()));
    }
}
