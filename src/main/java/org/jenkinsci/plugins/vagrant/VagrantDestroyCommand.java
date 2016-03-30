package org.jenkinsci.plugins.vagrant;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
* Created by elad on 9/18/14.
*/
public class VagrantDestroyCommand extends Builder {
  private VagrantWrapper wrapper;

  @Extension
  public static final VagrantDestroyCommandDescriptor DESCRIPTOR = new VagrantDestroyCommandDescriptor();

  @DataBoundConstructor
  public VagrantDestroyCommand(String vagrantFile, String vagrantVm) {
    this.wrapper = new VagrantWrapper(vagrantFile, vagrantVm);
  }

  public String getVagrantFile() {

    return wrapper.getVagrantFile();
  }

  public String getVagrantVm() {
    return this.wrapper.getVagrantVm();
  }

  @Override
  public Descriptor<Builder> getDescriptor() {
    return DESCRIPTOR;
  }

  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    this.wrapper.setBuild(build);
    this.wrapper.setLauncher(launcher);
    this.wrapper.setListener(listener);
    List<String> arg = new ArrayList<String>();

    arg.add("--force");

    try {
      return this.wrapper.executeCommand("destroy", arg);
    } catch (IOException e) {
      wrapper.log("Error starting up vagrant, caught IOException, message: " + e.getMessage());
      wrapper.log(e);
      return false;
    } catch (InterruptedException e) {
      wrapper.log("Error starting up vagrant, caught InterruptedException, message: " + e.getMessage());
      wrapper.log(e);
      return false;
    }
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
