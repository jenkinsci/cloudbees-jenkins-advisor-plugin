package com.cloudbees.jenkins.plugins.advisor;

/**
 * Results as of the latest Advisor rating.  Used to display information in the UI.
 * 
 * Results are saved into $JENKINS_HOME/advisor.
 */

import hudson.BulkChange;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.XmlFile;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import jenkins.model.Jenkins;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.kohsuke.stapler.DataBoundConstructor;


import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class AdvisorReports implements Describable<AdvisorReports>, ExtensionPoint, Saveable {
  private static final Logger LOG = Logger.getLogger(AdvisorReports.class.getName());

  public static final String ADVISOR_LOC = "advisor-results";
  private final String DIRECTORY = "cloudbees-jenkins-advisor";
  private final DateFormat DATE_FORMATTER = new SimpleDateFormat("MM/dd/yyyy");

  private Queue<AdvisorReport> allReports;

  @SuppressWarnings("unused")
  public AdvisorReports() {
    load();
    if(allReports == null) {
      allReports = new CircularFifoQueue<AdvisorReport>(10);
    }
  }

  @SuppressWarnings("unused")
  @DataBoundConstructor
  public AdvisorReports(Queue<AdvisorReport> allReports) {
    this.allReports = allReports;
  }

  @CheckForNull
  public String getUrlName() {
    return ADVISOR_LOC;
  }

  public Queue<AdvisorReport> getAllReports() {
    return allReports;
  }

  public void addNewReport(AdvisorReport report) {
    allReports.add(report);
  }

  public String getFormattedTimestamp(long timestamp) {
    return DATE_FORMATTER.format(new Date(timestamp));
  } 

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Descriptor<AdvisorReports> getDescriptor() {
    return Jenkins.getInstance().getDescriptorOrDie(getClass());
  }

  @SuppressWarnings("unused")
  @Extension
  public static final class DescriptorImpl extends Descriptor<AdvisorReports> {

    public DescriptorImpl() {
      load();
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public String getDisplayName() {
      return "Advisor Results";
    }
  }


  /*************** Load Saved Results ***************/
  @Override
  public synchronized void save() {
    if(BulkChange.contains(this))   return;
    try {
      getConfigFile().write(this);
      SaveableListener.fireOnChange(this, getConfigFile());
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Failed to save "+getConfigFile(),e);
    }
  }

  public synchronized void load() {
    XmlFile file = getConfigFile();
    if(!file.exists())
      return;

    try {
      file.unmarshal(this);
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Failed to load "+file, e);
    }
  }

  //work in progress -> will eventually want to change to work as an aggregate save
  private XmlFile getConfigFile() {
    return new XmlFile(new File(Jenkins.getInstance().getRootDir()+File.separator+DIRECTORY, getClass().getName()+".xml"));
  }

  public static AdvisorReports getInstance() {
    return Jenkins.getInstance().getExtensionList(AdvisorReports.class).get(0);
  }
}