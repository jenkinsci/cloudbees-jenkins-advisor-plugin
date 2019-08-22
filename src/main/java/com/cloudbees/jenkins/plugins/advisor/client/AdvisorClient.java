package com.cloudbees.jenkins.plugins.advisor.client;

import com.cloudbees.jenkins.plugins.advisor.client.model.AccountCredentials;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientResponse;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientUploadRequest;
import com.cloudbees.jenkins.plugins.advisor.utils.EmailUtil;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdvisorClient {

  private static final Logger LOG = Logger.getLogger(AdvisorClient.class.getName());

  static final String HEALTH_SUCCESS = "Successfully checked the service status";
  static final String EMAIL_SUCCESS = "Successfully sent a test email";

  private final AccountCredentials credentials;

  public AdvisorClient(AccountCredentials accountCredentials) {
    this.credentials = accountCredentials;
  }

  public String doTestEmail() {
    try {
      HttpURLConnection con = HttpUrlConnectionFactory.openGetConnection(AdvisorClientConfig.testEmailURI(credentials.getUsername()));

      int responseCode = con.getResponseCode();

      if(responseCode == HttpURLConnection.HTTP_OK) {
        return EMAIL_SUCCESS;
      } else {
        throw new IOException("Unable to check response from the server: " + responseCode);
      }

    } catch (Exception e) {
      throw new InsightsAuthenticationException("Exception while attempting to send test email. Message: " + e);
    }
  }

  public String doCheckHealth() {
    try {
      HttpURLConnection con = HttpUrlConnectionFactory.openGetConnection(AdvisorClientConfig.healthURI());

      int responseCode = con.getResponseCode();

      if(responseCode == HttpURLConnection.HTTP_OK) {
        return HEALTH_SUCCESS;
      } else {
        throw new IOException("Unable to check response from the server: " + responseCode);
      }

    } catch (Exception e) {
      throw new InsightsAuthenticationException("Exception when attempting to check health. Message: " + e);
    }
  }

  public ClientResponse uploadFile(ClientUploadRequest uploadRequest) {
    try {
      doCheckHealth();
      return doUploadFile(uploadRequest);
    } catch(Exception e) {
      throw new InsightsAuthenticationException("An error occurred while checking server status during bundle upload. Message: " + e);
    }
  }

  private ClientResponse doUploadFile(final ClientUploadRequest r) {
    File uploadFile = r.getFile();
    String cc = EmailUtil.urlEncode(r.getCc());

    String requestURL = AdvisorClientConfig.apiUploadURI(credentials.getUsername(), r.getInstanceId(), cc);

    try {
      MultipartConnection multipart = new MultipartConnection(requestURL, StandardCharsets.UTF_8);

      multipart.addHeader("X-ADVISOR-PLUGIN-VERSION", r.getPluginVersion() != null ? r.getPluginVersion() : "N/A");
      multipart.connect();

      multipart.addFilePart("file", uploadFile);

      ClientResponse clientResponse = multipart.finish();

      if (clientResponse.getCode() == HttpURLConnection.HTTP_OK) {
        if (LOG.isLoggable(Level.INFO)) {
          LOG.info(String.format("Bundle successfully uploaded. Response code was: %s", clientResponse.getCode()));
        }
      } else {
        if (LOG.isLoggable(Level.SEVERE)) {
          LOG.severe(String.format("Bundle upload failed. Response code was: [%s]. Response message: [%s]",
              clientResponse.getCode(), clientResponse.getMessage()));
        }
      }

      return clientResponse;
    } catch (Exception e) {
      String message = String.format(
          "Exception trying to upload support bundle. Message: [%s], File: [%s], Metadata: [%s]",
          e.getMessage(), r.getFile(), FileHelper.getFileMetadata(r.getFile()));

      LOG.log(Level.SEVERE, message, e.getCause());

      throw new InsightsUploadFileException(message);
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
