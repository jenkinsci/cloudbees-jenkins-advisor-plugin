package com.cloudbees.jenkins.plugins.advisor.client;

import com.cloudbees.jenkins.plugins.advisor.client.model.ClientResponse;
import hudson.ProxyConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

public class MultipartConnection {

  private static final String LINE_FEED = "\r\n";

  private final String boundary;
  private final HttpURLConnection httpConn;
  private OutputStream outputStream;
  private PrintWriter writer;
  private final Charset charset;

  public MultipartConnection(final String requestURL, final Charset charset) throws IOException {
    boundary = "===" + System.currentTimeMillis() + "===";

    httpConn = (HttpURLConnection) ProxyConfiguration.open(new URL(requestURL));
    httpConn.setUseCaches(false);
    httpConn.setDoOutput(true); // indicates POST method
    httpConn.setDoInput(true);
    httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
    httpConn.setConnectTimeout(AdvisorClientConfig.insightsUploadIdleTimeoutMilliseconds());
    httpConn.setReadTimeout(AdvisorClientConfig.insightsUploadTimeoutMilliseconds());
    httpConn.setInstanceFollowRedirects(true);

    this.charset = charset;
  }

  public void connect() {
    try {
      outputStream = httpConn.getOutputStream();
      writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),
          true);
    } catch (IOException e) {
      throw new MultipartConnectionException("Unable to connect", e);
    }
  }

  public void addHeader(String key, String value) {
    httpConn.addRequestProperty(key, value);
  }

  public void addFilePart(String fieldName, File uploadFile)
      throws IOException {
    String fileName = uploadFile.getName();
    writer.append("--").append(boundary).append(LINE_FEED);
    writer.append("Content-Disposition: form-data; name=\"").append(fieldName).append("\"; filename=\"").append(fileName).append("\"")
        .append(LINE_FEED);
    writer.append("Content-Type: ").append(URLConnection.guessContentTypeFromName(fileName)).append(LINE_FEED);
    writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
    writer.append(LINE_FEED);
    writer.flush();

    try (FileInputStream inputStream = new FileInputStream(uploadFile)) {
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }
      outputStream.flush();
    }

    writer.append(LINE_FEED);
    writer.flush();
  }

  public ClientResponse finish() throws IOException {
    StringBuilder response = new StringBuilder();

    writer.append(LINE_FEED).flush();
    writer.append("--").append(boundary).append("--").append(LINE_FEED);
    writer.close();

    // get code
    int status = httpConn.getResponseCode();

    // get message
    BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream(), charset));
    String line;
    while ((line = reader.readLine()) != null) {
      response.append(line);
    }

    // close connection
    reader.close();
    httpConn.disconnect();

    return new ClientResponse(status, response.toString());
  }

  private static final class MultipartConnectionException extends RuntimeException {
    public MultipartConnectionException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
