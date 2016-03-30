package org.jenkinsci.plugins.vagrant;

/**
 * Created by elad on 9/18/14.
 */

import hudson.Launcher;
import hudson.Extension;
import hudson.model.Descriptor;
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

  private boolean destroyOnError;
  private String provider;
  private VagrantWrapper wrapper;

  public boolean isDontKillMe() {
    return dontKillMe;
  }

  public void setDontKillMe(boolean dontKillMe) {
    this.dontKillMe = dontKillMe;
  }

  private boolean dontKillMe;

  @Extension
  public static final VagrantUpCommandDescriptor DESCRIPTOR = new VagrantUpCommandDescriptor();

  @DataBoundConstructor
  public VagrantUpCommand(String vagrantFile, String vagrantVm, boolean destroyOnError, String provider, boolean dontKillMe) {
    this.wrapper = new VagrantWrapper(vagrantFile, vagrantVm);
    this.destroyOnError = destroyOnError;
    this.provider = provider;
    this.dontKillMe = dontKillMe;
  }

  public boolean isDestroyOnError() {
    return destroyOnError;
  }

  public void setDestroyOnError(boolean destroyOnError) {
    this.destroyOnError = destroyOnError;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
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
    TreeMap<String, String> additinalVars = new TreeMap<String, String>();

    this.wrapper.setBuild(build);
    this.wrapper.setLauncher(launcher);
    this.wrapper.setListener(listener);
    List<String> arg = new ArrayList<String>();

    if (destroyOnError) {
      arg.add("--destroy-on-error");
    }

    if (provider != null && !provider.isEmpty()) {
      arg.add("--provider=" + provider);
    }

    if (this.dontKillMe) {
      additinalVars.put("BUILD_ID", "dontKillMe");
    }

    try {
      return this.wrapper.executeCommand("up", arg, additinalVars);
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

    /**
     * Default constructor
     */
    public VagrantUpCommandDescriptor() {
      load();
    }

    /**
     *
     * @return
     */
    public String getDisplayName() {
      return "Boot up Vagrant VM";
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      // Indicates that this builder can be used with all kinds of project types
      return true;
    }
  }
}
