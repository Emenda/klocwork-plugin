package com.emenda.klocwork.config;

import com.emenda.klocwork.KlocworkConstants;
import com.emenda.klocwork.KlocworkLogger;
import com.emenda.klocwork.services.KlocworkApiConnection;
import com.emenda.klocwork.util.KlocworkUtil;
import hudson.*;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;


public class KlocworkFileFilterConfig extends AbstractDescribableImpl<KlocworkFileFilterConfig> {

    private final String exclude;
    private final String include;
    private final boolean caseSensitive;
    private final boolean slashInsensitive;
    private final String modules;
    private final boolean checkModule;

    @DataBoundConstructor
    public KlocworkFileFilterConfig (String exclude,String include, boolean caseSensitive, boolean slashInsensitive,
                                     String modules, boolean checkModule) {
        this.exclude = exclude;
        this.include = include;
        this.caseSensitive = caseSensitive;
        this.slashInsensitive = slashInsensitive;
        this.modules = modules;
        this.checkModule = checkModule;
    }

    public String createDiffFileList(List<String> files, EnvVars envVars, FilePath workspace, Launcher launcher,
                                     KlocworkLogger logger) throws IOException {
        String diffFile = "file_filter_list.txt";
        if ((!include.equals("") || !exclude.equals("")) && !modules.equals("")) {
            throw new AbortException("cannot handle both filepaths and modules");
        }
        if (!include.equals("") || !exclude.equals("")) {
            filepath(include, exclude, files, caseSensitive, slashInsensitive, workspace, diffFile);
            return "@" + diffFile;
        }
        else if (!modules.equals("")) {
            int code = module(modules, files, caseSensitive, slashInsensitive, checkModule, workspace, diffFile, envVars, launcher, logger);
            if(code == 0) {
                return "@" + diffFile;
            }
            else {
                throw new AbortException("could not find module(s)");
            }
        }
        else {
            return "";
        }
    }

    public List<String> getLookFiles(FilePath look, boolean incremental) {
        List<String> files = new ArrayList<>();
        FileReader read;
        BufferedReader input;
        try {
            read = new FileReader(look.getRemote());
            input = new BufferedReader(read);
            String s;
            while (true) {
                s = input.readLine();
                if (s == null) {
                    break;
                }
                else {
                    if (!s.endsWith(">")) {
                        if (incremental) {
                            files.add(s);
                        } else {
                            files.add(s.split(";")[1]);
                        }
                    }
                }
            }
            read.close();
            input.close();
        }
        catch (IOException e) {e.printStackTrace();}

        return files;
    }

    private static void filepath(String include, String exclude, List<String> files, boolean caseSensitive, boolean slash,
                                 FilePath workspace, String diffFile) {
        String[] excludeFiles = exclude.split(",");
        String[] includeFiles = include.split(",");

        List<String> ex = transformFilesRegex(excludeFiles, caseSensitive, slash, false);
        List<String> in = transformFilesRegex(includeFiles, caseSensitive, slash, false);

        List<String> finalFiles;
        if (!exclude.equals("")) {
            if (!include.equals("")) {
                finalFiles = compareFilesList(files, ex, true);
                finalFiles = compareFilesList(finalFiles, in, false);
            }
            else {
                finalFiles = compareFilesList(files, ex, true);
            }
        }
        else {
            finalFiles = compareFilesList(files, in, false);
        }

        FilePath end = new FilePath(workspace, diffFile);
        FileWriter write = null;
        try {
            write = new FileWriter(end.getRemote(), false);
        }
        catch (IOException e) {e.printStackTrace();}
        PrintWriter diff = new PrintWriter(write);
        for (String i : finalFiles) {
            diff.println(i);
        }
        diff.close();
    }


    private static int module(String moduli, List<String> files, boolean sens, boolean slash, boolean checkModule,
                              FilePath workspace, String diffFile, EnvVars envVars, Launcher launcher, KlocworkLogger logger) {
        String[] modules = moduli.split(",");
        String url = envVars.get(KlocworkConstants.KLOCWORK_URL);
        String request = "action=modules&project=" + envVars.get(KlocworkConstants.KLOCWORK_PROJECT);
        List<String> paths = new ArrayList<>();
        boolean test = false;
        try {
            String[] ltokenlines = KlocworkUtil.getLtokenValues(envVars, launcher);
            KlocworkApiConnection connection = new KlocworkApiConnection(url,
                    ltokenlines[KlocworkConstants.LTOKEN_USER_INDEX], ltokenlines[KlocworkConstants.LTOKEN_HASH_INDEX]);
            JSONArray response = connection.sendRequest(request);
            int p;
            for (String modul : modules) {
                p = paths.size();
                for (int i = 0; i < response.size(); i++) {
                    JSONObject object = response.getJSONObject(i);
                    if (modul.equals(object.getString("name"))) {
                        for (String path : object.getString("paths").split("\",\"")) {
                            if (path.startsWith("[\"")) {
                                path = path.substring(2);
                            }
                            if (path.endsWith("\"]")) {
                                int l = path.length();
                                path = path.substring(0, l-2);
                            }
                            paths.add(path);
                        }
                    }
                }
                if (p == paths.size()) {
                    test = true;
                    logger.logMessage(String.format("Module \"%s\" has not been found", modul));
                }
            }
        }
        catch (IOException e) {e.printStackTrace();}
        if (test && checkModule) {
            return 1;
        }
        String[] Paths = paths.toArray(new String[0]);
        List<String> modulePaths = transformFilesRegex(Paths, sens, slash, true);
        List<String> finalFiles = compareFilesList(files, modulePaths, false);
        FilePath end = new FilePath(workspace, diffFile);
        FileWriter write = null;
        try {
            write = new FileWriter(end.getRemote(), false);
        }
        catch (IOException e) {e.printStackTrace();}
        PrintWriter diff = new PrintWriter(write);
        for (String i : finalFiles) {
            diff.println(i);
        }
        diff.close();
        return 0;
    }


    private static List<String> compareFilesList(List<String> start, List<String> compare, boolean exclude) {
        List<String> finalFiles = new ArrayList<>();
        for (String c : compare) {
            for (String s : start) {
                if (s.matches(c) && !exclude) {
                    finalFiles.add(s);
                }
                else if (!s.matches(c) && exclude) {
                    finalFiles.add(s);
                }
            }
        }
        return finalFiles;
    }

    private static List<String> transformFilesRegex(String[] files, boolean sens, boolean slash, boolean module) {
        List<String> fil = new ArrayList<>();
        for (String fi : files) {
            fi = fi.replaceAll("\\*\\*", Matcher.quoteReplacement(".+"));
            fi = fi.replaceAll("\\*", Matcher.quoteReplacement("[^\\]+"));
            fi = fi.replaceAll("\\.(?!\\+)", "\\.");
            fi = fi.replaceAll("$", Matcher.quoteReplacement("$"));
            if (sens) {
                fi = fi.replaceAll("^", Matcher.quoteReplacement("^"));
            }
            else {
                fi = fi.replaceAll("^", Matcher.quoteReplacement("(?i)^"));
            }
            if (module) {
                fi = fi.replaceAll("\\\\\\\\", "\\\\");
            }
            if (slash) {
                fi = fi.replaceAll("\\\\", "/");
                fil.add(fi);
                fi = fi.replaceAll("/", "\\\\");
                fi = fi.replaceAll("\\\\", "\\\\\\\\");
                fil.add(fi);
            }
            else {
                if (fi.matches(".*/.*")) {
                    fi = fi.replaceAll("\\\\", "/");
                }
                else {
                    fi = fi.replaceAll("\\\\", Matcher.quoteReplacement("\\\\"));
                }
                fil.add(fi);
            }
        }
        return fil;
    }


    public String getExclude() { return exclude; }
    public String getInclude() { return include; }
    public boolean isCaseSensitive() { return caseSensitive; }
    public boolean isSlashInsensitive() { return slashInsensitive; }
    public String getModules() { return modules; }
    public boolean isCheckModule() {return checkModule; }

    @Extension
    public static class DescriptorImpl extends Descriptor<KlocworkFileFilterConfig> {
        public String getDisplayName() { return null; }
    }
}


