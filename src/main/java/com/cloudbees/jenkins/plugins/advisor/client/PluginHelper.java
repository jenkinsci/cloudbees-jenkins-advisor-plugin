package com.cloudbees.jenkins.plugins.advisor.client;

import com.cloudbees.jenkins.plugins.advisor.AdvisorGlobalConfiguration;
import hudson.Plugin;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

public final class PluginHelper {

    private static final Logger LOG = Logger.getLogger(PluginHelper.class.getName());

    private PluginHelper() {
        throw new UnsupportedOperationException("Unable to instantiate class");
    }

    public static String getPluginVersion() {
        try {
            Plugin plugin = Jenkins.get().getPlugin(AdvisorGlobalConfiguration.PLUGIN_NAME);
            if (plugin != null) {
                return plugin.getWrapper().getVersion();
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unable to retrieve plugin version", e.getCause());
        }
        return null;
    }
}
