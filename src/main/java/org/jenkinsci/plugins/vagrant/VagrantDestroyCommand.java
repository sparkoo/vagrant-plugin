package org.jenkinsci.plugins.vagrant;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

/**
* Created by elad on 9/18/14.
*/
public class VagrantDestroyCommand extends Builder {
  private final String vagrantFile;
  private final String vagrantVm;

  @SuppressWarnings("WeakerAccess")
  @Extension
  public static final VagrantDestroyCommandDescriptor DESCRIPTOR = new VagrantDestroyCommandDescriptor();

  @DataBoundConstructor
  public VagrantDestroyCommand(String vagrantFile, String vagrantVm) {
    this.vagrantFile = vagrantFile;
    this.vagrantVm = vagrantVm;
  }

  public String getVagrantFile() {
    return vagrantFile;
  }

  public String getVagrantVm() {
    return vagrantVm;
  }

  @Override
  public Descriptor<Builder> getDescriptor() {
    return DESCRIPTOR;
  }

  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    VagrantWrapper wrapper = VagrantWrapper.createVagrantWrapper(vagrantFile, vagrantVm, build, launcher, listener);
    return new PerformVagrantDestroy().performVagrantVmDestroy(wrapper);
  }

  public static final class VagrantDestroyCommandDescriptor extends Descriptor<Builder> {

    public String getDisplayName() {
      return "Destroy a Vagrant VM";
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      // Indicates that this builder can be used with all kinds of project types
      return true;
    }
  }
}
