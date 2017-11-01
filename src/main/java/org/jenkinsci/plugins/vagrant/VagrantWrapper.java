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
import java.util.ArrayList;
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
    final String vagrantFileToUse;
    final String vagrantVmToUse;

    VagrantBuildSettings globalSettings = extractSettingsFromBuild(build);
    if (globalSettings == null) {
      vagrantFileToUse = vagrantFile;
      vagrantVmToUse = vagrantVm;
    } else {
      if (!isEmptyOrNull(vagrantFile)) {
        vagrantFileToUse = vagrantFile;
      } else {
        vagrantFileToUse = globalSettings.getVagrantFile();
      }

      if (!isEmptyOrNull(vagrantVm)) {
        vagrantVmToUse = vagrantVm;
      } else {
        vagrantVmToUse = globalSettings.getVagrantVm();
      }
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
    this.validate();

    TreeMap<String, String> joinedEnvVars = new TreeMap<>(build.getEnvironment(listener));
    if (additionalEnvVars != null) {
      joinedEnvVars.putAll(additionalEnvVars);
    }

    List<String> commandWithArgs = constructFullCommand(command, args);
    this.log("Executing command :" + commandWithArgs.toString() + " in folder " + this.getContainingFolder().getRemote());

    Launcher.ProcStarter settings = launcher.launch();
    settings.cmds(commandWithArgs);
    settings.envs(joinedEnvVars);
    settings.stdout(listener.getLogger());
    settings.stderr(listener.getLogger());
    settings.pwd(containingFolder);

    Proc proc = settings.start();
    int exitCode = proc.join();
    return exitCode <= 0;
  }

  private List<String> constructFullCommand(String command, List<String> args) {
    List<String> commandWithArgs = new ArrayList<>();

    commandWithArgs.add("vagrant");
    commandWithArgs.add(command);

    if (this.getVagrantVm() != null && !this.getVagrantVm().isEmpty()) {
      commandWithArgs.add(this.getVagrantVm());
    }

    commandWithArgs.addAll(args);

    return commandWithArgs;
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
