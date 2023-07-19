package com.cloudbees.jenkins.plugins.advisor.utils;

import hudson.Util;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Logger;

public final class EmailUtil {

    private static final Logger LOG = Logger.getLogger(EmailUtil.class.getName());

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
        if (value == null) {
            return null;
        }
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            LOG.warning("UTF-8 is not supported to encode the URL parameter " + value + " : "
                    + unsupportedEncodingException.getMessage());
            return value;
        }
    }
}
