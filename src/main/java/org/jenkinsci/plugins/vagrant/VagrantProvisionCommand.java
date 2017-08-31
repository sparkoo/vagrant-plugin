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

public class VagrantProvisionCommand extends Builder {
  private final String vagrantFile;
  private final String vagrantVm;
  private final String provisioners;
  private final boolean parallel;

  @SuppressWarnings("WeakerAccess")
  @Extension
  public static final VagrantProvisionCommandDescriptor DESCRIPTOR = new VagrantProvisionCommandDescriptor();

  @DataBoundConstructor
  public VagrantProvisionCommand(String vagrantFile, String vagrantVm, String provisioners, boolean parallel) {
    this.vagrantFile = vagrantFile;
    this.vagrantVm = vagrantVm;
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
    return vagrantFile;
  }

  public String getVagrantVm() {
    return vagrantVm;
  }

  @Override
  public BuildStepDescriptor<Builder> getDescriptor() {
    return DESCRIPTOR;
  }

  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    VagrantWrapper wrapper = VagrantWrapper.createVagrantWrapper(vagrantFile, vagrantVm, build, launcher, listener);

    List<String> arg = new ArrayList<>();
    if (parallel) {
      arg.add("--parallel");
    }
    if (provisioners != null && !provisioners.isEmpty()) {
      arg.add("--provision-with");
      arg.add(provisioners);
    }

    try {
      return wrapper.executeCommand("provision", arg);
    } catch (IOException e) {
      listener.getLogger().println("Error starting up vagrant, caught IOException, message: " + e.getMessage());
      wrapper.log(e);
      return false;
    } catch (InterruptedException e) {
      listener.getLogger().println("Error starting up vagrant, caught InterruptedException, message: " + e.getMessage
              ());
      wrapper.log(e);
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
