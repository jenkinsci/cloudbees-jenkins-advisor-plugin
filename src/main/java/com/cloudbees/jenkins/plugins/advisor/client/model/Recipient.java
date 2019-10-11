package com.cloudbees.jenkins.plugins.advisor.client.model;

public class Recipient {

  private String email;

  public Recipient() {
  }

  public Recipient(String email) {
    this.email = email;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

}
