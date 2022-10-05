package com.cloudbees.jenkins.plugins.advisor.client;

import com.cloudbees.jenkins.plugins.advisor.client.dto.UserInfo;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientResponse;
import com.cloudbees.jenkins.plugins.advisor.client.model.ClientUploadRequest;
import com.cloudbees.jenkins.plugins.advisor.client.model.Recipient;
import com.cloudbees.jenkins.plugins.advisor.utils.EmailUtil;
import com.cloudbees.jenkins.plugins.advisor.utils.FileHelper;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AdvisorClient {

  static final String HEALTH_SUCCESS = "Successfully checked the service status";
  static final String EMAIL_SUCCESS = "Successfully sent a test email";
  private static final Logger LOG = Logger.getLogger(AdvisorClient.class.getName());
  private final Recipient recipient;

  public AdvisorClient(Recipient recipient) {
    this.recipient = recipient;
  }

  public String doTestEmail() {
    try {
      HttpURLConnection con =
        HttpUrlConnectionFactory.openGetConnection(AdvisorClientConfig.testEmailURI());
      con.setRequestMethod("POST");
      con.setRequestProperty("Content-Type", "application/json");
      con.setDoOutput(true);
      
      try(OutputStream os = con.getOutputStream()) {
        new ObjectMapper().writeValue(os, new UserInfo(recipient.getEmail()));
      }

      int responseCode = con.getResponseCode();

      if (responseCode == HttpURLConnection.HTTP_OK) {
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

      if (responseCode == HttpURLConnection.HTTP_OK) {
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
    } catch (Exception e) {
      throw new InsightsAuthenticationException(
        "An error occurred while checking server status during bundle upload. Message: " + e);
    }
  }

  private ClientResponse doUploadFile(final ClientUploadRequest r) {
    File uploadFile = r.getFile();
    String cc = (r.getCc() == null || r.getCc().isEmpty()) ? null :
      EmailUtil.urlEncode(r.getCc().stream().map(Recipient::getEmail).collect(Collectors.joining(",")));

    String requestURL = AdvisorClientConfig.apiUploadURI(recipient.getEmail(), r.getInstanceId(), cc);

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
