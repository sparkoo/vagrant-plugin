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

  private boolean validated;

  public VagrantWrapper(String vagrantFile, String vagrantVm) {
    this.vagrantFile = vagrantFile;
    this.vagrantVm = vagrantVm;
    this.validated = false;
  }

  /**
   *
   * @return
   */
  public String getVagrantFileName() {
    return this.vagrantFileName;
  }

  /**
   *
   * @param build
   */
  public void setBuild(AbstractBuild build) {
    this.build = build;
  }

  /**
   *
   * @param launcher
   */
  public void setLauncher(Launcher launcher) {
    this.launcher = launcher;
  }

  /**
   *
   * @param listener
   */
  public void setListener(BuildListener listener) {
    this.listener = listener;
  }

  /**
   *
   * @return
   */

  public FilePath getContainingFolder() {
    return this.containingFolder;
  }

  /**
   *
   * @return
   */
  public String getVagrantFile() {
    return this.vagrantFile;
  }

  /**
   *
   * @return
   */
  public String getVagrantVm() {
    return this.vagrantVm;
  }

  /**
   *
   */
  protected void parseVagrantEnvironment() {
    if (this.vagrantFile == null) {
      this.vagrantFileName = "Vagrantfile";
      this.containingFolder = build.getWorkspace();
    } else {
      File file = new File(vagrantFile);
      this.vagrantFileName = file.getName();
      this.containingFolder = new FilePath(launcher.getChannel(), file.getAbsoluteFile().getParent());
    }
  }

  /**
   *
   * @param data
   */
  public void log(Object data) {
    listener.getLogger().print("[ vagrant ]: ");
    listener.getLogger().println(data);
  }

  /**
   *
   * @param command
   * @param args
   * @param additionalEnvVars
   * @return
   * @throws IOException
   * @throws InterruptedException
   */
  protected Boolean executeCommand(String command, List<String> args, Map<String, String> additionalEnvVars) throws IOException, InterruptedException {
    TreeMap<String,String> joinedEnvVars = new TreeMap<String, String>();

    if (!this.validated) {
      this.validate();
    }

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

  /**
   *
   * @param command
   * @param args
   * @return
   * @throws IOException
   * @throws InterruptedException
   */
  protected Boolean executeCommand(String command, List<String> args) throws IOException, InterruptedException {
    return this.executeCommand(command, args, null);
  }

  /**
   *
   * @return
   * @throws IOException
   * @throws InterruptedException
   */
  private Boolean validate() throws IOException, InterruptedException{
    if (this.vagrantFileName == null || this.containingFolder == null) {
      this.parseVagrantEnvironment();
    }

    List<FilePath> dirList = this.containingFolder.list();
    if (dirList == null) {
      listener.getLogger().println("Failed to iterate on remote directory " + this.containingFolder.getName());
      return false;
    }

    ListIterator i = dirList.listIterator();
    Boolean error_found = false;
    while (i.hasNext()) {
      FilePath file = (FilePath)i.next();
      if (file.getName().equals(this.vagrantFileName)) {
        error_found = false;
        break;
      }
    }
    if (error_found) {
      listener.getLogger().println("Failed to find Vagrantfile \"" + this.vagrantFileName + "\" in folder \"" + this.containingFolder.getRemote());
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
