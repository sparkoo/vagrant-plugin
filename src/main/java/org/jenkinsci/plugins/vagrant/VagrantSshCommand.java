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
 * Created by elad on 10/13/14.
 */
public class VagrantSshCommand extends Builder {
  private final String vagrantFile;
  private final String vagrantVm;
  private final String command;
  private final boolean asRoot;

  @DataBoundConstructor
  public VagrantSshCommand(String vagrantFile, String vagrantVm, String command, boolean asRoot) {
    this.vagrantFile = vagrantFile;
    this.vagrantVm = vagrantVm;
    this.command = command;
    this.asRoot = asRoot;
  }

  public String getCommand() {
    return command;
  }

  public boolean isAsRoot() {
    return asRoot;
  }

  public String getVagrantFile() {
    return vagrantFile;
  }

  public String getVagrantVm() {
    return vagrantVm;
  }

  @SuppressWarnings("WeakerAccess")
  @Extension
  public static final VagrantSshCommandDescriptor DESCRIPTOR = new VagrantSshCommandDescriptor();

  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    VagrantWrapper wrapper = VagrantWrapper.createVagrantWrapper(vagrantFile, vagrantVm, build, launcher, listener);

    List<String> arg = new ArrayList<String>();

    arg.add("--command");
    if (this.asRoot) {
      arg.add("sudo " + this.command);
    } else {
      arg.add(this.command);
    }

    try {
      return wrapper.executeCommand("ssh", arg);
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


  @Override
  public Descriptor<Builder> getDescriptor() {
    return DESCRIPTOR;
  }

  public static final class VagrantSshCommandDescriptor extends Descriptor<Builder> {

    public String getDisplayName() {
      return "Run a command in vagrant machine";
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      // Indicates that this builder can be used with all kinds of project types
      return true;
    }
  }

}
