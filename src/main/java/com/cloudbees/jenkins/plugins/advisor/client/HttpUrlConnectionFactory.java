package com.cloudbees.jenkins.plugins.advisor.client;

import hudson.ProxyConfiguration;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public final class HttpUrlConnectionFactory {

    private HttpUrlConnectionFactory() {
        throw new UnsupportedOperationException("Cannot instantiate class");
    }

    public static HttpURLConnection openGetConnection(String spec) throws IOException {
        HttpURLConnection con = (HttpURLConnection) ProxyConfiguration.open(new URL(spec));
        con.setRequestMethod("GET");
        con.setConnectTimeout(AdvisorClientConfig.insightsUploadIdleTimeoutMilliseconds());
        con.setReadTimeout(AdvisorClientConfig.insightsUploadTimeoutMilliseconds());
        con.setInstanceFollowRedirects(true);

        return con;
    }
}
