package com.cloudbees.jenkins.plugins.advisor.client.model;

public class AccountCredentials {

  private String username;

  public AccountCredentials() {
  }

  public AccountCredentials(String username) {
    this.username = username;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

}
