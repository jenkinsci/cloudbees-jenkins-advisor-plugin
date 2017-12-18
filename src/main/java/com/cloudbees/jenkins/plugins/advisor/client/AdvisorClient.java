package com.cloudbees.jenkins.plugins.advisor.client;

import com.cloudbees.jenkins.plugins.advisor.client.model.AccountCredentials;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientUploadRequest;
import com.ning.http.client.*;
import com.ning.http.client.multipart.FilePart;
import jenkins.plugins.asynchttpclient.AHCUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

public class AdvisorClient {

  private static final Logger LOG = Logger.getLogger(AdvisorClient.class.getName());
  public static final String HEALTH_SUCCESS = "Successfully checked the service status";

  private final AsyncHttpClient httpClient;
  private final AccountCredentials credentials;

  public AdvisorClient(AccountCredentials accountCredentials) {
    this.httpClient = new AsyncHttpClient((
        new AsyncHttpClientConfig.Builder()
            .setRequestTimeout(AdvisorClientConfig.insightsUploadTimeoutMilliseconds())
            .setProxyServer(AHCUtils.getProxyServer())
            .setFollowRedirect(true)
            .build()));
    this.credentials = accountCredentials;
  }

  public ListenableFuture<String> doCheckHealth() {
    try {
      return httpClient.prepareGet(AdvisorClientConfig.healthURI())
          .execute(new AsyncCompletionHandler<String>() {
            @Override
            public String onCompleted(Response response) throws Exception {
              int status = response.getStatusCode();
              if(status < 400) {
                return HEALTH_SUCCESS;
              } else {
                throw new IOException("Unable to check response from the server: " + status);
              }
            }

            @Override
            public void onThrowable(Throwable t) {
              throw new InsightsAuthenticationException("Unable to check health. Message: " + t.getMessage());
            }
          });
    } catch (Exception e) {
      throw new InsightsAuthenticationException("Exception when attempting to check health. Message: " + e);
    }
  }

  public ListenableFuture<Response> uploadFile(ClientUploadRequest uploadRequest) {
    try {
      doCheckHealth();
      return doUploadFile(uploadRequest);
    } catch(Exception e) {
      throw new InsightsAuthenticationException("An error occurred while checking server status during bundle upload. Message: " + e);
    }
  }

  private ListenableFuture<Response> doUploadFile(ClientUploadRequest r) {
    try {
      return httpClient.preparePost(AdvisorClientConfig.apiUploadURI(credentials.getUsername(), r.getInstanceId()))
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
    } catch (Exception e) {
      throw new InsightsUploadFileException("Exception trying to upload support bundle. Message: " + e.getMessage());
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
