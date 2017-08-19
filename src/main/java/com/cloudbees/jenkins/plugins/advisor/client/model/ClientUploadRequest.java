package com.cloudbees.jenkins.plugins.advisor.client.model;

import java.io.File;

public class ClientUploadRequest {

  private final String instanceId;
  private final File file;

  public ClientUploadRequest(String instanceId, File file) {
    this.instanceId = instanceId;
    this.file = file;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public File getFile() {
    return file;
  }
}
