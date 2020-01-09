package com.cloudbees.jenkins.plugins.advisor.client.model;
import com.cloudbees.jenkins.plugins.advisor.utils.EmailUtil;
import com.cloudbees.jenkins.plugins.advisor.utils.EmailValidator;
import hudson.Extension;
import hudson.RelativePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class Recipient extends AbstractDescribableImpl<Recipient> {

  private String email;

  @DataBoundConstructor
  public Recipient(String email) {
    this.email = EmailUtil.fixEmptyAndTrimAllSpaces(email);
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = EmailUtil.fixEmptyAndTrimAllSpaces(email);
  }
  
  @Extension
  public static class DescriptorImpl extends Descriptor<Recipient> {
    public DescriptorImpl() {
      super.load();
    }

    public String getDisplayName() {
      return "";
    }

    public FormValidation doCheckEmail(@QueryParameter String value) {
      return EmailValidator.validateEmail(value);
    }

    @POST
    public FormValidation doTestSendEmail(@QueryParameter("email") final String value,
                                          @RelativePath("..") @QueryParameter("acceptToS") final boolean acceptToS) {
      Jenkins.get().checkPermission(Jenkins.ADMINISTER);
      return EmailValidator.testSendEmail(value, acceptToS);
    }

  }

}
