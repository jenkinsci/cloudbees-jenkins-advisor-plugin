package com.cloudbees.jenkins.plugins.advisor;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Representation of the results sent by Advisor.
 * 
 */

public class AdvisorReport {
    
    private int securityCount;
    private int performanceCount;
    private int bestPracticeCount;
    private int issueCount;
    private int percent;

  public AdvisorReport() {
    this.securityCount = 0;
    this.performanceCount = 0;
    this.bestPracticeCount = 0;
    this.issueCount = 0;
    this.percent = 100;
  }

  @DataBoundConstructor
  public AdvisorReport(int securityCount, int performanceCount, int bestPracticeCount, int issueCount, int percent) {
    this.securityCount = securityCount;
    this.performanceCount = performanceCount;
    this.bestPracticeCount = bestPracticeCount;
    this.issueCount = issueCount;
    this.percent = percent;
  }

  public int getSecurityCount() {
        return securityCount;
  }

  public int getPerformanceCount() {
    return performanceCount;
  }
    
  public int getBestPracticeCount() {
    return bestPracticeCount;
  }

  public int getIssueCount() {
    return issueCount;
  }

  public int getPercent() {
    return percent;
  }
    
}