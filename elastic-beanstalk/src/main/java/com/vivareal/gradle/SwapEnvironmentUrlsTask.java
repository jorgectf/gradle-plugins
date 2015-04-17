package com.vivareal.gradle;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.SwapEnvironmentCNAMEsRequest;

import java.util.List;

public class SwapEnvironmentUrlsTask {
    private final AWSElasticBeanstalk awsElasticBeanstalk;
    private final String applicationName;
    private final String targetUrl;
    private final String newEnvironment;

    public SwapEnvironmentUrlsTask(AWSElasticBeanstalk awsElasticBeanstalk, String applicationName, String targetUrl,
            String newEnvironment) {
        this.awsElasticBeanstalk = awsElasticBeanstalk;
        this.applicationName = applicationName;
        this.targetUrl = targetUrl;
        this.newEnvironment = newEnvironment;
    }

    public void execute() {
        DescribeEnvironmentsRequest request = new DescribeEnvironmentsRequest();
        request.setApplicationName(applicationName);
        DescribeEnvironmentsResult describedEnvs = awsElasticBeanstalk.describeEnvironments(request);
        List<EnvironmentDescription> envs = describedEnvs.getEnvironments();

        EnvironmentDescription current = findTargetEnvironment(envs);
        EnvironmentDescription newEnv = findSourceEnvironment(envs);

        System.out.println("Current environment: " + current.getCNAME() + " " + current.getVersionLabel());
        System.out.println("Source environment: " + newEnv.getCNAME() + " " + newEnv.getVersionLabel());

        String newEnvCNAME = newEnv.getCNAME();

        if (newEnvCNAME.equals(current.getCNAME())) {
            throw new IllegalStateException("Source environment is already using target  url " + targetUrl);
        }

        SwapEnvironmentCNAMEsRequest swapRequest = new SwapEnvironmentCNAMEsRequest();
        swapRequest.setSourceEnvironmentId(current.getEnvironmentId());
        swapRequest.setDestinationEnvironmentId(newEnv.getEnvironmentId());
        awsElasticBeanstalk.swapEnvironmentCNAMEs(swapRequest);
    }

    private EnvironmentDescription findSourceEnvironment(List<EnvironmentDescription> envs) {
        for (EnvironmentDescription env : envs) {
            if (env.getEnvironmentName().equals(newEnvironment)) {
                return env;
            }
        }
        throw new IllegalArgumentException("Couldn't find an environment with " + newEnvironment + " name.");
    }

    private EnvironmentDescription findTargetEnvironment(List<EnvironmentDescription> environments) {
        for (EnvironmentDescription env : environments) {
            if (env.getCNAME().equals(targetUrl)) {
                return env;
            }
        }
        throw new IllegalArgumentException("Couldn't find an environment with " + targetUrl + " cname.");
    }

}
