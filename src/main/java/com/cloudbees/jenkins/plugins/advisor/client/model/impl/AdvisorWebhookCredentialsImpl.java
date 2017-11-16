package com.cloudbees.jenkins.plugins.advisor.client.model.impl;

import com.cloudbees.jenkins.plugins.advisor.client.model.AdvisorWebhookCredentials;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;


@SuppressWarnings("unused")
public class AdvisorWebhookCredentialsImpl extends BaseStandardCredentials implements AdvisorWebhookCredentials {

  @Nonnull
  private final Secret advisorServiceSecret;

  @DataBoundConstructor
  public AdvisorWebhookCredentialsImpl(@CheckForNull CredentialsScope scope,
        @CheckForNull String id, @CheckForNull String description,
        @CheckForNull String advisorServiceSecret) {
    super(scope, id, description);
    this.advisorServiceSecret = Secret.fromString(advisorServiceSecret);
  }

    

  @Override
  public Secret getAdvisorServiceSecret() {
    return advisorServiceSecret;
  }
    
    
  @Extension
  public static class DescriptorImpl extends BaseStandardCredentialsDescriptor { 
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return "CloudBees Jenkins Advisor Webhook Credentials";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIconClassName() {
        return "cloudbees-jenkins-advisor-credentials";
    }   
  }
}