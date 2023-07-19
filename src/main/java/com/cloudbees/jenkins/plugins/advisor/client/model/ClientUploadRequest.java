package com.cloudbees.jenkins.plugins.advisor.client.model;

import java.io.File;
import java.util.List;

public class ClientUploadRequest {

    private final String instanceId;
    private final File file;
    private final List<Recipient> cc;
    private final String pluginVersion;

    public ClientUploadRequest(String instanceId, File file, List<Recipient> cc, String pluginVersion) {
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

    public List<Recipient> getCc() {
        return cc;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }
}
