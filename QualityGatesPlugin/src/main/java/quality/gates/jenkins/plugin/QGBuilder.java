package quality.gates.jenkins.plugin;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

public class QGBuilder extends Builder {

    private JobConfigData jobConfigData;
    private BuildDecision buildDecision;
    private JobConfigurationService jobConfigurationService;
    private JobExecutionService jobExecutionService;
    private QGBuilderDescriptor builderDescriptor;
    private GlobalConfigDataForSonarInstance globalConfigDataForSonarInstance;

    @DataBoundConstructor
    public QGBuilder(JobConfigData jobConfigData) {
        this.jobConfigData = jobConfigData;
        this.jobExecutionService = new JobExecutionService();
        this.buildDecision = new BuildDecision();
        this.jobConfigurationService = new JobConfigurationService();
        this.builderDescriptor = jobExecutionService.getBuilderDescriptor();
        this.globalConfigDataForSonarInstance = null;
    }

    protected QGBuilder(JobConfigData jobConfigData, BuildDecision buildDecision, JobConfigurationService jobConfigurationService, QGBuilderDescriptor builderDescriptor, GlobalConfigDataForSonarInstance globalConfigDataForSonarInstance) {
        this.jobConfigData = jobConfigData;
        this.buildDecision = buildDecision;
        this.jobConfigurationService = jobConfigurationService;
        this.builderDescriptor = builderDescriptor;
        this.globalConfigDataForSonarInstance = globalConfigDataForSonarInstance;
    }

    public JobConfigData getJobConfigData() {
        return jobConfigData;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        builderDescriptor = jobExecutionService.getBuilderDescriptor();
        GlobalConfig globalConfig = builderDescriptor.getGlobalConfig();
        globalConfigDataForSonarInstance = buildDecision.chooseSonarInstance(globalConfig, jobConfigData.getSonarInstanceName());

        if(globalConfigDataForSonarInstance == null) {
            listener.error(JobExecutionService.GLOBAL_CONFIG_NO_LONGER_EXISTS_ERROR, jobConfigData.getSonarInstanceName());
            return false;
        }
        return true;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace(listener.getLogger());
        }
        boolean buildHasPassed;
        try {
            JobConfigData checkedJobConfigData = jobConfigurationService.checkProjectKeyIfVariable(jobConfigData, build, listener);
            buildHasPassed = buildDecision.getStatus(globalConfigDataForSonarInstance, checkedJobConfigData);
            if("".equals(jobConfigData.getSonarInstanceName()))
                listener.getLogger().println(JobExecutionService.DEFAULT_CONFIGURATION_WARNING);
            listener.getLogger().println("Build-Step: Quality Gates plugin build passed: " + String.valueOf(buildHasPassed).toUpperCase());
            return buildHasPassed;
        }
        catch (QGException e){
            e.printStackTrace(listener.getLogger());
        }
        return false;
    }

    @Override
    public QGBuilderDescriptor getDescriptor() {
        return (QGBuilderDescriptor) super.getDescriptor();
    }


}
