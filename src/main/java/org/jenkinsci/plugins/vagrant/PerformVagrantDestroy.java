package org.jenkinsci.plugins.vagrant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class PerformVagrantDestroy {
  boolean performVagrantVmDestroy(VagrantWrapper wrapper) {
    List<String> arg = new ArrayList<>();

    arg.add("--force");

    try {
      return wrapper.executeCommand("destroy", arg);
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
}
