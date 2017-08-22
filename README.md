# CloudBees Jenkins Advisor Plugin

Periodically uploads [support bundles](https://wiki.jenkins.io/display/JENKINS/Support+Core+Plugin) for processing by [CloudBees Jenkins Advisor](https://cloudbees.atlassian.net/wiki/display/CE/CloudBees+Insights+1.0).

To enable uploads, [Grand Central](grandcentral.cloudbees.com) credentials must First be configured - *Manage Jenkins > CloudBees Jenkins Advisor*.

## Project Tracking

* Jenkins JIRA

## Build Job

* TO REPLACE with link to job in ci.jenkins.io

## Configuration

### System Properties

#### CloudBees Jenkins Advisor Upload Recurrence Period

Cannot be overridden at runtime. Requires restart to take effect. Defaults to (60 * 24) (24hrs).

Overriding with Java System Property:

```bash
-Dcom.cloudbees.jenkins.plugins.advisor.BundleUpload.recurrencePeriodMinutes=60
```

#### CloudBees Jenkins Advisor Upload Timeout

Can be overridden dynamically at runtime, via Script Console:

```java
System.setProperty("com.cloudbees.jenkins.plugins.advisor.client.AdvisorClientConfig.advisorUploadTimeoutMinutes", "120");
```

Settings are lost after restart.

Can be permanently added by amending Jenkins Java System Properties:

```bash
-Dcom.cloudbees.jenkins.plugins.advisor.client.AdvisorClientConfig.advisorUploadTimeoutMinutes=120
```

Defaults to 60 (minutes)
