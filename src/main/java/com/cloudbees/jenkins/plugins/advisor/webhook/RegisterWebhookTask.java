package com.cloudbees.jenkins.plugins.advisor.webhook;

import com.cloudbees.jenkins.plugins.advisor.AdvisorGlobalConfiguration;
import com.cloudbees.jenkins.plugins.advisor.AdvisorReports;
import com.cloudbees.jenkins.plugins.advisor.client.AdvisorClient;
import com.cloudbees.jenkins.plugins.advisor.client.model.AccountCredentials;
import com.cloudbees.jenkins.plugins.advisor.client.model.WebhookUploadRequest;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

import hudson.triggers.SafeTimerTask;
import hudson.util.Secret;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

/**
 * Called to register or unregister a webhook for this Jenkins instance
 */
public class RegisterWebhookTask extends SafeTimerTask {

  public enum ACTION {
    REGISTER, REMOVE
  }

  private static final Logger LOG = Logger.getLogger(RegisterWebhookTask.class.getName());

  private final String HEADER = "CloudBees-Advisor-Signature";
  private ACTION action;

  public RegisterWebhookTask(ACTION action) {
    this.action = action;
  }

  protected void doRun() {
    AdvisorGlobalConfiguration config = AdvisorGlobalConfiguration.getInstance();
    if (config == null) {
      return;
    }

    if (!config.isValid()) {
      LOG.finest("User not registered. Skipping bundle upload.");
      return;
    }
      
    switch(action) {
      case REGISTER: registerWebhook(config.getEmail(), Secret.toString(config.getPassword()));
        break;
      case REMOVE: removeWebhook();
        break;
      default: LOG.info("Nothing to run with the webhook");
    }
  }

  private void registerWebhook(String email, String password) {
    AdvisorClient advisorClient = new AdvisorClient(new AccountCredentials(email, password));
    ListenableFuture<Response> futureResponse = advisorClient.registerWebhook(new WebhookUploadRequest(Jenkins.getInstance().getLegacyInstanceId(), action.toString()));
    Response res;
	try {
        String credId = AdvisorGlobalConfiguration.getInstance().getCredentialsId();
        if(credId == null) {
            res = futureResponse.get();
            if(res.getStatusCode() == 200) {
                //AdvisorWebhookCredentialsManager.setSecretFor("credentialsId", res.getHeader(HEADER));
            }
        }

	} catch (InterruptedException e) {
		e.printStackTrace();
	} catch (ExecutionException e) {
		e.printStackTrace();
	}
    
  }

  private void removeWebhook() {
    // build post for client
    // post simply the request to unregister
    // delete credential - optional
  }
}