package com.emenda.klocwork;

import com.emenda.klocwork.config.KlocworkReportConfig;
import com.emenda.klocwork.util.KlocworkUtil;

import hudson.EnvVars;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.tasks.SimpleBuildStep;

import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class KlocworkBuildAction implements Action, SimpleBuildStep.LastBuildAction {

    private final Run<?, ?> run;
    private final int criticalCount;
    private final int errorCount;
    private final int warningCount;
    private final int reviewCount;
    private final String buildName;
    private final String klocworkURL;
    private final String klocworkProject;
    private final String klocworkProjectId;
    private final KlocworkReportConfig reportConfig;

    public KlocworkBuildAction(Run<?, ?> run,
            Map<String, Integer> severityMap,
            EnvVars envVars,
            String buildName,
            KlocworkReportConfig reportConfig
    ) {
        this.run = run;
        this.criticalCount = severityMap.getOrDefault(KlocworkConstants.KLOCWORK_ISSUE_CRITICAL, 0);
        this.errorCount = severityMap.getOrDefault(KlocworkConstants.KLOCWORK_ISSUE_ERROR, 0);
        this.warningCount = severityMap.getOrDefault(KlocworkConstants.KLOCWORK_ISSUE_WARNING, 0);
        this.reviewCount = severityMap.getOrDefault(KlocworkConstants.KLOCWORK_ISSUE_REVIEW, 0);
        this.buildName = KlocworkUtil.getDefaultBuildName(buildName, envVars);
        this.klocworkURL = KlocworkUtil.getNormalizedKlocworkUrl(envVars);
        this.klocworkProject = envVars.get(KlocworkConstants.KLOCWORK_PROJECT);
        this.klocworkProjectId = envVars.get(KlocworkConstants.KLOCWORK_PROJECT_ID);
        this.reportConfig = reportConfig;
    }

    public Run<?, ?> getRun() {
        return run;
    }

    @Override
    public String getIconFileName() {
        return KlocworkConstants.ICON_URL;
    }

    @Override
    public String getDisplayName() {
        return KlocworkConstants.DISPLAY_NAME;
    }

    @Override
    public String getUrlName() {
        try {
            if ((klocworkProjectId == null) || StringUtils.isEmpty(klocworkProjectId))
            {
                return KlocworkUtil.getBuildIssueListUrl(klocworkURL, klocworkProject, buildName);
            }
            else
            {
                return KlocworkUtil.getBuildIssueListUrl(klocworkURL, klocworkProjectId, buildName);
            }
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    public int getCriticalCount() {
        return criticalCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public String getBuildName() {
        return buildName;
    }

    public String getKlocworkURL() {
        return klocworkURL;
    }

    public String getKlocworkProject() {
        return klocworkProject;
    }

    public String getKlocworkProjectId() {
        return klocworkProjectId;
    }

    public boolean isDisplayChart() {
        return reportConfig.isDisplayChart();
    }

    public String getChartWidth() {
        return reportConfig.getChartWidth();
    }

    public String getChartHeight() {
        return reportConfig.getChartHeight();
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        List<KlocworkProjectAction> projectActions = new ArrayList<>();
        projectActions.add(new KlocworkProjectAction(run.getParent()));
        return projectActions;
    }
}
