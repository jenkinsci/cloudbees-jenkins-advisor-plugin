package com.cloudbees.jenkins.plugins.advisor.client;

import com.cloudbees.jenkins.plugins.advisor.client.model.AccountCredentials;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientUploadRequest;
import com.cloudbees.jenkins.plugins.advisor.client.model.WebhookUploadRequest;
import com.google.gson.Gson;
import com.ning.http.client.*;
import com.ning.http.multipart.FilePart;
import jenkins.plugins.asynchttpclient.AHCUtils;
import org.apache.commons.lang.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class AdvisorClient {

  private static final Logger LOG = Logger.getLogger(AdvisorClient.class.getName());

  private final AsyncHttpClient httpClient;
  private final AccountCredentials credentials;
  private final Gson gson;

  public AdvisorClient(AccountCredentials accountCredentials) {
    this.httpClient = new AsyncHttpClient((
        new AsyncHttpClientConfig.Builder()
            .setRequestTimeoutInMs(AdvisorClientConfig.insightsUploadTimeoutMilliseconds())
            .setProxyServer(AHCUtils.getProxyServer())
            .setFollowRedirects(true)
            .build()));
    this.credentials = accountCredentials;
    this.gson = new Gson();
  }

  public ListenableFuture<String> doAuthenticate() {
    try {
      return httpClient.preparePost(AdvisorClientConfig.loginURI())
          .setHeader("Content-Type", "application/json")
          .setBody(gson.toJson(credentials))
          .execute(new AsyncCompletionHandler<String>() {
            @Override
            public String onCompleted(Response response) throws Exception {
              return getBearerToken(response);
            }

            @Override
            public void onThrowable(Throwable t) {
              throw new InsightsAuthenticationException("Unable to authenticate. Message: " + t.getMessage());
            }
          });
    } catch (IOException e) {
      throw new InsightsAuthenticationException("IOException try to authenticate. Message: " + e);
    }
  }

  private String getBearerToken(Response response) {
    try {
      String header = response.getHeader("Authorization");
      if (StringUtils.isEmpty(header)) {
        throw new InsightsAuthenticationException("Authorization failed. No authorization header found in response.");
      }
      return header.split("Bearer ")[1];
    } catch (Exception e) {
      throw new InsightsAuthenticationException("Authentication failed. Unable to get bearer token. Message: " + e.getMessage());
    }
  }

  public ListenableFuture<Response> uploadFile(ClientUploadRequest uploadRequest) {
    try {
      String token = doAuthenticate().get(AdvisorClientConfig.insightsUploadTimeoutMilliseconds(), TimeUnit.MILLISECONDS);
      return doUploadFile(uploadRequest, token);
    } catch (InterruptedException e) {
      throw new InsightsAuthenticationException("Interrupted trying to get bearer token from authentication request. Message: " + e.getMessage());
    } catch (ExecutionException e) {
      throw new InsightsAuthenticationException("Execution exception trying to get bearer token from authentication request. Message: " + e);
    } catch (TimeoutException e) {
      throw new InsightsAuthenticationException("Timeout trying to get bearer token from authentication request. Message: " + e);
    }
  }

  private ListenableFuture<Response> doUploadFile(ClientUploadRequest r, String token) {
    try {
      return httpClient.preparePost(AdvisorClientConfig.apiUploadURI(credentials.getUsername(), r.getInstanceId()))
          .addHeader("Authorization", token)
          .addBodyPart(new FilePart("file", r.getFile()))
          .execute(new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
              if (response.getStatusCode() == 200) {
                LOG.info("Bundle successfully uploaded. Response code was: " + response.getStatusCode() + ". " +
                    "Response status text: " + response.getStatusText());
              } else {
                LOG.severe("Bundle upload failed. Response code was: " + response.getStatusCode() + ". " +
                    "Response status text: " + response.getStatusText() + ". Response body: " + response.getResponseBody());
              }
              return response;
            }

            @Override
            public void onThrowable(Throwable t) {
              throw new InsightsUploadFileException("Unable to upload support bundle. Message: " + t.getMessage());
            }
          });
    } catch (FileNotFoundException e) {
      throw new InsightsUploadFileException(String.format("Support bundle to upload: [%s] not found. Message: [%s]", r.getFile().getPath(), e));
    } catch (IOException e) {
      throw new InsightsUploadFileException("IOException trying to upload support bundle. Message: " + e.getMessage());
    }
  }

  public ListenableFuture<Response> registerWebhook(WebhookUploadRequest webhookRequest) {
    try {
      String token = doAuthenticate().get(AdvisorClientConfig.insightsUploadTimeoutMilliseconds(), TimeUnit.MILLISECONDS);
      return doRegisterWebhook(webhookRequest, token);
    } catch (InterruptedException e) {
      throw new InsightsAuthenticationException("Interrupted trying to get bearer token from authentication request. Message: " + e.getMessage());
    } catch (ExecutionException e) {
      throw new InsightsAuthenticationException("Execution exception trying to get bearer token from authentication request. Message: " + e);
    } catch (TimeoutException e) {
      throw new InsightsAuthenticationException("Timeout trying to get bearer token from authentication request. Message: " + e);
    }
  }

  private ListenableFuture<Response> doRegisterWebhook(WebhookUploadRequest r, String token) {
    try {
      return httpClient.preparePost(AdvisorClientConfig.apiRegisterWebhookURI(credentials.getUsername(), r.getInstanceId()))
          .addHeader("Authorization", token)
          .execute(new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
              if (response.getStatusCode() == 200) {
                LOG.info("Successfully registered the webhook. Response code was: " + response.getStatusCode() + ". " +
                    "Response status text: " + response.getStatusText());
              } else {
                LOG.severe("Failed. Response code was: " + response.getStatusCode() + ". " +
                    "Response status text: " + response.getStatusText() + ". Response body: " + response.getResponseBody());
              }
              return response;
            }

            @Override
            public void onThrowable(Throwable t) {
              throw new InsightsUploadFileException("Unable to register a webhook. Message: " + t.getMessage());
            }
          });
    } catch (IOException e) {
      throw new InsightsUploadFileException("IOException trying to register a webhook. Message: " + e.getMessage());
    }
  }

  private static final class InsightsAuthenticationException extends RuntimeException {
    private InsightsAuthenticationException(String message) {
      super(message);
    }
  }

  private static final class InsightsUploadFileException extends RuntimeException {
    private InsightsUploadFileException(String message) {
      super(message);
    }
  }
}
