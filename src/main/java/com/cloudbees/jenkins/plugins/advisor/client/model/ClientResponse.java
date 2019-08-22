package com.cloudbees.jenkins.plugins.advisor.client.model;

public final class ClientResponse {

  private final int code;
  private final String message;

  public ClientResponse(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
