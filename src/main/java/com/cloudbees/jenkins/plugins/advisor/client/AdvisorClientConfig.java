package com.cloudbees.jenkins.plugins.advisor.client;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class AdvisorClientConfig {

  private AdvisorClientConfig() {
    throw new IllegalAccessError("Utility class");
  }

  public static String advisorURL() {
    return removeEnd(resolveProperty("com.cloudbees.jenkins.plugins.advisor.client.AdvisorClientConfig.advisorURL"),
      "/");
  }

  public static Integer advisorUploadTimeoutMinutes() {
    return Integer.valueOf(removeEnd(
      resolveProperty("com.cloudbees.jenkins.plugins.advisor.client.AdvisorClientConfig.advisorUploadTimeoutMinutes"),
      "/"));
  }

  public static Integer insightsUploadTimeoutMilliseconds() {
    return (int) TimeUnit.MINUTES.toMillis(advisorUploadTimeoutMinutes());
  }

  public static Integer advisorUploadIdleTimeoutMinutes() {
    return Integer.valueOf(removeEnd(resolveProperty(
      "com.cloudbees.jenkins.plugins.advisor.client.AdvisorClientConfig.advisorUploadIdleTimeoutMinutes"), "/"));
  }

  public static Integer insightsUploadIdleTimeoutMilliseconds() {
    return (int) TimeUnit.MINUTES.toMillis(advisorUploadIdleTimeoutMinutes());
  }

  public static String healthURI() {
    return advisorURL() + "/api/health";
  }

  public static String testEmailURI(String email) {
    return advisorURL() + format("/api/test/emails/%s", email);
  }

  public static String apiUploadURI(String username, String instanceId) {
    return advisorURL() + format("/api/users/%s/upload/%s", username, instanceId);
  }

  public static String apiUploadURI(String username, String instanceId, String cc) {
    if (StringUtils.isNotBlank(cc)) {
        return advisorURL() + format("/api/users/%s/upload/%s?cc=%s", username, instanceId, cc);
    } else {
      return apiUploadURI(username, instanceId);
    }
  }

  /**
   * Recursively resolves a property value, taking property substitution into account, and allowing System property
   * overrides to take precedence.
   *
   * @param key the key of the property to resolve
   * @return the resolved value.
   */
  private static String resolveProperty(String key) {

    StringBuilder result = new StringBuilder();

    String value = System.getProperty(key, ResourceHolder.INSTANCE.getProperty(key));

    if (value == null) {
      return null;
    }

    int i1;
    int i2;

    while ((i1 = value.indexOf("${")) >= 0) {
      // append prefix to result
      result.append(value, 0, i1);

      // strip prefix from original
      value = value.substring(i1 + 2);

      // if no matching } then bail
      if ((i2 = value.indexOf('}')) < 0) {
        break;
      }

      /*
      strip out the key and resolve it
      resolve the key/value for the ${statement}
      */
      String tmpKey = value.substring(0, i2);
      value = value.substring(i2 + 1);
      String tmpValue = System.getProperty(tmpKey, ResourceHolder.INSTANCE.getProperty(tmpKey));

      /*
      if the key cannot be resolved,
      leave it alone ( and don't parse again )
      else prefix the original string with the
      resolved property ( so it can be parsed further )
      taking recursion into account.
      */
      if (tmpValue == null || tmpValue.equals(key) || key.equals(tmpKey)) {
        result.append("${").append(tmpKey).append("}");
      } else {
        value = tmpValue + value;
      }
    }
    result.append(value);
    return result.toString();
  }

  /**
   * <p>Removes a substring only if it is at the end of a source string,
   * otherwise returns the source string.</p>
   *
   * @param str    the source String to search, may be null
   * @param remove the String to search for and remove, may be null
   * @return the substring with the string removed if found,
   * <code>null</code> if null String input
   */
  private static String removeEnd(String str, String remove) {
    if (isEmpty(str) || isEmpty(remove)) {
      return str;
    }
    if (str.endsWith(remove)) {
      return str.substring(0, str.length() - remove.length());
    }
    return str;
  }

  /**
   * <p>Checks if a String is empty ("") or null.</p>
   *
   * @param str the String to check, may be null
   * @return <code>true</code> if the String is empty or null
   */
  private static boolean isEmpty(String str) {
    return str == null || str.length() == 0;
  }

  private static final class ResourceHolder {

    private static final Properties INSTANCE = loadProperties();

    private static Properties loadProperties() {
      Properties properties = new Properties();
      InputStream inputStream = AdvisorClientConfig.class
        .getResourceAsStream("/" + AdvisorClientConfig.class.getName().replace('.', '/') + ".properties");
      if (inputStream != null) {
        try {
          properties.load(inputStream);
        } catch (IOException e) {
          // ignore
        } finally {
          try {
            inputStream.close();
          } catch (IOException e) {
            // ignore
          }
        }
      }
      return properties;
    }
  }
}
