package org.jenkinsci.plugins.vagrant;

/**
 * Created by elad on 9/18/14.
 */

import hudson.Launcher;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Environment;
import hudson.tasks.Builder;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;


public class VagrantUpCommand extends Builder {
  private final String vagrantFile;
  private final String vagrantVm;
  private final boolean destroyOnError;
  private final String provider;

  public boolean isDontKillMe() {
    return dontKillMe;
  }

  public void setDontKillMe(boolean dontKillMe) {
    this.dontKillMe = dontKillMe;
  }

  private boolean dontKillMe;

  @SuppressWarnings("WeakerAccess")
  @Extension
  public static final VagrantUpCommandDescriptor DESCRIPTOR = new VagrantUpCommandDescriptor();

  @DataBoundConstructor
  public VagrantUpCommand(String vagrantFile, String vagrantVm, boolean destroyOnError, String provider, boolean
          dontKillMe) {
    this.vagrantFile = vagrantFile;
    this.vagrantVm = vagrantVm;
    this.destroyOnError = destroyOnError;
    this.provider = provider;
    this.dontKillMe = dontKillMe;
  }

  public boolean isDestroyOnError() {
    return destroyOnError;
  }

  public String getProvider() {
    return provider;
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
    TreeMap<String, String> additionalVars = new TreeMap<>();

    VagrantWrapper wrapper = VagrantWrapper.createVagrantWrapper(vagrantFile, vagrantVm, build, launcher, listener);

    List<String> arg = new ArrayList<>();

    if (destroyOnError) {
      arg.add("--destroy-on-error");
    }

    if (provider != null && !provider.isEmpty()) {
      arg.add("--provider=" + provider);
    }

    if (this.dontKillMe) {
      additionalVars.put("BUILD_ID", "dontKillMe");
    }

    try {
      return wrapper.executeCommand("up", arg, additionalVars);
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

  public static final class VagrantUpCommandDescriptor extends Descriptor<Builder> {

    public VagrantUpCommandDescriptor() {
      load();
    }

    public String getDisplayName() {
      return "Boot up Vagrant VM";
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      // Indicates that this builder can be used with all kinds of project types
      return true;
    }
  }
}
