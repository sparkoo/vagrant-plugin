package org.jenkinsci.plugins.vagrant;

/**
 * Created by elad on 9/18/14.
 */

import hudson.FilePath;
import hudson.Launcher;

import java.io.File;

import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.remoting.VirtualChannel;

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

public class VagrantWrapper {

  private final String vagrantFile;
  protected final String vagrantVm;

  transient private String vagrantFileName;
  transient private FilePath containingFolder;

  transient private AbstractBuild build;
  transient private Launcher launcher;
  transient private BuildListener listener;

  public VagrantWrapper(String vagrantFile, String vagrantVm) {
    this.vagrantFile = vagrantFile;
    this.vagrantVm = vagrantVm;
  }

  public VagrantWrapper(String vagrantFile, String vagrantVm, AbstractBuild build, Launcher launcher, BuildListener
          listener) {
    this.vagrantFile = vagrantFile;
    this.vagrantVm = vagrantVm;
    this.build = build;
    this.launcher = launcher;
    this.listener = listener;
  }

  private static VagrantBuildSettings extractSettingsFromBuild(AbstractBuild build) {
    for (Environment e : build.getEnvironments()) {
      if (e instanceof VagrantBuildSettings.VagrantBuildSettingsEnvironment) {
        VagrantBuildSettings.VagrantBuildSettingsEnvironment env = (VagrantBuildSettings
                .VagrantBuildSettingsEnvironment) e;
        return env.getBuildSettings();
      }
    }
    return null;
  }

  static VagrantWrapper createVagrantWrapper(String vagrantFile, String vagrantVm, AbstractBuild build,
                                             Launcher launcher, BuildListener listener) {
    String vagrantFileToUse = vagrantFile;
    String vagrantVmToUse = vagrantVm;

    VagrantBuildSettings globalSettings = extractSettingsFromBuild(build);
    if (globalSettings != null) {
      if (isEmptyOrNull(vagrantFileToUse) && !isEmptyOrNull(globalSettings.getVagrantFile())) {
        vagrantFileToUse = globalSettings.getVagrantFile();
      }

      if (isEmptyOrNull(vagrantVmToUse) && !isEmptyOrNull(globalSettings.getVagrantVm())) {
        vagrantVmToUse = globalSettings.getVagrantVm();
      }
    }

    if (isEmptyOrNull(vagrantFileToUse)) {
      throw new RuntimeException("Vagrantfile is not specified");
    }

    return new VagrantWrapper(vagrantFileToUse, vagrantVmToUse, build, launcher, listener);
  }

  private static boolean isEmptyOrNull(String vagrantFile) {
    return vagrantFile == null || vagrantFile.isEmpty();
  }

  public String getVagrantFileName() {
    return this.vagrantFileName;
  }

  public void setBuild(AbstractBuild build) {
    this.build = build;
  }

  public void setLauncher(Launcher launcher) {
    this.launcher = launcher;
  }

  void setListener(BuildListener listener) {
    this.listener = listener;
  }

  private FilePath getContainingFolder() {
    return this.containingFolder;
  }

  public String getVagrantFile() {
    return this.vagrantFile;
  }

  public String getVagrantVm() {
    return this.vagrantVm;
  }

  private boolean parseVagrantEnvironment() {
    if (this.vagrantFile == null) {
      this.vagrantFileName = "Vagrantfile";
      this.containingFolder = build.getWorkspace();
    } else {
      File file = new File(vagrantFile);
      this.vagrantFileName = file.getName();
      String folderPath;
      if (file.isAbsolute()) {
        folderPath = file.getAbsoluteFile().getParent();
      } else {
        FilePath workspace = build.getWorkspace();
        if (workspace == null) {
          return false;
        }
        folderPath = new File(workspace.getRemote() + "/" + vagrantFile).getParent();
      }

      VirtualChannel vc = launcher.getChannel();
      if (vc == null) {
        return false;
      }
      this.containingFolder = new FilePath(vc, folderPath);
    }
    return true;
  }

  public void log(String data) {
    listener.getLogger().print("[ vagrant ]: ");
    listener.getLogger().println(data);
  }

  public void log(Throwable exception) {
    listener.getLogger().print("[ vagrant ]: ");
    exception.printStackTrace(listener.getLogger());
  }

  Boolean executeCommand(String command, List<String> args, Map<String, String> additionalEnvVars) throws
          IOException, InterruptedException {
    TreeMap<String, String> joinedEnvVars = new TreeMap<String, String>();

    this.validate();

    joinedEnvVars.putAll(build.getEnvironment(listener));
    if (additionalEnvVars != null) {
      joinedEnvVars.putAll(additionalEnvVars);
    }
    args.add(0, "vagrant");
    args.add(1, command);

    if (this.getVagrantVm() != null && !this.getVagrantVm().isEmpty()) {
      args.add(2, this.getVagrantVm());
    }

    this.log("Executing command :" + args.toString() + " in folder " + this.getContainingFolder().getRemote());

    Launcher.ProcStarter settings = launcher.launch();
    settings.cmds(args);
    settings.envs(joinedEnvVars);
    settings.stdout(listener.getLogger());
    settings.stderr(listener.getLogger());
    settings.pwd(containingFolder);

    Proc proc = settings.start();
    int exitCode = proc.join();
    if (exitCode > 0) {
      return false;
    }
    return true;
  }

  Boolean executeCommand(String command, List<String> args) throws IOException, InterruptedException {
    return this.executeCommand(command, args, null);
  }

  private Boolean validate() throws IOException, InterruptedException {
    if (!this.parseVagrantEnvironment()) {
      listener.getLogger().println("Error parsing environment");
      return false;
    }

    List<FilePath> dirList = this.containingFolder.list();
    if (dirList == null) {
      listener.getLogger().println("Failed to iterate on remote directory " + this.containingFolder.getName());
      return false;
    }

    ListIterator i = dirList.listIterator();
    Boolean error_found = false;
    while (i.hasNext()) {
      FilePath file = (FilePath) i.next();
      if (file.getName().equals(this.vagrantFileName)) {
        error_found = false;
        break;
      }
    }
    if (error_found) {
      listener.getLogger().println("Failed to find Vagrantfile \"" + this.vagrantFileName + "\" in folder \"" + this
              .containingFolder.getRemote());
      return false;
    }

    Launcher.ProcStarter settings = launcher.launch();
    settings.cmds("vagrant", "-v");
    settings.envs(build.getEnvironment(listener));
    settings.stdout(listener.getLogger());
    settings.stderr(listener.getLogger());
    settings.pwd(build.getWorkspace());

    Proc proc = settings.start();
    int exitCode = proc.join();
    if (exitCode > 0) {
      return false;
    }
    return true;
  }
}
