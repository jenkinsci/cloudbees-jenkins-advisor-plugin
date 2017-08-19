package com.cloudbees.jenkins.plugins.advisor.client.model;

import java.util.List;

public class AccountCredentials {

  private String username;
  private String password;
  private String proxyHost;
  private int proxyPort;
  private String proxyUsername;
  private String proxyPassword;
  private List<String> nonProxyHosts;

  public AccountCredentials() {
  }

  public AccountCredentials(String username, String password, String proxyHost, int proxyPort, String proxyUsername,
                            String proxyPassword, List<String> nonProxyHosts) {
    this.username = username;
    this.password = password;
    this.proxyHost = proxyHost;
    this.proxyPort = proxyPort;
    this.proxyUsername = proxyUsername;
    this.proxyPassword = proxyPassword;
    this.nonProxyHosts = nonProxyHosts;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getProxyHost() {
    return proxyHost;
  }

  public void setProxyHost(String proxyHost) {
    this.proxyHost = proxyHost;
  }

  public int getProxyPort() {
    return proxyPort;
  }

  public void setProxyPort(int proxyPort) {
    this.proxyPort = proxyPort;
  }

  public String getProxyUsername() {
    return proxyUsername;
  }

  public void setProxyUsername(String proxyUsername) {
    this.proxyUsername = proxyUsername;
  }

  public String getProxyPassword() {
    return proxyPassword;
  }

  public void setProxyPassword(String proxyPassword) {
    this.proxyPassword = proxyPassword;
  }

  public List<String> getNonProxyHosts() {
    return nonProxyHosts;
  }

  public void setNonProxyHosts(List<String> nonProxyHosts) {
    this.nonProxyHosts = nonProxyHosts;
  }
}
