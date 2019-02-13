package com.emenda.klocwork;

import com.emenda.klocwork.KlocworkConstants;
import com.emenda.klocwork.config.KlocworkServerAvailabilityCheckConfig;
import com.emenda.klocwork.util.KlocworkUtil;
import hudson.*;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.List;
import hudson.util.ArgumentListBuilder;

import java.io.IOException;

public class KlocworkServerAvailabilityCheckBuilder extends Builder implements SimpleBuildStep {

    private KlocworkServerAvailabilityCheckConfig buildSpecConfig;

    @DataBoundConstructor
    public KlocworkServerAvailabilityCheckBuilder(KlocworkServerAvailabilityCheckConfig buildSpecConfig) {
        this.buildSpecConfig = buildSpecConfig;
    }

    public KlocworkServerAvailabilityCheckConfig getBuildSpecConfig() {
        return buildSpecConfig;
    }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
        throws AbortException {

        EnvVars envVars = null;
        try {
            envVars = build.getEnvironment(listener);
        } catch (IOException | InterruptedException ex) {
            throw new AbortException(ex.getMessage());
        }
        perform(build, envVars, workspace, launcher, listener);
    }

    public void perform(Run<?, ?> build, EnvVars envVars, FilePath workspace, Launcher launcher, TaskListener listener)
        throws AbortException {

        KlocworkLogger logger = new KlocworkLogger("ServerAvailabilityCheckBuilder", listener.getLogger());
        logger.logMessage("Starting Klocwork Server Availability Check Step");


        // TODO: validate server settings needed for server check step. AbortException
        //should be thrown if URL is not provided as we cannot perform
        // a check without this.
        //KlocworkUtil.validateServerConfigs(envVars);


        if (buildSpecConfig.getServerStatus()) {
            logger.logMessage("Making sure Klocwork server is online...");

            ArgumentListBuilder kwadminCmd = new ArgumentListBuilder("kwadmin");
                kwadminCmd.add("--url", envVars.get(KlocworkConstants.KLOCWORK_URL));
                kwadminCmd.add("list-projects");

            //KlocworkUtil.executeCommand(launcher, listener, workspace, envVars, kwadminCmd);

            int exitCode = KlocworkUtil.executeCommand(launcher, listener,
                    workspace, envVars,
                    kwadminCmd, true);
            if (exitCode != 0) {
              //TODO: Set up email to support@emenda.se
              throw new AbortException("Non-zero return code: " + Integer.toString(exitCode));
            } else {
              logger.logMessage("Successfully established connection to the Klocwork Server at " + envVars.get(KlocworkConstants.KLOCWORK_URL));
            }
        } else {
            logger.logMessage("Not checking Klocwork server availability.");
        }



        //String cmd =  "kwadmin --url http://10.0.2.15:8080/ list-projects";


    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }


    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public String getDisplayName() {
            return KlocworkConstants.KLOCWORK_SERVER_AVAILABILITY_CHECK_DISPLAY_NAME;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req,formData);
        }
    }
}
