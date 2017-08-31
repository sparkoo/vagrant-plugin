package org.jenkinsci.plugins.vagrant;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Destroy Vagrant VM in Post-build actions.
 */
public class VagrantDestroyPostCommand extends Recorder {
  private final String vagrantFile;
  private final String vagrantVm;

  @SuppressWarnings("WeakerAccess")
  @Extension
  public static final VagrantDestroyCommandDescriptor DESCRIPTOR = new VagrantDestroyCommandDescriptor();

  @DataBoundConstructor
  public VagrantDestroyPostCommand(String vagrantFile, String vagrantVm) {
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
  public BuildStepDescriptor<Publisher> getDescriptor() {
    return DESCRIPTOR;
  }

  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    VagrantWrapper wrapper = VagrantWrapper.createVagrantWrapper(vagrantFile, vagrantVm, build, launcher, listener);
    return new PerformVagrantDestroy().performVagrantVmDestroy(wrapper);
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  public static final class VagrantDestroyCommandDescriptor extends BuildStepDescriptor<Publisher> {

    public String getDisplayName() {
      return "Destroy a Vagrant VM";
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      // Indicates that this builder can be used with all kinds of project types
      return true;
    }
  }
}
