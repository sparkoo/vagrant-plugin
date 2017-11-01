package org.jenkinsci.plugins.vagrant;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class VagrantPackageCommand extends Builder {

  private final String vagrantFile;
  private final String vagrantVm;
  private final String outputName;

  @DataBoundConstructor
  public VagrantPackageCommand(String vagrantFile, String vagrantVm, String outputName) {
    this.vagrantFile = vagrantFile;
    this.vagrantVm = vagrantVm;
    this.outputName = outputName;
  }

  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException,
          InterruptedException {
    VagrantWrapper wrapper = VagrantWrapper.createVagrantWrapper(vagrantFile, vagrantVm, build, launcher, listener);

    List<String> arg = Arrays.asList("--output", build.getWorkspace().toURI().getPath() + outputName);

    try {
      return wrapper.executeCommand("package", arg);
    } catch (IOException | InterruptedException e) {
      listener.getLogger().println("Error starting up vagrant, caught " +
              e.getClass().getSimpleName() + ", message: " + e.getMessage());
      wrapper.log(e);
      return false;
    }
  }

  public String getVagrantFile() {
    return vagrantFile;
  }

  public String getVagrantVm() {
    return vagrantVm;
  }

  public String getOutputName() {
    return outputName;
  }

  @Override
  public BuildStepDescriptor<Builder> getDescriptor() {
    return DESCRIPTOR;
  }

  @Extension
  public static final VagrantPackageCommandDescriptor DESCRIPTOR = new VagrantPackageCommandDescriptor();

  @Extension
  public static final class VagrantPackageCommandDescriptor extends BuildStepDescriptor<Builder> {

    public String getDisplayName() {
      return "Package a Vagrant VM";
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      // Indicates that this builder can be used with all kinds of project types
      return true;
    }
  }
}
