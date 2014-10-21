package org.jenkinsci.plugins.vagrant;

/**
* Created by elad on 9/18/14.
*/

import hudson.Launcher;
import hudson.Extension;
import hudson.tasks.Builder;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VagrantProvisionCommand extends Builder  {

  private final String provisioners;
  private final boolean parallel;

  private VagrantWrapper wrapper;

  @DataBoundConstructor
  public VagrantProvisionCommand(String vagrantFile, String vagrantVm, String provisioners, boolean parallel) {
    this.wrapper = new VagrantWrapper(vagrantFile, vagrantVm);
    this.parallel = parallel;
    this.provisioners = provisioners;
  }

  public String getProvisioners() {
    return provisioners;
  }

  public boolean isParallel() {
    return parallel;
  }

  public String getVagrantFile() {
    return wrapper.getVagrantFile();

  }

  public String getVagrantVm() {
    return this.wrapper.getVagrantVm();
  }

  @Override
  public BuildStepDescriptor<Builder> getDescriptor() {
    return new VagrantProvisionCommandDescriptor();
  }

  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    this.wrapper.setBuild(build);
    this.wrapper.setLauncher(launcher);
    this.wrapper.setListener(listener);
    List<String> arg = new ArrayList<String>();

    if (parallel) {
      arg.add("--parallel");
    }

    if (provisioners != null && !provisioners.isEmpty()) {
      arg.add("--provision-with");
      arg.add(provisioners);
    }

    try {
      return this.wrapper.executeCommand("provision", arg);
    } catch (IOException e) {
      listener.getLogger().println("Error starting up vagrant, caught IOException, message: " + e.getMessage());
      wrapper.log(e.getStackTrace());
      return false;
    } catch (InterruptedException e) {
      listener.getLogger().println("Error starting up vagrant, caught InterruptedException, message: " + e.getMessage());
      wrapper.log(e.getStackTrace());
      return false;
    }


  }

  @Extension
  public static final class VagrantProvisionCommandDescriptor extends BuildStepDescriptor<Builder> {

    public String getDisplayName() {
      return "Provision a Vagrant VM";
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      // Indicates that this builder can be used with all kinds of project types
      return true;
    }
  }
}
