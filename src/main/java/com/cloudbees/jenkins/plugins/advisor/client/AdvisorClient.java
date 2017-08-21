package com.cloudbees.jenkins.plugins.advisor.client;

import com.cloudbees.jenkins.plugins.advisor.client.model.AccountCredentials;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientUploadRequest;
import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.asynchttpclient.*;
import org.asynchttpclient.proxy.ProxyServer;
import org.asynchttpclient.request.body.multipart.FilePart;

import java.util.concurrent.CompletableFuture;

public class AdvisorClient {

  private final AsyncHttpClient httpClient;
  private final AccountCredentials credentials;
  private final Gson gson;

  public AdvisorClient(AccountCredentials accountCredentials) {
    this.httpClient = new DefaultAsyncHttpClient();
    this.credentials = accountCredentials;
    this.gson = new Gson();
  }

  public CompletableFuture<String> doAuthenticate() {
    BoundRequestBuilder requestBuilder = httpClient.preparePost(AdvisorClientConfig.loginURI())
      .setHeader("Content-Type", "application/json")
      .setBody(gson.toJson(credentials))
      .setFollowRedirect(true);

    addProxySettings(requestBuilder);

    return requestBuilder.execute()
      .toCompletableFuture()
      .exceptionally(e -> {throw new InsightsAuthenticationException("Unable to authenticate. Reason: " + e.getMessage());})
      .thenApply(this::getBearerToken);
  }

  private void addProxySettings(BoundRequestBuilder requestBuilder) {
    if (StringUtils.isNotBlank(credentials.getProxyHost())) {
      ProxyServer.Builder proxyBuilder = new ProxyServer.Builder(credentials.getProxyHost(), credentials.getProxyPort());
      proxyBuilder.setNonProxyHosts(credentials.getNonProxyHosts());

      if (StringUtils.isNotBlank(credentials.getProxyUsername())) {
        Realm realm = new Realm.Builder(credentials.getProxyUsername(), credentials.getProxyPassword()).setScheme(Realm.AuthScheme.BASIC).build();
        proxyBuilder.setRealm(realm);
      }
      requestBuilder.setProxyServer(proxyBuilder);
    }
  }

  private String getBearerToken(Response response) {
    try {
      String header = response.getHeader("Authorization");
      if (StringUtils.isEmpty(header)) {
        throw new InsightsAuthenticationException("No authorization header found in response.");
      }
      return header.split("Bearer ")[1];
    } catch (Exception e) {
      throw new InsightsAuthenticationException("Authentication failed. Unable to get bearer token", e);
    }
  }

  public CompletableFuture<Response> uploadFile(ClientUploadRequest uploadRequest) {
    return doAuthenticate().thenApply(t -> doUploadFile(uploadRequest, t)).join();
  }

  private CompletableFuture<Response> doUploadFile(ClientUploadRequest r, String token) {
    BoundRequestBuilder requestBuilder = httpClient.preparePost(AdvisorClientConfig.apiUploadURI(credentials.getUsername(), r.getInstanceId()))
      .addHeader("Authorization", token)
      .addBodyPart(new FilePart("file", r.getFile()))
      .setRequestTimeout(AdvisorClientConfig.insightsUploadTimeoutMilliseconds())
      .setFollowRedirect(true);

    addProxySettings(requestBuilder);

    return requestBuilder.execute()
      .toCompletableFuture()
      .exceptionally(e -> {throw new InsightsUploadFileException("Unable to upload file. Reason: " + e.getMessage());});
  }

  private static final class InsightsAuthenticationException extends RuntimeException {
    private InsightsAuthenticationException(String message) {
      super(message);
    }

    private InsightsAuthenticationException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  private static final class InsightsUploadFileException extends RuntimeException {
    private InsightsUploadFileException(String message) {
      super(message);
    }
  }
}
