package com.cloudbees.jenkins.plugins.advisor.client.model;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.util.Secret;

import java.io.IOException;

public interface AdvisorWebhookCredentials extends StandardCredentials {

    Secret getAdvisorServiceSecret() throws IOException, InterruptedException;
}
  
