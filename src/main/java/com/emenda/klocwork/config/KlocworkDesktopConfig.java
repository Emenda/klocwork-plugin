
package com.emenda.klocwork.config;

import com.emenda.klocwork.KlocworkConstants;
import com.emenda.klocwork.util.KlocworkBuildSpecParser;
import com.emenda.klocwork.util.KlocworkUtil;

import org.apache.commons.lang3.StringUtils;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;

import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;

import java.io.IOException;
import java.lang.InterruptedException;
import java.net.URL;
import java.util.List;

public class KlocworkDesktopConfig extends AbstractDescribableImpl<KlocworkDesktopConfig> {

    private final String buildSpec;
    private final String projectDir;
    private final boolean cleanupProject;
    private final String reportFile;
    private final String additionalOpts;
    // private final boolean setupKwdtagent;
    // private final String kwdtagentPort;
    private final boolean incrementalAnalysis;
    private final KlocworkDiffAnalysisConfig diffAnalysisConfig;

    @DataBoundConstructor
    public KlocworkDesktopConfig(String buildSpec, String projectDir, boolean cleanupProject, String reportFile, String additionalOpts,
    // boolean setupKwdtagent, String kwdtagentPort,
    boolean incrementalAnalysis, KlocworkDiffAnalysisConfig diffAnalysisConfig) {
        this.buildSpec = buildSpec;
        this.projectDir = projectDir;
        this.cleanupProject = cleanupProject;
        this.reportFile = reportFile;
        this.additionalOpts = additionalOpts;
        // this.setupKwdtagent = setupKwdtagent;
        // this.kwdtagentPort = kwdtagentPort;
        this.incrementalAnalysis = incrementalAnalysis;
        this.diffAnalysisConfig = diffAnalysisConfig;
    }

    public ArgumentListBuilder getVersionCmd()
                                        throws IOException, InterruptedException {
        ArgumentListBuilder versionCmd = new ArgumentListBuilder("kwcheck");
        versionCmd.add("--version");
        return versionCmd;
    }

    public ArgumentListBuilder getKwcheckCreateCmd(EnvVars envVars, FilePath workspace)
                                        throws IOException, InterruptedException {

        validateParentProjectDir(getKwlpDir(workspace, envVars).getParent());

        ArgumentListBuilder kwcheckCreateCmd = new ArgumentListBuilder("kwcheck", "create");
        String projectUrl = KlocworkUtil.getKlocworkProjectUrl(envVars);
        if (!StringUtils.isEmpty(projectUrl)) {
            kwcheckCreateCmd.add("--url", projectUrl);
        }
        kwcheckCreateCmd.add("--project-dir", getKwlpDir(workspace, envVars).getRemote());
        kwcheckCreateCmd.add("--settings-dir", getKwpsDir(workspace, envVars).getRemote());
        kwcheckCreateCmd.add("--build-spec", envVars.expand(KlocworkUtil.getDefaultBuildSpec(buildSpec)));
        return kwcheckCreateCmd;
    }

    public ArgumentListBuilder getKwcheckSetCmd(EnvVars envVars, FilePath workspace)
                                        throws IOException, InterruptedException {

        validateParentProjectDir(getKwlpDir(workspace, envVars).getParent());

        ArgumentListBuilder kwcheckSetCmd = new ArgumentListBuilder("kwcheck", "set");
        kwcheckSetCmd.add("--project-dir", getKwlpDir(workspace, envVars).getRemote());
        String serverUrl = envVars.get(KlocworkConstants.KLOCWORK_URL);
        if (!StringUtils.isEmpty(serverUrl)) {
            URL url = new URL(serverUrl);
            kwcheckSetCmd.add("klocwork.host=" + url.getHost());
            kwcheckSetCmd.add("klocwork.port=" + Integer.toString(url.getPort()));
            kwcheckSetCmd.add("klocwork.project=" + envVars.get(KlocworkConstants.KLOCWORK_PROJECT));
        }
        return kwcheckSetCmd;
    }

    public ArgumentListBuilder getKwcheckListCmd(EnvVars envVars, FilePath workspace,
        String diffList)
                                        throws IOException, InterruptedException {
        ArgumentListBuilder kwcheckRunCmd =
            new ArgumentListBuilder("kwcheck", "list");
        kwcheckRunCmd.add("--project-dir", getKwlpDir(workspace, envVars).getRemote());
        String licenseHost = envVars.get(KlocworkConstants.KLOCWORK_LICENSE_HOST);
        if (!StringUtils.isEmpty(licenseHost)) {
            kwcheckRunCmd.add("--license-host", licenseHost);
        }

        String licensePort = envVars.get(KlocworkConstants.KLOCWORK_LICENSE_PORT);
        if (!StringUtils.isEmpty(licensePort)) {
            kwcheckRunCmd.add("--license-port", licensePort);
        }

        //TODO: Cleanup here
//        String xmlReport = envVars.expand(KlocworkUtil.getDefaultKwcheckReportFile(reportFile));
//        kwcheckRunCmd.add("-F", "xml", "--report", xmlReport);
        kwcheckRunCmd.add("-F", "scriptable");

        if (!StringUtils.isEmpty(additionalOpts)) {
            kwcheckRunCmd.addTokenized(envVars.expand(additionalOpts));
        }

        // add list of changed files to end of kwcheck run command
        kwcheckRunCmd.addTokenized(diffList);

        return kwcheckRunCmd;
    }

    public ArgumentListBuilder getKwcheckRunCmd(EnvVars envVars, FilePath workspace,
        String diffList)
                                        throws IOException, InterruptedException {
        ArgumentListBuilder kwcheckRunCmd =
            new ArgumentListBuilder("kwcheck", "run");
        kwcheckRunCmd.add("--project-dir", getKwlpDir(workspace, envVars).getRemote());

        if (!StringUtils.isEmpty(envVars.get(KlocworkConstants.KLOCWORK_LICENSE_HOST))) {
            kwcheckRunCmd.add("--license-host", envVars.get(KlocworkConstants.KLOCWORK_LICENSE_HOST));
            if (!StringUtils.isEmpty(envVars.get(KlocworkConstants.KLOCWORK_LICENSE_PORT))) {
                kwcheckRunCmd.add("--license-port", envVars.get(KlocworkConstants.KLOCWORK_LICENSE_PORT));
            }
        }

        //TODO: Clean up here
//        String xmlReport = envVars.expand(KlocworkUtil.getDefaultKwcheckReportFile(reportFile));
//        kwcheckRunCmd.add("-F", "xml", "--report", xmlReport);
        kwcheckRunCmd.add("-Y", "-L"); // Report nothing

        kwcheckRunCmd.add("--build-spec", envVars.expand(KlocworkUtil.getDefaultBuildSpec(buildSpec)));
        if (!StringUtils.isEmpty(additionalOpts)) {
            kwcheckRunCmd.addTokenized(envVars.expand(additionalOpts));
        }

        // add list of changed files to end of kwcheck run command
        kwcheckRunCmd.addTokenized(diffList);

        return kwcheckRunCmd;
    }

    // public ArgumentListBuilder getKwdtagentCmd(EnvVars envVars, FilePath workspace)
    //                                     throws IOException, InterruptedException {
    //     ArgumentListBuilder kwdtagentCmd =
    //         new ArgumentListBuilder("kwdtagent");
    //     kwdtagentCmd.add("--project-dir", getKwlpDir(workspace, envVars).getRemote());
    //     kwdtagentCmd.add("--port", kwdtagentPort);
    //     return kwdtagentCmd;
    // }

    public ArgumentListBuilder getGitDiffCmd(EnvVars envVars) {
        ArgumentListBuilder gitDiffCmd = new ArgumentListBuilder("git");
        gitDiffCmd.add("diff", "--name-only", envVars.expand(diffAnalysisConfig.getGitPreviousCommit()));
        gitDiffCmd.add(">", getDiffFileList(envVars));
        return gitDiffCmd;
    }

    /*
    function to check if a local project already exists.
    If the creation of a project went wrong before, there may be some left over .kwlp or .kwps directories
    so we need to make sure to clean these up.
    If both .kwlp and .kwps exist then we reuse them
     */
    public boolean hasExistingProject(FilePath workspace, EnvVars envVars)
        throws IOException, InterruptedException {
        FilePath kwlp = getKwlpDir(workspace, envVars);
        FilePath kwps = getKwpsDir(workspace, envVars);

        if (cleanupProject) {
            // cleanup is forced
            cleanupExistingProject(kwlp, kwps);
        }
        // else check if a cleanup is needed because a kwcheck create command
        // failed and left some things lying around that will make it fail
        // next time...
        if (kwlp.exists()) {
            if (kwps.exists()) {
                // both directories exist
                return true;
            } else {
                // clean up directories because something has gone wrong
                cleanupExistingProject(kwlp, kwps);
                return false;
            }
        } else if (kwps.exists()) {
            // clean up directories because something has gone wrong
            cleanupExistingProject(kwlp, kwps);
            return false;
        } else {
            // no existing project
            return false;
        }
    }

    private void validateParentProjectDir(FilePath dir) throws IOException, InterruptedException {
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private FilePath getKwlpDir(FilePath workspace, EnvVars envVars) {
        return new FilePath(
            workspace.child(envVars.expand(projectDir)), ".kwlp");
    }

    private FilePath getKwpsDir(FilePath workspace, EnvVars envVars) {
        return new FilePath(
            workspace.child(envVars.expand(projectDir)), ".kwps");
    }

    private void cleanupExistingProject(FilePath kwlp, FilePath kwps)
        throws IOException, InterruptedException {
        if (kwlp.exists()) {
            kwlp.deleteRecursive();
        }
        if (kwps.exists()) {
            kwps.deleteRecursive();
        }
    }

    public String getKwcheckDiffList(EnvVars envVars, FilePath workspace, Launcher launcher) throws AbortException {
        try {
            List<String> fileList = launcher.getChannel().call(
                new KlocworkBuildSpecParser(workspace.getRemote(),
                    envVars.expand(getDiffFileList(envVars)),
                    envVars.expand(KlocworkUtil.getBuildSpecPath(buildSpec, workspace))));
            return String.join(" ", fileList); // TODO: is Java 8 OK?
        } catch (IOException | InterruptedException ex) {
            throw new AbortException(ex.getMessage());
        }

    }

    public String getDiffFileList(EnvVars envVars) {
        String diffFileList = envVars.expand(diffAnalysisConfig.getDiffFileList());
        return diffFileList;
    }

    public boolean isGitDiffType() {
        return diffAnalysisConfig.isGitDiffType();
    }

    public String getBuildSpec() { return buildSpec; }
    public String getProjectDir() { return projectDir; }
    public boolean getCleanupProject() { return cleanupProject; }
    public String getReportFile() { return reportFile; }
    public String getAdditionalOpts() { return additionalOpts; }
    // public boolean getSetupKwdtagent() { return setupKwdtagent; }
    // public String getKwdtagentPort() { return kwdtagentPort; }
    public boolean getIncrementalAnalysis() { return incrementalAnalysis; }
    public KlocworkDiffAnalysisConfig getDiffAnalysisConfig() { return diffAnalysisConfig; }

    @Extension
    public static class DescriptorImpl extends Descriptor<KlocworkDesktopConfig> {
        public String getDisplayName() { return null; }
    }


}
