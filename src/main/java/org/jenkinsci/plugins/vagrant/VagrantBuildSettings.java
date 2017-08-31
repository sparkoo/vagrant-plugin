package org.jenkinsci.plugins.vagrant;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class VagrantBuildSettings extends BuildWrapper {
    private final String vagrantFile;
    private final String vagrantVm;
    private final boolean destroyAfterBuild;

    @SuppressWarnings("WeakerAccess")
    @Extension
    public static final BuildWrapperDescriptor DESCRIPTOR = new DescriptorImpl();

    @DataBoundConstructor
    public VagrantBuildSettings(String vagrantFile, String vagrantVm, boolean destroyAfterBuild) {
        this.vagrantFile = vagrantFile;
        this.vagrantVm = vagrantVm;
        this.destroyAfterBuild = destroyAfterBuild;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException,
            InterruptedException {
        return new VagrantBuildSettingsEnvironment();
    }

    public String getVagrantFile() {
        return vagrantFile;
    }

    public String getVagrantVm() {
        return vagrantVm;
    }

    public boolean isDestroyAfterBuild() {
        return destroyAfterBuild;
    }

    @Override
    public Descriptor<BuildWrapper> getDescriptor() {
        return DESCRIPTOR;
    }

    private static class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Vagrant settings";
        }
    }

    public class VagrantBuildSettingsEnvironment extends Environment {
        public VagrantBuildSettings getBuildSettings() {
            return VagrantBuildSettings.this;
        }

        @Override
        public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
            if (isDestroyAfterBuild()) {
                VagrantWrapper wrapper = VagrantWrapper.createVagrantWrapper(vagrantFile, vagrantVm, build, null, listener);
                return new PerformVagrantDestroy().performVagrantVmDestroy(wrapper);

            }
            return true;
        }
    }
}
