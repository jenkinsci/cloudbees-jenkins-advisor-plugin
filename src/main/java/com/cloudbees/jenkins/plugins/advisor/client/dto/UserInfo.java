package com.cloudbees.jenkins.plugins.advisor.client.dto;

public class UserInfo {

  private final String email;

  public UserInfo(String email) {
    this.email = email;
  }

  public String getEmail() {
    return email;
  }
}
