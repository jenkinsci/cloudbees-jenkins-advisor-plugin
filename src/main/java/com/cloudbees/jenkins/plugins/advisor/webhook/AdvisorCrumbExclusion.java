package com.cloudbees.jenkins.plugins.advisor.webhook;

/**
 * Ignore the crumbs since we're posting from an external service.  Check out
 * https://github.com/jenkinsci/github-plugin/blob/93d40692ff3866705175624e93ec584d4ac88132/src/main/java/com/cloudbees/jenkins/GitHubWebHookCrumbExclusion.java
 * 
 * For tests:
 * https://github.com/jenkinsci/github-plugin/blob/93d40692ff3866705175624e93ec584d4ac88132/src/test/java/com/cloudbees/jenkins/GitHubWebHookCrumbExclusionTest.java
 */

import hudson.Extension;
import hudson.security.csrf.CrumbExclusion;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Extension
public class AdvisorCrumbExclusion extends CrumbExclusion {

    @Override
    public boolean process(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        String pathInfo = req.getPathInfo();
        if (isEmpty(pathInfo)) {
            return false;
        }
        pathInfo = pathInfo.endsWith("/") ? pathInfo : pathInfo + '/';
        if (!pathInfo.equals(getExclusionPath())) {
            return false;
        }
        chain.doFilter(req, resp);
        return true;
    }

    public String getExclusionPath() {
        return "/" + AdvisorWebhook.URLNAME + "/";
    }
}