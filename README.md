# Jenkins Health Advisor by CloudBees

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/cloudbees-jenkins-advisor.svg)](https://plugins.jenkins.io/cloudbees-jenkins-advisor)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/cloudbees-jenkins-advisor-plugin.svg?label=changelog)](https://github.com/jenkinsci/cloudbees-jenkins-advisor-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/badge/Supported%20by-CloudBees-blue?logo=cloudbees&logoColor=white)](https://www.cloudbees.com/products/cloudbees-jenkins-support)

This jenkins plugin periodically uploads [support bundles](https://wiki.jenkins.io/display/JENKINS/Support+Core+Plugin) for processing by Jenkins Health Advisor by CloudBees. 
Jenkins Health Advisor by CloudBees analyse these bundles and send in return an email when it detects a new known issue referenced by the [CloudBees Support team](https://support.cloudbees.com).

To configure the plugin go into `Manage Jenkins` > `Manage Jenkins Health Advisor by CloudBees.

## Project Tracking

* [Jenkins JIRA with component `cloudbees-jenkins-advisor-plugin`](https://issues.jenkins-ci.org/issues/?jql=project%20%3D%20JENKINS%20AND%20component%20%3D%20cloudbees-jenkins-advisor-plugin)

## Build Job

* You can see the plugin's build status [here](https://ci.jenkins.io/job/Plugins/job/cloudbees-advisor-plugin/).

## Configuration

### Configure Programmatically

```java
import com.cloudbees.jenkins.plugins.advisor.*

def config = AdvisorGlobalConfiguration.instance
  
config.email = "test@email.com"
config.cc = "testCC@email.com" // optional
config.isValid = true
config.nagDisabled = true
config.acceptToS = true

config.save()
```

### System Properties

#### Upload Recurrence Period

Cannot be overridden at runtime. Requires restart to take effect. Defaults to 24hrs.

Overriding with Java System Property:

```bash
-Dcom.cloudbees.jenkins.plugins.advisor.BundleUpload.recurrencePeriodHours=1
```

#### Upload Timeout

Available properties:

| Property                                                                                           | Default | Unit    | Description                                      |
|----------------------------------------------------------------------------------------------------|---------|---------|--------------------------------------------------|
| com.cloudbees.jenkins.plugins.advisor.client.AdvisorClientConfig.advisorUploadTimeoutMinutes       | 60      | minutes | The maximum time to wait for a response          |
| com.cloudbees.jenkins.plugins.advisor.client.AdvisorClientConfig.advisorUploadIdleTimeoutMinutes   | 60      | minutes | The maximum time an upload request can stay idle |

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

#### Upload Initial Delay

Cannot be overridden at runtime. Requires restart to take effect. Defaults to 5mins.

Overriding with Java System Property:

```bash
-Dcom.cloudbees.jenkins.plugins.advisor.BundleUpload.initialDelayMinutes=60
```

## Troubleshooting

### Manual upload launch

The following can be run from the Script Console to manually trigger an upload:

```
import hudson.model.*
import hudson.triggers.*
import jenkins.util.Timer;


for(trigger in PeriodicWork.all()) {
  if(trigger.class.name == "com.cloudbees.jenkins.plugins.advisor.BundleUpload"){
    trigger.run()
    return
  }
}
```

### Idle timeout

Bundle uploads fail with:

```
SEVERE: Issue while uploading file to bundle upload service: java.util.concurrent.TimeoutException: Request reached idle time out of 120000 ms after 120483 ms
```

Try adjusting the idle timeout period.

See the [Upload Timeout](#Upload-Timeout) section for how to do that.

### File is not a normal file.

Bundle uploads fail with:

```
Jenkins Health Advisor by CloudBees failed to upload a bundle: ERROR: Issue while uploading file to bundle upload service: An error occurred while checking server status during bundle upload. 
Message: com.cloudbees.jenkins.plugins.advisor.client.AdvisorClient$InsightsUploadFileException: Exception trying to upload support bundle. Message: File is not a normal file.
```

Check the logs as the cause should be printed as  a `SEVERE` message along with the above message (`File is not a normal file.`) duplicated. 
