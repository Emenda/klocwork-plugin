package com.emenda.klocwork.util;

import com.emenda.klocwork.KlocworkConstants;

import org.apache.commons.lang3.StringUtils;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Project;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.InterruptedException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KlocworkUtil {

    public static void validateServerConfigs(EnvVars envVars) throws AbortException {
        validateServerURL(envVars);
        validateServerProject(envVars);
    }

    public static void validateServerURL(EnvVars envVars) throws AbortException {
        if (StringUtils.isEmpty(envVars.get(KlocworkConstants.KLOCWORK_URL))) {
            throw new AbortException("Klocwork Server not specified. Klocwork " +
            "servers are configured on the Jenkins global configuration page and " +
            "referenced under Build Environment settings on the Job configuration " +
            "page.");
        }
    }

    public static void validateServerProject(EnvVars envVars) throws AbortException {
        if (StringUtils.isEmpty(envVars.get(KlocworkConstants.KLOCWORK_PROJECT))) {
            throw new AbortException("Klocwork Server Project not specified. " +
            "Server projects are provided under Build Environment settings on the " +
            "Job configuration page.");
        }
    }

    public static String[] getLtokenValues(EnvVars envVars, Launcher launcher) throws AbortException {
        try {
            String[] ltokenLine = launcher.getChannel().call(
                new KlocworkLtokenFetcher(
                getAndExpandEnvVar(envVars, KlocworkConstants.KLOCWORK_URL)));

            if (ltokenLine.length < 4) {
                throw new IOException("Error: ltoken string returned is too short: " +
                "\"" + Arrays.toString(ltokenLine) + "\"");
            } else if (StringUtils.isEmpty(ltokenLine[KlocworkConstants.LTOKEN_USER_INDEX])) {
                throw new IOException("Error: ltoken invalid. Reason: user is empty" +
                "\"" + Arrays.toString(ltokenLine) + "\"");
            }  else if (StringUtils.isEmpty(ltokenLine[KlocworkConstants.LTOKEN_HASH_INDEX])) {
                throw new IOException("Error: ltoken invalid. Reason: ltoken is empty" +
                "\"" + Arrays.toString(ltokenLine) + "\"");
            } else {
                return ltokenLine;
            }
        } catch (IOException | InterruptedException ex) {
            throw new AbortException(ex.getMessage());
        }
    }

    // public static String exceptionToString(Exception e) {
    //     StringWriter sw = new StringWriter();
    //     PrintWriter pw = new PrintWriter(sw);
    //     e.printStackTrace(pw);
    //     return sw.toString();
    // }

    public static String getAndExpandEnvVar(EnvVars envVars, String var) {
        String value = envVars.get(var, "");
        if (StringUtils.isEmpty(value)) {
            return ""; // TODO - handle empty vs null
        }
        return envVars.expand(value);
    }

    public static String getKlocworkProjectUrl(EnvVars envVars) throws AbortException {
        try {
            // handle URLs ending with "/", e.g. http://kwserver:8080/
            String urlStr = getAndExpandEnvVar(envVars, KlocworkConstants.KLOCWORK_URL);
            String separator = (urlStr.endsWith("/")) ? "" : "/";
            URL url = new URL(urlStr + separator +
                getAndExpandEnvVar(envVars, KlocworkConstants.KLOCWORK_PROJECT));
            return url.toString();
        } catch (MalformedURLException ex) {
            throw new AbortException(ex.getMessage());
        }
    }

    public static String getBuildSpecFile(EnvVars envVars)
                    throws AbortException {
        String envBuildSpec = getAndExpandEnvVar(envVars, KlocworkConstants.KLOCWORK_BUILD_SPEC);
        return (StringUtils.isEmpty(envBuildSpec)) ? KlocworkConstants.DEFAULT_BUILD_SPEC : envBuildSpec;
    }

    public static String getBuildSpecPath(EnvVars envVars, FilePath workspace)
                    throws AbortException {
        return (new FilePath(workspace, getBuildSpecFile(envVars))).getRemote();
    }

    public static String getKwtablesDir(String tablesDir) {
        return (StringUtils.isEmpty(tablesDir)) ? KlocworkConstants.DEFAULT_TABLES_DIR : tablesDir;
    }

    public static int executeCommand(Launcher launcher, BuildListener listener,
                        FilePath buildDir, EnvVars envVars, ArgumentListBuilder cmds) throws AbortException {
        return executeCommand(launcher, listener, buildDir, envVars, cmds, false);
    }

    public static int executeCommand(Launcher launcher, BuildListener listener,
                        FilePath buildDir, EnvVars envVars, ArgumentListBuilder cmds,
                        boolean ignoreReturnCode)
                        throws AbortException {
        if (launcher.isUnix()) {
            cmds = new ArgumentListBuilder("/bin/sh", "-c", cmds.toString());
        } else {
            cmds = cmds.toWindowsCommand();
        }
        try {
            int returnCode = launcher.launch().
                stdout(listener).stderr(listener.getLogger()).
                pwd(buildDir).envs(envVars).cmds(cmds)
                .join();
            listener.getLogger().println("Return code: " + Integer.toString(returnCode));
            if (!ignoreReturnCode && returnCode != 0) {
                throw new AbortException("Non-zero Return Code. Aborting.");
            } else {
                return returnCode;
            }
        } catch (IOException | InterruptedException ex) {
            throw new AbortException(ex.getMessage());
        }
    }

    public static Object getInstanceOfBuilder(Class<? extends Builder> classType, AbstractBuild<?,?> build) {
        AbstractProject p = build.getProject();
        List<Builder> builders;
        if (p instanceof Project) {
            builders = ((Project) p).getBuilders();
        } else if (p instanceof MatrixProject) {
            builders = ((MatrixProject) p).getBuilders();
        } else {
            builders = Collections.emptyList();
        }

        for (Builder builder : builders) {
            if (classType.isInstance(builder)) {
                return builder;
            }
        }
        return null;
    }


}