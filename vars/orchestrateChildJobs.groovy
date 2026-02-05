import jenkins.model.*
import hudson.model.*
import org.company.jenkins.DependencyPolicy

/**
 * Enterprise orchestration entry point.
 *
 * Responsibilities:
 * - Discover parameters from child jobs
 * - Render correct input controls (choices, booleans, strings)
 * - Enforce per-job dependency policies
 * - Apply timeouts to human input and job execution
 * - Execute jobs sequentially with explicit failure handling
 */
def call(List<Map> jobs) {

    /*
     * Default timeouts (enterprise best practice)
     * Can be overridden per job via jobConfig
     */
    final int DEFAULT_INPUT_TIMEOUT_MIN = 30
    final int DEFAULT_BUILD_TIMEOUT_MIN = 90

    jobs.each { jobConfig ->

        stage(jobConfig.stage) {

            String jobName = jobConfig.name

            int inputTimeout =
                jobConfig.get('inputTimeoutMinutes', DEFAULT_INPUT_TIMEOUT_MIN)
            int buildTimeout =
                jobConfig.get('buildTimeoutMinutes', DEFAULT_BUILD_TIMEOUT_MIN)

            /*
             * Resolve child job
             */
            def job = Jenkins.instance.getItemByFullName(jobName)
            if (!job) {
                error "Child job not found: ${jobName}"
            }

            def prop = job.getProperty(ParametersDefinitionProperty)
            if (!prop || !prop.parameterDefinitions) {
                error "No parameters defined for job: ${jobName}"
            }

            /*
             * Build input parameters dynamically from child job
             */
            def inputParams = []

            prop.parameterDefinitions.each { p ->

                if (p instanceof StringParameterDefinition) {
                    inputParams << string(
                        name: p.name,
                        defaultValue: p.defaultValue ?: '',
                        description: p.description ?: ''
                    )
                }
                else if (p instanceof ChoiceParameterDefinition) {
                    inputParams << choice(
                        name: p.name,
                        choices: p.choices.join('\n'),
                        description: p.description ?: ''
                    )
                }
                else if (p instanceof BooleanParameterDefinition) {
                    inputParams << booleanParam(
                        name: p.name,
                        defaultValue: p.defaultValue,
                        description: p.description ?: ''
                    )
                }
                else {
                    error "Unsupported parameter type: ${p.class.name}"
                }
            }

            /*
             * Human input with timeout
             */
            Map userInput
            try {
                timeout(time: inputTimeout, unit: 'MINUTES') {
                    userInput = input(
                        id: "input-${jobName}",
                        message: "Review and confirm parameters for ${jobName}",
                        parameters: inputParams
                    )
                }
            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                error "Input timed out after ${inputTimeout} minutes for job ${jobName}"
            }

            /*
             * Enforce job-specific dependency policy
             */
            try {
                DependencyPolicy.validate(jobName, userInput)
            } catch (IllegalArgumentException e) {
                error e.message
            }

            /*
             * Normalize parameters for downstream build
             */
            def buildParams = userInput.collect { k, v ->
                string(name: k, value: v.toString())
            }

            /*
             * Downstream build with timeout
             */
            try {
                timeout(time: buildTimeout, unit: 'MINUTES') {
                    build job: jobName,
                          wait: true,
                          parameters: buildParams
                }
            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                error "Execution of ${jobName} exceeded ${buildTimeout} minutes and was aborted"
            }
        }
    }
}
