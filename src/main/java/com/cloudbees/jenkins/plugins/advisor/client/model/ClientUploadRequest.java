package com.cloudbees.jenkins.plugins.advisor.client.model;

import java.io.File;

public class ClientUploadRequest {

  private final String instanceId;
  private final File file;
  private final String cc;
  private final String pluginVersion;

  public ClientUploadRequest(String instanceId, File file, String cc, String pluginVersion) {
    this.instanceId = instanceId;
    this.file = file;
    this.cc = cc;
    this.pluginVersion = pluginVersion;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public File getFile() {
    return file;
  }

  public String getCc() {
    return cc;
  }

  public String getPluginVersion() {
    return pluginVersion;
  }
}
