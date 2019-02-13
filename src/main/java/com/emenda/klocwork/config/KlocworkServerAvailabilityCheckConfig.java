
package com.emenda.klocwork.config;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class KlocworkServerAvailabilityCheckConfig extends AbstractDescribableImpl<KlocworkServerAvailabilityCheckConfig> {

    private final boolean serverStatus;

    @DataBoundConstructor
    public KlocworkServerAvailabilityCheckConfig(boolean serverStatus) {
        this.serverStatus = serverStatus;
    }

    public ArgumentListBuilder getVersionCmd() {
        ArgumentListBuilder versionCmd = new ArgumentListBuilder("kwadmin");
        versionCmd.add("--version");
        return versionCmd;
    }

    public boolean getServerStatus() {
        return serverStatus;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<KlocworkServerAvailabilityCheckConfig> {
        public String getDisplayName() { return null; }
    }

}
