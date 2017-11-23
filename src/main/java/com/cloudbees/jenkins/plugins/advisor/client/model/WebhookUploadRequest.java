package com.cloudbees.jenkins.plugins.advisor.client.model;

public class WebhookUploadRequest {

  private final String instanceId;
  private final String action;

  public WebhookUploadRequest(String instanceId, String action) {
    this.instanceId = instanceId;
    this.action = action;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getAction() {
    return action;
  }
}
